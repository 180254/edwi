package pl.edwi;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
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
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edwi.gut.Book;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("Duplicates")
public class App6 {

    private static final String GUTENBERG_DIR = "C:\\Users\\Adrian\\Desktop\\pgdvd042010\\";
    private static final String UNZIP_DIR = "C:\\Users\\Adrian\\Desktop\\pgdvd042010\\uz\\";
    private static final String LUCENE_DIR = "C:\\Users\\Adrian\\Desktop\\lucene\\";

    private static final Pattern FILE_TXT_FORMAT = Pattern.compile(
            "([0-9]+)(?:[_\\-][0-9])?\\.txt", Pattern.CASE_INSENSITIVE // 00000.txt / 00000-9.txt
    );
    private static final Pattern CARRIAGE_RETURN = Pattern.compile("\r", Pattern.LITERAL);
    private static final Pattern WHITESPACES = Pattern.compile("\\s+");

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final StandardAnalyzer analyzer = new StandardAnalyzer();
    private final Directory index = FSDirectory.open(Paths.get(LUCENE_DIR));

    private final Set<String> parsedBooks = ConcurrentHashMap.newKeySet();
    private final AtomicInteger counterZips = new AtomicInteger();
    private final AtomicInteger counterBooks = new AtomicInteger();
    private final AtomicInteger counterUnknown = new AtomicInteger();

    public App6() throws IOException {
    }

    // ---------------------------------------------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        App6 app6 = new App6();
        app6.logger.info("start");

//        app6.unzipAllZips();
        app6.logger.debug("unzip.done");
        app6.logger.info("unzip.counter: {}", app6.counterZips.get());

//        app6.parseAllBooks();
        app6.logger.debug("parse.done");
        app6.logger.info("parse.total.counter: {}", app6.counterBooks.get());
        app6.logger.info("parse.unknown.counter: {}", app6.counterUnknown.get());

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String searching = scanner.nextLine();
            if (searching.equals("q")) {
                break;
            } else {
                try {
                    app6.searchTask(searching.trim());
                } catch (ParseException e) {
                    System.out.println("Query cannot be parsed: {}" + e.toString());
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void unzipAllZips() throws IOException {

        Files.find(
                Paths.get(GUTENBERG_DIR),
                Integer.MAX_VALUE,
                (path, attr) -> attr.isRegularFile()
        )
                .parallel()
                .filter(path -> path.toString().toLowerCase().endsWith(".zip"))
                .filter(path -> !path.toString().contains("ETEXT"))
                .forEach(path -> {
                    try {
                        ZipFile zipFile = new ZipFile(path.toString());
                        zipFile.extractAll(UNZIP_DIR);
                    } catch (ZipException e) {
                        logger.warn("unzip.error {} {}", path, e.toString());
                    }

                    int cnt = counterZips.incrementAndGet();
                    if (cnt % 100 == 0) {
                        logger.debug("unzip.counter: {}", cnt);
                    }
                });
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void parseAllBooks() throws IOException {

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        try (IndexWriter indexWriter = new IndexWriter(index, config)) {

            Files.find(
                    Paths.get(GUTENBERG_DIR),
                    Integer.MAX_VALUE,
                    (path, attr) -> attr.isRegularFile()
            )
                    .parallel()
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        Matcher matcher = FILE_TXT_FORMAT.matcher(fileName);
                        return matcher.matches() && parsedBooks.add(matcher.group(1));
                    })
                    .forEach(path -> {
                        try {
                            String fileName = path.getFileName().toString();
                            Charset charset = StandardCharsets.UTF_8;

                            byte[] bytes = Files.readAllBytes(path);
                            String string = new String(bytes, charset);
                            Book book = parseBook(fileName, string);

                            Document doc = new Document();
                            doc.add(new StringField("id", book.getId(), Field.Store.YES));
                            doc.add(new TextField("title", book.getTitle(), Field.Store.YES));
                            doc.add(new TextField("content", book.getContent(), Field.Store.YES));
                            indexWriter.addDocument(doc);

                        } catch (IOException e) {
                            logger.error("parse.error {}, {}", path, e.toString());
                        }

                        int cnt = counterBooks.incrementAndGet();
                        if (cnt % 100 == 0) {
                            logger.debug("parse.counter: {}", cnt);
                        }
                    });
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private Book parseBook(String filename, String raw) {
        String[] text = CARRIAGE_RETURN.matcher(raw).replaceAll("").split("\n");

        int titleIndex = 0;
        boolean titleProcess = false;
        StringBuilder titleBuilder = new StringBuilder(50);
        for (int i = 0; i < text.length; i++) {
            if (text[i].startsWith("Title:")) {
                titleBuilder.append(text[i].substring(6));
                titleIndex = i;
                titleProcess = true;

            } else if (titleProcess) {
                if (!text[i].isEmpty()) {
                    titleBuilder.append(text[i]);
                } else {
                    break;
                }
            }
        }

        int contentIndex = 0;
        for (int i = titleIndex; i < text.length; i++) {
            if (text[i].startsWith("*** START OF THIS PROJECT GUTENBERG")) {
                contentIndex = i + 1;
                break;
            }
        }

        String title = WHITESPACES.matcher(titleBuilder).replaceAll(" ").trim();
        String content = String.join("\n", Arrays.asList(text).subList(contentIndex, text.length)).trim();

        if (title.isEmpty()) {
            counterUnknown.incrementAndGet();
        }

        return new Book(filename, title, content);
    }


    private void searchTask(String find) throws ParseException, IOException {
        Query query = new QueryParser("title", analyzer).parse(find);

        try (IndexReader reader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs docs = searcher.search(query, 10);
            ScoreDoc[] hits = docs.scoreDocs;

            System.out.println("Found " + docs.totalHits + " hits.");
            for (int i = 0; i < hits.length; ++i) {
                Document d = searcher.doc(hits[i].doc);
                System.out.printf("%2d. [%2.4f] %-13s %s%n", i + 1, hits[i].score, d.get("id"), d.get("title"));
            }
        }
    }
}
