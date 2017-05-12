package pl.edwi.search;

import okhttp3.*;
import org.eclipse.collections.impl.factory.Lists;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pl.edwi.web.WebDownloader;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

public class DuckDuckGoSearch {

    private final OkHttpClient okClient;

    public DuckDuckGoSearch(WebDownloader webDownloader) {
        this.okClient = webDownloader.getOkClient();
    }

    public List<SearchResult> search(String phrase, int limit) throws IOException {
        List<SearchResult> results = Lists.mutable.empty();

        String param = URLEncoder.encode(phrase, "utf-8");
        String url = "https://duckduckgo.com/html/?q=" + param;
        Request request = new Request.Builder().url(url).build();

        do {
            Call call = okClient.newCall(request);
            try (Response response = call.execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Not successful " + response);
                }

                try (ResponseBody responseBody = response.body()) {
                    String html = responseBody.string();
                    Document document = Jsoup.parse(html);

                    for (Element find : document.select("div.result")) {
                        String srTitle = find.select(".result__title").text();
                        String srContext = find.select(".result__snippet").text();
                        String srUrl = find.select(".result__snippet").attr("href");

                        if (!srTitle.isEmpty()) {
                            results.add(new SearchResult(srTitle, srContext, srUrl));
                        }
                    }

                    request = results.size() < limit
                            ? nextPage(document)
                            : null;
                }
            }
        } while (request != null);

        return results;
    }

    private Request nextPage(Document document) {
        Request request;
        Elements next = document.select(".nav-link form input");
        if (next.isEmpty()) {
            request = null;
        } else {
            FormBody.Builder postBuilder = new FormBody.Builder();

            for (Element input : next) {
                postBuilder.add(input.attr("name"), input.attr("value"));
            }

            request = new Request.Builder()
                    .url("https://duckduckgo.com/html/")
                    .post(postBuilder.build())
                    .build();
        }
        return request;
    }
}
