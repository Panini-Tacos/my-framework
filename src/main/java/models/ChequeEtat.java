package main.java.models;

public class ChequeEtat {

    private int id_etat;
    private int id_cheque;
    private int id_statut;
    private String daty;

    public ChequeEtat(int id_etat, int id_cheque, int id_statut, String daty) {
        this.id_etat = id_etat;
        this.id_cheque = id_cheque;
        this.id_statut = id_statut;
        this.daty = daty;
    }

    public int getId_etat() {
        return id_etat;
    }

    public void setId_etat(int id_etat) {
        this.id_etat = id_etat;
    }

    public int getId_cheque() {
        return id_cheque;
    }

    public void setId_cheque(int id_cheque) {
        this.id_cheque = id_cheque;
    }

    public int getId_statut() {
        return id_statut;
    }

    public void setId_statut(int id_statut) {
        this.id_statut = id_statut;
    }

    public String getDaty() {
        return daty;
    }

    public void setDaty(String daty) {
        this.daty = daty;
    }

}
