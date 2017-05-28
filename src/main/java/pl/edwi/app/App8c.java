package pl.edwi.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edwi.search.DuckDuckGoSearch;
import pl.edwi.search.SearchEngine;
import pl.edwi.search.SearchResult;
import pl.edwi.sentiment.*;
import pl.edwi.web.WebDownloader;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Scanner;

public class App8c {

    public static final String SEARCH_FORUM = "linustechtips.com";
    public static final int SEARCH_LIMIT = 50;
    public static final int EXAMPLES_LIMIT = 4;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final WebDownloader webDownloader = new WebDownloader();
    private final SearchEngine searchEngine = new DuckDuckGoSearch(webDownloader);
    private final SentimentAnalyser sentimentAnalyser = new TpSentimentAnalyser(webDownloader);
    private final StatCollector statCollector = new StatCollectorImpl();

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
                        logger.debug("state: start");

                        String phrase = MessageFormat.format("site:{0} \"{1}\"", SEARCH_FORUM, searching);
                        List<SearchResult> searchResults = searchEngine.search(phrase, SEARCH_LIMIT);
                        logger.debug("state: search done");

                        searchResults
                                .stream()
                                .parallel()
                                .forEach(searchResult -> {
                                    try {
                                        if (!searchResult.context.trim().isEmpty()) {
                                            Sentiment sentiment = sentimentAnalyser.analyze(searchResult.context);
                                            statCollector.addResult(searchResult.context, sentiment);
                                        }
                                    } catch (IOException e) {
                                        logger.warn("Exception: {}", e.toString());
                                    }
                                });

                        logger.info("state: sentiment done");

                        printResult(Sentiment.NEGATIVE);
                        printResult(Sentiment.NEUTRAL);
                        printResult(Sentiment.POSITIVE);

                        logger.debug("state: all done");

                    } catch (Exception e) {
                        logger.warn("Processing failed: {}", e.toString());
                    }
                }
            }
        }
    }

    private void printResult(Sentiment sentiment) {
        logger.info(statCollector.getStatInfo(sentiment));

        for (String example : statCollector.getExamples(sentiment, EXAMPLES_LIMIT)) {
            logger.info("- " + example);

        }
    }
}
