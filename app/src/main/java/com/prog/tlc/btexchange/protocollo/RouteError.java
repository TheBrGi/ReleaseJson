package com.prog.tlc.btexchange.protocollo;

import java.io.Serializable;

/**
 * Created by Domenico on 26/04/2016.
 */
public class RouteError implements Serializable {
    private String dest;
    private String source;

    public RouteError(String source, String dest) {
        this.dest = dest;
        this.source = source;
    }

    public String getDest() { return dest; }

    public String getSource() { return source; }
}
