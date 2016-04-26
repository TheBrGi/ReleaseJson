package com.prog.tlc.btexchange.protocollo;

import com.prog.tlc.btexchange.gestioneDispositivo.Node;

import java.io.Serializable;

/**
 * Created by Domenico on 24/04/2016.
 */
public class Messaggio implements Serializable {
    private String mex;
    private Node dest;
    private String lastSender;
    private String source;

    public Messaggio(String s, Node dest,String lastSender, String source) {
        this.mex = s;
        this.dest = dest;
        this.lastSender = lastSender;
        this.source = source;
    }

    public String getMex() {
        return mex;
    }

    public Node getDest() {
        return dest;
    }

    public String getLastSender() { return  lastSender; }

    public String getSource() { return source; }
}
