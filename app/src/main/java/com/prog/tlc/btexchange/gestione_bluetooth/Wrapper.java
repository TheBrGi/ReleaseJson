package com.prog.tlc.btexchange.gestione_bluetooth;

import android.util.Log;

import com.prog.tlc.btexchange.protocollo.Messaggio;
import com.prog.tlc.btexchange.protocollo.NeighborGreeting;
import com.prog.tlc.btexchange.protocollo.RouteError;
import com.prog.tlc.btexchange.protocollo.RouteReply;
import com.prog.tlc.btexchange.protocollo.RouteRequest;

/**
 * Created by BrGi on 03/05/2016.
 */

public class Wrapper {
    private Messaggio mess;
    private NeighborGreeting neigh;
    private RouteError rerr;
    private RouteReply rrep;
    private RouteRequest rreq;
    private String type;

    public Wrapper(Object obj) {
        if (obj instanceof NeighborGreeting) {
            neigh = (NeighborGreeting) obj;
            type = "neigh";
        } else if (obj instanceof RouteReply) {
            rrep = (RouteReply) obj;
            type = "rrep";
        } else if (obj instanceof RouteRequest) {
            rreq = (RouteRequest) obj;
            type = "rreq";
        } else if (obj instanceof Messaggio) {
            mess = (Messaggio) obj;
            type = "mess";
        } else if (obj instanceof RouteError) {
            rerr = (RouteError) obj;
            type = "rerr";
        }
    }

    public Object getContent() {
        Object obj = null;
        if (type == null){
            Log.e("getContent","null");
            return obj;}
        if (type.equals("neigh")) {
            obj = neigh;
        } else if (type.equals("rrep")) {
            obj = rrep;
        } else if (type.equals("rreq")) {
            obj = rreq;
        } else if (type.equals("mess")) {
            obj = mess;
        } else if (type.equals("rerr")) {
            obj = rerr;
        }
        Log.d("getContent",type);
        return obj;
    }
}
