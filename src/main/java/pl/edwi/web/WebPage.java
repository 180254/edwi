package pl.edwi.web;

import org.jsoup.nodes.Document;

public class WebPage {

    private final String raw;
    private final String clean;
    private final Document doc;
    private final String[] words;

    public WebPage(String raw, String clean, Document doc, String[] words) {
        this.raw = raw;
        this.clean = clean;
        this.doc = doc;
        this.words = words;
    }

    public String raw() {
        return raw;
    }

    public String clean() {
        return clean;
    }

    public Document doc() {
        return doc.clone();
    }

    public String[] getWords() {
        return words.clone();
    }
}
