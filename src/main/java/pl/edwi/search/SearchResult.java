package pl.edwi.search;

import java.text.MessageFormat;
import java.util.Objects;

public class SearchResult {

    public final String title;
    public final String context;
    public final String url;

    public SearchResult(String title, String context, String url) {
        this.title = title;
        this.context = context;
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResult that = (SearchResult) o;
        return Objects.equals(title, that.title) &&
                Objects.equals(context, that.context) &&
                Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        //noinspection ObjectInstantiationInEqualsHashCode
        return Objects.hash(title, context, url);
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "SearchResult[title={0}, context={1}, url={2}]",
                title, context, url
        );
    }
}
