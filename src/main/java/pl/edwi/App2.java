package pl.edwi;

import pl.edwi.mcw.Mcw;
import pl.edwi.mcw.McwNaive;
import pl.edwi.mcw.McwSuper;
import pl.edwi.util.Pair;
import pl.edwi.web.WebDownloader;
import pl.edwi.web.WebPage;
import pl.edwi.web.WebSaver;

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
        WebSaver ws = new WebSaver();

        // -----------------------------------------------------------------------------------------------------------

        WebPage page = wd.download(url);
        ws.save(url, page);

        // -----------------------------------------------------------------------------------------------------------

        Mcw[] algorithms = new Mcw[]{
                new McwNaive(),
                new McwSuper(),
        };

        List<Pair<String, Integer>> result = new ArrayList<>(0);

        int algCnt = 0;
        for (Mcw mcw : algorithms) {
            for (int i = 0; i < 7; i++) {
                String[] words_c = page.getWords();
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
