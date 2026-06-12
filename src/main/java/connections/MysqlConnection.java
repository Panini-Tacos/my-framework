package main.java.connections;

import java.sql.Connection;
import java.sql.DriverManager;

public class MysqlConnection {
    public static Connection getCon() throws Exception {
        String url = "jdbc:mysql://localhost:3306/banque";
        String user = "root";
        String password = "root";
        Connection c = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            c = DriverManager.getConnection(url, user, password);
            // System.out.println("Connecté à MySQL avec succès !");
        } catch (Exception e) {
            System.out.println("Échec de la connexion MySQL !");
            throw e;
        }
        return c;
    }
}
