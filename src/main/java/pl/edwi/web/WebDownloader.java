package pl.edwi.web;

import com.google.common.collect.ImmutableSet;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Set;
import java.util.regex.Pattern;

public class WebDownloader {

    private static final String USER_AGENT = "some-robot-agent/1.0";

    // ---------------------------------------------------------------------------------------------------------------

    private static final Set<String> ALLOWED_TYPES = ImmutableSet.of(
            "text/html", "application/xhtml+xml", "text/plain"
    );

    // ---------------------------------------------------------------------------------------------------------------

    private static final Pattern PAGE_UNWANTED_CHARS = Pattern.compile("[^\\w\r\n ]", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern DOUBLE_SPACE = Pattern.compile(" {2,}");
    private static final Pattern DOUBLE_NEWLINE = Pattern.compile("(\r?\n\\s*){2,}");
    private static final Pattern WHITESPACES = Pattern.compile("\\s+");

    // ---------------------------------------------------------------------------------------------------------------

    private final OkHttpClient okClient = new OkHttpClient();
    private final Cleaner tagsCleaner = new Cleaner(Whitelist.none());

    // ---------------------------------------------------------------------------------------------------------------

    public WebPage download(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENT)
                .build();

        Call call = okClient.newCall(request);

        try (Response response = call.execute()) {
            if (!hasValidCode(response)) {
                int code = response.code();
                throw new IOException("Fail: response.code = " + code + " so != successful.");
            }

            if (!hasValidType(response)) {
                String type = response.body().contentType().toString();
                throw new IOException("Fail: content.type  = " + type + " so != html page.");
            }

            byte[] bytes = response.body().bytes();
            Charset charset = getCharset(response, bytes);
            String page = new String(bytes, charset);
            return transformPage(page);
        }
    }
    // ---------------------------------------------------------------------------------------------------------------

    private Charset getCharset(Response response, byte[] pageBytes) throws IOException {
        Charset charset = response.body().contentType().charset();

        if (charset == null) {
            String pageString = new String(pageBytes);
            Document pageDocument = Jsoup.parse(pageString);
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

    // ---------------------------------------------------------------------------------------------------------------

    private boolean hasValidCode(Response response) {
        return response.isSuccessful();
    }

    private boolean hasValidType(Response response) {
        ResponseBody body = response.body();
        MediaType contentType = body.contentType();
        String simplifiedType = contentType.type() + '/' + contentType.subtype();
        return ALLOWED_TYPES.contains(simplifiedType);
    }

    // ---------------------------------------------------------------------------------------------------------------

    private WebPage transformPage(String page) {
        Document docDirty = Jsoup.parse(page);

        Document docClean = tagsCleaner.clean(docDirty);
        docClean.outputSettings().prettyPrint(false);

        String strClean = docClean.body().html();
        strClean = PAGE_UNWANTED_CHARS.matcher(strClean).replaceAll(" ");
        strClean = DOUBLE_SPACE.matcher(strClean).replaceAll(" ");
        strClean = DOUBLE_NEWLINE.matcher(strClean).replaceAll("\r\n");
        strClean = strClean.toLowerCase();

        String[] words = WHITESPACES.split(strClean);

        return new WebPage(page, strClean, docDirty, words);
    }
}
