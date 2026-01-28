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

class GETHandler extends MyHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        if (!checkMethod(httpExchange, "GET")) return;

        String json =
            "{" +
            "\"voltage\": \"" + SensorData.voltage + "\"," +
            "\"resistance\": \"" + SensorData.resistance + "\"" +
            "}";

        Headers headers = httpExchange.getResponseHeaders();
        headers.add("Content-Type", "application/json");

        httpExchange.sendResponseHeaders(200, json.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(json.getBytes());
        os.close();
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
                "<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>" +
                "<style>" +
                    "body { font-family: Arial; padding: 20px; }" +
                    "canvas { max-width: 800px; }" +
                "</style>" +
            "</head>" +
            "<body>" +
                "<h1>Slimme Meter - Dashboard</h1>" +
                "<p><strong>Voltage (LDR):</strong> <span id='voltageVal'>" + SensorData.voltage + "</span> V</p>" +
                "<p><strong>Weerstand (Potmeter):</strong> <span id='resistanceVal'>" + SensorData.resistance + "</span> Ohm</p>" +
                "<canvas id='sensorChart'></canvas>" +

                "<script>" +
                "const ctx = document.getElementById('sensorChart').getContext('2d');" +
                "const data = {" +
                    "labels: []," +
                    "datasets: [" +
                        "{ label: 'Voltage (V)', borderColor: 'blue', backgroundColor: 'rgba(0,0,255,0.2)', data: [] }," +
                        "{ label: 'Resistance (Ohm)', borderColor: 'red', backgroundColor: 'rgba(255,0,0,0.2)', data: [] }" +
                    "]" +
                "};" +

                "const config = { type: 'line', data: data, options: { responsive: true, animation: false, scales: { y: { beginAtZero: true } } } };" +
                "const myChart = new Chart(ctx, config);" +

                "function updateChart() {" +
                    "fetch('/get_data')" +
                    ".then(response => response.json())" +
                    ".then(json => {" +
                        "const now = new Date().toLocaleTimeString();" +
                        "data.labels.push(now);" +
                        "data.datasets[0].data.push(parseFloat(json.voltage));" +
                        "data.datasets[1].data.push(parseFloat(json.resistance));" +
                        "if(data.labels.length > 20) { data.labels.shift(); data.datasets[0].data.shift(); data.datasets[1].data.shift(); }" +
                        "document.getElementById('voltageVal').textContent = json.voltage;" +
                        "document.getElementById('resistanceVal').textContent = json.resistance;" +
                        "myChart.update();" +
                    "});" +
                "}" +

                "setInterval(updateChart, 2000);" +
                "</script>" +
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

            // maakt een tabel aan voor de attiny inputs met waarde en tijd
            String createTableATTiny = "CREATE TABLE IF NOT EXISTS ATTiny85 (" +
                                 "id INTEGER PRIMARY KEY," +
                                 "weerstand FLOAT NOT NULL," +
                                 "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                 ")";
            stmt.execute(createTableATTiny);

            // maakt een tabel aan voor de LDR inputs met waarde en tijd
            String createTableLDR = "CREATE TABLE IF NOT EXISTS LDR (" +
                                 "id INTEGER PRIMARY KEY," +
                                 "voltage FLOAT NOT NULL," +
                                 "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                 ")";
            stmt.execute(createTableLDR);

            // insert meteen de eerste waarde
            String InsertDataATTiny = "INSERT INTO ATTiny85 (weerstand) VALUES (" + SensorData.resistance + ")";
            stmt.execute(InsertDataATTiny);
            // insert meteen de eerste waarde
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
            // Voorkomt dat er lege data in de database wordt gezet en de database vol zet met onbruikbare data 
            // (je kan ook hierdoor pauzes in de data zien en tijd wanneer er iets fout is gegaan)
            if(SensorData.resistance.equals("-")){ 
                return;
            }else{

            // Inserts sturen dezelfde data zowel naar de webpagina als database
                stmt.execute(
                    "INSERT INTO ATTiny85 (weerstand) VALUES (" + SensorData.resistance + ")"
                );
            }
            if(SensorData.voltage.equals("-") ){
                return;
            }else{
                stmt.execute(
                    "INSERT INTO LDR (voltage) VALUES (" + SensorData.voltage + ")"
                );
            }

        } catch (SQLException e) {
            System.out.println("Fout bij verbinden: " + e.getMessage());    //geeft foutmelding in terminal
        }
    }
}

// Hoofdklasse maakt gebruik van alle handlers voor de server en InputData en DataBase klassen voor database interactie

public class Server {
    public static void main(String[] args) {

        DataBase dbbuild = new DataBase();  // maak een object aan van de klasse DataBase om te runnen
        dbbuild.BuildBase();               // bouw de database en tabellen op (verwijder oude tabellen eerst)

        InsertData db = new InsertData(); // maakt een object aan van klasse InsetData om in Thread te gebruiken voor elke 2 sec nieuwe data

        InetAddress localhost; 
        try {
            localhost = InetAddress.getLocalHost();
        }
        catch (UnknownHostException e) {
            return;
        }
        System.out.println("Local address: " + localhost);
        try {
            // maakt een nieuwe server aan op localhost poort 8000
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/", new WebPageHandler()); // koppelt toegangspunten aan handlers
            server.createContext("/get_data", new GETHandler()); // koppelt toegangspunten aan handlers
            server.createContext("/post_data", new POSTHandler());  // koppelt toegangspunten aan handlers
            server.start(); // start de server
        }
        catch (IOException e) {
            System.err.println("IOException: " + e.getMessage()); // print foutboodschap
        }

        // Gebruik gemaakt van AI om threads uit te leggen zodat ik er een kon maken voor automatische database input
        new Thread(() -> {
            while (true){
                db.InputData(); // runt de InputData methode om nieuwe data te verzenden naar SQLite.db
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }     
}


// command gekregen van AI na problemen .jar bestand toe te voegen als lib
// RUN bestand met : java -cp ".;libs\sqlite-jdbc-3.51.1.0.jar" Server