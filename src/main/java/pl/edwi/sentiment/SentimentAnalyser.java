package pl.edwi.sentiment;

import java.io.IOException;

/**
 * Created by Adrian on 2017-05-12.
 */
public interface SentimentAnalyser {
    Sentiment analyze(String text) throws IOException;
}
