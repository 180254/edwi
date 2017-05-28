package pl.edwi.sentiment;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatCollectorImpl implements StatCollector {

    private final Map<Sentiment, Integer> sentiments = ConcurrentHashMap.newMap();
    private final Map<Sentiment, Set<String>> examples = ConcurrentHashMap.newMap();

    public StatCollectorImpl() {
        reset();
    }

    @Override
    public void addResult(String text, Sentiment sentiment) {
        sentiments.compute(sentiment, (key, value) -> value + 1);
        examples.get(sentiment).add(text);
    }

    @Override
    public String getStatInfo(Sentiment sentiment) {
        int hits = sentiments.values().stream().mapToInt(i -> i).sum();
        int value = sentiments.getOrDefault(sentiment, 0);
        return String.format("%s: %d (%.2f%%)", sentiment.name(), value, (double) value / hits * 100);
    }

    @Override
    public List<String> getExamples(Sentiment sentiment, int limit) {
        List<String> examples = Lists.mutable.empty();

        int counter = 0;
        for (String txt : this.examples.get(sentiment)) {
            examples.add(txt);

            counter++;
            if (counter >= limit) {
                break;
            }
        }

        return examples;
    }

    @Override
    public void reset() {
        sentiments.put(Sentiment.NEGATIVE, 0);
        sentiments.put(Sentiment.NEUTRAL, 0);
        sentiments.put(Sentiment.POSITIVE, 0);

        examples.put(Sentiment.NEGATIVE, Sets.mutable.empty());
        examples.put(Sentiment.NEUTRAL, Sets.mutable.empty());
        examples.put(Sentiment.POSITIVE, Sets.mutable.empty());
    }
}
