package pl.edwi;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.regex.Pattern;

public class App1 {

    public static void main(String[] args) throws IOException {
        Pattern pageNotAllowedChars = Pattern.compile("[^\\w \r\n]", Pattern.UNICODE_CHARACTER_CLASS);
        Pattern filenameNotAllowedChars = Pattern.compile("\\W", Pattern.UNICODE_CHARACTER_CLASS);
        Pattern doubleNewLines = Pattern.compile("[\r\n]+");

        Scanner scanner = new Scanner(System.in);
        OkHttpClient client = new OkHttpClient();

        System.out.print("URL: ");
        String url = scanner.nextLine();

        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();

        byte[] pageBytes = response.body().bytes();
        String page = new String(pageBytes);

        Charset charset = response.body().contentType().charset();
        if (charset == null) {
            for (Element meta : Jsoup.parse(page).getElementsByTag("meta")) {
                if ("Content-Type".equals(meta.attr("http-equiv"))) {
                    MediaType mt = MediaType.parse(meta.attr("content"));
                    if (mt != null) {
                        charset = mt.charset();
                    }
                }
            }
        }
        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }

        page = new String(pageBytes, charset);

        String clean = Jsoup.clean(
                page, "", Whitelist.none(),
                new Document.OutputSettings().prettyPrint(false)
        );

        clean = pageNotAllowedChars.matcher(clean).replaceAll("");
        clean = doubleNewLines.matcher(clean).replaceAll("\r\n");
        clean = clean.toLowerCase();

        String filename = filenameNotAllowedChars.matcher(url).replaceAll("_");
        Files.write(Paths.get(filename + ".html"), page.getBytes());
        Files.write(Paths.get(filename + ".txt"), clean.getBytes());
    }
}
