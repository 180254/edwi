package pl.edwi.app;

import com.google.common.base.MoreObjects;
import pl.edwi.search.DuckDuckGoSearch;
import pl.edwi.search.SearchResult;
import pl.edwi.sentiment.Sentiment;
import pl.edwi.sentiment.SentimentAnalyser;
import pl.edwi.web.WebDownloader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class App8c {

    public static final int SEARCH_LIMIT = 100;

    private final WebDownloader webDownloader = new WebDownloader();
    private final DuckDuckGoSearch searchEngine = new DuckDuckGoSearch(webDownloader);
    private final SentimentAnalyser sentimentAnalyser = new SentimentAnalyser(webDownloader);

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
                        Map<String, Integer> sentiments = new HashMap<>(SEARCH_LIMIT);

                        for (SearchResult searchResult : searchEngine.search("site:linustechtips.com " + searching, SEARCH_LIMIT)) {
                            Sentiment sentiment = sentimentAnalyser.analyze(searchResult.context);
                            sentiments.compute(sentiment.name(), (key, value) -> MoreObjects.firstNonNull(value, 0) + 1);
                        }

                        int sum = sentiments.values().stream().mapToInt(i -> i).sum();
                        printInfo(sentiments, sum, "Negative", "NEGATIVE");
                        printInfo(sentiments, sum, "Neutral", "NEUTRAL");
                        printInfo(sentiments, sum, "Positive", "POSITIVE");

                    } catch (Exception e) {
                        System.out.println("Processing failed " + e.toString());
                    }
                }
            }
        }
    }

    public void printInfo(Map<String, Integer> map, int sumOfValues, String printName, String mapKey) {
        int value = map.getOrDefault(mapKey, 0);
        System.out.printf("%s: %d (%.2f%%)%n", printName, value, (double) value / sumOfValues * 100);
    }
}
