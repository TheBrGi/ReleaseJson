package com.prog.tlc.btexchange.gestioneDispositivo;

import com.prog.tlc.btexchange.gestione_bluetooth.BtUtil;
import com.prog.tlc.btexchange.protocollo.RouteRequest;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Domenico on 10/03/2016.
 */
public class Dispositivo {
    private List<Node> listaNodi;
    private ConcurrentHashMap<String, Percorso> tabellaDiRouting;
    private ConcurrentHashMap<String, Long> RREQRicevuti;
    private String nome, MACAddress;
    private long sequenceNumber; //si incrementa dopo una req o una reply



    public Dispositivo(String n) {
        nome = n;
        MACAddress = BtUtil.getMACMioDispositivo();
        sequenceNumber = Calendar.getInstance().getTimeInMillis();
        listaNodi = Collections.synchronizedList(new LinkedList<Node>());
        listaNodi.add(new Node(n, MACAddress));
        tabellaDiRouting = new ConcurrentHashMap<>();
        RREQRicevuti = new ConcurrentHashMap<>();
    }

    public List<Node> getListaNodi() {
        return listaNodi;
    }

    public String getNome() {
        return nome;
    }

    public ConcurrentHashMap<String, Percorso> getTabellaDiRouting() {
        return tabellaDiRouting;
    }

    public String getMACAddress() {
        return MACAddress;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void incrementaSeqNum() {
        sequenceNumber = Calendar.getInstance().getTimeInMillis(); ;
    }

    public void aggiungiNodo(Node n) {
        synchronized (listaNodi) {
            listaNodi.add(n);
        }
    }

    public void rimuoviNodo(Node n) {
        synchronized (listaNodi) {
            listaNodi.remove(n);
        }
    }

    public void aggiungiPercorso(Percorso p) { //aggiunge il percorso
        String destinazione = p.getDestinazione();
        tabellaDiRouting.put(destinazione, p);
    }

    public boolean esistePercorso(String destinazione) {
        return tabellaDiRouting.containsKey(destinazione);
    }

    public void rimuoviPercorso(String destinazione) {
        if(esistePercorso(destinazione))
            tabellaDiRouting.remove(destinazione);
    }

    public Percorso getPercorso(String destinazione) {
        if (esistePercorso(destinazione))
            return tabellaDiRouting.get(destinazione);
        return null;
    }

    public ConcurrentHashMap<String, Long> getRREQRicevuti() {
        return RREQRicevuti;
    }

    public void aggiungiRREQ(RouteRequest rr) {
        RREQRicevuti.put(rr.getSource_addr(), rr.getSource_sequence_number());
    }


}
