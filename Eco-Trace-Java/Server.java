import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {
    private static final int PORT = 8080;
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ConcurrentHashMap<String, String> localDatabase = new ConcurrentHashMap<>();
    private static final Path LOCAL_DB_PATH = Paths.get("local_products.jsonl");

    static {
        loadLocalDatabase();
    }

    private static void loadLocalDatabase() {
        if (Files.exists(LOCAL_DB_PATH)) {
            try {
                for (String line : Files.readAllLines(LOCAL_DB_PATH)) {
                    String barcode = extractJsonValue(line, "barcode");
                    if (!barcode.isEmpty()) {
                        localDatabase.put(barcode, line);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error loading local database: " + e.getMessage());
            }
        }
    }

    private static void saveToLocalDatabase(String barcode, String productJson) {
        localDatabase.put(barcode, productJson);
        try {
            Files.writeString(LOCAL_DB_PATH, productJson.replace("\n", " ").replace("\r", "") + "\n", 
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Error saving to local database: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/api/product/", new ProductHandler());
        server.createContext("/api/supply-chain/", new SupplyChainHandler());
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Server running on http://localhost:" + PORT);
    }

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String pathStr = exchange.getRequestURI().getPath();
            if (pathStr.equals("/") || pathStr.isEmpty()) {
                pathStr = "/index.html";
            }

            // Prevent path traversal
            Path base = Paths.get("..").toAbsolutePath().normalize();
            Path path = Paths.get(".." + pathStr).toAbsolutePath().normalize();
            
            // Fallback just in case they run it from the root directory instead of Eco-Trace-Java
            if (!Files.exists(path)) {
                base = Paths.get(".").toAbsolutePath().normalize();
                path = Paths.get("." + pathStr).toAbsolutePath().normalize();
            }

            if (!path.startsWith(base)) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }

            if (Files.exists(path) && !Files.isDirectory(path)) {
                String contentType = "text/plain";
                if (pathStr.endsWith(".html")) contentType = "text/html";
                else if (pathStr.endsWith(".css")) contentType = "text/css";
                else if (pathStr.endsWith(".js")) contentType = "application/javascript";

                exchange.getResponseHeaders().set("Content-Type", contentType);
                byte[] bytes = Files.readAllBytes(path);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                String response = "404 (Not Found)\n";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }

    private static class ProductHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), "UTF-8");
                String postBarcode = extractJsonValue(body, "barcode");
                if (postBarcode.isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"Barcode required in payload\"}");
                    return;
                }
                saveToLocalDatabase(postBarcode, body);
                sendResponse(exchange, 200, "{\"success\":true}");
                return;
            }

            String uri = exchange.getRequestURI().getPath();
            String barcode = uri.substring(uri.lastIndexOf("/") + 1);
            if (barcode.isEmpty()) {
                sendResponse(exchange, 400, "{\"error\":\"Barcode required\"}");
                return;
            }

            // 1. Check Local DB
            if (localDatabase.containsKey(barcode)) {
                String cached = localDatabase.get(barcode);
                String resJson = "{\n" +
                        "  \"success\": true,\n" +
                        "  \"mock\": false,\n" +
                        "  \"source\": \"local\",\n" +
                        "  \"product\": " + cached + "\n" +
                        "}";
                sendResponse(exchange, 200, resJson);
                return;
            }

            // 2. Open Food Facts
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://world.openfoodfacts.org/api/v2/product/" + barcode + ".json"))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String json = response.body();
                    String status = extractJsonValue(json, "status");
                    if ("1".equals(status)) {
                        String productBlock = extractJsonBlock(json, "product");

                        String name = extractJsonValue(productBlock, "product_name");
                        String brand = extractJsonValue(productBlock, "brands");
                        String image = extractJsonValue(productBlock, "image_url");
                        String ingredients = extractJsonValue(productBlock, "ingredients_text");
                        String origin = extractJsonValue(productBlock, "origins");
                        String weight = extractJsonValue(productBlock, "quantity");

                        String resJson = "{\n" +
                                "  \"success\": true,\n" +
                                "  \"mock\": false,\n" +
                                "  \"source\": \"Open Food Facts\",\n" +
                                "  \"product\": {\n" +
                                "    \"name\": \"" + escapeJson(name.isEmpty() ? "Unknown Product" : name) + "\",\n" +
                                "    \"brand\": \"" + escapeJson(brand.isEmpty() ? "Unknown Brand" : brand) + "\",\n" +
                                "    \"image\": \"" + escapeJson(image) + "\",\n" +
                                "    \"ingredients\": \"" + escapeJson(ingredients.isEmpty() ? "No ingredients listed" : ingredients) + "\",\n" +
                                "    \"origin\": \"" + escapeJson(origin.isEmpty() ? "Unknown origin" : origin) + "\",\n" +
                                "    \"weight\": \"" + escapeJson(weight.isEmpty() ? "Unknown weight" : weight) + "\"\n" +
                                "  }\n" +
                                "}";
                        sendResponse(exchange, 200, resJson);
                        return;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error fetching Open Food Facts data: " + e.getMessage());
            }

            // 2.5 BarcodeLookup
            String barcodeLookupKey = System.getenv("aqsoy0ed01380tby72pb98irn976t2");
            if (barcodeLookupKey != null && !barcodeLookupKey.trim().isEmpty()) {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.barcodelookup.com/v3/products?barcode=" + barcode + "&formatted=y&key=" + barcodeLookupKey))
                            .timeout(Duration.ofSeconds(10))
                            .GET()
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        String json = response.body();
                        String title = extractJsonValue(json, "product_name");
                        if (title.isEmpty()) title = extractJsonValue(json, "title");
                        String brand = extractJsonValue(json, "brand");
                        String ingredients = extractJsonValue(json, "ingredients");

                        Matcher imgMatcher = Pattern.compile("\"images\"\\s*:\\s*\\[\\s*\"([^\"]+)\"").matcher(json);
                        String image = imgMatcher.find() ? imgMatcher.group(1) : "";

                        if (!title.isEmpty()) {
                            String resJson = "{\n" +
                                    "  \"success\": true,\n" +
                                    "  \"mock\": false,\n" +
                                    "  \"source\": \"BarcodeLookup\",\n" +
                                    "  \"product\": {\n" +
                                    "    \"name\": \"" + escapeJson(title) + "\",\n" +
                                    "    \"brand\": \"" + escapeJson(brand.isEmpty() ? "Unknown Brand" : brand) + "\",\n" +
                                    "    \"image\": \"" + escapeJson(image) + "\",\n" +
                                    "    \"ingredients\": \"" + escapeJson(ingredients.isEmpty() ? "No ingredients listed" : ingredients) + "\",\n" +
                                    "    \"origin\": \"Unknown origin\",\n" +
                                    "    \"weight\": \"Unknown weight\"\n" +
                                    "  }\n" +
                                    "}";
                            sendResponse(exchange, 200, resJson);
                            return;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching BarcodeLookup data: " + e.getMessage());
                }
            }

            // 3. UPCitemdb
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.upcitemdb.com/prod/trial/lookup?upc=" + barcode))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String json = response.body();
                    String code = extractJsonValue(json, "code");
                    if ("OK".equals(code)) {
                        String title = extractJsonValue(json, "title");
                        String brand = extractJsonValue(json, "brand");
                        
                        Matcher imgMatcher = Pattern.compile("\"images\"\\s*:\\s*\\[\\s*\"([^\"]+)\"").matcher(json);
                        String image = imgMatcher.find() ? imgMatcher.group(1) : "";

                        if (!title.isEmpty()) {
                            String resJson = "{\n" +
                                    "  \"success\": true,\n" +
                                    "  \"mock\": false,\n" +
                                    "  \"source\": \"UPCitemdb\",\n" +
                                    "  \"product\": {\n" +
                                    "    \"name\": \"" + escapeJson(title) + "\",\n" +
                                    "    \"brand\": \"" + escapeJson(brand.isEmpty() ? "Unknown Brand" : brand) + "\",\n" +
                                    "    \"image\": \"" + escapeJson(image) + "\",\n" +
                                    "    \"ingredients\": \"No ingredients listed\",\n" +
                                    "    \"origin\": \"Unknown origin\",\n" +
                                    "    \"weight\": \"Unknown weight\"\n" +
                                    "  }\n" +
                                    "}";
                            sendResponse(exchange, 200, resJson);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error fetching UPCitemdb data: " + e.getMessage());
            }

            // 4. Return Not Found (No Fake Data)
            String resJson = "{\n" +
                    "  \"success\": false,\n" +
                    "  \"error\": \"Product not found in any database.\"\n" +
                    "}";
            sendResponse(exchange, 404, resJson);
        }
    }

    private static class SupplyChainHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            String uri = exchange.getRequestURI().getPath();
            String barcode = uri.substring(uri.lastIndexOf("/") + 1);
            if (barcode.isEmpty()) {
                sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Barcode required\"}");
                return;
            }

            long baseNum = parseBaseNum(barcode);

            double co2Stage1 = (baseNum % 5) + 1.2;
            double co2Stage2 = (baseNum % 10) + 5.5;
            double co2Stage3 = (baseNum % 8) + 3.0;
            double co2Stage4 = 0.5;

            double totalCo2 = co2Stage1 + co2Stage2 + co2Stage3 + co2Stage4;
            String rating = getRating(totalCo2);

            long now = System.currentTimeMillis();
            String date1 = formatIsoDate(now - 30L * 24 * 60 * 60 * 1000);
            String date2 = formatIsoDate(now - 15L * 24 * 60 * 60 * 1000);
            String date3 = formatIsoDate(now - 5L * 24 * 60 * 60 * 1000);
            String date4 = formatIsoDate(now);

            String s1 = generateStageJson("Raw Material Extraction", "Farm A, Country X", date1, co2Stage1, "Low", 
                "\"Method\": \"Sustainable Farming\", \"Water Usage\": \"" + ((baseNum % 100) + 50) + " Liters\", \"Soil Quality\": \"Grade A\"");
            String s2 = generateStageJson("Manufacturing", "Factory B, Country Y", date2, co2Stage2, "Medium", 
                "\"Energy Source\": \"70% Solar, 30% Grid\", \"Waste Recycled\": \"" + (80 + (baseNum % 15)) + "%\", \"Certifications\": \"ISO 14001, FairTrade\"");
            String s3 = generateStageJson("Transportation", "Logistics Hub C", date3, co2Stage3, "High", 
                "\"Mode\": \"" + (baseNum % 2 == 0 ? "Ocean Freight & Rail" : "Air & Truck") + "\", \"Distance\": \"" + ((baseNum % 5000) + 1000) + " km\", \"Fuel Type\": \"Bio-Diesel Blend\", \"Carrier\": \"EcoTrans Logistics\"");
            String s4 = generateStageJson("Retail Checkout", "Local Store", date4, co2Stage4, "Low", 
                "\"Packaging\": \"100% Recyclable Cardboard\", \"Storage\": \"Ambient Temperature\", \"Local Transport\": \"" + ((baseNum % 50) + 5) + " km via EV Van\"");

            String json = "{\n" +
                    "  \"success\": true,\n" +
                    "  \"blockchainNetwork\": \"Ethereum (Simulated)\",\n" +
                    "  \"contractAddress\": \"0xMockContract1234567890abcdef1234567890\",\n" +
                    "  \"totalCo2EmissionsKg\": " + String.format(java.util.Locale.US, "%.2f", totalCo2) + ",\n" +
                    "  \"ecoRating\": \"" + rating + "\",\n" +
                    "  \"stages\": [\n" +
                    s1 + ",\n" +
                    s2 + ",\n" +
                    s3 + ",\n" +
                    s4 + "\n" +
                    "  ]\n" +
                    "}";

            sendResponse(exchange, 200, json);
        }

        private String getRating(double totalCo2) {
            if (totalCo2 < 15) return "A";
            if (totalCo2 < 25) return "B";
            return "C";
        }

        private String generateStageJson(String stage, String location, String date, double co2, String pollution, String detailsJson) {
            String baseObj = stage + location + date + co2 + pollution;
            String hash = generateMockHash(baseObj);
            
            return "    {\n" +
                    "      \"stage\": \"" + stage + "\",\n" +
                    "      \"location\": \"" + location + "\",\n" +
                    "      \"date\": \"" + date + "\",\n" +
                    "      \"co2EmissionsKg\": " + co2 + ",\n" +
                    "      \"pollutionIndex\": \"" + pollution + "\",\n" +
                    "      \"details\": { " + detailsJson + " },\n" +
                    "      \"blockchainTxHash\": \"0x" + hash.substring(0, 40) + "\"\n" +
                    "    }";
        }
    }

    private static String generateMockHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            return "1234567890abcdef1234567890abcdef1234567890";
        }
    }

    private static long parseBaseNum(String barcode) {
        String numStr = barcode.replaceAll("\\\\D", "");
        if (numStr.length() > 5) {
            numStr = numStr.substring(0, 5);
        }
        try {
            return numStr.isEmpty() ? 12345L : Long.parseLong(numStr);
        } catch (NumberFormatException e) {
            return 12345L;
        }
    }

    private static String formatIsoDate(long timeMs) {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(timeMs));
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String extractJsonValue(String json, String key) {
        if (json == null || json.isEmpty()) return "";
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) return m.group(1);
        
        // Try numeric/boolean without quotes
        p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*([^,}]+)");
        m = p.matcher(json);
        if (m.find()) return m.group(1).trim();
        
        return "";
    }
    
    private static String extractJsonBlock(String json, String key) {
        if (json == null || json.isEmpty()) return "";
        int ki = json.indexOf("\"" + key + "\"");
        if (ki < 0) return "";
        int ci = json.indexOf(':', ki);
        if (ci < 0) return "";
        int bi = json.indexOf('{', ci);
        if (bi < 0) return "";
        
        int depth = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = bi; i < json.length(); i++) {
            char c = json.charAt(i);
            sb.append(c);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return sb.toString();
            }
        }
        return "";
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
