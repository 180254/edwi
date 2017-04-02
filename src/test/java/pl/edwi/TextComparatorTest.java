package pl.edwi;

import org.junit.Test;
import pl.edwi.tool.TextComparator;
import pl.edwi.tool.WebDownloader;
import pl.edwi.tool.WebPage;

import java.io.IOException;

public class TextComparatorTest {

    private final String text[] = {
            "The diagram on the right shows two memories. Each location in each memory has a datum (a cache\n" +
                    "line), which in different designs ranges in size from 8 to 512 bytes. The size of the cache line is\n" +
                    "usually larger than the size of the usual access requested by a CPU instruction, which ranges from 1\n" +
                    "to 16 bytes. Each location in each memory also has an index, which is a unique number used to\n" +
                    "refer to that location. The index for a location in main memory is called an address. Each location in\n" +
                    "the cache has a tag that contains the index of the datum in main memory that has been cached. In a\n" +
                    "CPU's data cache these entries are called cache lines or cache blocks.",

            "Johann Sebastian Bach (pronounced [jo'han/'jo?han ze'bastjan 'bax]) (31 March 1685 [O.S. 21\n" +
                    "March] – 28 July 1750) was a German composer and organist whose sacred and secular works for\n" +
                    "choir, orchestra, and solo instruments drew together the strands of the Baroque period and brought it\n" +
                    "to its ultimate maturity.[1] Although he introduced no new forms, he enriched the prevailing\n" +
                    "German style with a robust contrapuntal technique, an unrivalled control of harmonic and motivic\n" +
                    "organisation in composition for diverse musical forces, and the adaptation of rhythms and textures\n" +
                    "from abroad, particularly Italy and France.",

            "Revered for their intellectual depth, technical command and artistic beauty, Bach's works include\n" +
                    "the Brandenburg concertos, the Goldberg Variations, the English Suites, the French Suites, the\n" +
                    "Partitas, the Well-Tempered Clavier, the Mass in B Minor, the St. Matthew Passion, the St. John\n" +
                    "Passion, the Magnificat, The Musical Offering, The Art of Fugue, the Sonatas and Partitas for violin\n" +
                    "solo, the Cello Suites, more than 200 surviving cantatas, and a similar number of organ works,\n" +
                    "including the celebrated Toccata and Fugue in D Minor.",

            "While Bach's fame as an organist was great during his lifetime, he was not particularly well-known\n" +
                    "as a composer. His adherence to Baroque forms and contrapuntal style was considered \"oldfashioned\"\n" +
                    "by his contemporaries, especially late in his career when the musical fashion tended\n" +
                    "towards Rococo and later Classical styles. A revival of interest and performances of his music began\n" +
                    "early in the 19th century, and he is now widely considered to be one of the greatest composers in the\n" +
                    "Western tradition, being included with Ludwig van Beethoven and Johannes Brahms as one of the\n" +
                    "\"three Bs\"."
    };

    private final WebPage[] wp = new WebPage[text.length];
    private final WebDownloader wd = new WebDownloader();
    private final TextComparator wc = new TextComparator(wd);

    public TextComparatorTest() throws IOException {
        for (int i = 0; i < text.length; i++) {
            wp[i] = new WebPage("localhost", text[i]);
        }
    }

    @Test
    public void test() throws IOException {
        System.out.println("0 1 " + wc.compare(wp[0], wp[1]));
        System.out.println("0 2 " + wc.compare(wp[0], wp[2]));
        System.out.println("0 3 " + wc.compare(wp[0], wp[3]));
        System.out.println("1 2 " + wc.compare(wp[1], wp[2]));
        System.out.println("1 3 " + wc.compare(wp[1], wp[3]));
        System.out.println("2 3 " + wc.compare(wp[2], wp[3]));
    }
}
