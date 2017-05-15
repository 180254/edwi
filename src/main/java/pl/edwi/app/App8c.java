package pl.edwi.app;

import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edwi.search.DuckDuckGoSearch;
import pl.edwi.search.SearchEngine;
import pl.edwi.search.SearchResult;
import pl.edwi.sentiment.Sentiment;
import pl.edwi.sentiment.SentimentAnalyser;
import pl.edwi.sentiment.TpSentimentAnalyser;
import pl.edwi.web.WebDownloader;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class App8c {

    public static final int SEARCH_LIMIT = 50;
    public static final String SEARCH_FORUM = "linustechtips.com";
    public static final int EXAMPLES_LIMIT = 4;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final WebDownloader webDownloader = new WebDownloader();
    private final SearchEngine searcher = new DuckDuckGoSearch(webDownloader);
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

                Map<String, Set<String>> texts = Maps.fixedSize.of(
                        "NEGATIVE", Sets.mutable.<String>empty(),
                        "NEUTRAL", Sets.mutable.<String>empty(),
                        "POSITIVE", Sets.mutable.<String>empty()
                );

                if (searching.equals("q")) {
                    break;

                } else {
                    try {
                        logger.info("state: start");

                        String phrase = MessageFormat.format("site:{0} \"{1}\"", SEARCH_FORUM, searching);
                        List<SearchResult> searchResults = searcher.search(phrase, SEARCH_LIMIT);
                        logger.info("state: search done");

                        Map<String, Integer> sentiments = ConcurrentHashMap.newMap();
                        sentiments.put("NEGATIVE", 0);
                        sentiments.put("NEUTRAL", 0);
                        sentiments.put("POSITIVE", 0);

                        searchResults
                                .stream()
                                .parallel()
                                .forEach(searchResult -> {
                                    try {
                                        if (!searchResult.context.trim().isEmpty()) {
                                            Sentiment sentiment = sentimentAnalyser.analyze(searchResult.context);
                                            sentiments.compute(sentiment.name(), (key, value) -> value + 1);
                                            texts.get(sentiment.name()).add(searchResult.context);
                                        }
                                    } catch (IOException e) {
                                        logger.info("exception: {}", e.toString());
                                    }
                                });
                        logger.info("state: sentiment done");

                        int hits = sentiments.values().stream().mapToInt(i -> i).sum();

                        printInfo(sentiments, hits, "Negative", "NEGATIVE");
                        printExamples(texts, "NEGATIVE");

                        printInfo(sentiments, hits, "Neutral", "NEUTRAL");
                        printExamples(texts, "NEUTRAL");

                        printInfo(sentiments, hits, "Positive", "POSITIVE");
                        printExamples(texts, "POSITIVE");

                    } catch (Exception e) {
                        logger.warn("Processing failed {}", e.toString());
                    }
                }
            }
        }
    }

    private void printExamples(Map<String, Set<String>> texts, String mapKey) {
        int counter = 0;
        for (String txt : texts.get(mapKey)) {
            logger.info("- " + txt);
            counter++;
            if (counter >= EXAMPLES_LIMIT) {
                break;
            }
        }
    }

    public void printInfo(Map<String, Integer> map, int sumOfValues, String printName, String mapKey) {
        int value = map.getOrDefault(mapKey, 0);
        String msg = String.format("%s: %d (%.2f%%)", printName, value, (double) value / sumOfValues * 100);
        logger.info(msg);
    }
}
