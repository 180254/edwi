package pl.edwi.tool;

import java.io.Serializable;

public class FindResult implements Serializable {

    private static final long serialVersionUID = 3L;

    public String resultUrl;
    public double resultScore;
    public String similarUrl;
}
