package pl.edwi;

import org.apache.commons.validator.routines.UrlValidator;
import org.jsoup.nodes.Element;
import pl.edwi.web.WebDownloader;
import pl.edwi.web.WebHelper;
import pl.edwi.web.WebPage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App4 {


    public static void main(String[] args) {

        long startTime = System.nanoTime();

        ExecutorService sv = Executors.newFixedThreadPool(5);

        WebDownloader wd = new WebDownloader();
        WebHelper wh = new WebHelper();

        UrlValidator urlValidator = new UrlValidator(new String[]{"http", "https"});

        List<String> processed = new ArrayList<>(30);
        Queue<Holder> queue = new LinkedList<>();

        List<String> sameIp = new ArrayList<>(30);
        List<String> differentIp = new ArrayList<>(30);

        String startPage = "http://pduch.kis.p.lodz.pl/";
        InetAddress startIp = wh.ipAddress(startPage);


        queue.add(new Holder(startPage, 0));
        processed.add(startPage);

        while (!queue.isEmpty()) {
            Holder entry = queue.poll();
            WebPage page;

            try {
                page = wd.download(entry.url);
            } catch (IOException e) {
                System.out.printf("FAIL: %s/%s%n", entry.url, e.toString());
                continue;
            }

            InetAddress ipAddress = wh.ipAddress(entry.url);
            (ipAddress.equals(startIp) ? sameIp : differentIp).add(entry.url);

            if (entry.depth == 2) {
                continue;
            }

            int i = 0;
            for (Element link : page.document().select("a[href]")) {

                try {

                    String href = link.attr("abs:href");
                    href = wh.fixUrl(href);
                    if (!processed.contains(href) && urlValidator.isValid(href)) {
                        i++;
                        queue.add(new Holder(href, entry.depth + 1));
                        processed.add(href);

                    }
                } catch (URISyntaxException e) {
                    System.out.printf("FAIL: %s/%s%n", "?", e.toString());
                }
            }
        }

        double timeMs = (System.nanoTime() - startTime) / 1_000_000.0;
        System.out.printf("time = %.2f,s", timeMs);
    }

    private static class Holder {
        String url;
        int depth;

        public Holder(String url, int depth) {
            this.url = url;
            this.depth = depth;
        }
    }


}
