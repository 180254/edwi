package pl.edwi.tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class McwNaive implements Mcw {

    @Override
    // 2*n*log(n) + n + k
    public List<Pair<String, Integer>> get(String[] words, int k, int thresh) {

        Arrays.sort(words); // n*log(n)

        // -----------------------------------------------------------------------------------------------------------

        List<Pair<String, Integer>> pairs = new ArrayList<>(words.length);

        for (int i = 0; i < words.length; i++) { // n
            if (i == 0 || !words[i].equals(words[i - 1])) {
                pairs.add(new Pair<>(words[i], 1));

            } else {
                int lastIndex = pairs.size() - 1;
                Pair<String, Integer> lastPair = pairs.get(lastIndex);
                lastPair.setValue(lastPair.getValue() + 1);
            }
        }

        // -----------------------------------------------------------------------------------------------------------

        pairs.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue())); // n*log(n)

        // -----------------------------------------------------------------------------------------------------------

        List<Pair<String, Integer>> results = new ArrayList<>(k);
        for (int i = 0; i < k; i++) { // k
            Pair<String, Integer> current = pairs.get(i);

            if (current.getValue() < thresh) {
                break;
            }

            results.add(current);
        }

        return results;
    }
}
