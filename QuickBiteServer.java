import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class QuickBiteServer {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/placeOrder", new PlaceOrderHandler());
        server.setExecutor(null);
        System.out.println("QuickBite server running at http://localhost:8080");
        server.start();
    }

    // ----------------- HTTP Handler -----------------
    static class PlaceOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Only POST allowed");
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = parseForm(body);

            String name = params.getOrDefault("customerName", "Guest");
            String phone = params.getOrDefault("phone", "");

            int q1 = parseInt(params.get("qty_1"));
            int q2 = parseInt(params.get("qty_2"));
            int q3 = parseInt(params.get("qty_3"));
            int q4 = parseInt(params.get("qty_4"));

            if (q1 + q2 + q3 + q4 == 0) {
                sendResponse(exchange, 200, "<h2>No items selected.</h2>");
                return;
            }

            try (Connection con = DatabaseConnection.getConnection()) {
                if (con == null) {
                    sendResponse(exchange, 500, "<h2>Database connection failed.</h2>");
                    return;
                }

                // prices from DB (food_items)
                Map<Integer, Double> priceMap = new HashMap<>();
                Map<Integer, String> nameMap = new HashMap<>();

                String priceQuery = "SELECT id, name, price FROM food_items";
                try (Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery(priceQuery)) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        nameMap.put(id, rs.getString("name"));
                        priceMap.put(id, rs.getDouble("price"));
                    }
                }

                con.setAutoCommit(false);

                double total = 0.0;
                StringBuilder itemsText = new StringBuilder();

                total += addItemLine(itemsText, 1, q1, nameMap, priceMap);
                total += addItemLine(itemsText, 2, q2, nameMap, priceMap);
                total += addItemLine(itemsText, 3, q3, nameMap, priceMap);
                total += addItemLine(itemsText, 4, q4, nameMap, priceMap);

                if (itemsText.length() == 0) {
                    sendResponse(exchange, 200, "<h2>No valid items selected.</h2>");
                    return;
                }

                // insert into orders table
                String insertSql =
                        "INSERT INTO orders (customer_name, phone, items, total_amount) VALUES (?, ?, ?, ?)";
                int orderId;

                try (PreparedStatement ps = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setString(2, phone);
                    ps.setString(3, itemsText.toString());
                    ps.setDouble(4, total);
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            orderId = keys.getInt(1);
                        } else {
                            con.rollback();
                            sendResponse(exchange, 500, "<h2>Failed to get order id.</h2>");
                            return;
                        }
                    }
                }

                con.commit();

                String html = "<html><body style='font-family:Arial;background:#111;color:#f4f4f4;'>"
                        + "<div style='max-width:500px;margin:40px auto;padding:20px;background:#1c1c1c;border-radius:8px;'>"
                        + "<h2>Order Placed Successfully ✅</h2>"
                        + "<p><b>Order ID:</b> " + orderId + "</p>"
                        + "<p><b>Name:</b> " + escape(name) + "</p>"
                        + "<p><b>Phone:</b> " + escape(phone) + "</p>"
                        + "<p><b>Items:</b> " + escape(itemsText.toString()) + "</p>"
                        + "<p><b>Total Amount:</b> Rs. " + total + "</p>"
                        + "<a href='javascript:history.back()' style='color:#ff6b00;'>Back to Order Page</a>"
                        + "</div></body></html>";

                sendResponse(exchange, 200, html);

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "<h2>Server error while placing order.</h2>");
            }
        }

        private static int parseInt(String s) {
            try {
                if (s == null || s.isEmpty()) return 0;
                return Integer.parseInt(s);
            } catch (Exception e) {
                return 0;
            }
        }

        private static double addItemLine(StringBuilder sb, int id, int qty,
                                          Map<Integer, String> nameMap,
                                          Map<Integer, Double> priceMap) {
            if (qty <= 0) return 0.0;
            String itemName = nameMap.get(id);
            Double price = priceMap.get(id);
            if (itemName == null || price == null) return 0.0;

            if (sb.length() > 0) sb.append(", ");
            sb.append(itemName).append(" x").append(qty);

            return price * qty;
        }

        private static Map<String, String> parseForm(String body) {
            Map<String, String> map = new HashMap<>();
            String[] pairs = body.split("&");
            for (String p : pairs) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2) {
                    String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                    String val = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    map.put(key, val);
                }
            }
            return map;
        }

        private static void sendResponse(HttpExchange ex, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            ex.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }

        private static String escape(String s) {
            return s == null ? "" : s.replace("<", "&lt;").replace(">", "&gt;");
        }
    }

    // --------------- Database Connection ---------------
    static class DatabaseConnection {

        private static final String URL = "jdbc:mysql://localhost:3306/quickbite_db";
        private static final String USER = "root";        // your MySQL username
        private static final String PASSWORD = "magsafe1"; // your MySQL password

        public static Connection getConnection() {
            try {
                return DriverManager.getConnection(URL, USER, PASSWORD);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}