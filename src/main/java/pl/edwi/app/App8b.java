package pl.edwi.app;

import com.google.common.base.MoreObjects;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class App8b {

    public static void main(String[] args) throws IOException {
        new App8b().go();
    }

    public void go() throws IOException {
        try (Scanner scanner = new Scanner(System.in);
             Analyzer analyzer = new StandardAnalyzer();
             Directory index = FSDirectory.open(Paths.get(App8a.LUCENE_DIR))) {
            while (true) {
                System.out.print("> ");
                System.out.flush();
                String searching = scanner.nextLine().trim();

                if (searching.equals("q")) {
                    break;
                } else {
                    try {
                        process(analyzer, index, searching);
                    } catch (Exception e) {
                        System.out.println("Query processing failed {}" + e.toString());
                    }
                }
            }
        }
    }

    public void process(Analyzer analyzer, Directory index, String find) throws Exception {
        try (IndexReader reader = DirectoryReader.open(index)) {
            Query query = new QueryParser("par", analyzer).parse(find);

            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs docs = searcher.search(query, BooleanQuery.getMaxClauseCount());
            ScoreDoc[] hits = docs.scoreDocs;

            System.out.println("Found " + docs.totalHits + " hits.");

            Map<String, Integer> sentiments = new HashMap<>(1000);
            for (ScoreDoc hit : hits) {
                int docId = hit.doc;
                Document doc = searcher.doc(docId);
                String sentiment = doc.get("sen");
                sentiments.compute(sentiment, (key, value) -> MoreObjects.firstNonNull(value, 0) + 1);
            }

            int sum = sentiments.values().stream().mapToInt(i -> i).sum();
            printInfo(sentiments, sum, "Negative", "NEGATIVE");
            printInfo(sentiments, sum, "Neutral", "NEUTRAL");
            printInfo(sentiments, sum, "Positive", "POSITIVE");
        }
    }

    public void printInfo(Map<String, Integer> map, int sumOfValues, String printName, String mapKey) {
        int value = map.getOrDefault(mapKey, 0);
        System.out.printf("%s: %d (%.2f)%n", printName, value, (double) value / sumOfValues * 100);
    }
}
