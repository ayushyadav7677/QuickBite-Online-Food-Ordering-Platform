import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Model class for food items
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
        return name + " (Rs. " + price + ")";
    }
}

// Database connection helper
class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/quickbite_db";
    private static final String USER = "root";        // your MySQL user
    private static final String PASSWORD = "magsafe1"; // your MySQL password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}

// DAO to load food items from DB
class FoodItemDAO {

    public List<FoodItem> getAllFoodItems() {
        List<FoodItem> items = new ArrayList<>();

        String query = "SELECT id, name, price FROM food_items";

        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");

                items.add(new FoodItem(id, name, price));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }
}

// DAO to insert order into DB
class OrderDAO {

    public int createOrder(String customerName, String phone, String itemsText, double totalAmount) {
        String sql = "INSERT INTO orders (customer_name, phone, items, total_amount) VALUES (?, ?, ?, ?)";
        int orderId = -1;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, customerName);
            ps.setString(2, phone);
            ps.setString(3, itemsText);
            ps.setDouble(4, totalAmount);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    orderId = keys.getInt(1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return orderId;
    }
}

// GUI class
public class QuickBiteGUI extends JFrame {

    private JTextField nameField;
    private JTextField phoneField;
    private JPanel itemsPanel;

    private List<FoodItem> foodItems;
    private List<JSpinner> qtySpinners;

    private FoodItemDAO foodItemDAO;
    private OrderDAO orderDAO;

    public QuickBiteGUI() {
        super("QuickBite - Restaurant Ordering System");

        foodItemDAO = new FoodItemDAO();
        orderDAO = new OrderDAO();
        foodItems = foodItemDAO.getAllFoodItems();
        qtySpinners = new ArrayList<>();

        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 600);
        setLocationRelativeTo(null);

        // Main container
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(Color.DARK_GRAY);
        add(mainPanel);

        // Top section: Customer details
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(4, 1, 5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.setBackground(Color.DARK_GRAY);

        JLabel titleLabel = new JLabel("QuickBite - Place Your Order");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));

        topPanel.add(titleLabel);

        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.setBackground(Color.DARK_GRAY);
        JLabel nameLabel = new JLabel("Customer Name: ");
        nameLabel.setForeground(Color.WHITE);
        nameField = new JTextField();
        namePanel.add(nameLabel, BorderLayout.WEST);
        namePanel.add(nameField, BorderLayout.CENTER);

        JPanel phonePanel = new JPanel(new BorderLayout());
        phonePanel.setBackground(Color.DARK_GRAY);
        JLabel phoneLabel = new JLabel("Phone: ");
        phoneLabel.setForeground(Color.WHITE);
        phoneField = new JTextField();
        phonePanel.add(phoneLabel, BorderLayout.WEST);
        phonePanel.add(phoneField, BorderLayout.CENTER);

        topPanel.add(namePanel);
        topPanel.add(phonePanel);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center: Menu items with quantity
        itemsPanel = new JPanel();
        itemsPanel.setLayout(new GridLayout(0, 1, 5, 5));
        itemsPanel.setBackground(new Color(40, 40, 40));
        itemsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                "Menu",
                0, 0,
                new Font("Arial", Font.BOLD, 14),
                Color.WHITE
        ));

        if (foodItems.isEmpty()) {
            JLabel noItemsLabel = new JLabel("No food items found in database.");
            noItemsLabel.setForeground(Color.WHITE);
            itemsPanel.add(noItemsLabel);
        } else {
            for (FoodItem item : foodItems) {
                JPanel row = new JPanel(new BorderLayout());
                row.setBackground(new Color(40, 40, 40));

                JLabel itemLabel = new JLabel(item.getName() + " (Rs. " + item.getPrice() + ")");
                itemLabel.setForeground(Color.WHITE);

                JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 20, 1));
                qtySpinners.add(qtySpinner);

                row.add(itemLabel, BorderLayout.WEST);
                row.add(qtySpinner, BorderLayout.EAST);

                itemsPanel.add(row);
            }
        }

        JScrollPane scrollPane = new JScrollPane(itemsPanel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom: Place Order button
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(Color.DARK_GRAY);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton placeOrderButton = new JButton("Place Order");
        placeOrderButton.setFocusPainted(false);

        placeOrderButton.addActionListener(this::handlePlaceOrder);

        bottomPanel.add(placeOrderButton);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void handlePlaceOrder(ActionEvent e) {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter customer name.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        StringBuilder itemsText = new StringBuilder();
        double total = 0.0;
        int totalQty = 0;

        for (int i = 0; i < foodItems.size(); i++) {
            FoodItem item = foodItems.get(i);
            int qty = (Integer) qtySpinners.get(i).getValue();

            if (qty > 0) {
                if (itemsText.length() > 0) {
                    itemsText.append(", ");
                }
                itemsText.append(item.getName()).append(" x").append(qty);
                total += item.getPrice() * qty;
                totalQty += qty;
            }
        }

        if (totalQty == 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select at least one item.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int orderId = orderDAO.createOrder(name, phone, itemsText.toString(), total);

        if (orderId > 0) {
            JOptionPane.showMessageDialog(this,
                    "Order placed successfully!\n\nOrder ID: " + orderId +
                            "\nName: " + name +
                            "\nItems: " + itemsText +
                            "\nTotal: Rs. " + total,
                    "Order Confirmed",
                    JOptionPane.INFORMATION_MESSAGE);

            // reset quantities
            for (JSpinner spinner : qtySpinners) {
                spinner.setValue(0);
            }

        } else {
            JOptionPane.showMessageDialog(this,
                    "Failed to place order. Please try again.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            QuickBiteGUI gui = new QuickBiteGUI();
            gui.setVisible(true);
        });
    }
}