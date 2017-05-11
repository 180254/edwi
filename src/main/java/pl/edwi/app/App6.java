package pl.edwi.app;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
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
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edwi.tool.Book;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("Duplicates")
public class App6 {

    private static final String GUTENBERG_DIR = "pgdvd042010\\";
    private static final String LUCENE_DIR = "lucene\\";

    private static final Pattern WHITESPACES = Pattern.compile("\\s+");
    private static final Pattern FILE_FORMAT = Pattern.compile(
            "([0-9]+)(?:[_\\-][0-9])?\\.(txt|zip)", Pattern.CASE_INSENSITIVE // 00000.txt / 00000-9.txt
    );

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Set<String> books = ConcurrentHashMap.newKeySet();
    private final AtomicInteger counter = new AtomicInteger();

    public App6() throws IOException {
    }

    public static void main(String[] args) throws IOException {
        try (Analyzer analyzer = new StandardAnalyzer();
             Directory index = FSDirectory.open(Paths.get(LUCENE_DIR))) {

            App6 app6 = new App6();
            app6.logger.info("start");

            app6.processAll(analyzer, index);
            app6.logger.debug("process.done");
            app6.logger.info("process.counter: {}", app6.counter.get());

            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("> ");
                    System.out.flush();
                    String searching = scanner.nextLine();
                    if (searching.equals("q")) {
                        break;
                    } else {
                        try {
                            app6.doSearchTask(analyzer, index, searching.trim());
                        } catch (ParseException | InvalidTokenOffsetsException e) {
                            System.out.println("Query cannot be parsed: {}" + e.toString());
                        }
                    }
                }
            }
        }
    }

    private void processAll(Analyzer analyzer, Directory index) throws IOException {
        try (IndexWriter indexWriter = new IndexWriter(index, new IndexWriterConfig(analyzer))) {

            Files.find(
                    Paths.get(GUTENBERG_DIR),
                    Integer.MAX_VALUE,
                    (path, attr) -> attr.isRegularFile()
            )
                    .parallel()
                    .filter(path -> !path.toString().contains("ETEXT"))
                    .forEach(path -> {

                        String pathLower = path.toString().toLowerCase();

                        if (pathLower.endsWith(".zip")) {
                            processZip(indexWriter, path);
                        } else if (pathLower.endsWith(".txt")) {
                            processTxt(indexWriter, path);
                        }
                    });

            indexWriter.commit();
        }
    }

    private void processZip(IndexWriter indexWriter, Path path) {
        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.isDirectory()) {
                    continue;
                }

                if (!isNewBook(new File(zipEntry.getName()).getName())) {
                    continue;
                }

                try (InputStream is = zipFile.getInputStream(zipEntry)) {
                    Book book = parseBook(path.getFileName().toString(), is);
                    Document doc = bookDocument(book);
                    indexWriter.addDocument(doc);
                    counterPlusPlus();
                }
            }

        } catch (IOException e) {
            logger.error("process.error: {} {}", path, e.toString());
        }
    }

    private void processTxt(IndexWriter indexWriter, Path path) {
        if (!isNewBook(path.getFileName().toString())) {
            return;
        }

        try (InputStream is = Files.newInputStream(path)) {
            Book book = parseBook(path.getFileName().toString(), is);
            Document doc = bookDocument(book);
            indexWriter.addDocument(doc);
            counterPlusPlus();

        } catch (IOException e) {
            logger.error("process.error: {} {}", path, e.toString());
        }
    }

    private void counterPlusPlus() {
        int cnt = counter.incrementAndGet();
        if (cnt % 100 == 0) {
            logger.debug("process.counter: {}", cnt);
        }
    }

    private boolean isNewBook(String fileName) {
        Matcher matcher = FILE_FORMAT.matcher(fileName);
        return matcher.matches() && books.add(matcher.group(1));
    }

    private Document bookDocument(Book book) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("id", book.getId(), Field.Store.YES));
        doc.add(new TextField("title", book.getTitle(), Field.Store.YES));
        doc.add(new TextField("content", book.getContent(), Field.Store.YES));
        return doc;
    }

    private Book parseBook(String filename, InputStream inputStream) throws IOException {
        String raw = readInputStream(filename, inputStream);
        String title = "";
        String content = "";

        int titleBegin = raw.indexOf("Title:") + 6;
        if (titleBegin != -1) {
            int titleEnd = raw.indexOf("\r\n\r\n", titleBegin);
            if (titleEnd == -1) {
                titleEnd = raw.indexOf("\n\n", titleBegin);
            }
            title = raw.substring(titleBegin, titleEnd);
            title = WHITESPACES.matcher(title).replaceAll(" ").trim();


            int startBegin = raw.indexOf("***", titleEnd);
            if (startBegin != -1) {
                int startEnd = raw.indexOf("\r\n", startBegin);
                if (startEnd == -1) {
                    startEnd = raw.indexOf('\n', titleBegin);
                }

                content = raw.substring(startEnd);
                content = WHITESPACES.matcher(content).replaceAll(" ").trim();
            }
        }

        return new Book(filename, title, content);
    }

    private String readInputStream(String filename, InputStream inputStream) throws IOException {
        Charset charset;
        if (filename.contains("-0") || filename.contains("_0")) {
            charset = StandardCharsets.UTF_8;
        } else if (filename.contains("-8") || filename.contains("_8")) {
            charset = StandardCharsets.ISO_8859_1;
        } else {
            charset = StandardCharsets.US_ASCII;
        }

        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024 * 8];

            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            return result.toString(charset.name());
        }
    }

    private void doSearchTask(Analyzer analyzer, Directory index, String find)
            throws ParseException, IOException, InvalidTokenOffsetsException {

        try (IndexReader reader = DirectoryReader.open(index)) {
            Query query = new QueryParser("content", analyzer).parse(find);

            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs docs = searcher.search(query, 10);
            ScoreDoc[] hits = docs.scoreDocs;

            Formatter formatter = new SimpleHTMLFormatter();
            Highlighter highlighter = new Highlighter(formatter, new QueryScorer(query));

            System.out.println("Found " + docs.totalHits + " hits.");
            for (int i = 0; i < hits.length; ++i) {
                int docId = hits[i].doc;
                Document doc = searcher.doc(docId);

                String content = doc.get("content");
                TokenStream tokenStream = TokenSources.getTokenStream("content", reader.getTermVectors(docId), content, analyzer, -1);
                TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, content, false, 1);

                System.out.printf("%2d. [%2.4f] %-13s %s%n", i + 1, hits[i].score, doc.get("id"), doc.get("title"));
                System.out.printf("%11s %s%n", "", frag[0].toString());
            }
        }
    }
}
