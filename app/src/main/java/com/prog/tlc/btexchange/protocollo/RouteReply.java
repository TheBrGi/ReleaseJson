package com.prog.tlc.btexchange.protocollo;

import java.io.Serializable;

/**
 * Created by BrGi on 12/03/2016.
 */
public class RouteReply implements Serializable {
    private String source_addr;
    private String dest_addr;
    private int dest_sequence_number;
    private int hop_cnt;
    private String last_sender; /*indirizzo MAC del nodo che ha appena inviato questa RREP*/


    public RouteReply(String source_addr, String dest_addr, int dest_sequence_number, int hop_cnt, String last_sender) {
        this.source_addr = source_addr;
        this.dest_addr = dest_addr;
        this.dest_sequence_number = dest_sequence_number;
        this.hop_cnt = hop_cnt;
        this.last_sender = last_sender;

    }

    public String getLast_sender() {
        return last_sender;
    }

    public void setLast_sender(String last_sender) {
        this.last_sender = last_sender;
    }

    public String getSource_addr() {
        return source_addr;
    }

    public String getDest_addr() {
        return dest_addr;
    }

    public int getDest_sequence_number() {
        return dest_sequence_number;
    }

    public int getHop_cnt() {
        return hop_cnt;
    }

    public void incrementaHop_cnt() { hop_cnt++; }
}
