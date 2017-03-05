package pl.edwi.cmp;

import pl.edwi.web.WebDownloader;
import pl.edwi.web.WebPage;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.DoubleStream;

public class WebComparator {

    private static final Pattern WHITESPACES = Pattern.compile("\\s+");
    Set<String> stopWords = new HashSet<>(Arrays.asList(getStopWords()));

    public WebComparator() throws IOException {
    }

    public double compare(WebPage page1, WebPage page2) throws IOException {

        SortedMap<String, Integer> wordMap1 = getWordMap(page1);
        SortedMap<String, Integer> wordMap2 = getWordMap(page2);

        wordMap1.keySet().removeAll(stopWords);
        wordMap2.keySet().removeAll(stopWords);

        fillTheDictionary(wordMap1, wordMap2);
        fillTheDictionary(wordMap2, wordMap1);

        double[] vector1 = computeVector(wordMap1);
        double[] vector2 = computeVector(wordMap2);

        double numerator = 0;
        for (int i = 0; i < vector1.length; i++) {
            numerator += vector1[i] * vector2[i];
        }

        double denominator = Math.sqrt(DoubleStream.of(vector1).map(a -> a * a).sum()) *
                Math.sqrt(DoubleStream.of(vector2).map(a -> a * a).sum());

        return numerator / denominator;
    }

    // ---------------------------------------------------------------------------------------------------------------

    private static String[] getStopWords() throws IOException {
        WebPage download = new WebDownloader().download("https://raw.githubusercontent.com/bieli/stopwords/master/polish.stopwords.txt");
        return WHITESPACES.split(download.clean());
    }

    // ---------------------------------------------------------------------------------------------------------------

    private SortedMap<String, Integer> getWordMap(WebPage page) {
        String[] words = WHITESPACES.split(page.clean());

        SortedMap<String, Integer> wordMap = new TreeMap<>(String::compareTo);

        for (String word : words) {
            wordMap.compute(word, (s, count) -> (count != null) ? (count + 1) : 1);
        }

        return wordMap;
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void fillTheDictionary(
            SortedMap<String, Integer> destination,
            SortedMap<String, Integer> source
    ) {
        for (String word : source.keySet()) {
            destination.putIfAbsent(word, 0);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private double[] computeVector(SortedMap<String, Integer> wordMap) {
        int numberOfWords = numberOfWords(wordMap);
        double[] vector = new double[wordMap.size()];

        int i = 0;
        for (Integer current : wordMap.values()) {
            vector[i] = current / (double) numberOfWords;
            i++;
        }

        return vector;
    }

    private int numberOfWords(SortedMap<String, Integer> wordMap) {
        return wordMap.values().stream().mapToInt(a -> a).sum();
    }


}

