import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.util.UUID;

public class RailControlSystem {
    private static final String DB_URL = "jdbc:sqlite:transit_audit.db";
    
    // Core Shared State Variables
    private static boolean isTrackOccupied = false;
    private static String currentTrainOnTrack = "NONE";

    public static void main(String[] args) throws Exception {
        initDatabase();
        
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        
        server.createContext("/rail/request", new TrackRequestHandler());
        server.createContext("/rail/release", new TrackReleaseHandler());
        server.createContext("/rail/status", new StatusHandler());
        
        server.setExecutor(null);
        System.out.println("⚡ HelixRail Concurrency Broker Active on port 8000...");
        server.start();
    }

    private static void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transit_ledger (
                    log_id TEXT PRIMARY KEY,
                    train_id TEXT,
                    action_type TEXT,
                    track_state TEXT,
                    timestamp TEXT
                )
            """);
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }

    // ── CRITICAL SECTION MULTI-THREAD SYNCHRONIZATION ───────────────────────
    public static synchronized boolean acquireTrack(String trainId, long timeoutMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        
        // Guarded Suspension Pattern using wait loop to prevent race conditions
        while (isTrackOccupied) {
            long remainingTime = timeoutMs - (System.currentTimeMillis() - startTime);
            if (remainingTime <= 0) {
                logTransitEvent(trainId, "LOCK_TIMEOUT", "OCCUPIED");
                return false; // Thread pool block timeout reached
            }
            RailControlSystem.class.wait(remainingTime);
        }
        
        // Critical Section Modification
        isTrackOccupied = true;
        currentTrainOnTrack = trainId;
        logTransitEvent(trainId, "ENTER_TRACK", "LOCKED");
        return true;
    }

    public static synchronized void releaseTrack(String trainId) {
        if (currentTrainOnTrack.equals(trainId)) {
            isTrackOccupied = false;
            currentTrainOnTrack = "NONE";
            logTransitEvent(trainId, "EXIT_TRACK", "FREE");
            
            // Wake up all threads waiting in the monitor lock pool queue
            RailControlSystem.class.notifyAll();
        }
    }

    private static void logTransitEvent(String trainId, String action, String state) {
        String logId = "LOG-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO transit_ledger VALUES (?, ?, ?, ?, ?)")) {
            pstmt.setString(1, logId);
            pstmt.setString(2, trainId);
            pstmt.setString(3, action);
            pstmt.setString(4, state);
            pstmt.setString(5, Instant.now().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to write to persistent ledger: " + e.getMessage());
        }
    }

    // ── HTTP ENDPOINT UTILITIES ─────────────────────────────────────────────
    static class TrackRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            enableCORS(exchange);
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String trainId = parseJsonField(body, "train_id");
                
                try {
                    boolean success = acquireTrack(trainId, 4000); // 4 second max lock wait bounds
                    String jsonResponse = String.format("{\"acquired\":%b,\"train_id\":\"%s\"}", success, trainId);
                    sendResponse(exchange, success ? 200 : 429, jsonResponse);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    sendResponse(exchange, 500, "{\"error\":\"Thread Interrupted\"}");
                }
            }
        }
    }

    static class TrackReleaseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            enableCORS(exchange);
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String trainId = parseJsonField(body, "train_id");
                
                releaseTrack(trainId);
                String jsonResponse = String.format("{\"status\":\"RELEASED\",\"train_id\":\"%s\"}", trainId);
                sendResponse(exchange, 200, jsonResponse);
            }
        }
    }

    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            enableCORS(exchange);
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append(String.format(
                    "{\"track_occupied\":%b,\"current_train\":\"%s\",\"history\":[", 
                    isTrackOccupied, currentTrainOnTrack
                ));
                
                try (Connection conn = DriverManager.getConnection(DB_URL);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM transit_ledger ORDER BY timestamp DESC LIMIT 10")) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) jsonBuilder.append(",");
                        jsonBuilder.append(String.format(
                            "{\"train_id\":\"%s\",\"action\":\"%s\",\"state\":\"%s\",\"time\":\"%s\"}",
                            rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)
                        ));
                        first = false;
                    }
                } catch (SQLException ignored) {}
                
                jsonBuilder.append("]}");
                sendResponse(exchange, 200, jsonBuilder.toString());
            }
        }
    }

    private static void enableCORS(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String parseJsonField(String json, String fieldName) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }
}