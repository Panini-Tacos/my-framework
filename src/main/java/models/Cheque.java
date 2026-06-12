package main.java.models;

public class Cheque {

    private int id_cheque;
    private String num_cheque;
    private String num_compte;
    private String daty;

    public Cheque(int id_cheque, String num_cheque, String num_compte, String daty) {
        this.id_cheque = id_cheque;
        this.num_cheque = num_cheque;
        this.num_compte = num_compte;
        this.daty = daty;
    }

    public int getId_cheque() {
        return id_cheque;
    }

    public void setId_cheque(int id_cheque) {
        this.id_cheque = id_cheque;
    }

    public String getNum_cheque() {
        return num_cheque;
    }

    public void setNum_cheque(String num_cheque) {
        this.num_cheque = num_cheque;
    }

    public String getNum_compte() {
        return num_compte;
    }

    public void setNum_compte(String num_compte) {
        this.num_compte = num_compte;
    }

    public String getDate() {
        return daty;
    }

    public void setDate(String daty) {
        this.daty = daty;
    }

}
