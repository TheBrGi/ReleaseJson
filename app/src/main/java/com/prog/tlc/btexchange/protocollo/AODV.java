package com.prog.tlc.btexchange.protocollo;

import android.util.Log;

import com.prog.tlc.btexchange.gestioneDispositivo.*;
import com.prog.tlc.btexchange.gestione_bluetooth.BtUtil;
import com.prog.tlc.btexchange.gestione_bluetooth.Contatore;

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
        new HandlerReq().start();
        new HandlerReply().start();
        new HandlerMex().start();
        new HandlerError().start();
    }

    public Percorso cercaPercorso(String dest) { //stiamo già assumendo che non ci sia un percorso valido per dest
        int destSeqNumber = 0, hopCount = 0;                                                                            //poi conterrà il last sender
        RouteRequest req = new RouteRequest(myDev.getMACAddress(), myDev.getSequenceNumber(), dest, destSeqNumber, hopCount, myDev.getMACAddress());
        Log.d("MACaddr", myDev.getMACAddress());
        Log.d("req sour addr:", req.getSource_addr());
        List<Node> vicini = gestoreVicini.getVicini();
        List<Node> conApp = myDev.getListaNodi();
        for (Node vicino : vicini) {
            if(conApp.contains(vicino))
                BtUtil.inviaRREQ(req, vicino.getMACAddress());
        }

        myDev.incrementaSeqNum();
        //ora ci mettiamo in attesa della repljhy
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (myDev.esistePercorso(dest))
                return myDev.getPercorso(dest);
        }
        return null; //se ritorna null l'utente deve avvisare che non c'è un percorso per la destinazione
    }

    public void inviaMessaggio(String MACdestinazione, String contenuto, String nomeDest) {
        Percorso p = null;
        boolean connessioneOk = false;
        if (!myDev.esistePercorso(MACdestinazione)) {
            Log.d("ricerco il percorso per", nomeDest);
            p = cercaPercorso(MACdestinazione);
        } else {
            p = myDev.getPercorso(MACdestinazione);
        }
        if (p == null) {
            BtUtil.mostraMess("NON si è trovato nessun percorso!");
            //BtUtil.appendLog("NON si è trovato nessun percorso verso"+MACdestinazione);
        } else {
            Log.d("percorso trovato", "42");
            Messaggio m = new Messaggio(contenuto, new Node(nomeDest, MACdestinazione), myDev.getMACAddress(), myDev.getMACAddress());
            Log.d("invio il mex", "5143");
            //BtUtil.appendLog("invio il messaggio a "+nomeDest);
            //mando al next hop fino a dest (eseguito dopo che rep e reply hanno fissato il path)
            BtUtil.inviaMess(m, p.getNextHop());
            connessioneOk = BtUtil.checkSocket(p.getNextHop());
        }
        if (!connessioneOk) {
            myDev.rimuoviPercorso(MACdestinazione);
            String daMostrare = "ERRORE! Il percorso verso " + MACdestinazione + " non è disponibile ";
            BtUtil.mostraMess(daMostrare);
            //BtUtil.appendLog(daMostrare);
        }
    }

    private class HandlerReq extends Thread {
        public void run() {
            while (true) {
                RouteRequest rr = BtUtil.riceviRichiesta();
                String s = "ricevuto REQ da " + rr.getLast_sender();
                BtUtil.mostraMess(s);
                Log.d("rreq ricevuta isNull?:", rr.getSource_addr());
                //flooding controllato
                if (!myDev.getRREQRicevuti().containsKey(rr.getSource_addr())) {
                    gestisciRREQ(rr);
                    myDev.aggiungiRREQ(rr);
                } else { // potrebbe andare male
                    long sequenceNumberSource = myDev.getRREQRicevuti().get(rr.getSource_addr());
                    if (sequenceNumberSource < rr.getSource_sequence_number()) {
                        gestisciRREQ(rr);
                        myDev.aggiungiRREQ(rr);
                    }
                }
            }
        }

        private void gestisciRREQ(RouteRequest rr) {
            Log.d("BtExc in gestisciRREQ", "noi dest?" + String.valueOf(myDev.getMACAddress().equals(rr.getDest_addr())));
            Log.d("BtExc rreq dest addr", rr.getDest_addr());
            estrapolaPercorsoRREQ(rr);
            if (myDev.getMACAddress().equals(rr.getDest_addr()))  //se siamo noi la destinazione
                reply(rr);
            else if (myDev.esistePercorso(rr.getDest_addr())) //se conosciamo un percorso fino alla destinazione lo inviamo
                reply(rr, myDev.getPercorso(rr.getSource_addr()));
            else
                rilanciaRREQ(rr);
        }

        private void estrapolaPercorsoRREQ(RouteRequest rr) {
            Log.d("BtExc", "estrapolaPercorsoRREQ");
            //dest, next hop, hop count, seq number
            Percorso p = new Percorso(rr.getSource_addr(), rr.getLast_sender(), rr.getHop_cnt(), rr.getSource_sequence_number());
            Percorso seEsiste = myDev.getPercorso(rr.getSource_addr());
            if (seEsiste != null) {
                long sn = seEsiste.getSequenceNumber();
                if (sn < p.getSequenceNumber()) {
                    myDev.aggiungiPercorso(p);
                }
            } else
                myDev.aggiungiPercorso(p);
        }

        private void rilanciaRREQ(RouteRequest rr) {
            List<Node> vicini = gestoreVicini.getVicini();
            RouteRequest nuovoRR = new RouteRequest(rr.getSource_addr(), rr.getSource_sequence_number(), rr.getDest_addr(), rr.getDest_sequence_number(), rr.getHop_cnt(), rr.getLast_sender());
            nuovoRR.incrementaHop_cnt();
            nuovoRR.setLast_sender(myDev.getMACAddress());
            for (Node n : vicini) {
                //mando a tutti i vicini tranne al last sender
                if (!rr.getLast_sender().equals(n.getMACAddress())) {
                    BtUtil.inviaRREQ(rr, n.getMACAddress());
                }
            }
        }

        //conosco un percorso verso dest
        private void reply(RouteRequest rr, Percorso p) { //il source sarà sempre tale sia in un verso che nell'altro
            long seqDest = p.getSequenceNumber();
            int numHopDaQuiADest = p.getNumeroHop();
            RouteReply routeRep = new RouteReply(rr.getSource_addr(), rr.getDest_addr(), seqDest, numHopDaQuiADest, myDev.getMACAddress());
            myDev.incrementaSeqNum();
            Log.d("invio reply prec", rr.getSource_addr());
            BtUtil.inviaRREP(routeRep, rr.getLast_sender());
        }

        //siamo noi la destinaione
        private void reply(RouteRequest rr) { //l'hop count è sicuramente 1 in questo momento, poi (probabilmente) verrà incrementato
            RouteReply routeRep = new RouteReply(rr.getSource_addr(), rr.getDest_addr(), myDev.getSequenceNumber(), 1, myDev.getMACAddress());
            myDev.incrementaSeqNum();
            Log.d("invio reply dest", rr.getSource_addr());
            BtUtil.inviaRREP(routeRep, rr.getLast_sender());
        }
    }


    private class HandlerReply extends Thread {
        public void run() {
            while (true) {
                RouteReply rr = BtUtil.riceviRisposta();
                String s = "ricevuto REPLY da " + rr.getLast_sender();
                BtUtil.mostraMess(s);
                estrapolaPercorsoRREP(rr);
                if (!rr.getSource_addr().equals(myDev.getMACAddress())) { //non siamo noi la sorgente, quindi ripropaghiamo per arrivare ad essa
                    rr.incrementaHop_cnt();
                    rr.setLast_sender(myDev.getMACAddress());
                    rilanciaReply(rr);
                }
                //non facciamo niente
            }
        }

        private void estrapolaPercorsoRREP(RouteReply rr) { //il source sarà sempre tale sia in un verso che nell'altro
            Percorso p = new Percorso(rr.getDest_addr(), rr.getLast_sender(), rr.getHop_cnt(), rr.getDest_sequence_number());
            Percorso seEsiste = myDev.getPercorso(rr.getDest_addr());
            if (seEsiste != null) {
                long sn = seEsiste.getSequenceNumber();
                if (sn < p.getSequenceNumber()) {
                    myDev.aggiungiPercorso(p);
                }
            } else
                myDev.aggiungiPercorso(p);
        }

        private void rilanciaReply(RouteReply rr) {
            String MACNextHop = myDev.getPercorso(rr.getSource_addr()).getNextHop();
            myDev.incrementaSeqNum();
            Log.d("rilancia reply verso", MACNextHop);
            BtUtil.inviaRREP(rr, MACNextHop);
        }
    }

    private class HandlerMex extends Thread {
        public void run() {
            while (true) {
                Messaggio mess = BtUtil.riceviMessaggio();
                String s = "ricevuto messaggio per " + mess.getDest();
                BtUtil.mostraMess(s);
                if (mess.getDest().getMACAddress().equals(myDev.getMACAddress())) {
                    BtUtil.mostraMess(mess.getMex());
                    BtUtil.appendLog("ricevuto messaggio da " + mess.getSource()); //TODO vedere se va cancellato
                } else {
                    rilanciaMess(mess);
                }

            }

        }

        private void rilanciaMess(Messaggio mess) {
            String dest = mess.getDest().getMACAddress();
            Percorso p = myDev.getPercorso(dest);
            boolean connessioneOk = false;
            if (p != null) {
                Messaggio m = new Messaggio(mess.getMex(), mess.getDest(), myDev.getMACAddress(), mess.getSource());
                BtUtil.inviaMess(m, p.getNextHop());//manda al nodo successivo
                Log.d("invio al nodo succ", dest);
                connessioneOk = BtUtil.checkSocket(p.getNextHop());
            } else {
                RouteError re = new RouteError(mess.getSource(), mess.getDest().getMACAddress());
                BtUtil.inviaError(re, mess.getLastSender());
                Log.d("Non esite il percorso", dest);
            }
            if (!connessioneOk) {
                RouteError re = new RouteError(mess.getSource(), mess.getDest().getMACAddress());
                BtUtil.inviaError(re, mess.getLastSender());
                Log.d("Non esite il percorso", dest);
            }
        }
    }

    private class HandlerError extends Thread {
        public void run() {
            while (true) {
                RouteError re = BtUtil.riceviError();
                myDev.rimuoviPercorso(re.getDest()); //rimuovo il percorso verso la destinazione
                if (!re.getSource().equals(myDev.getMACAddress())) //se non siamo noi source
                    rilanciaErrore(re);
                else {
                    BtUtil.mostraMess("ERRORE! Il percorso non è più disponibile");
                }
            }
        }

        private void rilanciaErrore(RouteError re) {
            BtUtil.inviaError(re, myDev.getPercorso(re.getSource()).getNextHop());
        }
    }


}
