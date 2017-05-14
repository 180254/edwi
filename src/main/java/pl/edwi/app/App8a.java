package pl.edwi.app;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edwi.forum.AvParser;
import pl.edwi.forum.ForumParser;
import pl.edwi.sentiment.Sentiment;
import pl.edwi.sentiment.SentimentAnalyser;
import pl.edwi.sentiment.TsSentimentAnalyser;
import pl.edwi.web.WebDownloader;
import pl.edwi.web.WebPage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class App8a {

    public static final String LUCENE_DIR = "lucene8/";
    public static final int DL_LIMIT = 10_000_000;
    public static final int THREAD_MULTIPLIER = 15;
    public static final int EXECUTOR_AWAIT_TERMINATION_MIN = 60 * 8;

    public static final Pattern SPACE_CHARACTER = Pattern.compile(" ", Pattern.LITERAL);
    public static final Pattern REDUNDANT_END_OF_URL = Pattern.compile("/?(?:#[a-zA-Z0-9_-]+)?/?$");

    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    public final int threads = Runtime.getRuntime().availableProcessors() * THREAD_MULTIPLIER;
    public final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);

    public final WebDownloader webDownloader = new WebDownloader(executor);
    public final ForumParser forumParser = new AvParser();
    public final SentimentAnalyser sentimentAnalyser = new TsSentimentAnalyser(webDownloader);

    public final AtomicInteger siteCounter = new AtomicInteger();
    public final AtomicInteger threadCounter = new AtomicInteger();
    public final AtomicInteger paragraphCounter = new AtomicInteger();
    public final Set<String> scheduledSet = ConcurrentHashMap.newKeySet();

    public App8a() throws IOException {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new App8a().go();
    }

    public void go() throws IOException, InterruptedException {
        logger.info("start");

        try (Analyzer analyzer = new StandardAnalyzer();
             Directory directory = FSDirectory.open(Paths.get(LUCENE_DIR));
             IndexWriter iWriter = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {

            forumParser.startUrls().forEach((startUrl) -> {
                scheduledSet.add(startUrl);
                executor.execute(() -> process(iWriter, startUrl));
            });

            boolean awaitTermination = executor.awaitTermination(EXECUTOR_AWAIT_TERMINATION_MIN, TimeUnit.MINUTES);
            logger.info("awaitTermination={}", awaitTermination);

            logger.info("siteCounter={}", siteCounter.get());
            logger.info("threadCounter={}", threadCounter.get());
            logger.info("paragraphCounter={}", paragraphCounter.get());

            Files.write(Paths.get(LUCENE_DIR + "/x-scheduled.txt"), scheduledSet);
        }

        logger.info("finish");
        System.exit(0);
    }

    public void process(IndexWriter indexWriter, String url) {
        WebPage webPage;

        try {
            if (scheduledSet.size() >= DL_LIMIT) {
                executor.shutdown();
            }

            webPage = webDownloader.downloadPage(url);

            if (forumParser.isThatUrlThread(url)) {
                List<String> paragraphs = forumParser.getAllParagraphs(webPage.document());
                String pageTitleLc = webPage.document().title().toLowerCase();

                for (String paragraph : paragraphs) {
                    try {
                        Sentiment sentiment = sentimentAnalyser.analyze(paragraph);
                        String paragraphLc = paragraph.toLowerCase();
                        String text = "[ " + pageTitleLc + " ]" + paragraphLc;

                        Document doc = new Document();
                        doc.add(new StringField("url", url, Field.Store.YES));
                        doc.add(new TextField("par", text, Field.Store.YES));
                        doc.add(new TextField("sen", sentiment.name(), Field.Store.YES));
                        indexWriter.addDocument(doc);

                        incrementAndDebug(paragraphCounter, "PA");

                    } catch (IOException e) {
                        logger.info("process.exception.b: {} {}", url, e.toString());

                    }
                }

                incrementAndDebug(threadCounter, "TH");
            }

            webPage.document()
                    .select("a[href]").stream()
                    .map(p -> p.attr("abs:href"))
                    .filter(p -> !p.isEmpty())
                    .filter(p -> p.startsWith("http"))
                    .filter(forumParser::isThatUrlForum)
                    .map(p -> SPACE_CHARACTER.matcher(p).replaceAll("%20"))
                    .map(p -> REDUNDANT_END_OF_URL.matcher(p).replaceFirst(""))
                    .filter(p -> scheduledSet.size() < DL_LIMIT)
                    .filter(scheduledSet::add)
                    .forEach((u) -> executor.execute(() -> process(indexWriter, u)));

            incrementAndDebug(siteCounter, "SI");

        } catch (IOException e) {
            logger.info("process.exception.a: {} {}", url, e.toString());
        }
    }

    public void incrementAndDebug(AtomicInteger counter, String name) {
        int cnt = counter.incrementAndGet();
        if (cnt % 200 == 0) {
            logger.debug("i.{}.counter={}", name, cnt);
        }
    }
}
