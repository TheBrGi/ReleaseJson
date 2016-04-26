package com.prog.tlc.btexchange.gestioneDispositivo;

import java.io.Serializable;

/**
 * Created by Domenico on 10/03/2016.
 */
//RIGA NELLA TABELLA DI ROUTING(record in hasmap di percorsi)
public class Percorso implements Serializable{
    private String destinazione, nextHop; //MACaddress di destinazione e nexthop
    private int numeroHop;
    private long sequenceNumber;

    public Percorso(String dest, String nextH, int nHop,long sequenceNumber){
        destinazione = dest;
        nextHop = nextH;
        numeroHop = nHop;
        this.sequenceNumber = sequenceNumber;
    }
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public String getDestinazione() {
        return destinazione;
    }

    public String getNextHop() {
        return nextHop;
    }

    public int getNumeroHop() {
        return numeroHop;
    }
}
