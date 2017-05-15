package pl.edwi.search;

import java.io.IOException;
import java.util.List;

public interface SearchEngine {

    List<SearchResult> search(String phrase, int limit) throws IOException;
}
