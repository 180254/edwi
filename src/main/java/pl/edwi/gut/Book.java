package pl.edwi.gut;

import java.text.MessageFormat;

public class Book {

    private final String id;
    private final String title;

    public Book(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return MessageFormat.format("Book[{0} {1}]", id, title);
    }
}
