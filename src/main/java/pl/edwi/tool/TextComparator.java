package pl.edwi.tool;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class TextComparator {

    private final Set<String> stopWords;

    public TextComparator(WebDownloader wd) throws IOException {
        String[] stopWordsEnglish = wd
                .downloadPage("https://raw.githubusercontent.com/bieli/stopwords/master/english.stopwords.txt")
                .wordsArray();

        String[] stopWordsPolish = wd
                .downloadPage("https://raw.githubusercontent.com/bieli/stopwords/master/polish.stopwords.txt")
                .wordsArray();

        int length = stopWordsEnglish.length + stopWordsPolish.length;
        Set<String> stopWords = new HashSet<>(length);

        Collections.addAll(stopWords, stopWordsPolish);
        Collections.addAll(stopWords, stopWordsEnglish);

        this.stopWords = Collections.unmodifiableSet(stopWords);
    }

    public double compare(WebPage page1, WebPage page2) throws IOException {
        SortedMap<String, Integer> wordMap1 = page1.wordsMap();
        SortedMap<String, Integer> wordMap2 = page2.wordsMap();

        wordMap1.keySet().removeAll(stopWords);
        wordMap2.keySet().removeAll(stopWords);

        fillTheDictionary(wordMap1, wordMap2);
        fillTheDictionary(wordMap2, wordMap1);

        double[] vector1 = computeVector(wordMap1);
        double[] vector2 = computeVector(wordMap2);

        double numerator
                = IntStream.range(0, vector1.length)
                .mapToDouble(i -> vector1[i] * vector2[i])
                .sum();

        double denominator
                = Math.sqrt(DoubleStream.of(vector1).map(a -> a * a).sum())
                * Math.sqrt(DoubleStream.of(vector2).map(a -> a * a).sum());

        return numerator / denominator;
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void fillTheDictionary(
            SortedMap<String, Integer> source,
            SortedMap<String, Integer> destination
    ) {
        for (String word : source.keySet()) {
            destination.putIfAbsent(word, 0);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private double[] computeVector(SortedMap<String, Integer> wordMap) {
        double numberOfWords = numberOfWords(wordMap);
        double[] vector = new double[wordMap.size()];

        int i = 0;
        for (Integer current : wordMap.values()) {
            vector[i] = current / numberOfWords;
            i++;
        }

        return vector;
    }

    private int numberOfWords(SortedMap<String, Integer> wordMap) {
        return wordMap.values().stream().mapToInt(a -> a).sum();
    }
}
