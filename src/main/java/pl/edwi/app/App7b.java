package pl.edwi.app;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import pl.edwi.tool.FindResult7;
import pl.edwi.tool.FindTableMode7;
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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class App7b {

    private static final Pattern QUOTATION_MARK = Pattern.compile("\"", Pattern.LITERAL);

    private JPanel jPanel;
    private JTextField exactText;
    private JButton exactButton;
    private JTextField approxText;
    private JButton approxButton;
    private JTable resultTable;
    private JLabel statusText;

    private FindTableMode7 findTableModel;

    // ---------------------------------------------------------------------------------------------------------------

    public App7b(Analyzer analyzer, IndexReader reader) {

    }

    // ---------------------------------------------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        try {
            UIManager.setLookAndFeel(WindowsLookAndFeel.class.getName());
        } catch (ClassNotFoundException | InstantiationException |
                UnsupportedLookAndFeelException | IllegalAccessException ignored) {
            System.out.println("X");
        }

        Analyzer analyzer = new StandardAnalyzer();
        Directory index = FSDirectory.open(Paths.get(App7a.LUCENE_DIR));
        IndexReader reader = DirectoryReader.open(index);

        App7b app7b = new App7b(analyzer, reader);

        JFrame frame = new JFrame("Bot internetowy");
        frame.setContentPane(app7b.jPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Component p = app7b.jPanel;
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

        app7b.jPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                app7b.resizeResultTableColumns();
            }
        });

        app7b.exactText.addActionListener((event) -> app7b.exactSearch(analyzer, reader));
        app7b.exactButton.addActionListener((event) -> app7b.exactSearch(analyzer, reader));

        app7b.approxButton.addActionListener((event) -> app7b.approxSearch(analyzer, reader));
        app7b.approxText.addActionListener((event) -> app7b.approxSearch(analyzer, reader));

        app7b.findTableModel = new FindTableMode7();
        app7b.resultTable.setModel(app7b.findTableModel);
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void resizeResultTableColumns() {
        float[] columnWidthPercentage = {2, 5, 48, 45};
        int tW = resultTable.getWidth();

        TableColumnModel jTableColumnModel = resultTable.getColumnModel();
        int cantCols = jTableColumnModel.getColumnCount();

        for (int i = 0; i < cantCols; i++) {
            TableColumn column = jTableColumnModel.getColumn(i);
            int pWidth = Math.round(columnWidthPercentage[i] * tW);
            column.setPreferredWidth(pWidth);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void exactSearch(Analyzer analyzer, IndexReader reader) {
        String find0 = exactText.getText();
        String find1 = '"' + QUOTATION_MARK.matcher(find0).replaceAll("\\\"") + '"';
        anySearch(analyzer, reader, find1);
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void approxSearch(Analyzer analyzer, IndexReader reader) {
        String find0 = approxText.getText();
        String find1 = '"' + QUOTATION_MARK.matcher(find0).replaceAll("\\\"") + "\"~";
        anySearch(analyzer, reader, find1);
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void anySearch(Analyzer analyzer, IndexReader reader, String find) {
        try {
            Query query = new QueryParser("text", analyzer).parse(find);
            IndexSearcher searcher = new IndexSearcher(reader);

            MoreLikeThis mlt = new MoreLikeThis(reader);
            mlt.setAnalyzer(analyzer);
            mlt.setFieldNames(new String[]{"text"});

            TopDocs topDocs = searcher.search(query, 5);
            ScoreDoc[] topHits = topDocs.scoreDocs;

            statusText.setText("Znaleziono " + topDocs.totalHits + " wyników.");

            List<FindResult7> findResults = new ArrayList<>(5);
            for (ScoreDoc hit : topHits) {
                FindResult7 fr = new FindResult7();
                Document doc = searcher.doc(hit.doc);

                fr.resultUrl = doc.get("url");
                fr.resultScore = hit.score;

                Query queryMlt = mlt.like(hit.doc);
                TopDocs similarDocs = searcher.search(queryMlt, 3);
                if (similarDocs.totalHits > 1) {
                    ScoreDoc similarHit =
                            Arrays.stream(similarDocs.scoreDocs)
                                    .filter(h -> h.doc != hit.doc)
                                    .findFirst()
                                    .orElse(null);
                    Document similarDoc = searcher.doc(similarHit.doc);
                    fr.similarUrl = similarDoc.get("url");
                }

                findResults.add(fr);
            }

            findTableModel.setModelData(findResults);
            resizeResultTableColumns();

        } catch (ParseException | IOException e) {
            statusText.setText("Wystąpił błąd: " + e.toString());
        }
    }
}
