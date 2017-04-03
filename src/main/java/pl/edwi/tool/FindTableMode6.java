package pl.edwi.tool;


import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class FindTableMode6 extends AbstractTableModel {

    private static final long serialVersionUID = 3L;
    private final static String[] COLUMN_NAMES = {"", "Ocena", "Id", "Tytuł", "Wyszukana treść"};
    private final List<FindResult6> results = new ArrayList<>(100);

    public FindTableMode6() {
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        FindResult6 fr = results.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return rowIndex + 1;
            case 1:
                return String.format("%2.2f", fr.score);
            case 2:
                return fr.id;
            case 3:
                return fr.title;
            case 4:
                return "<html>" + fr.text + "</html>";
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

    public void setModelData(List<FindResult6> results) {
        this.results.clear();
        this.results.addAll(results);
        fireTableDataChanged();
    }
}
