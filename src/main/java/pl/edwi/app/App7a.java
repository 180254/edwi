package pl.edwi.app;

import com.panforge.robotstxt.RobotsTxt;
import io.mola.galimatias.ErrorHandler;
import io.mola.galimatias.StrictErrorHandler;
import io.mola.galimatias.URL;
import io.mola.galimatias.URLParsingSettings;
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
import pl.edwi.tool.Try;
import pl.edwi.tool.WebCache;
import pl.edwi.tool.WebDownloader;
import pl.edwi.tool.WebPage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@SuppressWarnings("Duplicates")
public class App7a {

    public static final int DL_LIMIT = 1_000;
    public static final String LUCENE_DIR = "lucene7/";

    public static final Pattern SPACE_PATTEN = Pattern.compile(" ");
    public static final Pattern END_CHARS_PATTERN = Pattern.compile("[#/]+$");

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public final int threads = Runtime.getRuntime().availableProcessors() * 3;
    public final ExecutorService executor = Executors.newFixedThreadPool(threads);
    public final WebDownloader webDownloader = new WebDownloader(executor);
    public final WebCache webCache = new WebCache();

    public final ErrorHandler errorHandler = StrictErrorHandler.getInstance();
    public final URLParsingSettings urlParsingSettings = URLParsingSettings.create().withErrorHandler(errorHandler);

    public final AtomicInteger indexedCnt = new AtomicInteger();
    public final Set<String> indexedSet = ConcurrentHashMap.newKeySet();
    public final Set<String> scheduledSet = ConcurrentHashMap.newKeySet();

    public final CountDownLatch theEnd = new CountDownLatch(1);

    public App7a() throws IOException {

    }

    // ---------------------------------------------------------------------------------------------------------------

    public static void main(String[] args) throws IOException, InterruptedException {
        try (Analyzer indexAnalyzer = new StandardAnalyzer();
             Directory indexDirectory = FSDirectory.open(Paths.get(LUCENE_DIR));
             IndexWriter indexWriter = new IndexWriter(indexDirectory, new IndexWriterConfig(indexAnalyzer))) {

            App7a app7a = new App7a();
            app7a.logger.info("start.");

            Stream.of(
                    "https://www.p.lodz.pl/CHANGELOG.txt",
                    "http://pduch.kis.p.lodz.pl",
                    "https://pl.wikipedia.org/wiki/Komputer",
                    "http://www.ekologia.pl/wiedza/zwierzeta/ssaki",
                    "http://agencjafilharmonia.pl/jan-sebastian-bach/"
            )
                    .map((p) -> {
                        app7a.scheduledSet.add(p);
                        return p;
                    })
                    .forEach((p) -> app7a.executor.execute(() -> app7a.process(indexWriter, p)));

            app7a.theEnd.await();
            app7a.executor.shutdown();
            app7a.executor.awaitTermination(5, TimeUnit.MINUTES);

            app7a.logger.info("done.");
            app7a.logger.info("indexedCnt={}", app7a.indexedCnt.get());
            app7a.logger.info("indexedSet={}", app7a.indexedSet.size());
            app7a.logger.info("scheduledSet.size={}", app7a.scheduledSet.size());
            app7a.logger.info("scheduler.completed={}", ((ThreadPoolExecutor) app7a.executor).getCompletedTaskCount());

            indexWriter.commit();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void process(IndexWriter indexWriter, String url) {
        try {
            if (indexedCnt.get() >= DL_LIMIT) {
                return;
            }

            if (!isRobotWelcome(url)) {
//                logger.debug("robots.no.access: {}", url);
                return;
            }

            WebPage webPage;
            Optional<WebPage> cachedPage = webCache.getPage(url);
            if (cachedPage.isPresent()) {
                webPage = cachedPage.get();
            } else {
                webPage = webDownloader.downloadPage(url);
                webCache.savePage(webPage);
            }

            String title = webPage.document().title();
            String text = webPage.cleanText();

            Document doc = new Document();
            doc.add(new StringField("url", url, Field.Store.YES));
            doc.add(new TextField("title", title, Field.Store.YES));
            doc.add(new TextField("text", text, Field.Store.YES));
            indexWriter.addDocument(doc);
            indexedSet.add(url);

            int indCnt = indexedCnt.incrementAndGet();
            if (indCnt % 200 == 0) {
                logger.debug("counter={}", indCnt);
            }
            if (indCnt >= DL_LIMIT) {
                if (indCnt == DL_LIMIT) {
                    theEnd.countDown();
                }
                return;
            }

            webPage.document().select("a[href]").stream()
                    .map(p -> p.attr("abs:href"))
                    .map(p -> SPACE_PATTEN.matcher(p).replaceAll("%20"))
                    .filter(p -> !p.isEmpty())
                    .map(p -> Try.ex(() -> URL.parse(urlParsingSettings, p).withFragment("")))
                    .filter(Objects::nonNull)
                    .filter(p -> p.host() != null)
                    .filter(p -> p.scheme().equals("http") || p.scheme().equals("https"))
                    .map(p -> END_CHARS_PATTERN.matcher(p.toString()).replaceAll(""))
                    .filter(p -> indexedCnt.get() < DL_LIMIT)
                    .filter(e -> scheduledSet.size() < DL_LIMIT * 3 && scheduledSet.add(e))
                    .forEach(p -> {
                        try {
                            if (!executor.isShutdown()) {
                                executor.execute(() -> process(indexWriter, p));
                            }
                        } catch (RejectedExecutionException ignored) {
                        }
                    });

        } catch (Exception e) {
//            logger.debug("fail: {}, {}", url, e.toString());
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

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
