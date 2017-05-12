package pl.edwi.web;

import com.google.common.base.MoreObjects;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class WebPage {

    public static final Pattern UNWANTED_CHARS = Pattern.compile("[^\\w\r\n ]", Pattern.UNICODE_CHARACTER_CLASS);
    public static final Pattern DOUBLE_SPACE = Pattern.compile(" {2,}");
    public static final Pattern DOUBLE_NEWLINE = Pattern.compile("(\r?\n\\s*){2,}");
    public static final Pattern WHITESPACES = Pattern.compile("\\s+");

    private String _url;
    private String _rawText;
    private String _cleanText;
    private Document _document;
    private String[] _wordsArray;
    private SortedMap<String, Integer> _wordsMap;

    public WebPage(String _url, String _rawText) {
        this._url = _url;
        this._rawText = _rawText;
    }

    public String url() {
        return _url;
    }

    public String rawText() {
        return _rawText;
    }

    public String cleanText() {
        if (_cleanText == null) {
            Cleaner cleaner = new Cleaner(Whitelist.none());
            Document cleanDoc = cleaner.clean(document());
            cleanDoc.outputSettings().prettyPrint(false);

            String cleanStr = cleanDoc.body().html();
            cleanStr = UNWANTED_CHARS.matcher(cleanStr).replaceAll(" ");
            cleanStr = DOUBLE_SPACE.matcher(cleanStr).replaceAll(" ");
            cleanStr = DOUBLE_NEWLINE.matcher(cleanStr).replaceAll("\r\n");
            cleanStr = cleanStr.toLowerCase();

            _cleanText = cleanStr;
        }

        return _cleanText;
    }

    public Document document() {
        if (_document == null) {
            _document = Jsoup.parse(rawText(), url());
        }

        return _document.clone();
    }

    public String[] wordsArray() {
        if (_wordsArray == null) {
            _wordsArray = WHITESPACES.split(cleanText());
        }

        return _wordsArray.clone();
    }

    public SortedMap<String, Integer> wordsMap() {
        if (_wordsMap == null) {
            SortedMap<String, Integer> wordsMap = new TreeMap<>(String::compareTo);

            for (String word : wordsArray()) {
                wordsMap.compute(word, (s, count) -> MoreObjects.firstNonNull(count, 0) + 1);
            }

            _wordsMap = wordsMap;
        }

        return new TreeMap<>(_wordsMap);
    }
}
