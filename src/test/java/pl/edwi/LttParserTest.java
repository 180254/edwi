package pl.edwi;

import org.jsoup.nodes.Document;
import org.junit.Test;
import pl.edwi.tool.ForumParser;
import pl.edwi.tool.LttParser;
import pl.edwi.tool.WebDownloader;
import pl.edwi.tool.WebPage;

import java.io.IOException;

public class LttParserTest {

    @Test
    public void test() throws IOException {
        WebDownloader wd = new WebDownloader();
        WebPage wp = wd.downloadPage("https://linustechtips.com/main/topic/167905-wan-show-audio-podcast-downloads/");
        Document doc = wp.document();

        ForumParser fp = new LttParser();
        for (String par : fp.getAllParagraphs(doc)) {
            System.out.println(par);
        }
    }
}
