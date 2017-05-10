package pl.edwi.tool;


import org.jsoup.nodes.Document;

import java.util.List;

public interface ForumParser {

    String startUrl();

    boolean isThatUrlForum(String url);

    boolean isThatUrlThread(String url);

    List<String> getAllParagraphs(Document document);
}
