package pl.edwi.app;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;
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

@SuppressWarnings("Duplicates")
public class App7b {

    private JPanel jPanel;
    private JTextField findText;
    private JComboBox<String> findType;
    private JComboBox<String> findWhere;
    private JButton findButton;
    private JTable resultTable;
    private JLabel statusText;
    private FindTableMode7 findTableModel;

    public App7b(Analyzer analyzer, IndexReader reader) {

    }

    public static void main(String[] args) throws IOException {
        try {
            UIManager.setLookAndFeel(WindowsLookAndFeel.class.getName());
        } catch (ClassNotFoundException | InstantiationException |
                UnsupportedLookAndFeelException | IllegalAccessException ignored) {
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

        app7b.findType.addItem("zawiera");
        app7b.findType.addItem("dokładne");
        app7b.findWhere.addItem("w treści");
        app7b.findWhere.addItem("w adresie");

        app7b.jPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                app7b.resizeResultTableColumns();
            }
        });

        app7b.findText.addActionListener((event) -> app7b.search(analyzer, reader));
        app7b.findButton.addActionListener((event) -> app7b.search(analyzer, reader));

        app7b.findTableModel = new FindTableMode7();
        app7b.resultTable.setModel(app7b.findTableModel);
    }

    private void resizeResultTableColumns() {
        float[] columnWidthPercentage = {2, 38, 30, 30};
        int tW = resultTable.getWidth();

        TableColumnModel jTableColumnModel = resultTable.getColumnModel();
        int cantCols = jTableColumnModel.getColumnCount();

        for (int i = 0; i < cantCols; i++) {
            TableColumn column = jTableColumnModel.getColumn(i);
            int pWidth = Math.round(columnWidthPercentage[i] * tW);
            column.setPreferredWidth(pWidth);
        }
    }

    private void search(Analyzer analyzer, IndexReader reader) {
        long currentTimeMillis = System.currentTimeMillis();
        String findTextStr = findText.getText();
        int findTypeIndex = findType.getSelectedIndex();
        int findWhereIndex = findWhere.getSelectedIndex();
        String field = findWhereIndex == 0 ? "text" : "url_1";

        try {
            QueryBuilder queryBuilder = new QueryBuilder(analyzer);
            Query query = findTypeIndex == 0
                    ? queryBuilder.createBooleanQuery(field, findTextStr, BooleanClause.Occur.MUST) // zawiera
                    : queryBuilder.createPhraseQuery(field, findTextStr); // dokładnie
            if (query == null) {
                query = new MatchAllDocsQuery();
            }

            IndexSearcher searcher = new IndexSearcher(reader);

            MoreLikeThis mlt = new MoreLikeThis(reader);
            mlt.setAnalyzer(analyzer);
            mlt.setFieldNames(new String[]{"text"});

            TopDocs topDocs = searcher.search(query, 5);
            ScoreDoc[] topHits = topDocs.scoreDocs;

            statusText.setText(
                    String.format("Znaleziono %d wyników. (%d)", topDocs.totalHits, currentTimeMillis)
            );

            Formatter formatter = new SimpleHTMLFormatter();
            Highlighter highlighter = new Highlighter(formatter, new QueryScorer(query));

            List<FindResult7> findResults = new ArrayList<>(5);
            for (ScoreDoc hit : topHits) {
                FindResult7 fr = new FindResult7();
                Document doc = searcher.doc(hit.doc);

                fr.resultUrl = doc.get("url_0");

                String content = doc.get(field);
                Fields termVectors = reader.getTermVectors(hit.doc);
                TokenStream tokenStream = TokenSources.getTokenStream(field, termVectors, content, analyzer, -1);
                TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, content, false, 1);
                if (frag.length > 0) {
                    fr.matchStr = frag[0].toString().trim();
                }

                Query queryMlt = mlt.like(hit.doc);
                TopDocs similarDocs = searcher.search(queryMlt, 3);
                if (similarDocs.totalHits > 1) {
                    ScoreDoc similarHit =
                            Arrays.stream(similarDocs.scoreDocs)
                                    .filter(h -> h.doc != hit.doc)
                                    .findFirst()
                                    .orElse(null);
                    Document similarDoc = searcher.doc(similarHit.doc);
                    fr.similarUrl = similarDoc.get("url_0");
                }

                findResults.add(fr);
            }

            findTableModel.setModelData(findResults);
            resizeResultTableColumns();

        } catch (IOException e) {
            statusText.setText(
                    String.format("Wystąpił błąd %s wyników. (%d)", e.toString(), currentTimeMillis)
            );
        } catch (InvalidTokenOffsetsException e) {
            e.printStackTrace();
        }
    }
}
