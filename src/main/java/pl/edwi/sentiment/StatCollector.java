package pl.edwi.sentiment;

import java.util.List;

public interface StatCollector {

    void addResult(String text, Sentiment sentiment);

    String getStatInfo(Sentiment sentiment);

    List<String> getExamples(Sentiment sentiment, int limit);

    void reset();
}
