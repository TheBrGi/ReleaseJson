package com.prog.tlc.btexchange.protocollo;

import android.util.Log;

import com.prog.tlc.btexchange.gestioneDispositivo.*;
import com.prog.tlc.btexchange.gestione_bluetooth.BtUtil;

import java.util.List;

/**
 * Created by Domenico on 15/03/2016.
 */
public class AODV {
    private Dispositivo myDev;
    private GestoreVicini gestoreVicini;

    /*dentro il costruttore, passo passo, svolgiamo le attività protocollari, appoggiandoci
    ad altri metodi privati della classe*/
    public AODV(Dispositivo d, long tempoAttesaAggVicini) {
        myDev = d;
        gestoreVicini = new GestoreVicini(d, tempoAttesaAggVicini);
        gestoreVicini.start();
        //new HandlerReq().start();
        //new HandlerReply().start();
    }


    public Percorso cercaPercorso(String dest) { //stiamo già assumendo che non ci sia un percorso valido per dest
        int destSeqNumber = 0, hopCount = 0;
        RouteRequest req = new RouteRequest(myDev.getMACAddress(), myDev.getSequenceNumber(), dest, destSeqNumber, hopCount, myDev.getMACAddress());
        List<Node> vicini = gestoreVicini.getVicini();
        for (Node vicino : vicini) {
            BtUtil.inviaRREQ(req, vicino.getMACAddress());
        }
        myDev.incrementaSeqNum();
        //ora ci mettiamo in attesa della reply
        for (int i = 0; i < 20; i++) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (myDev.esistePercorso(dest))
                return myDev.getPercorso(dest);
        }
        return null; //se ritorna null l'utente deve avvisare che non c'è un percorso per la destinazione
    }

    public void inviaMessaggio(String MACdestinazione, String contenuto, String nomeDest) {
        Percorso p=null;
        if(!myDev.esistePercorso(MACdestinazione))
            p = cercaPercorso(MACdestinazione);
        else{
            p=myDev.getPercorso(MACdestinazione);
        }
        if(p==null) {
            //TODO stampare che non si è trovato un percorso
        }
        else {
            Messaggio m = new Messaggio(contenuto,new Node(nomeDest,MACdestinazione));
            BtUtil.inviaMess(m,p.getNextHop());
        }
    }

    private class HandlerReq extends Thread {
        public void run() {
            while (true) {
                RouteRequest rr = BtUtil.riceviRichiesta();
                if (!myDev.getRREQRicevuti().containsKey(rr.getSource_addr())) {
                    gestisciRREQ(rr);
                    myDev.aggiungiRREQ(rr);
                } else { // potrebbe andare male
                    int sequenceNumberSource = myDev.getRREQRicevuti().get(rr.getSource_addr());
                    if (sequenceNumberSource < rr.getSource_sequence_number()) {
                        gestisciRREQ(rr);
                        myDev.aggiungiRREQ(rr);
                    }
                }
            }
        }

        private void gestisciRREQ(RouteRequest rr) {
            estrapolaPercorsoRREQ(rr);
            if (myDev.getMACAddress().equals(rr.getDest_addr()))  //se siamo noi la destinazione
                reply(rr);
            else if (myDev.esistePercorso(rr.getDest_addr())) //se conosciamo un percorso fino alla destinazione lo inviamo
                reply(rr, myDev.getPercorso(rr.getSource_addr()));
            else
                rilanciaRREQ(rr);
        }

        private void estrapolaPercorsoRREQ(RouteRequest rr) {
            Percorso p = new Percorso(rr.getSource_addr(), rr.getLast_sender(), rr.getHop_cnt(), rr.getSource_sequence_number());
            myDev.aggiungiPercorso(p);
        }

        private void rilanciaRREQ(RouteRequest rr) {
            List<Node> vicini = gestoreVicini.getVicini();
            RouteRequest nuovoRR = new RouteRequest(rr.getSource_addr(), rr.getSource_sequence_number(), rr.getDest_addr(), rr.getDest_sequence_number(), rr.getHop_cnt(), rr.getLast_sender());
            nuovoRR.incrementaHop_cnt();
            nuovoRR.setLast_sender(myDev.getMACAddress());
            for (Node n : vicini) {
                if (!rr.getLast_sender().equals(n)) {
                    BtUtil.inviaRREQ(rr, n.getMACAddress());
                }
            }
        }

        private void reply(RouteRequest rr, Percorso p) { //il source sarà sempre tale sia in un verso che nell'altro
            int seqDest = p.getSequenceNumber();
            int numHopDaQuiADest = p.getNumeroHop();
            RouteReply routeRep = new RouteReply(rr.getSource_addr(), rr.getDest_addr(), seqDest, numHopDaQuiADest, myDev.getMACAddress());
            myDev.incrementaSeqNum();
            BtUtil.inviaRREP(routeRep, rr.getLast_sender());
        }

        private void reply(RouteRequest rr) { //l'hop count è sicuramente 1 in questo momento, poi (probablilmente) verrà incrementato
            RouteReply routeRep = new RouteReply(rr.getSource_addr(), rr.getDest_addr(), myDev.getSequenceNumber(), 1, myDev.getMACAddress());
            myDev.incrementaSeqNum();
            BtUtil.inviaRREP(routeRep, rr.getLast_sender());
        }
    }


    private class HandlerReply extends Thread {
        public void run() {
            while (true) {
                RouteReply rr = BtUtil.riceviRisposta();
                estrapolaPercorsoRREP(rr);
                if (!rr.getSource_addr().equals(myDev.getMACAddress())) { //non siamo noi la sorgente
                    rr.incrementaHop_cnt();
                    rr.setLast_sender(myDev.getMACAddress());
                    rilanciaReply(rr);
                }
            }
        }

        private void estrapolaPercorsoRREP(RouteReply rr) { //il source sarà sempre tale sia in un verso che nell'altro
            Percorso p = new Percorso(rr.getDest_addr(), rr.getLast_sender(), rr.getHop_cnt(), rr.getDest_sequence_number());
            myDev.aggiungiPercorso(p);
        }

        private void rilanciaReply(RouteReply rr) {
            String MACNextHop = myDev.getPercorso(rr.getSource_addr()).getNextHop();
            myDev.incrementaSeqNum();
            BtUtil.inviaRREP(rr, MACNextHop);
        }
    }

    private class HandlerMex extends Thread {
        public void run() {
            while (true) {
                Messaggio mess = BtUtil.riceviMessaggio();
                if(mess.getDest().getMACAddress().equals(myDev.getMACAddress())) {
                    BtUtil.mostraMess(mess.getMex());
                }
                else {
                    rilanciaMess(mess);
                }

            }

        }

        private void rilanciaMess(Messaggio mess) {
            String dest = mess.getDest().getMACAddress();
            Percorso p = myDev.getPercorso(dest);
            if(p!=null) {
                BtUtil.inviaMess(mess, p.getNextHop());//manda al nodo successivo
                Log.d("invio al nodo succ", dest);
            }
            else {
                Log.d("Non esite il percorso", dest);
            }
        }
    }


}
