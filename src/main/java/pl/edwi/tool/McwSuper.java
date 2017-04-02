package pl.edwi.tool;

import com.google.common.base.MoreObjects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class McwSuper implements Mcw {

    @Override
    // n*log(n) + 2n + k
    public List<Pair<String, Integer>> get(String[] words, int k, int thresh) {

        Map<String, Integer> wordsMap = new HashMap<>(words.length);

        for (String word : words) { // n
            wordsMap.compute(word, (s, count) -> MoreObjects.firstNonNull(count, 0) + 1); // 1
        }

        return wordsMap.entrySet().stream()
                .filter(o -> o.getValue() >= thresh) // n
                .sorted((o1, o2) -> o2.getValue().compareTo(o1.getValue())) // n*log(n)
                .limit(k)
                .map(o -> new Pair<>(o.getKey(), o.getValue())) // k
                .collect(Collectors.toList());
    }
}
