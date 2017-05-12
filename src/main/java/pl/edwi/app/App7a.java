package pl.edwi.app;

import com.panforge.robotstxt.RobotsTxt;
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
import pl.edwi.web.WebCache;
import pl.edwi.web.WebDownloader;
import pl.edwi.web.WebPage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@SuppressWarnings("Duplicates")
public class App7a {

    public static final String LUCENE_DIR = "lucene7/";
    public static final int DL_LIMIT = 25_000;
    public static final int THREAD_MULTIPLIER = 10;
    public static final int EXECUTOR_AWAIT_TERMINATION_MIN = 60;

    public static final Pattern SPACE_CHARACTER = Pattern.compile(" ", Pattern.LITERAL);
    public static final Pattern NOT_ALNUM_CHARACTER = Pattern.compile("[^\\p{L}0-9]", Pattern.UNICODE_CHARACTER_CLASS);
    public static final Pattern REDUNDANT_END_OF_URL = Pattern.compile("/?(?:#[a-zA-Z0-9_-]+)?/?$");

    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    public final int threads = Runtime.getRuntime().availableProcessors() * THREAD_MULTIPLIER;
    public final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
    public final WebDownloader webDownloader = new WebDownloader(executor);
    public final WebCache webCache = new WebCache();

    public final AtomicInteger scheduledCounter = new AtomicInteger();
    public final AtomicInteger indexedCounter = new AtomicInteger();
    public final Set<String> scheduledSet = ConcurrentHashMap.newKeySet();
    public final Set<String> indexedSet = ConcurrentHashMap.newKeySet();
    public final CountDownLatch theEnd = new CountDownLatch(1);

    public App7a() throws IOException {

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new App7a().go();
    }

    public void go() throws IOException, InterruptedException {
        try (Analyzer analyzer = new StandardAnalyzer();
             Directory directory = FSDirectory.open(Paths.get(LUCENE_DIR));
             IndexWriter iWriter = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {

            logger.info("start");

            Stream.of(
                    "https://www.p.lodz.pl/CHANGELOG.txt",
                    "http://pduch.kis.p.lodz.pl",
                    "https://pl.wikipedia.org/wiki/Komputer",
                    "http://www.ekologia.pl/wiedza/zwierzeta/ssaki",
                    "http://agencjafilharmonia.pl/jan-sebastian-bach"
            )
                    .forEach((p) -> {
                        scheduledSet.add(p);
                        executor.execute(() -> process(iWriter, p));
                    });

            theEnd.await();
            logger.info("the end");

            executor.shutdown();
            boolean awaitTermination = executor.awaitTermination(EXECUTOR_AWAIT_TERMINATION_MIN, TimeUnit.MINUTES);
            logger.info("awaitTermination={}", awaitTermination);

            logger.info("scheduledCnt={}", scheduledCounter.get());
            logger.info("scheduledSet.size={}", scheduledSet.size());
            logger.info("scheduler.completed={}", executor.getCompletedTaskCount());
            logger.info("scheduler.active={}", executor.getActiveCount());
            logger.info("indexedCnt={}", indexedCounter.get());
            logger.info("indexedSet={}", indexedSet.size());

            iWriter.commit();
            Files.write(Paths.get(LUCENE_DIR + "/x-scheduled.txt"), scheduledSet);
            Files.write(Paths.get(LUCENE_DIR + "/x-indexed.txt"), indexedSet);
        }
    }

    public void process(IndexWriter indexWriter, String url) {
        try {
            if (!isRobotWelcome(url)) {
                logger.trace("robots.no.access: {}", url);
                return;
            }

            WebPage webPage = webDownloader.downloadPage(url);
            /*
            WebPage webPage = webCache.getPage(url).orElse(null);
            if (webPage == null) {
                webPage = webDownloader.downloadPage(url);
                webCache.savePage(webPage);
            }
            */

            String url_0 = URLDecoder.decode(url, "UTF-8");
            String url_1 = NOT_ALNUM_CHARACTER.matcher(url_0).replaceAll(" ");
            // String title = webPage.document().title();
            String text = webPage.cleanText();

            Document doc = new Document();
            doc.add(new StringField("url_0", url_0, Field.Store.YES));
            doc.add(new TextField("url_1", url_1, Field.Store.YES));
            // doc.add(new TextField("title", title, Field.Store.YES));
            doc.add(new TextField("text", text, Field.Store.YES));
            indexWriter.addDocument(doc);

            indexedSet.add(url);

            int indexedCnt = indexedCounter.incrementAndGet();
            if (indexedCnt % 200 == 0) {
                logger.debug("counter={}", indexedCnt);
            }

            if (scheduledCounter.get() >= DL_LIMIT) {
                return;
            }

            webPage.document().select("a[href]").stream()
                    .map(p -> p.attr("abs:href"))
                    .filter(p -> !p.isEmpty())
                    .filter(p -> p.startsWith("http"))
                    .map(p -> SPACE_CHARACTER.matcher(p).replaceAll("%20"))
                    .map(p -> REDUNDANT_END_OF_URL.matcher(p).replaceFirst(""))
                    .filter(scheduledSet::add)
                    .forEach(p -> {
                        try {
                            if (!executor.isShutdown()) {
                                executor.execute(() -> process(indexWriter, p));

                                int scheduledCnt = scheduledCounter.incrementAndGet();
                                if (scheduledCnt == DL_LIMIT) {
                                    theEnd.countDown();
                                }
                            }
                        } catch (RejectedExecutionException ignored) {
                        }
                    });

        } catch (Exception e) {
            logger.trace("fail: {}, {}", url, e.toString());
        }
    }

    private boolean isRobotWelcome(String url) {
        String robotsFile = webCache.getRobots(url, webDownloader);

        try (InputStream robotsIs = new ByteArrayInputStream(robotsFile.getBytes())) {
            RobotsTxt robotsTxt = RobotsTxt.read(robotsIs);
            return robotsTxt.query(WebDownloader.USER_AGENT, url);

        } catch (IOException e) {
            logger.error("robots.test.fail {} {}", url, e.toString());
            return false;
        }
    }
}
