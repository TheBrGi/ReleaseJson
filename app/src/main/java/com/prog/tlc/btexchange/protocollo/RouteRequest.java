package com.prog.tlc.btexchange.protocollo;

import java.io.Serializable;

/**
 * Created by BrGi on 12/03/2016.
 */
public class RouteRequest implements Serializable{
    private String source_addr;
    private long source_sequence_number; //identifica univocamente questo messaggio RREQ
    private String dest_addr;
    private long dest_sequence_number; /*The latest sequence number received in the past
                                        by the originator for any route towards the
                                        destination.*/
    private int hop_cnt;
    private String last_sender; /*indirizzo MAC del nodo che ha appena inviato questa RREQ*/

    public RouteRequest(String source_addr, long source_sequence_number, String dest_addr, long dest_sequence_number, int hop_cnt, String last_sender) {
        this.source_addr = source_addr;
        this.source_sequence_number = source_sequence_number;
        this.dest_addr = dest_addr;
        this.dest_sequence_number = dest_sequence_number;
        this.hop_cnt = hop_cnt;
        this.last_sender = last_sender;

    }

    public String getSource_addr() {
        return source_addr;
    }

    public long getSource_sequence_number() {
        return source_sequence_number;
    }

    public String getDest_addr() {
        return dest_addr;
    }

    public long getDest_sequence_number() {
        return dest_sequence_number;
    }

    public int getHop_cnt() {
        return hop_cnt;
    }

    public String getLast_sender() {
        return last_sender;
    }

    public void setLast_sender(String last_sender) {
        this.last_sender = last_sender;
    }

    public void incrementaHop_cnt() { hop_cnt++; }
}
