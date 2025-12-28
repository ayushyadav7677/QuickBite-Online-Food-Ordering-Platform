import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MainApp {

    public static void main(String[] args) {

        FoodItemDAO dao = new FoodItemDAO();

        System.out.println("Fetching food items from database...\n");

        List<FoodItem> items = dao.getAllFoodItems();

        if (items == null || items.isEmpty()) {
            System.out.println("No food items found in database.");
        } else {
            for (FoodItem item : items) {
                System.out.println(item);
            }
        }

        System.out.println("\nQuickBite backend test completed.");
    }
}

// ----------------- FoodItem class (Model / OOP) -----------------
class FoodItem {
    private int id;
    private String name;
    private double price;

    public FoodItem(int id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return id + " - " + name + " : Rs. " + price;
    }
}

// ----------------- DatabaseConnection class (JDBC) -----------------
class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/quickbite_db";
    private static final String USER = "root";       // your MySQL username
    private static final String PASSWORD = "magsafe1";   // your MySQL password

    public static Connection getConnection() {
        try {
            Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connected successfully.");
            return con;
        } catch (Exception e) {
            System.out.println("Failed to connect to database.");
            e.printStackTrace();
            return null;
        }
    }
}

// ----------------- FoodItemDAO class (DB Operations) -----------------
class FoodItemDAO {

    public List<FoodItem> getAllFoodItems() {
        List<FoodItem> items = new ArrayList<FoodItem>();

        String query = "SELECT id, name, price FROM food_items";

        try (
            Connection con = DatabaseConnection.getConnection();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(query)
        ) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");

                FoodItem item = new FoodItem(id, name, price);
                items.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }
}