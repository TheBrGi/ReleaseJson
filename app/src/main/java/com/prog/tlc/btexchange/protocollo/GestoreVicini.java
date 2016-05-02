package com.prog.tlc.btexchange.protocollo;

import com.prog.tlc.btexchange.gestioneDispositivo.*;
import com.prog.tlc.btexchange.gestione_bluetooth.BtUtil;
import com.prog.tlc.btexchange.gestione_bluetooth.Contatore;

import java.util.List;

/**
 * Created by Domenico on 15/03/2016.
 */
public class GestoreVicini extends Thread {
    /*invia greetings ai vicini e gestisce la ricezione,
    aspetta per un periodo fissato T e ripete le operazioni ciclicamente
    [modifica la lista dei nodi del dispositivo]
     */

    private Dispositivo myDisp;
    private final long ATTESA;
    private List<Node> vicini;

    public GestoreVicini(Dispositivo d, long tempoAttesa) {

        myDisp = d;
        ATTESA = tempoAttesa;
        new Ascoltatore().start();
    }

    public List<Node> getVicini() {
        return vicini;
    }

    public void run() { //invia greetings ai vicini (temporizzato)
        while (true) {
            vicini = BtUtil.cercaVicini();
            NeighborGreeting ng = new NeighborGreeting(myDisp.getMACAddress(), myDisp.getListaNodi());
            for(int i=0; i<3; i++) {
                for (Node vicino : vicini) {
                    BtUtil.inviaGreeting(ng, vicino.getMACAddress());

                }
                try {
                    this.sleep(ATTESA);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class Ascoltatore extends Thread {
        public void run() {
            while (true) {
                List<Node> nuoveInfo = BtUtil.riceviGreeting().getNodiNoti();
                mergeNodi(nuoveInfo); /*fondende gli elementi della lista
                                        myDisp insieme ai nodi della lista nuoveInfo*/
            }
        }

        private void mergeNodi(List <Node> nuoveInfo) {
            for (Node n : nuoveInfo) {
                if (!(myDisp.getListaNodi().contains(n))) {
                    myDisp.aggiungiNodo(n);
                }
            }
        }
    }
}
