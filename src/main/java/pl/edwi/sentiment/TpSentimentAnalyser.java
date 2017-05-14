package pl.edwi.sentiment;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import okhttp3.*;
import pl.edwi.web.WebDownloader;

import java.io.IOException;

//http://text-processing.com/docs/sentiment.html
public class TpSentimentAnalyser implements SentimentAnalyser {

    private final OkHttpClient okClient;

    public TpSentimentAnalyser(WebDownloader wd) {
        this.okClient = wd.getOkClient();
    }

    @Override
    public Sentiment analyze(String text) throws IOException {
        RequestBody formBody = new FormBody.Builder()
                .add("language", "english")
                .add("text", text)
                .build();

        Request request = new Request.Builder()
                .url("http://text-processing.com/api/sentiment/")
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

                String label = json.asObject().get("label").asString();
                switch (label) {
                    case "pos":
                        return Sentiment.POSITIVE;
                    case "neutral":
                        return Sentiment.NEUTRAL;
                    case "neg":
                        return Sentiment.NEGATIVE;
                }

                throw new IOException("Unknown response " + body);
            }
        }
    }
}
