package pl.edwi.gut;

import java.text.MessageFormat;

public class Book {

    private final String id;
    private final String title;
    private final String content;

    public Book(String id, String title) {
        this.id = id;
        this.title = title;
        this.content = "";
    }

    public Book(String id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return MessageFormat.format("Book[{0} {1}]", id, title);
    }
}
