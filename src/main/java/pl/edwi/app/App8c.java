package pl.edwi.app;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import com.google.common.io.CharStreams;
import pl.edwi.tool.WebDownloader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;

public class App8c {

    public static void main(String[] args) throws IOException {
        String google = "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=";
        String search = "stackoverflow";
        String charset = "UTF-8";

        WebDownloader webDownloader = new WebDownloader();

        URL url = new URL(google + URLEncoder.encode(search, charset));
        try (Reader reader = new InputStreamReader(url.openStream(), charset)) {
            String response = CharStreams.toString(reader);
            JsonValue parse = Json.parse(response);
            System.out.println(parse.toString());
        }
    }
}
