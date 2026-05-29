import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

public class EcoTraceGUI extends JFrame {

    // Theme Colors
    private static final Color BG_DARK = new Color(15, 23, 42);
    private static final Color PANEL_BG = new Color(30, 41, 59);
    private static final Color TEXT_LIGHT = new Color(248, 250, 252);
    private static final Color TEXT_MUTED = new Color(148, 163, 184);
    private static final Color ACCENT_GREEN = new Color(16, 185, 129);
    private static final Color ACCENT_WARNING = new Color(245, 158, 11);
    private static final Color ACCENT_DANGER = new Color(239, 68, 68);

    private JTextField barcodeField;
    private JButton searchBtn;

    // Main Card Layout
    private JPanel mainContentPanel;
    private CardLayout cardLayout;

    // Results Card Components
    private JLabel productNameLabel;
    private JLabel imageLabel;
    private JLabel brandLabel;
    private JLabel manufacturerLabel;
    private JLabel categoryLabel;
    private JLabel originLabel;
    private JLabel weightLabel;
    private JLabel packagingLabel;
    private JLabel certificationsLabel;
    private JLabel allergensLabel;
    private JLabel sourcesLabel;
    private JTextArea descriptionArea;

    private JLabel co2Label;
    private JLabel ratingLabel;

    private JPanel timelineContainer;
    private AnimatedBackgroundPanel backgroundPanel;

    // Crowdsource Card Components
    private String activeMissingBarcode;
    private JTextField csNameField;
    private JTextField csBrandField;
    private JTextField csManufacturerField;
    private JTextField csCategoryField;
    private JTextField csOriginField;
    private JTextField csWeightField;
    private JTextField csPackagingField;
    private JTextField csCertificationsField;
    private JTextField csAllergensField;
    private JTextArea csDescriptionArea;
    private JButton csSubmitBtn;

    private final HttpClient httpClient;

    public EcoTraceGUI() {
        super("Eco-Trace Advanced Dashboard");

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        setupUI();
    }

    private void setupUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 800);
        setLocationRelativeTo(null);

        backgroundPanel = new AnimatedBackgroundPanel();
        backgroundPanel.setLayout(new BorderLayout(10, 10));
        backgroundPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(backgroundPanel);

        // Top Search Bar Panel
        RoundedPanel topPanel = new RoundedPanel(20);
        topPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 15));
        topPanel.setBackground(new Color(30, 41, 59, 220));
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Eco-Trace Scanner");
        titleLabel.setForeground(ACCENT_GREEN);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));

        barcodeField = new JTextField(18);
        barcodeField.setFont(new Font("SansSerif", Font.PLAIN, 18));
        barcodeField.setBackground(new Color(15, 23, 42));
        barcodeField.setForeground(TEXT_LIGHT);
        barcodeField.setCaretColor(TEXT_LIGHT);
        barcodeField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_GREEN, 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));

        searchBtn = new JButton("INITIALIZE SCAN");
        searchBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        searchBtn.setBackground(ACCENT_GREEN);
        searchBtn.setForeground(BG_DARK);
        searchBtn.setFocusPainted(false);
        searchBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchBtn.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));

        searchBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (searchBtn.isEnabled()) {
                    searchBtn.setBackground(new Color(52, 211, 153));
                }
            }

            public void mouseExited(MouseEvent e) {
                if (searchBtn.isEnabled()) {
                    searchBtn.setBackground(ACCENT_GREEN);
                }
            }
        });
        searchBtn.addActionListener(this::onSearch);

        topPanel.add(titleLabel);
        topPanel.add(barcodeField);
        topPanel.add(searchBtn);

        backgroundPanel.add(topPanel, BorderLayout.NORTH);

        // CardLayout Container for main content
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setOpaque(false);

        // 1. Welcome Card
        RoundedPanel welcomeCard = new RoundedPanel(25);
        welcomeCard.setBackground(new Color(30, 41, 59, 200));
        welcomeCard.setOpaque(false);
        welcomeCard.setLayout(new GridBagLayout());
        
        JLabel welcomeIcon = new JLabel("🌿", SwingConstants.CENTER);
        welcomeIcon.setFont(new Font("SansSerif", Font.PLAIN, 72));
        JLabel welcomeTitle = new JLabel("Ready to Scan", SwingConstants.CENTER);
        welcomeTitle.setFont(new Font("SansSerif", Font.BOLD, 28));
        welcomeTitle.setForeground(TEXT_LIGHT);
        JLabel welcomeDesc = new JLabel("<html><center>Scan a product barcode, paste a Digital Product Passport QR JSON string,<br>or input a manual barcode above to begin tracing the blockchain journey.</center></html>", SwingConstants.CENTER);
        welcomeDesc.setFont(new Font("SansSerif", Font.PLAIN, 16));
        welcomeDesc.setForeground(TEXT_MUTED);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(10, 10, 20, 10);
        welcomeCard.add(welcomeIcon, gbc);
        gbc.gridy = 1; gbc.insets = new Insets(0, 10, 10, 10);
        welcomeCard.add(welcomeTitle, gbc);
        gbc.gridy = 2;
        welcomeCard.add(welcomeDesc, gbc);

        // 2. Results Card
        JPanel resultsCard = new JPanel(new BorderLayout(25, 25));
        resultsCard.setOpaque(false);
        resultsCard.setBorder(new EmptyBorder(10, 0, 0, 0));

        // Results - Left Side Panel (Scrollable Details & Metrics)
        RoundedPanel leftPanel = new RoundedPanel(25);
        leftPanel.setLayout(new BorderLayout());
        leftPanel.setBackground(new Color(30, 41, 59, 230));
        leftPanel.setOpaque(false);
        leftPanel.setBorder(new EmptyBorder(25, 25, 25, 25));
        leftPanel.setPreferredSize(new Dimension(420, 0));

        JPanel detailsScrollContainer = new JPanel();
        detailsScrollContainer.setLayout(new BoxLayout(detailsScrollContainer, BoxLayout.Y_AXIS));
        detailsScrollContainer.setOpaque(false);

        // Image container
        imageLabel = new JLabel("No Image", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(150, 150));
        imageLabel.setMaximumSize(new Dimension(150, 150));
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        imageLabel.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 30), 1));
        imageLabel.setForeground(TEXT_MUTED);

        productNameLabel = createLabel("Product Name", new Font("SansSerif", Font.BOLD, 22), TEXT_LIGHT);
        productNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        productNameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        brandLabel = new JLabel("-");
        manufacturerLabel = new JLabel("-");
        categoryLabel = new JLabel("-");
        originLabel = new JLabel("-");
        weightLabel = new JLabel("-");
        packagingLabel = new JLabel("-");
        certificationsLabel = new JLabel("-");
        allergensLabel = new JLabel("-");
        sourcesLabel = new JLabel("-");

        descriptionArea = new JTextArea("No product description available.");
        descriptionArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        descriptionArea.setForeground(TEXT_LIGHT);
        descriptionArea.setBackground(new Color(0, 0, 0, 0));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setEditable(false);
        descriptionArea.setFocusable(false);
        descriptionArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 20)),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)));

        detailsScrollContainer.add(imageLabel);
        detailsScrollContainer.add(Box.createVerticalStrut(15));
        detailsScrollContainer.add(productNameLabel);
        detailsScrollContainer.add(Box.createVerticalStrut(20));

        // Details grid list
        detailsScrollContainer.add(createDetailRow("Brand:", brandLabel));
        detailsScrollContainer.add(Box.createVerticalStrut(8));
        detailsScrollContainer.add(createDetailRow("Mfg:", manufacturerLabel));
        detailsScrollContainer.add(Box.createVerticalStrut(8));
        detailsScrollContainer.add(createDetailRow("Category:", categoryLabel));
        detailsScrollContainer.add(Box.createVerticalStrut(8));
        detailsScrollContainer.add(createDetailRow("Origin:", originLabel));
        detailsScrollContainer.add(Box.createVerticalStrut(8));
        detailsScrollContainer.add(createDetailRow("Weight:", weightLabel));
        detailsScrollContainer.add(Box.createVerticalStrut(8));
        detailsScrollContainer.add(createDetailRow("Packaging:", packagingLabel));
        detailsScrollContainer.add(Box.createVerticalStrut(8));
        detailsScrollContainer.add(createDetailRow("Certifications:", certificationsLabel));
        detailsScrollContainer.add(Box.createVerticalStrut(8));
        detailsScrollContainer.add(createDetailRow("Allergens:", allergensLabel));
        detailsScrollContainer.add(Box.createVerticalStrut(8));
        detailsScrollContainer.add(createDetailRow("Sources:", sourcesLabel));
        detailsScrollContainer.add(Box.createVerticalStrut(15));
        detailsScrollContainer.add(descriptionArea);
        detailsScrollContainer.add(Box.createVerticalGlue());

        JScrollPane detailsScrollPane = new JScrollPane(detailsScrollContainer);
        detailsScrollPane.setOpaque(false);
        detailsScrollPane.getViewport().setOpaque(false);
        detailsScrollPane.setBorder(null);
        detailsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        detailsScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        detailsScrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = ACCENT_GREEN;
                this.trackColor = BG_DARK;
            }
            @Override
            protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton jb = new JButton();
                jb.setPreferredSize(new Dimension(0, 0));
                return jb;
            }
        });

        leftPanel.add(detailsScrollPane, BorderLayout.CENTER);

        // Eco Metrics Panel (placed at bottom of Left Side Panel)
        JPanel metricsPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        metricsPanel.setOpaque(false);
        metricsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 30)),
                BorderFactory.createEmptyBorder(20, 0, 0, 0)));

        JPanel co2Wrapper = new JPanel(new BorderLayout());
        co2Wrapper.setOpaque(false);
        JLabel co2Title = createLabel("CARBON EMISSIONS", new Font("SansSerif", Font.BOLD, 11), TEXT_MUTED);
        co2Label = createLabel("Total CO₂: -", new Font("SansSerif", Font.BOLD, 16), TEXT_LIGHT);
        co2Wrapper.add(co2Title, BorderLayout.NORTH);
        co2Wrapper.add(co2Label, BorderLayout.CENTER);

        JPanel ratingWrapper = new JPanel(new BorderLayout());
        ratingWrapper.setOpaque(false);
        JLabel ratingTitle = createLabel("ECO RATING", new Font("SansSerif", Font.BOLD, 11), TEXT_MUTED);
        ratingLabel = createLabel("?", new Font("SansSerif", Font.BOLD, 38), ACCENT_GREEN);
        ratingLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        ratingWrapper.add(ratingTitle, BorderLayout.NORTH);
        ratingWrapper.add(ratingLabel, BorderLayout.CENTER);

        metricsPanel.add(co2Wrapper);
        metricsPanel.add(ratingWrapper);
        leftPanel.add(metricsPanel, BorderLayout.SOUTH);

        resultsCard.add(leftPanel, BorderLayout.WEST);

        // Results - Right Side Panel (Supply Chain Timeline)
        timelineContainer = new JPanel();
        timelineContainer.setLayout(new BoxLayout(timelineContainer, BoxLayout.Y_AXIS));
        timelineContainer.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(timelineContainer);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = ACCENT_GREEN;
                this.trackColor = BG_DARK;
            }
            @Override
            protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton jb = new JButton();
                jb.setPreferredSize(new Dimension(0, 0));
                return jb;
            }
        });

        RoundedPanel timelineWrapper = new RoundedPanel(25);
        timelineWrapper.setLayout(new BorderLayout());
        timelineWrapper.setBackground(new Color(30, 41, 59, 210));
        timelineWrapper.setOpaque(false);
        timelineWrapper.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel timelineTitle = createLabel("SUPPLY CHAIN JOURNEY (VERIFIED)", new Font("SansSerif", Font.BOLD, 16), ACCENT_GREEN);
        timelineTitle.setBorder(new EmptyBorder(0, 0, 15, 0));

        timelineWrapper.add(timelineTitle, BorderLayout.NORTH);
        timelineWrapper.add(scrollPane, BorderLayout.CENTER);

        resultsCard.add(timelineWrapper, BorderLayout.CENTER);

        // 3. Crowdsource Card
        RoundedPanel csCard = new RoundedPanel(25);
        csCard.setBackground(new Color(30, 41, 59, 230));
        csCard.setOpaque(false);
        csCard.setBorder(new EmptyBorder(30, 40, 30, 40));
        csCard.setLayout(new BorderLayout(0, 20));

        JPanel csHeader = new JPanel(new GridLayout(2, 1, 5, 5));
        csHeader.setOpaque(false);
        JLabel csTitle = createLabel("Product Not Found", new Font("SansSerif", Font.BOLD, 26), ACCENT_DANGER);
        JLabel csSubtitle = createLabel("Help us grow our database! Enter the product information below to register it.", new Font("SansSerif", Font.PLAIN, 15), TEXT_MUTED);
        csHeader.add(csTitle);
        csHeader.add(csSubtitle);
        csCard.add(csHeader, BorderLayout.NORTH);

        // Form Fields Grid
        JPanel csForm = new JPanel(new GridLayout(5, 2, 20, 15));
        csForm.setOpaque(false);

        csNameField = createStyledTextField();
        csBrandField = createStyledTextField();
        csManufacturerField = createStyledTextField();
        csCategoryField = createStyledTextField();
        csOriginField = createStyledTextField();
        csWeightField = createStyledTextField();
        csPackagingField = createStyledTextField();
        csCertificationsField = createStyledTextField();
        csAllergensField = createStyledTextField();
        
        csForm.add(createFormGroup("Product Name *", csNameField));
        csForm.add(createFormGroup("Brand", csBrandField));
        csForm.add(createFormGroup("Manufacturer", csManufacturerField));
        csForm.add(createFormGroup("Category", csCategoryField));
        csForm.add(createFormGroup("Origin Country", csOriginField));
        csForm.add(createFormGroup("Weight/Volume", csWeightField));
        csForm.add(createFormGroup("Packaging Type", csPackagingField));
        csForm.add(createFormGroup("Certifications", csCertificationsField));
        csForm.add(createFormGroup("Allergens", csAllergensField));

        JPanel descGroup = new JPanel(new BorderLayout(0, 5));
        descGroup.setOpaque(false);
        JLabel descLabel = createLabel("Description", new Font("SansSerif", Font.BOLD, 12), TEXT_MUTED);
        csDescriptionArea = new JTextArea(3, 20);
        csDescriptionArea.setBackground(new Color(15, 23, 42));
        csDescriptionArea.setForeground(TEXT_LIGHT);
        csDescriptionArea.setCaretColor(TEXT_LIGHT);
        csDescriptionArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        csDescriptionArea.setLineWrap(true);
        csDescriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(csDescriptionArea);
        descScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TEXT_MUTED, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        descGroup.add(descLabel, BorderLayout.NORTH);
        descGroup.add(descScroll, BorderLayout.CENTER);
        csForm.add(descGroup);

        csCard.add(csForm, BorderLayout.CENTER);

        // Buttons Panel
        JPanel csButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        csButtons.setOpaque(false);

        JButton csCancelBtn = new JButton("CANCEL");
        csCancelBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        csCancelBtn.setBackground(new Color(71, 85, 105));
        csCancelBtn.setForeground(TEXT_LIGHT);
        csCancelBtn.setFocusPainted(false);
        csCancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        csCancelBtn.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        csCancelBtn.addActionListener(e -> {
            barcodeField.setText("");
            cardLayout.show(mainContentPanel, "WELCOME");
        });

        csSubmitBtn = new JButton("SUBMIT TO ECOTRACE");
        csSubmitBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        csSubmitBtn.setBackground(ACCENT_GREEN);
        csSubmitBtn.setForeground(BG_DARK);
        csSubmitBtn.setFocusPainted(false);
        csSubmitBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        csSubmitBtn.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
        csSubmitBtn.addActionListener(this::onCrowdsourceSubmit);

        csButtons.add(csCancelBtn);
        csButtons.add(csSubmitBtn);
        csCard.add(csButtons, BorderLayout.SOUTH);

        // Add views to main card manager
        mainContentPanel.add(welcomeCard, "WELCOME");
        mainContentPanel.add(resultsCard, "RESULTS");
        mainContentPanel.add(csCard, "CROWDSOURCE");

        backgroundPanel.add(mainContentPanel, BorderLayout.CENTER);

        cardLayout.show(mainContentPanel, "WELCOME");
    }

    private JLabel createLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JTextField createStyledTextField() {
        JTextField tf = new JTextField();
        tf.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tf.setBackground(new Color(15, 23, 42));
        tf.setForeground(TEXT_LIGHT);
        tf.setCaretColor(TEXT_LIGHT);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TEXT_MUTED, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        return tf;
    }

    private JPanel createFormGroup(String labelStr, JTextField textField) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        JLabel label = createLabel(labelStr, new Font("SansSerif", Font.BOLD, 12), TEXT_MUTED);
        panel.add(label, BorderLayout.NORTH);
        panel.add(textField, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDetailRow(String title, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        titleLabel.setForeground(TEXT_MUTED);
        titleLabel.setPreferredSize(new Dimension(110, 0));
        row.add(titleLabel, BorderLayout.WEST);
        valueLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        valueLabel.setForeground(TEXT_LIGHT);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    private boolean isJson(String str) {
        if (str == null) return false;
        String t = str.trim();
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"));
    }

    private void onSearch(ActionEvent e) {
        String barcode = barcodeField.getText().trim();
        if (barcode.isEmpty())
            return;

        searchBtn.setText("ANALYZING...");
        searchBtn.setEnabled(false);
        timelineContainer.removeAll();

        // 1. Direct local JSON parse check
        if (isJson(barcode)) {
            parseAndShowDirectJson(barcode);
            return;
        }

        // 2. Fetch from backend APIs
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            String prodJson = "";
            String chainJson = "";
            boolean success = false;
            int responseCode = 200;

            @Override
            protected Void doInBackground() {
                try {
                    HttpRequest prodReq = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/api/product/" + barcode))
                            .GET().build();
                    
                    HttpResponse<String> prodRes = httpClient.send(prodReq, HttpResponse.BodyHandlers.ofString());
                    responseCode = prodRes.statusCode();
                    prodJson = prodRes.body();

                    if (responseCode == 200) {
                        HttpRequest chainReq = HttpRequest.newBuilder()
                                .uri(URI.create("http://localhost:8080/api/supply-chain/" + barcode))
                                .GET().build();
                        chainJson = httpClient.send(chainReq, HttpResponse.BodyHandlers.ofString()).body();
                        success = true;
                    } else if (responseCode == 404) {
                        success = true; // Still a valid execution path, but goes to crowdsource
                    }
                    Thread.sleep(800);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                searchBtn.setText("INITIALIZE SCAN");
                searchBtn.setEnabled(true);

                if (success) {
                    if (responseCode == 200) {
                        populateUI(prodJson, chainJson);
                        cardLayout.show(mainContentPanel, "RESULTS");
                    } else {
                        // Product not found (404), trigger Crowdsource screen
                        activeMissingBarcode = barcode;
                        clearCrowdsourceFields();
                        cardLayout.show(mainContentPanel, "CROWDSOURCE");
                    }
                } else {
                    JOptionPane.showMessageDialog(EcoTraceGUI.this,
                            "Failed to connect to backend on port 8080.",
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void parseAndShowDirectJson(String jsonStr) {
        try {
            JSONObject obj = new JSONObject(jsonStr);
            
            // Build mock product JSON
            JSONObject prodObj = new JSONObject();
            prodObj.put("success", true);
            
            JSONObject pObj = new JSONObject();
            pObj.put("name", obj.optString("name", "QR Digital Passport Product"));
            pObj.put("brand", obj.optString("brand", "Unknown"));
            pObj.put("image", obj.optString("image", ""));
            pObj.put("ingredients", obj.optString("ingredients", ""));
            pObj.put("origin", obj.optString("origin", "-"));
            pObj.put("weight", obj.optString("weight", "-"));
            pObj.put("manufacturer", obj.optString("manufacturer", "-"));
            pObj.put("category", obj.optString("category", "-"));
            pObj.put("packaging", obj.optString("packaging", "-"));
            pObj.put("certifications", obj.optString("certifications", "-"));
            pObj.put("allergens", obj.optString("allergens", "-"));
            pObj.put("sources", obj.optString("sources", "-"));
            pObj.put("description", obj.optString("description", "No product description available."));
            
            prodObj.put("product", pObj);
            
            // Build mock supply chain JSON
            JSONObject chainObj = new JSONObject();
            chainObj.put("success", true);
            chainObj.put("blockchainNetwork", "Digital Product Passport (Direct)");
            
            JSONArray stages = obj.optJSONArray("stages");
            if (stages == null) {
                stages = new JSONArray();
            }
            chainObj.put("stages", stages);
            
            double totalCo2 = 0;
            for (int i = 0; i < stages.length(); i++) {
                JSONObject stage = stages.getJSONObject(i);
                totalCo2 += stage.optDouble("co2EmissionsKg", 0.0);
            }
            chainObj.put("totalCo2EmissionsKg", String.format(java.util.Locale.US, "%.2f", totalCo2));
            chainObj.put("ecoRating", totalCo2 < 15 ? "A" : (totalCo2 < 25 ? "B" : "C"));
            
            populateUI(prodObj.toString(), chainObj.toString());
            cardLayout.show(mainContentPanel, "RESULTS");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Invalid QR Code JSON format.", "Format Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            searchBtn.setText("INITIALIZE SCAN");
            searchBtn.setEnabled(true);
        }
    }

    private void onCrowdsourceSubmit(ActionEvent e) {
        String name = csNameField.getText().trim();
        String brand = csBrandField.getText().trim();
        String manufacturer = csManufacturerField.getText().trim();
        String category = csCategoryField.getText().trim();
        String origin = csOriginField.getText().trim();
        String weight = csWeightField.getText().trim();
        String packaging = csPackagingField.getText().trim();
        String certifications = csCertificationsField.getText().trim();
        String allergens = csAllergensField.getText().trim();
        String description = csDescriptionArea.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Product Name is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        csSubmitBtn.setText("SUBMITTING...");
        csSubmitBtn.setEnabled(false);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    JSONObject bodyObj = new JSONObject();
                    bodyObj.put("barcode", activeMissingBarcode);
                    bodyObj.put("name", name);
                    bodyObj.put("brand", brand);
                    bodyObj.put("manufacturer", manufacturer);
                    bodyObj.put("category", category);
                    bodyObj.put("origin", origin);
                    bodyObj.put("weight", weight);
                    bodyObj.put("packaging", packaging);
                    bodyObj.put("certifications", certifications);
                    bodyObj.put("allergens", allergens);
                    bodyObj.put("sources", "Crowdsourced");
                    bodyObj.put("description", description);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/api/product/"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(bodyObj.toString()))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    return response.statusCode() == 200;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return false;
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        csSubmitBtn.setText("SUCCESS!");
                        Timer t = new Timer(1000, evt -> {
                            csSubmitBtn.setText("SUBMIT TO ECOTRACE");
                            csSubmitBtn.setEnabled(true);
                            // Query the newly added product
                            barcodeField.setText(activeMissingBarcode);
                            searchBtn.doClick();
                        });
                        t.setRepeats(false);
                        t.start();
                    } else {
                        JOptionPane.showMessageDialog(EcoTraceGUI.this, "Failed to submit product details.", "Network Error", JOptionPane.ERROR_MESSAGE);
                        csSubmitBtn.setText("SUBMIT TO ECOTRACE");
                        csSubmitBtn.setEnabled(true);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(EcoTraceGUI.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    csSubmitBtn.setText("SUBMIT TO ECOTRACE");
                    csSubmitBtn.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void clearCrowdsourceFields() {
        csNameField.setText("");
        csBrandField.setText("");
        csManufacturerField.setText("");
        csCategoryField.setText("");
        csOriginField.setText("");
        csWeightField.setText("");
        csPackagingField.setText("");
        csCertificationsField.setText("");
        csAllergensField.setText("");
        csDescriptionArea.setText("");
    }

    private void loadImageAsync(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageLabel.setIcon(null);
            imageLabel.setText("No Image");
            return;
        }

        imageLabel.setIcon(null);
        imageLabel.setText("Loading Image...");

        SwingWorker<ImageIcon, Void> worker = new SwingWorker<>() {
            @Override
            protected ImageIcon doInBackground() {
                try {
                    java.net.URL url = new java.net.URL(imageUrl);
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(url);
                    if (img != null) {
                        Image scaled = img.getScaledInstance(140, 140, Image.SCALE_SMOOTH);
                        return new ImageIcon(scaled);
                    }
                } catch (Exception e) {
                    // Ignore, defaults to placeholder
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        imageLabel.setText("");
                        imageLabel.setIcon(icon);
                    } else {
                        imageLabel.setIcon(null);
                        imageLabel.setText("No Image");
                    }
                } catch (Exception e) {
                    imageLabel.setIcon(null);
                    imageLabel.setText("No Image");
                }
            }
        };
        worker.execute();
    }

    private void populateUI(String prodJson, String chainJson) {
        try {
            JSONObject prodObj = new JSONObject(prodJson);
            JSONObject product = prodObj.optJSONObject("product");

            String name = product != null ? product.optString("name", "Unknown Product") : "Unknown Product";
            String brand = product != null ? product.optString("brand", "-") : "-";
            String mfg = product != null ? product.optString("manufacturer", "-") : "-";
            String cat = product != null ? product.optString("category", "-") : "-";
            String origin = product != null ? product.optString("origin", "-") : "-";
            String weight = product != null ? product.optString("weight", "-") : "-";
            String packaging = product != null ? product.optString("packaging", "-") : "-";
            String certs = product != null ? product.optString("certifications", "-") : "-";
            String allergens = product != null ? product.optString("allergens", "-") : "-";
            String sources = product != null ? product.optString("sources", "Direct Scan") : "Direct Scan";
            String desc = product != null ? product.optString("description", "No product description available.") : "No product description available.";
            String imageUrl = product != null ? product.optString("image", "") : "";

            productNameLabel.setText("<html><center>" + name + "</center></html>");
            brandLabel.setText(brand);
            manufacturerLabel.setText(mfg);
            categoryLabel.setText(cat);
            originLabel.setText(origin);
            weightLabel.setText(weight);
            packagingLabel.setText(packaging);
            certificationsLabel.setText(certs);
            allergensLabel.setText(allergens);
            sourcesLabel.setText(sources);
            descriptionArea.setText(desc);

            loadImageAsync(imageUrl);

            // Parse Eco Metrics
            JSONObject chainObj = new JSONObject(chainJson);
            String totalCo2 = chainObj.optString("totalCo2EmissionsKg", "0.00");
            String ecoRating = chainObj.optString("ecoRating", "?");

            co2Label.setText("Total CO₂: " + totalCo2 + " kg");
            ratingLabel.setText(ecoRating);

            if ("A".equals(ecoRating)) {
                ratingLabel.setForeground(ACCENT_GREEN);
                backgroundPanel.setNetColor(ACCENT_GREEN);
            } else if ("B".equals(ecoRating)) {
                ratingLabel.setForeground(ACCENT_WARNING);
                backgroundPanel.setNetColor(ACCENT_WARNING);
            } else {
                ratingLabel.setForeground(ACCENT_DANGER);
                backgroundPanel.setNetColor(ACCENT_DANGER);
            }

            // Parse and Populate Supply Chain Stages
            JSONArray stages = chainObj.optJSONArray("stages");
            if (stages != null) {
                for (int i = 0; i < stages.length(); i++) {
                    JSONObject stage = stages.getJSONObject(i);
                    String sName = stage.optString("stage", "-");
                    String sLoc = stage.optString("location", "-");
                    double sCo2 = stage.optDouble("co2EmissionsKg", 0.0);
                    String sHash = stage.optString("blockchainTxHash", "-");

                    RoundedPanel sPanel = new RoundedPanel(15);
                    sPanel.setLayout(new BoxLayout(sPanel, BoxLayout.Y_AXIS));
                    sPanel.setBackground(new Color(15, 23, 42, 200));
                    sPanel.setOpaque(false);
                    sPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
                    sPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

                    JLabel nameL = createLabel(sName, new Font("SansSerif", Font.BOLD, 15), TEXT_LIGHT);
                    JLabel locL = createLabel("📍 " + sLoc, new Font("SansSerif", Font.PLAIN, 13), TEXT_MUTED);
                    JLabel co2L = createLabel("Emissions: " + String.format("%.2f", sCo2) + " kg CO₂", new Font("SansSerif", Font.BOLD, 13), ACCENT_WARNING);

                    ScramblingLabel hashL = new ScramblingLabel(sHash, new Font("Monospaced", Font.PLAIN, 12), ACCENT_GREEN);

                    sPanel.add(nameL);
                    sPanel.add(Box.createVerticalStrut(4));
                    sPanel.add(locL);
                    sPanel.add(Box.createVerticalStrut(6));
                    sPanel.add(co2L);
                    sPanel.add(Box.createVerticalStrut(6));
                    sPanel.add(hashL);

                    timelineContainer.add(sPanel);
                    timelineContainer.add(Box.createVerticalStrut(15));

                    final int index = i;
                    Timer t = new Timer((index + 1) * 350, e -> hashL.startScramble());
                    t.setRepeats(false);
                    t.start();
                }
            }

            timelineContainer.revalidate();
            timelineContainer.repaint();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // --- Custom UI Components ---

    /** Animated Vanta.js style Network Background */
    class AnimatedBackgroundPanel extends JPanel {
        private final List<Particle> particles = new ArrayList<>();
        private Color netColor = ACCENT_GREEN;

        public AnimatedBackgroundPanel() {
            setBackground(BG_DARK);
            for (int i = 0; i < 40; i++)
                particles.add(new Particle());
            Timer timer = new Timer(30, e -> {
                for (Particle p : particles)
                    p.update(getWidth(), getHeight());
                repaint();
            });
            timer.start();
        }

        public void setNetColor(Color c) {
            this.netColor = c;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            for (int i = 0; i < particles.size(); i++) {
                Particle p1 = particles.get(i);
                g2.setColor(new Color(netColor.getRed(), netColor.getGreen(), netColor.getBlue(), 100));
                g2.fillOval((int) p1.x - 2, (int) p1.y - 2, 4, 4);

                for (int j = i + 1; j < particles.size(); j++) {
                    Particle p2 = particles.get(j);
                    double dist = Math.hypot(p1.x - p2.x, p1.y - p2.y);
                    if (dist < 120) {
                        int alpha = (int) (100 * (1 - dist / 120));
                        g2.setColor(new Color(netColor.getRed(), netColor.getGreen(), netColor.getBlue(), alpha));
                        g2.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
                    }
                }
            }
        }

        class Particle {
            double x, y, dx, dy;

            Particle() {
                Random r = new Random();
                x = r.nextInt(1000);
                y = r.nextInt(800);
                dx = (r.nextDouble() - 0.5) * 1.5;
                dy = (r.nextDouble() - 0.5) * 1.5;
            }

            void update(int w, int h) {
                x += dx;
                y += dy;
                if (x < 0 || x > w)
                    dx = -dx;
                if (y < 0 || y > h)
                    dy = -dy;
            }
        }
    }

    /** Cyberpunk Text Decrypt Effect */
    class ScramblingLabel extends JLabel {
        private final String finalString;
        private double iterations;
        private Timer timer;
        private static final String CHARS = "0123456789ABCDEF!@#$%^&*()";

        public ScramblingLabel(String finalStr, Font f, Color c) {
            this.finalString = finalStr;
            setFont(f);
            setForeground(c);
            setText("TxHash: " + finalString.replaceAll(".", "-")); // Initial state
        }

        public void startScramble() {
            iterations = 0;
            if (timer != null)
                timer.stop();
            timer = new Timer(30, e -> {
                StringBuilder sb = new StringBuilder();
                Random r = new Random();
                for (int i = 0; i < finalString.length(); i++) {
                    if (i < iterations)
                        sb.append(finalString.charAt(i));
                    else
                        sb.append(CHARS.charAt(r.nextInt(CHARS.length())));
                }
                setText("TxHash: " + sb.toString());
                iterations += 0.5;
                if (iterations >= finalString.length()) {
                    setText("TxHash: " + finalString);
                    timer.stop();
                }
            });
            timer.start();
        }
    }

    /** Rounded JPanel */
    class RoundedPanel extends JPanel {
        private final int radius;

        public RoundedPanel(int radius) {
            this.radius = radius;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EcoTraceGUI gui = new EcoTraceGUI();
            gui.setVisible(true);
        });
    }
}