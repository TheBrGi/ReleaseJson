package com.prog.tlc.btexchange.gestione_bluetooth;

import com.prog.tlc.btexchange.protocollo.RouteError;
import com.prog.tlc.btexchange.protocollo.RouteReply;
import com.prog.tlc.btexchange.protocollo.RouteRequest;

import java.io.Serializable;

/**
 * Created by vincenzo on 29/04/2016.
 */
public class Contatore implements Serializable {

    private int num_RREQ, num_RREP, num_Mess,num_RERR, num_Greet;



    public synchronized void  incrNum_RREQ() {
         num_RREQ++;
    }

    public synchronized void  incrNum_RREP() {
        num_RREP++;
    }

    public synchronized void incrNum_RERR() {
        num_RERR++;
    }

    public synchronized void incrNum_Greet() {
        num_Greet++;
    }
    public synchronized void incrNum_Mess() {
        num_Mess++;
    }



    public int getNum_RREQ() {
        return num_RREQ;
    }

    public int getNum_RREP() {
        return num_RREP;
    }

    public int getNum_Mess() {
        return num_Mess;
    }

    public int getNum_Greet() {
        return num_Greet;
    }

    public int getNum_RERR(){
        return num_RERR;
    }
}
