// QuickBiteGUI.java
// Handles GUI and client-side validation

import javax.swing.*;
import java.awt.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;

public class QuickBiteGUI extends JFrame {

    private JTextField customerName;
    private JTextField foodItem;
    private JButton orderButton;

    public QuickBiteGUI() {
        setTitle("QuickBite - Food Ordering System");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(3, 2, 10, 10));

        add(new JLabel("Customer Name:"));
        customerName = new JTextField();
        add(customerName);

        add(new JLabel("Food Item:"));
        foodItem = new JTextField();
        add(foodItem);

        orderButton = new JButton("Place Order");
        add(new JLabel());
        add(orderButton);

        orderButton.addActionListener(e -> placeOrder());

        setVisible(true);
    }

    private void placeOrder() {
        String name = customerName.getText().trim();
        String item = foodItem.getText().trim();

        // CLIENT-SIDE VALIDATION
        if (name.isEmpty() || item.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "All fields are required!",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        sendOrderToServer(name + "," + item);
    }

    private void sendOrderToServer(String data) {
        try {
            URL url = new URL("http://localhost:8000/order");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("POST");
            con.setDoOutput(true);

            OutputStream os = con.getOutputStream();
            os.write(data.getBytes());
            os.flush();
            os.close();

            int responseCode = con.getResponseCode();

            if (responseCode == 200) {
                JOptionPane.showMessageDialog(this,
                        "Order placed successfully!");
                customerName.setText("");
                foodItem.setText("");
            } else {
                JOptionPane.showMessageDialog(this,
                        "Server error while placing order");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Unable to connect to server",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}