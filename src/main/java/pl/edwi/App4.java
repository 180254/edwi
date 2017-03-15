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
import java.util.concurrent.Phaser;

public class App4 {


    ExecutorService sv = Executors.newFixedThreadPool(20);

    WebDownloader wd = new WebDownloader();
    WebHelper wh = new WebHelper();

    UrlValidator urlValidator = new UrlValidator(new String[]{"http", "https"});

    List<String> processed = new ArrayList<>(30);
    Queue<Holder> queue = new LinkedList<>();

    List<String> sameIp = new ArrayList<>(30);
    List<String> differentIp = new ArrayList<>(30);

    String startPage = "http://pduch.kis.p.lodz.pl/";
    InetAddress startIp = wh.ipAddress(startPage);

    Phaser cnt = new Phaser(1);

    public static void main(String[] args) {

        App4 app = new App4();
        long startTime = System.nanoTime();


        app.processed.add(app.startPage);
        app.sv.submit(() -> app.process(app.startPage, 0));


        app.cnt.awaitAdvance(0);
        double timeSec = (System.nanoTime() - startTime) / 1_000_000.0 / 1_000.0;
        System.out.printf("time = %.4f,s", timeSec);
        app.sv.shutdown();
    }

    public void process(String url, int depth) {

        WebPage page;
        try {
            page = wd.download(url);
        } catch (IOException e) {
            System.out.printf("FAIL: %s/%s%n", url, e.toString());
            cnt.arrive();
            return;
        }

        InetAddress ipAddress = wh.ipAddress(url);
        (ipAddress.equals(startIp) ? sameIp : differentIp).add(url);

        if (depth == 2) {
            cnt.arrive();
            return;
        }

        int i = 0;
        for (Element link : page.document().select("a[href]")) {

            try {

                String href1 = link.attr("abs:href");
                String href2 = wh.fixUrl(href1);
                if (!processed.contains(href2) && urlValidator.isValid(href2)) {
                    i++;
                    cnt.register();
                    processed.add(href2);
                    sv.submit(() -> process(href2, depth + 1));


                }
            } catch (URISyntaxException e) {
                System.out.printf("FAIL: %s/%s%n", "?", e.toString());
            }
        }

        cnt.arrive();
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
