package main.java.models;

public class Mouvement {

    private int id_mouvement;
    private String num_compte;
    private String daty;
    private String typany;
    private double montant;

    public Mouvement(int id_mouvement, String num_compte, String daty, String typany, double montant) {
        this.id_mouvement = id_mouvement;
        this.num_compte = num_compte;
        this.daty = daty;
        this.typany = typany;
        this.montant = montant;
    }

    public int getId_mouvement() {
        return id_mouvement;
    }

    public void setId_mouvement(int id_mouvement) {
        this.id_mouvement = id_mouvement;
    }

    public String getNum_compte() {
        return num_compte;
    }

    public void setNum_compte(String num_compte) {
        this.num_compte = num_compte;
    }

    public String getDaty() {
        return daty;
    }

    public void setDaty(String daty) {
        this.daty = daty;
    }

    public String getTypany() {
        return typany;
    }

    public void setTypany(String typany) {
        this.typany = typany;
    }

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

}
