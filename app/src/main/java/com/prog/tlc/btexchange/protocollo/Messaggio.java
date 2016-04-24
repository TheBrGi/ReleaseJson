package com.prog.tlc.btexchange.protocollo;

import com.prog.tlc.btexchange.gestioneDispositivo.Node;

import java.io.Serializable;

/**
 * Created by Domenico on 24/04/2016.
 */
public class Messaggio implements Serializable {
    private String mex;
    private Node dest;

    public Messaggio(String s, Node dest) {
        this.mex = s;
        this.dest = dest;
    }

    public String getMex() {
        return mex;
    }

    public Node getDest() {
        return dest;
    }
}
