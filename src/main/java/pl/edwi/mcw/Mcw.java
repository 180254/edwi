package pl.edwi.mcw;

import pl.edwi.tool.Pair;

import java.util.List;

public interface Mcw {

    /**
     * MostCommonWords
     *
     * @param words  array of words
     * @param k      k-most-common
     * @param thresh thresh-min-amount
     * @return list of pairs [Word,Cnt]
     */
    List<Pair<String, Integer>> get(String[] words, int k, int thresh);
}
