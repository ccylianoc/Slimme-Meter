import java.sql.*;

public class SQLinJAVA {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:libs/SQLite.db");
            System.out.println("Verbinding gemaakt!");
            
            Statement stmt = conn.createStatement();
            
            String createTable = "CREATE TABLE IF NOT EXISTS ATTiny85 (" +
                                 "id INTEGER PRIMARY KEY," +
                                 "weerstand FLOAT NOT NULL," +
                                 "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                 ")";
            stmt.execute(createTable);
            
            String insertData = "INSERT INTO ATTiny85 (weerstand) VALUES (220.5)";
            stmt.execute(insertData);
            
            stmt.close();
        } catch (SQLException e) {
            System.out.println("Fout bij verbinden: " + e.getMessage());
        }
    }
}




// RUN bestand met : java -cp ".;libs\sqlite-jdbc-3.51.1.0.jar" SQLinJAVA
