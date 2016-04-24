package com.prog.tlc.btexchange.gestioneDispositivo;

import java.io.Serializable;

/**
 * Created by Domenico on 10/03/2016.
 */
public class Node implements Serializable {
    private String nome, MACAddress;

    public Node(String n, String MAC) {
        nome = n;
        MACAddress = MAC;
    }

    public String getMACAddress() {
        return MACAddress;
    }
    public String getNome() {
        return nome;
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof Node))
            return false;
        Node n=(Node)o;
        if(this.nome.equals(n.getNome()) && this.MACAddress.equals(n.getMACAddress()))
            return true;
        return false;
    }//equals

    public String toString() {
        return this.getNome()+"\n"+this.getMACAddress();
    }
}
