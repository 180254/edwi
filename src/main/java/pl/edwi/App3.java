package pl.edwi;

import pl.edwi.cmp.WebComparator;
import pl.edwi.web.WebDownloader;
import pl.edwi.web.WebPage;

import java.io.IOException;


public class App3 {
    // zoologia, muzyka powazna, informatyka

    public static void main(String[] args) throws IOException {

        String[] labels = {
                "zoologia_1",
                "zoologia_2",
                "zoologia_3",
                "zoologia_4",
                "zoologia_5",
                "muzyka_1",
                "muzyka_2",
                "muzyka_3",
                "muzyka_4",
                "muzyka_5",
                "informatyka_1",
                "informatyka_2",
                "informatyka_3",
                "informatyka_4",
                "informatyka_5",
        };

        String[] urls = {
                "https://pl.wikipedia.org/wiki/Zoologia",
                "https://pl.wikipedia.org/wiki/Zwierz%C4%99ta",
                "https://pl.wikipedia.org/wiki/Pies_domowy",
                "https://pl.wikipedia.org/wiki/Kot_domowy",
                "https://pl.wikipedia.org/wiki/Ssaki",
                "https://pl.wikipedia.org/wiki/Muzyka_powa%C5%BCna",
                "https://pl.wikipedia.org/wiki/Symfonia",
                "https://pl.wikipedia.org/wiki/Opera",
                "https://pl.wikipedia.org/wiki/Msza_(muzyka)",
                "https://pl.wikipedia.org/wiki/Sonata",
                "https://pl.wikipedia.org/wiki/Informatyka",
                "https://pl.wikipedia.org/wiki/Programowanie_komputer%C3%B3w",
                "https://pl.wikipedia.org/wiki/Kompilator",
                "https://pl.wikipedia.org/wiki/C%2B%2B",
                "https://pl.wikipedia.org/wiki/Wieloplatformowo%C5%9B%C4%87",
        };

        WebPage[] pages = new WebPage[urls.length];

        WebDownloader webDownloader = new WebDownloader();
        for (int i = 0; i < urls.length; i++) {
            pages[i] = webDownloader.download(urls[i]);
        }

        WebComparator webComparator = new WebComparator();
        double[][] results = new double[urls.length][urls.length];
        for (int i = 0; i < urls.length; i++) {
            for (int j = 0; j < urls.length; j++) {
                results[i][j] = webComparator.compare(pages[i], pages[j]);
            }
        }

        for (int i = 0; i < urls.length; i++) {
            for (int j = 0; j < urls.length; j++) {
                System.out.printf("%.2f ", results[i][j]);
            }
            System.out.println();
        }
    }

}
