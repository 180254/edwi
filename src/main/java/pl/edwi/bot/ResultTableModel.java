package pl.edwi.bot;


import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class ResultTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 3L;
    private final static String[] COLUMN_NAMES = {"", "Wynik", "Strona wyszukana", "Strona podoba"};
    private final List<FindResult> results = new ArrayList<>(5);

    public ResultTableModel() {
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        FindResult fr = results.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return rowIndex + 1;
            case 1:
                return String.format("%2.2f", fr.resultScore);
            case 2:
                return fr.resultUrl;
            case 3:
                return fr.similarUrl;
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

    public void setModelData(List<FindResult> results) {
        this.results.clear();
        this.results.addAll(results);
        fireTableDataChanged();
    }
}
