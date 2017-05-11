package pl.edwi.app;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import pl.edwi.tool.FindResult6;
import pl.edwi.tool.FindTableMode6;
import pl.edwi.tool.Try;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings("Duplicates")
public class App6b {

    private static final Pattern QUOTATION_MARK = Pattern.compile("\"", Pattern.LITERAL);

    private JPanel jPanel;
    private JTextField titleText;
    private JButton titleButton;
    private JTextField contentText;
    private JButton contentButton;
    private JTable resultTable;
    private JLabel statusText;

    private FindTableMode6 findTableModel;

    public App6b(Analyzer analyzer, IndexReader reader) {

    }

    public static void main(String[] args) throws IOException {
        try {
            UIManager.setLookAndFeel(WindowsLookAndFeel.class.getName());
        } catch (ClassNotFoundException | InstantiationException |
                UnsupportedLookAndFeelException | IllegalAccessException ignored) {
        }

        Analyzer analyzer = new StandardAnalyzer();
        Directory index = FSDirectory.open(Paths.get(App6a.LUCENE_DIR));
        IndexReader reader = DirectoryReader.open(index);

        App6b app6b = new App6b(analyzer, reader);

        JFrame frame = new JFrame("Wyszukiwanie w projekcie Gutenberg");
        frame.setContentPane(app6b.jPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Component p = app6b.jPanel;
        while (p != null && !(p instanceof Window)) {
            p = p.getParent();
        }

        if (p != null) {
            ((Window) p).addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent winEvt) {
                    Try.ex(reader::close);
                    Try.ex(index::close);
                    Try.ex(analyzer::close);
                }
            });
        }

        app6b.jPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                app6b.resizeResultTableColumns();
            }
        });

        app6b.titleText.addActionListener((e) -> app6b.search(analyzer, reader, app6b.titleText, "title"));
        app6b.titleButton.addActionListener((e) -> app6b.search(analyzer, reader, app6b.titleText, "title"));
        app6b.contentButton.addActionListener((e) -> app6b.search(analyzer, reader, app6b.contentText, "content"));
        app6b.contentText.addActionListener((e) -> app6b.search(analyzer, reader, app6b.contentText, "content"));

        app6b.findTableModel = new FindTableMode6();
        app6b.resultTable.setModel(app6b.findTableModel);
    }

    private void resizeResultTableColumns() {
        float[] columnWidthPercentage = {2, 6, 6, 40, 46};
        int tW = resultTable.getWidth();

        TableColumnModel jTableColumnModel = resultTable.getColumnModel();
        int cantCols = jTableColumnModel.getColumnCount();

        for (int i = 0; i < cantCols; i++) {
            TableColumn column = jTableColumnModel.getColumn(i);
            int pWidth = Math.round(columnWidthPercentage[i] * tW);
            column.setPreferredWidth(pWidth);
        }
    }

    private void search(Analyzer analyzer, IndexReader reader, JTextField find0, String field) {
        statusText.setText("Przetwarzanie ...");
        findTableModel.setModelData(new ArrayList<>(0));

        String find1 = find0.getText();
        String find2 = '"' + QUOTATION_MARK.matcher(find1).replaceAll("\\\"") + '"';

        try {
            int numberOfResults = 50;

            Query query = new QueryParser(field, analyzer).parse(find2);
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs docs = searcher.search(query, numberOfResults);
            ScoreDoc[] hits = docs.scoreDocs;

            statusText.setText("Znaleziono " + docs.totalHits + " wyników.");

            Formatter formatter = new SimpleHTMLFormatter();
            Highlighter highlighter = new Highlighter(formatter, new QueryScorer(query));

            List<FindResult6> results = new ArrayList<>(numberOfResults);
            for (ScoreDoc hit : hits) {
                int docId = hit.doc;
                Document doc = searcher.doc(docId);

                String content = doc.get(field);
                Fields termVectors = reader.getTermVectors(docId);
                TokenStream tokenStream = TokenSources.getTokenStream(field, termVectors, content, analyzer, -1);
                TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, content, true, 1);

                FindResult6 findResult = new FindResult6();
                findResult.score = hit.score;
                findResult.id = doc.get("id");
                findResult.title = doc.get("title");
                if (frag.length > 0) {
                    findResult.text = frag[0].toString();
                } else {
                    findResult.text = "";
                }
                results.add(findResult);
            }

            findTableModel.setModelData(results);
            resizeResultTableColumns();

        } catch (ParseException | IOException | InvalidTokenOffsetsException e) {
            statusText.setText("Wystąpił błąd przetwarzania: " + e.toString());
        }
    }
}
