package pl.edwi.app;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edwi.tool.Book;
import pl.edwi.tool.WebDownloader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class App5 {
    private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\r?\n");

    public static final String CACHE_DIR = "gutenberg";
    public static final String CATALOG_URL = "http://www.gutenberg.org/cache/epub/feeds/catalog.marc.bz2";
    public static final String CATALOG_FILE_BZ2 = CACHE_DIR + "/catalog.marc.bz2";
    public static final String CATALOG_FILE_MARC = CACHE_DIR + "/catalog.marc";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final WebDownloader webDownloader = new WebDownloader();

    private final List<Book> books = new ArrayList<>(60000);
    private final StandardAnalyzer analyzer = new StandardAnalyzer();
    private final Directory index = new RAMDirectory();

    public static void main(String[] args) throws IOException {
        App5 app5 = new App5();

        if (!app5.pathExists(CACHE_DIR)) {
            app5.createDir(CACHE_DIR);
        }
        app5.logger.debug("CACHE_DIR");

        if (!app5.pathExists(CATALOG_FILE_BZ2)) {
            app5.downloadCatalog();
        }
        app5.logger.debug("CATALOG_FILE_BZ2");

        if (!app5.pathExists(CATALOG_FILE_MARC)) {
            app5.unpackCatalog();
        }
        app5.logger.debug("CATALOG_FILE_MARC");

        app5.parseCatalog();
        app5.logger.debug("PARSE_CATALOG");

        app5.createLuceneIndex();
        app5.logger.debug("CREATE_LUCENE_INDEX");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String searching = scanner.nextLine();
            if (searching.equals("q")) {
                break;
            } else {
                try {
                    app5.searchTask(searching.trim());
                } catch (ParseException e) {
                    app5.logger.warn("Query cannot be parsed: " + e.toString());
                }
            }
        }
    }

    private boolean pathExists(String path) {
        return Paths.get(path).toFile().exists();
    }

    private void createDir(String path) throws IOException {
        Files.createDirectories(Paths.get(path));
    }

    private void downloadCatalog() throws IOException {
        byte[] catalog = webDownloader.downloadBytes(CATALOG_URL);

        Files.write(
                Paths.get(CATALOG_FILE_BZ2),
                catalog,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private void unpackCatalog() throws IOException {
        try (
                FileInputStream in = new FileInputStream(CATALOG_FILE_BZ2);
                BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(in);
                FileOutputStream out = new FileOutputStream(CATALOG_FILE_MARC)
        ) {
            final byte[] buffer = new byte[5_000_000];

            int n;
            while (-1 != (n = bzIn.read(buffer))) {
                out.write(buffer, 0, n);
            }
        }
    }

    private void parseCatalog() throws FileNotFoundException {
        FileInputStream fileStream = new FileInputStream(CATALOG_FILE_MARC);
        MarcReader mr = new MarcStreamReader(fileStream);

        while (mr.hasNext()) {
            Record next = mr.next();

            ControlField idField = (ControlField) next.getVariableField("001");
            String id = idField.getData();

            DataField titleField = (DataField) next.getVariableField("245");
            titleField.removeSubfield(titleField.getSubfield('h'));
            String title = titleField.getSubfields().stream()
                    .map(Subfield::getData)
                    .collect(Collectors.joining("; "));
            String titleFix = NEW_LINE_PATTERN.matcher(title).replaceAll(" ").trim();

            books.add(new Book(id, titleFix));
        }
    }

    private void createLuceneIndex() throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        try (IndexWriter w = new IndexWriter(index, config)) {
            for (Book book : books) {
                Document doc = new Document();
                doc.add(new StringField("id", book.getId(), Field.Store.YES));
                doc.add(new TextField("title", book.getTitle(), Field.Store.YES));
                w.addDocument(doc);
            }
        }
    }

    private void searchTask(String find) throws ParseException, IOException {
        Query query = new QueryParser("content", analyzer).parse(find);

        try (IndexReader reader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs docs = searcher.search(query, 10);
            ScoreDoc[] hits = docs.scoreDocs;

            System.out.println("Found " + docs.totalHits + " hits.");
            for (int i = 0; i < hits.length; ++i) {
                Document d = searcher.doc(hits[i].doc);
                System.out.printf("%2d. [%2.4f] %-10s %s%n", i + 1, hits[i].score, d.get("id"), d.get("title"));
            }
        }
    }
}
