package pl.edwi.app;

import pl.edwi.tool.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class App2 {

    public static void main(String[] args) throws IOException {

        String url = "https://pl.wikipedia.org/wiki/II_wojna_%C5%9Bwiatowa";
        int k = 10;
        int thresh = 4;

        // -----------------------------------------------------------------------------------------------------------

        WebDownloader wd = new WebDownloader();
        WebCache wc = new WebCache();

        // -----------------------------------------------------------------------------------------------------------

        WebPage page = wd.downloadPage(url);
        wc.savePage(page);

        // -----------------------------------------------------------------------------------------------------------

        Mcw[] algorithms = new Mcw[]{
                new McwNaive(),
                new McwSuper(),
        };

        List<Pair<String, Integer>> result = new ArrayList<>(0);

        int algCnt = 0;
        for (Mcw mcw : algorithms) {
            for (int i = 0; i < 7; i++) {
                String[] words_c = page.wordsArray();
                long start = System.nanoTime();

                result = mcw.get(words_c, k, thresh);

                double timeMs = (System.nanoTime() - start) / 1_000_000.0;
                System.out.printf("[A%s] = %.3fms\n", algCnt, timeMs);
            }

            algCnt++;
        }

        for (Pair<String, Integer> cw : result) {
            System.out.println(cw.toString());
        }
    }
}
