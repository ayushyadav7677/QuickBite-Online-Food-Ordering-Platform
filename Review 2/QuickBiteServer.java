import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class QuickBiteServer {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/placeOrder", new Handler());
        server.start();
        System.out.println("🔥 NEW SERVER WITH NAME MAPPING RUNNING 🔥");
    }

    static class Handler implements HttpHandler {

        public void handle(HttpExchange ex) throws IOException {

            BufferedReader br = new BufferedReader(
                new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8));
            String body = br.readLine();

            Map<String,String> data = new HashMap<>();
            for (String p : body.split("&")) {
                String[] kv = p.split("=");
                data.put(URLDecoder.decode(kv[0],"UTF-8"),
                         URLDecoder.decode(kv[1],"UTF-8"));
            }

            String name = data.get("customerName");
            String phone = data.get("phone");

            try {
                Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/quickbite_db","root","magsafe1");

                PreparedStatement foodStmt =
                    con.prepareStatement("SELECT name, price FROM food_items WHERE id=?");

                StringBuilder items = new StringBuilder();
                double total = 0;

                for (String key : data.keySet()) {
                    if (key.startsWith("qty_")) {
                        int qty = Integer.parseInt(data.get(key));
                        if (qty <= 0) continue;

                        int id = Integer.parseInt(key.split("_")[1]);
                        foodStmt.setInt(1, id);

                        ResultSet rs = foodStmt.executeQuery();
                        if (rs.next()) {
                            String food = rs.getString("name");
                            double price = rs.getDouble("price");

                            items.append(food).append(" x").append(qty).append(", ");
                            total += qty * price;
                        }
                        rs.close();
                    }
                }

                String finalItems = items.substring(0, items.length()-2);

                PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO orders(customer_name,phone,items,total_amount) VALUES(?,?,?,?)");

                ps.setString(1,name);
                ps.setString(2,phone);
                ps.setString(3,finalItems);
                ps.setDouble(4,total);

                ps.executeUpdate();
                con.close();

                ex.sendResponseHeaders(200,2);
                ex.getResponseBody().write("OK".getBytes());
                ex.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}