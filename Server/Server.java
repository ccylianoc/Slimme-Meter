import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;

// class om data op te slaan wordt gebruikt door de server handlers
class SensorData {
    public static String voltage = "-";
    public static String resistance = "-";
}


interface HTTPHandler extends HttpHandler{
    public void handle(HttpExchange httpExchange) throws IOException;
}

abstract class MyHandler implements HTTPHandler{
    boolean checkMethod(HttpExchange httpExchange, String method) throws IOException {
        if (httpExchange.getRequestMethod().equals(method)) {
            return true;
        }
        httpExchange.sendResponseHeaders(405, -1); // Method not allowed
            return false;
    }
    void sendResponse(HttpExchange httpExchange, String response) throws IOException {
        Headers headers = httpExchange.getResponseHeaders();
        headers.add("Content-type", "text/plain");
        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}

class GETHandler extends MyHandler{
    public void handle(HttpExchange httpExchange) throws IOException{
        if (checkMethod(httpExchange, "GET")) { 
              sendResponse(httpExchange, "It's the most wonderful time of the year!");
        }
    }
}

class POSTHandler extends MyHandler {
    public void handle(HttpExchange httpExchange) throws IOException {
        if (!checkMethod(httpExchange, "POST")) return;

        InputStream is = httpExchange.getRequestBody();
        String payload = new String(is.readAllBytes());
        is.close();

        System.out.println("Ontvangen JSON: " + payload);

        // Simpel JSON parsen (voor school voldoende)
        payload = payload.replace("{", "").replace("}", "").replace("\"", "");
        String[] parts = payload.split(",");

        for (String part : parts) {
            String[] keyValue = part.split(":");
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();

            if (key.equals("voltage")) {
                SensorData.voltage = value;
            }
            if (key.equals("weerstand attiny")) {
                SensorData.resistance = value;
            }
        }

        sendResponse(httpExchange, "JSON ontvangen");
    }
}

// HTML pagina handler, toont sensor data op localhost:8000
// auto refresh elke 2 sec zodat je data up to date blijft zien
// stuurt reponse code als iets goed gaat code:200
class WebPageHandler extends MyHandler {
    public void handle(HttpExchange httpExchange) throws IOException {
        if (!checkMethod(httpExchange, "GET")) return;

        String html =
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
                "<meta charset='UTF-8'>" +
                "<title>ESP Dashboard</title>" +
                "<meta http-equiv='refresh' content='2'>" +
            "</head>" +
            "<body>" +
                "<h1>ESP Sensor Dashboard</h1>" +
                "<p><strong>Voltage:</strong> " + SensorData.voltage + " V</p>" +
                "<p><strong>Weerstand ATtiny:</strong> " + SensorData.resistance + " Ohm</p>" +
            "</body>" +
            "</html>";

        Headers headers = httpExchange.getResponseHeaders();
        headers.add("Content-Type", "text/html");
        httpExchange.sendResponseHeaders(200, html.length());

        OutputStream os = httpExchange.getResponseBody();
        os.write(html.getBytes());
        os.close();
    }
}



class DataBase{
    public void BuildBase(){
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:libs/SQLite.db");
            System.out.println("Verbinding gemaakt!");
            
            Statement stmt = conn.createStatement();

            String dropTableATTiny = "DROP TABLE IF EXISTS ATTiny85"; //clean start
            stmt.execute(dropTableATTiny);

            String dropTableLDR = "DROP TABLE IF EXISTS LDR"; //clean start
            stmt.execute(dropTableLDR);

            String createTableATTiny = "CREATE TABLE IF NOT EXISTS ATTiny85 (" +
                                 "id INTEGER PRIMARY KEY," +
                                 "weerstand FLOAT NOT NULL," +
                                 "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                 ")";
            stmt.execute(createTableATTiny);

            String createTableLDR = "CREATE TABLE IF NOT EXISTS LDR (" +
                                 "id INTEGER PRIMARY KEY," +
                                 "voltage FLOAT NOT NULL," +
                                 "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                 ")";
            stmt.execute(createTableLDR);

            String InsertDataATTiny = "INSERT INTO ATTiny85 (weerstand) VALUES (" + SensorData.resistance + ")";
            stmt.execute(InsertDataATTiny);

            String InsertDataLDR = "INSERT INTO LDR (voltage) VALUES (" + SensorData.voltage + ")";
            stmt.execute(InsertDataLDR);
            
            
        } catch (SQLException e) {
            System.out.println("Fout bij verbinden: " + e.getMessage());
        }
    }
}

class InsertData{
    public void InputData(){
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:libs/SQLite.db");
            Statement stmt = conn.createStatement();
            if(SensorData.resistance.equals("-") ||SensorData.voltage.equals("-") ){
                return;
            }

            stmt.execute(
                "INSERT INTO ATTiny85 (weerstand) VALUES (" + SensorData.resistance + ")"
            );

            stmt.execute(
                "INSERT INTO LDR (voltage) VALUES (" + SensorData.voltage + ")"
            );

            System.out.println("Nieuwe data in database");

        } catch (SQLException e) {
            System.out.println("Fout bij verbinden: " + e.getMessage());
        }
    }
}


public class Server {
    public static void main(String[] args) {

        DataBase dbbuild = new DataBase();
        dbbuild.BuildBase();

        InsertData db = new InsertData();

        InetAddress localhost;
        try {
            localhost = InetAddress.getLocalHost();
        }
        catch (UnknownHostException e) {
            return;
        }
        System.out.println("Local address: " + localhost);
        try {
            // maak een nieuwe server aan op localhost poort 8000
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/", new WebPageHandler()); // koppel toegangspunten aan handlers
            server.createContext("/get_data", new GETHandler()); // koppel toegangspunten aan handlers
            server.createContext("/post_data", new POSTHandler());
            server.start(); // start de server
        }
        catch (IOException e) {
            System.err.println("IOException: " + e.getMessage()); // print foutboodschap
        }

        new Thread(() -> {
            while (true){
                db.InputData();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }     
}

// RUN bestand met : java -cp ".;libs\sqlite-jdbc-3.51.1.0.jar" Server