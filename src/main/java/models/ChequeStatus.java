package main.java.models;

public class ChequeStatus {

    private int id_statut;
    private String statut;

    public ChequeStatus(int id_statut, String statut) {
        this.id_statut = id_statut;
        this.statut = statut;
    }

    public int getId_statut() {
        return id_statut;
    }

    public void setId_statut(int id_statut) {
        this.id_statut = id_statut;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

}
