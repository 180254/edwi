package pl.edwi.tool;

import com.google.common.base.MoreObjects;
import org.eclipse.collections.impl.factory.Lists;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

public class DuckDuckGoSearch {

    private final WebDownloader webDownloader;

    public DuckDuckGoSearch(WebDownloader webDownloader) {
        this.webDownloader = webDownloader;
    }

    public void search(String phrase) throws IOException {
        String param = URLEncoder.encode(phrase, "utf-8");
        String url = "https://duckduckgo.com/html/?q=" + param;
        WebPage webPage = webDownloader.downloadPage(url);
        Document document = webPage.document();

        List<SearchResult> results = Lists.mutable.empty();

        for (Element find : document.select("div.result")) {
            String srTitle = find.select(".result__title").text();
            String srContext = find.select(".result__snippet").text();
            String srUrl = find.select(".result__snippet").attr("href");
            results.add(new SearchResult(srTitle, srContext, srUrl));
        }


    }

    public static class SearchResult {

        public final String title;
        public final String context;
        public final String url;

        public SearchResult(String title, String context, String url) {
            this.title = title;
            this.context = context;
            this.url = url;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("title", title)
                    .add("context", context)
                    .add("url", url)
                    .toString();
        }
    }
}
