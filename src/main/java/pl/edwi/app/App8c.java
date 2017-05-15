package pl.edwi.app;

import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edwi.search.DuckDuckGoSearch;
import pl.edwi.search.SearchResult;
import pl.edwi.sentiment.Sentiment;
import pl.edwi.sentiment.SentimentAnalyser;
import pl.edwi.sentiment.TsSentimentAnalyser;
import pl.edwi.web.WebDownloader;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class App8c {

    public static final int SEARCH_LIMIT = 50;

    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final WebDownloader webDownloader = new WebDownloader();
    private final DuckDuckGoSearch searcher = new DuckDuckGoSearch(webDownloader);
    private final SentimentAnalyser sentimentAnalyser = new TsSentimentAnalyser(webDownloader);

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

                Map<String, Set<String>> texts = new HashMap<>();
                texts.put("NEGATIVE", new HashSet<>());
                texts.put("NEUTRAL", new HashSet<>());
                texts.put("POSITIVE", new HashSet<>());

                if (searching.equals("q")) {
                    break;
                } else {
                    try {
                        logger.info("state: start");

                        String phrase = "site:linustechtips.com \"" + searching + '"';
                        List<SearchResult> searchResults = searcher.search(phrase, SEARCH_LIMIT);
                        logger.info("state: search");

                        Map<String, Integer> sentiments = new ConcurrentHashMap<>(SEARCH_LIMIT);
                        searchResults
                                .stream()
                                .parallel()
                                .forEach(searchResult -> {
                                    try {
                                        if (!searchResult.context.trim().isEmpty()) {
                                            Sentiment sentiment = sentimentAnalyser.analyze(searchResult.context);
                                            sentiments.compute(sentiment.name(), (key, value) -> MoreObjects.firstNonNull(value, 0) + 1);
                                            texts.get(sentiment.name()).add(searchResult.context);
                                        }
                                    } catch (IOException e) {
                                        //logger.info("exception: []", e.toString());
                                    }
                                });
                        logger.info("state: sentiment");

                        int aa = 0;
                        int sum = sentiments.values().stream().mapToInt(i -> i).sum();
                        printInfo(sentiments, sum, "Negative", "NEGATIVE");
                        for (String txt : texts.get("NEGATIVE")) {
                            logger.info("- " + txt);
                            aa++;
                            if (aa >= 4) break;
                        }

                        aa = 0;
                        printInfo(sentiments, sum, "Neutral", "NEUTRAL");
                        for (String txt : texts.get("NEUTRAL")) {
                            logger.info("- " + txt);
                            aa++;
                            if (aa >= 4) break;
                        }

                        aa = 0;
                        printInfo(sentiments, sum, "Positive", "POSITIVE");
                        for (String txt : texts.get("POSITIVE")) {
                            logger.info("- " + txt);
                            aa++;
                            if (aa >= 4) break;
                        }

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
