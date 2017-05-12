package pl.edwi.ui;


import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class FindTableMode7 extends AbstractTableModel {

    private static final long serialVersionUID = 3L;
    private final static String[] COLUMN_NAMES = {"", "Strona wyszukana", "Strona podoba", "Dopasowanie"};
    private final List<FindResult7> results = new ArrayList<>(5);

    public FindTableMode7() {
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        FindResult7 fr = results.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return rowIndex + 1;
            case 1:
                return fr.resultUrl;
            case 2:
                return fr.similarUrl;
            case 3:
                return "<html>" + fr.matchStr + "</html>";
            default:
                return "";
        }
    }

    @Override
    public int getRowCount() {
        return results.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return true;
    }

    public void setModelData(List<FindResult7> results) {
        this.results.clear();
        this.results.addAll(results);
        fireTableDataChanged();
    }
}
