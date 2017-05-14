package pl.edwi.sentiment;

import java.io.IOException;

public interface SentimentAnalyser {

    Sentiment analyze(String text) throws IOException;
}
