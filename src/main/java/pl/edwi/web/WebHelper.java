package pl.edwi.web;

import java.net.*;

public class WebHelper {

    public InetAddress ipAddress(String url) {
        try {
            return InetAddress.getByName(new URL(url).getHost());
        } catch (UnknownHostException | MalformedURLException e) {
            return null;
        }
    }

    public String fixUrl(String url) throws URISyntaxException {
        String s0 = new URI(url).normalize().toString();
        String s1 = s0.split("#")[0];
        String s2 = s1.endsWith("/") ? s1.substring(0, s1.length() - 1) : s1;
        return s2;
    }
}
