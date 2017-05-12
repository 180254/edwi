package pl.edwi;

import org.junit.Test;
import pl.edwi.search.DuckDuckGoSearch;
import pl.edwi.search.SearchResult;
import pl.edwi.web.WebDownloader;

import java.io.IOException;
import java.util.List;

public class DuckDuckGoSearchTest {

    @Test

    public void test() throws IOException {
        WebDownloader wd = new WebDownloader();
        DuckDuckGoSearch ddgs = new DuckDuckGoSearch(wd);

        List<SearchResult> search = ddgs.search("xiaomi", 99);
        for (SearchResult searchResult : search) {
            System.out.println(searchResult);
        }
    }
}
