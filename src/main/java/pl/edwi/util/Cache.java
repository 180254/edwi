package pl.edwi.util;

import pl.edwi.web.WebPage;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.HashMap;

public class Cache implements Serializable {

    public final HashMap<String, InetAddress> ipAddresses = new HashMap<>(5000);
    public final HashMap<String, WebPage> webPages = new HashMap<>(5000);

}
