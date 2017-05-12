package pl.edwi.web;

import com.google.common.collect.ImmutableSet;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class WebDownloader {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";

    public static final Set<String> ALLOWED_TYPES = ImmutableSet.of(
            "text/html", "application/xhtml+xml", "text/plain"
    );

    private final OkHttpClient okClient;

    public WebDownloader() {
        this.okClient = new OkHttpClient.Builder()
                .build();
    }

    public WebDownloader(ExecutorService executorService) {
        Dispatcher dispatcher = new Dispatcher(executorService);

        this.okClient = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .build();
    }

    public OkHttpClient getOkClient() {
        return okClient;
    }

    public WebPage downloadPage(String url) throws IOException {
        return downloadPage(url, null);
    }

    public WebPage downloadPage(String url, Charset charset) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENT)
                .build();

        Call call = okClient.newCall(request);

        try (Response response = call.execute()) {
            if (!hasValidCode(response)) {
                int code = response.code();
                if (code != 420) {
                    throw new IOException("Fail: response.code = " + code + " so != successful.");
                }
            }

            if (!hasValidType(response)) {
                String type = response.body().contentType().toString();
                throw new IOException("Fail: content.type  = " + type + " so != html page.");
            }

            byte[] bytes = response.body().bytes();
            if (charset == null) {
                charset = checkCharset(response, bytes);
            }

            String page = new String(bytes, charset);
            return new WebPage(url, page);

        }
    }

    public byte[] downloadBytes(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENT)
                .build();

        Call call = okClient.newCall(request);
        try (Response response = call.execute()) {
            return response.body().bytes();
        }
    }

    private Charset checkCharset(Response response, byte[] pageBytes) throws IOException {
        Charset charset = response.body().contentType().charset();

        if (charset == null) {
            String pageString = new String(pageBytes);

            Document pageDocument;
            try {
                // May be thrown if str is not parsable text.
                // I found web page that send text/plain even if has binary data.
                // http://arzone.kis.p.lodz.pl/GKTM/photoediting/pliki-lab_02/Thumbs.db
                pageDocument = Jsoup.parse(pageString);
            } catch (IllegalArgumentException e) {
                throw new IOException(e);
            }

            Elements metaTags = pageDocument.getElementsByTag("meta");

            for (Element meta : metaTags) {
                // <meta charset="UTF-8"/>
                if (!meta.attr("charset").isEmpty() && meta.attributes().size() == 1) {
                    try {
                        charset = Charset.forName(meta.attr("charset"));
                        break;
                    } catch (UnsupportedCharsetException ignored) {
                    }
                }

                // <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
                if (meta.attr("http-equiv").equals("Content-Type")) {
                    MediaType mt = MediaType.parse(meta.attr("content"));
                    if (mt != null) {
                        charset = mt.charset();

                        if (charset != null) {
                            break;
                        }
                    }
                }
            }
        }

        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }

        return charset;
    }

    private boolean hasValidCode(Response response) {
        return response.isSuccessful();
    }

    private boolean hasValidType(Response response) {
        ResponseBody body = response.body();
        MediaType contentType = body.contentType();
        String simplifiedType = contentType.type() + '/' + contentType.subtype();
        return ALLOWED_TYPES.contains(simplifiedType);
    }
}
