package pl.edwi.app;

import com.google.common.base.MoreObjects;
import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;
import org.eclipse.collections.impl.factory.Maps;
import pl.edwi.tool.Try;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

public class App8w {

    private JTextArea statsArea;
    private JTextArea negArea;
    private JTextArea neuArea;
    private JTextArea posArea;
    private JTextField searchField;
    private JButton searchButton;
    private JLabel statusLabel;
    private JPanel jPanel;

    public App8w(Analyzer analyzer, IndexReader reader) {
    }

    public static void main(String[] args) throws IOException {

        Analyzer analyzer = new StandardAnalyzer();
        Directory index = FSDirectory.open(Paths.get(App8a.LUCENE_DIR));
        IndexReader reader = DirectoryReader.open(index);

        App8w app8w = new App8w(analyzer, reader);

        try {
            UIManager.setLookAndFeel(WindowsLookAndFeel.class.getName());
        } catch (ClassNotFoundException | InstantiationException |
                UnsupportedLookAndFeelException | IllegalAccessException ignored) {
        }
        JFrame frame = new JFrame("Czy to fajny telefon?");
        frame.setContentPane(app8w.jPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Component parent = app8w.jPanel;
        while (parent != null && !(parent instanceof Window)) {
            parent = parent.getParent();
        }

        if (parent != null) {
            Window window = (Window) parent;
            window.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent winEvt) {
                    Try.ex(reader::close);
                    Try.ex(index::close);
                    Try.ex(analyzer::close);
                }
            });
        }

        DefaultCaret negCaret = (DefaultCaret) app8w.negArea.getCaret();
        negCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        DefaultCaret neuCaret = (DefaultCaret) app8w.neuArea.getCaret();
        neuCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        DefaultCaret posCaret = (DefaultCaret) app8w.posArea.getCaret();
        posCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        app8w.negArea.setLineWrap(true);
        app8w.neuArea.setLineWrap(true);
        app8w.posArea.setLineWrap(true);

        app8w.searchField.addActionListener((event) -> app8w.search(analyzer, reader));
        app8w.searchButton.addActionListener((event) -> app8w.search(analyzer, reader));
    }

    public void search(Analyzer analyzer, IndexReader reader) {
        statusLabel.setText("...");
        statsArea.setText("");
        negArea.setText("");
        neuArea.setText("");
        posArea.setText("");

        String find = searchField.getText();
        process(analyzer, reader, find);
    }

    public void process(Analyzer analyzer, IndexReader reader, String find) {
        try {
            Query query = new QueryParser("par", analyzer).parse(find);
            IndexSearcher searcher = new IndexSearcher(reader);
            Map<String, Integer> sentiments = Maps.mutable.empty();

            LeafCollector leafCollector = new LeafCollector() {
                @Override
                public void setScorer(Scorer scorer) throws IOException {
                }

                @Override
                public void collect(int docId) throws IOException {
                    Document doc = searcher.doc(docId);
                    String sentiment = doc.get("sen");
                    sentiments.compute(sentiment, (key, value) -> MoreObjects.firstNonNull(value, 0) + 1);
                }
            };

            Collector collector = new Collector() {
                @Override
                public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
                    return leafCollector;
                }

                @Override
                public boolean needsScores() {
                    return false;
                }
            };

            searcher.search(query, collector);

            int hits = sentiments.values().stream().mapToInt(i -> i).sum();
            statusLabel.setText("Znaleziono " + hits + " wpisów (timestamp: " + System.currentTimeMillis() + ").");
            statsArea.append(formatStatInfo(sentiments, hits, "Negatywne", "NEGATIVE"));
            statsArea.append(formatStatInfo(sentiments, hits, "Neutralne", "NEUTRAL"));
            statsArea.append(formatStatInfo(sentiments, hits, "Posytywne", "POSITIVE"));


            negArea.setText(getExamples(analyzer, searcher, query, "NEGATIVE"));
            neuArea.setText(getExamples(analyzer, searcher, query, "NEUTRAL"));
            posArea.setText(getExamples(analyzer, searcher, query, "POSITIVE"));

        } catch (Exception e) {
            statusLabel.setText("Błąd podczas przetwarzania: " + abbreviateString(e.toString(), 80));
        }
    }

    public String formatStatInfo(Map<String, Integer> map, int sumOfValues, String printName, String mapKey) {
        int value = map.getOrDefault(mapKey, 0);
        return String.format("%s: %d (%.2f)%n", printName, value, (double) value / sumOfValues * 100);
    }

    public String getExamples(Analyzer analyzer, IndexSearcher searcher, Query baseQuery, String sentiment) throws IOException {
        Query sentimentQuery = new QueryBuilder(analyzer).createPhraseQuery("sen", sentiment);
        Query examplesQuery = new BooleanQuery.Builder()
                .add(baseQuery, BooleanClause.Occur.MUST)
                .add(sentimentQuery, BooleanClause.Occur.MUST)
                .build();

        TopDocs topDocs = searcher.search(examplesQuery, 10);
        ScoreDoc[] topHits = topDocs.scoreDocs;

        StringBuilder examples = new StringBuilder(1000);
        for (ScoreDoc hit : topHits) {
            String paragraph = searcher.doc(hit.doc).get("par");
            examples.append(" - ").append(fixParagraph(paragraph)).append("\r\n");
        }

        return examples.toString();
    }

    public String abbreviateString(String input, int maxLength) {
        if (input.length() <= maxLength) {
            return input;
        } else {
            return input.substring(0, maxLength - 2) + "..";
        }
    }

    public String fixParagraph(String text) {
        return text
                .replace("[ ", "(")
                .replace(" ]", ") ");
    }
}
