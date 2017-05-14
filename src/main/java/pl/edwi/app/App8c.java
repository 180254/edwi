package pl.edwi.app;

import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edwi.search.DuckDuckGoSearch;
import pl.edwi.search.SearchResult;
import pl.edwi.sentiment.Sentiment;
import pl.edwi.sentiment.SentimentAnalyser;
import pl.edwi.sentiment.TpSentimentAnalyser;
import pl.edwi.web.WebDownloader;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class App8c {

    public static final int SEARCH_LIMIT = 100;

    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final WebDownloader webDownloader = new WebDownloader();
    private final DuckDuckGoSearch ddgSearchEngine = new DuckDuckGoSearch(webDownloader);
    private final SentimentAnalyser sentimentAnalyser = new TpSentimentAnalyser(webDownloader);

    public static void main(String[] args) throws IOException {
        new App8c().go();
    }

    public App8c() throws IOException {
    }

    public void go() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                System.out.flush();
                String searching = scanner.nextLine().trim();

                if (searching.equals("q")) {
                    break;
                } else {
                    try {
                        logger.info("state: start");

                        String phrase = "site:linustechtips.com " + searching;
                        List<SearchResult> searchResults = ddgSearchEngine.search(phrase, SEARCH_LIMIT);
                        logger.info("state: search");

                        Map<String, Integer> sentiments = new ConcurrentHashMap<>(SEARCH_LIMIT);
                        searchResults
                                .stream()
                                .parallel()
                                .forEach(searchResult -> {
                                    try {
                                        Sentiment sentiment = sentimentAnalyser.analyze(searchResult.context);
                                        sentiments.compute(sentiment.name(), (key, value) -> MoreObjects.firstNonNull(value, 0) + 1);
                                    } catch (IOException e) {
                                        logger.info("exception: []", e.toString());
                                    }
                                });
                        logger.info("state: sentiment");

                        int sum = sentiments.values().stream().mapToInt(i -> i).sum();
                        printInfo(sentiments, sum, "Negative", "NEGATIVE");
                        printInfo(sentiments, sum, "Neutral", "NEUTRAL");
                        printInfo(sentiments, sum, "Positive", "POSITIVE");

                    } catch (Exception e) {
                        logger.warn("Processing failed {}", e.toString());
                    }
                }
            }
        }
    }

    public void printInfo(Map<String, Integer> map, int sumOfValues, String printName, String mapKey) {
        int value = map.getOrDefault(mapKey, 0);
        String msg = String.format("%s: %d (%.2f%%)", printName, value, (double) value / sumOfValues * 100);
        logger.info(msg);
    }
}
