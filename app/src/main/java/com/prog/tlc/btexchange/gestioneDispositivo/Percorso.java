package com.prog.tlc.btexchange.gestioneDispositivo;

import java.io.Serializable;

/**
 * Created by Domenico on 10/03/2016.
 */
//RIGA NELLA TABELLA DI ROUTING(record in hasmap di percorsi)
public class Percorso implements Serializable{
    private String destinazione, nextHop; //MACaddress di destinazione e nexthop
    private int numeroHop,sequenceNumber;

    public Percorso(String dest, String nextH, int nHop,int sequenceNumber){
        destinazione = dest;
        nextHop = nextH;
        numeroHop = nHop;
        this.sequenceNumber = sequenceNumber;
    }
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sq) {
        sequenceNumber = sq;
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
