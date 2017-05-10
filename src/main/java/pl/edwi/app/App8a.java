package pl.edwi.app;

import pl.edwi.tool.*;

import java.io.IOException;
import java.util.List;

public class App8a {

    private final WebDownloader wd = new WebDownloader();
    private final ForumParser fp = new TomsParser();
    private final SentimentAnalyser sa = new SentimentAnalyser(wd);

    public App8a() throws IOException {
        String url = "http://www.tomshardware.co.uk/forum/id-3341285/amd-naples-server-cpu-info-rumours.html";
        WebPage webPage = wd.downloadPage(url);
        List<String> paragraphs = fp.getAllParagraphs(webPage.document());

        for (String paragraph : paragraphs) {
            Sentiment sentiment = sa.analyze(paragraph);
            System.out.printf("%-10.10s%-10.10s\n", sentiment, paragraph);
        }

    }

    public static void main(String[] args) throws IOException {
        new App8a();
    }
}


