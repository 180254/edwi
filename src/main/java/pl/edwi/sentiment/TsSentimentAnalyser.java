package pl.edwi.sentiment;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import okhttp3.*;
import pl.edwi.web.WebDownloader;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

// https://market.mashape.com/fyhao/text-sentiment-analysis-method
public class TsSentimentAnalyser implements SentimentAnalyser {

    public static final Path SENTIMENT_ANALYSER_API_KEY_FILE = Paths.get("sentimentAnalyserApiKey.txt");
    public static final Pattern BANNED_CHARS = Pattern.compile("[\r\n.]");

    private final String sentimentAnalyserApiKey;
    private final OkHttpClient okClient;

    public TsSentimentAnalyser(WebDownloader wd) throws IOException {
        this.sentimentAnalyserApiKey = new String(
                Files.readAllBytes(SENTIMENT_ANALYSER_API_KEY_FILE),
                Charset.forName("UTF-8")
        ).trim();

        this.okClient = wd.getOkClient();
    }

    @Override
    public Sentiment analyze(String text) throws IOException {
        RequestBody formBody = new FormBody.Builder()
                .add("text", BANNED_CHARS.matcher(text).replaceAll(" "))
                .build();

        Request request = new Request.Builder()
                .url("https://text-sentiment.p.mashape.com/analyze")
                .addHeader("X-Mashape-Key", sentimentAnalyserApiKey)
                .post(formBody)
                .build();

        Call call = okClient.newCall(request);
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            try (ResponseBody responseBody = response.body()) {
                String body = responseBody.string();
                JsonValue json = Json.parse(body);
                JsonObject object = json.asObject();

                if (object.get("pos").asInt() == 1) {
                    return Sentiment.POSITIVE;
                } else if (object.get("neg").asInt() == 1) {
                    return Sentiment.NEGATIVE;
                } else if (object.get("mid").asInt() == 1) {
                    return Sentiment.NEUTRAL;
                }

                throw new IOException("Unknown response " + body);
            }
        }
    }
}
