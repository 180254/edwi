package pl.edwi.tool;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import okhttp3.*;

import java.io.IOException;

//https://market.mashape.com/twinword/sentiment-analysis
public class SentimentAnalyser {

    private static final String X_API_KEY = "X";
    private final OkHttpClient okClient;

    public SentimentAnalyser(WebDownloader wd) {
        this.okClient = wd.getOkClient();
    }

    public Sentiment analyze(String text) throws IOException {
        RequestBody formBody = new FormBody.Builder()
                .add("text", text)
                .build();

        Request request = new Request.Builder()
                .url("https://twinword-sentiment-analysis.p.mashape.com/analyze/")
                .addHeader("X-Mashape-Key", X_API_KEY)
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

                String type = json.asObject().get("type").asString();
                switch (type) {
                    case "positive":
                        return Sentiment.POSITIVE;
                    case "neutral":
                        return Sentiment.NEUTRAL;
                    case "negative":
                        return Sentiment.NEGATIVE;
                }

                throw new IOException("Unknown response " + body);
            }
        }
    }
}
