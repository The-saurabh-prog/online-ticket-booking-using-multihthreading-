import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;

public class WebServer {

    private final BookingSystem bookingSystem;
    private final ExecutorService executorService;
    private HttpServer server;

    public WebServer(BookingSystem bookingSystem, ExecutorService executorService) {
        this.bookingSystem = bookingSystem;
        this.executorService = executorService;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", new StaticFileHandler("index.html"));
        server.createContext("/api/seats", new SeatsApiHandler());
        server.createContext("/api/book", new BookApiHandler());
        
        // Use a small thread pool for HTTP requests to prevent blocking
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool()); 
        server.start();
        BookingLogger.log("WebServer started on http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // --- Handlers ---

    private class StaticFileHandler implements HttpHandler {
        private final String fileName;

        public StaticFileHandler(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                byte[] response = Files.readAllBytes(Paths.get(fileName));
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            } catch (IOException e) {
                String response = "404 File Not Found";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }

    private class SeatsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Build a simple JSON array manually to avoid external libraries
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (Seat seat : bookingSystem.getSeats()) {
                if (!first) {
                    json.append(",");
                }
                first = false;
                
                // Read fields safely
                String status = seat.getStatus().toString();
                String holder = seat.getHolderName() != null ? seat.getHolderName() : "";
                
                json.append("{")
                    .append("\"seatNumber\":").append(seat.getSeatNumber()).append(",")
                    .append("\"status\":\"").append(status).append("\",")
                    .append("\"holderName\":\"").append(holder).append("\"")
                    .append("}");
            }
            json.append("]");

            byte[] response = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            
            // Allow CORS just in case
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }

    private class BookApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String query = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                // Simple parsing for: seatNumber=3&userName=Alice
                String[] params = query.split("&");
                int seatNumber = -1;
                String userName = "Anonymous";

                for (String param : params) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        if ("seatNumber".equals(pair[0])) {
                            try {
                                seatNumber = Integer.parseInt(pair[1]);
                            } catch (NumberFormatException ignored) {}
                        } else if ("userName".equals(pair[0])) {
                            userName = java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                        }
                    }
                }

                if (seatNumber != -1) {
                    // Simulate normal wait (false) for payment via WebUI
                    UserTask task = new UserTask(bookingSystem, userName, seatNumber, false);
                    executorService.submit(task);

                    String response = "{\"success\":true}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(200, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                    return;
                }
            }

            // Bad request
            String response = "{\"error\":\"Invalid request\"}";
            exchange.sendResponseHeaders(400, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}
