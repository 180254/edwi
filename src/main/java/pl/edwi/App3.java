package pl.edwi;

import pl.edwi.cmp.TextComparator;
import pl.edwi.web.WebDownloader;
import pl.edwi.web.WebPage;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;


public class App3 {
    public static void main(String[] args) throws IOException {

        String[] names = {
                "Kręgowce", "Ssaki", "Zwierzęta", "Pies", "Kot",
                "Symfonia", "Opera", "Bach", "Mozart",
                "Algorytm,", "Programista", "Kompilator", "C++", "Java"
        };

        String[] urls = {
                // zoologia
                "https://pl.wikipedia.org/wiki/Kr%C4%99gowce",
                "https://pl.wikipedia.org/wiki/Ssaki",
                "https://pl.wikipedia.org/wiki/Zwierz%C4%99ta",
                "https://pl.wikipedia.org/wiki/Pies_domowy",
                "https://pl.wikipedia.org/wiki/Kot_domowy",

                // muzyka powazna
                "https://pl.wikipedia.org/wiki/Symfonia",
                "https://pl.wikipedia.org/wiki/Opera",
                "https://pl.wikipedia.org/wiki/Johann_Sebastian_Bach",
                "https://pl.wikipedia.org/wiki/Wolfgang_Amadeus_Mozart",

                // informatyka
                "https://pl.wikipedia.org/wiki/Algorytm",
                "https://pl.wikipedia.org/wiki/Programista",
                "https://pl.wikipedia.org/wiki/Kompilator",
                "https://pl.wikipedia.org/wiki/C%2B%2B",
                "https://pl.wikipedia.org/wiki/Java",
        };

        WebPage[] pages = new WebPage[urls.length];

        WebDownloader webDownloader = new WebDownloader();
        for (int i = 0; i < urls.length; i++) {
            pages[i] = webDownloader.download(urls[i]);
        }

        TextComparator textComparator = new TextComparator(webDownloader);
        Map<String, Double> results = new HashMap<>(120);

        for (int i = 0; i < urls.length; i++) {
            for (int j = 0; j <= i; j++) {
                if (i == j) continue;

                results.put(
                        String.format("[%-13s %-13s]", names[i], names[j]),
                        textComparator.compare(pages[i], pages[j])
                );
            }
        }

        Comparator<Map.Entry<String, Double>> comparator = Comparator.comparing(Map.Entry::getValue);

        System.out.println("NAJBARDZIEJ PODOBNE");
        results.entrySet().stream()
                .sorted(comparator.reversed())
                .limit(10)
                .forEach(e -> System.out.printf("%s --> %.4f\n", e.getKey(), e.getValue()));

        System.out.println("\nNAJMNIEJ PODOBNE");
        results.entrySet().stream()
                .sorted(comparator)
                .limit(10)
                .forEach(e -> System.out.printf("%s --> %.4f\n", e.getKey(), e.getValue()));
    }
}
