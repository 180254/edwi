package pl.edwi;

import org.jsoup.nodes.Document;
import org.junit.Test;
import pl.edwi.forum.AvParser;
import pl.edwi.forum.ForumParser;
import pl.edwi.web.WebDownloader;
import pl.edwi.web.WebPage;

import java.io.IOException;

public class AvParserTest {

    @Test
    public void test() throws IOException {
        WebDownloader wd = new WebDownloader();
        WebPage wp = wd.downloadPage("https://www.avforums.com/threads/samsung-galaxy-s8-s8-cases-and-screen-protectors.2094671/");
        Document doc = wp.document();

        ForumParser fp = new AvParser();
        for (String par : fp.getAllParagraphs(doc)) {
            System.out.println(par);
        }
    }
}
