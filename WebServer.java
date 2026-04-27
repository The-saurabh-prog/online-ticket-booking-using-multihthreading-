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
        server.createContext("/api/cancel", new CancelApiHandler());
        server.createContext("/api/reset", new ResetApiHandler());
        
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool()); 
        server.start();
        BookingLogger.log("WebServer started on http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] response = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

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
            String query = exchange.getRequestURI().getQuery();
            String busId = "Express-101"; // Default
            String date = "2026-04-28"; // Default
            
            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        if ("busId".equals(pair[0])) busId = pair[1];
                        if ("date".equals(pair[0])) date = pair[1];
                    }
                }
            }

            Bus bus = bookingSystem.getBus(busId);
            if (bus == null) {
                sendJsonResponse(exchange, 404, "[]");
                return;
            }

            SeatManager manager = bus.getSeatManager(date);
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (Seat seat : manager.getSeats()) {
                if (!first) json.append(",");
                first = false;
                
                String status = seat.getStatus().toString();
                String holder = seat.getHolderName() != null ? seat.getHolderName() : "";
                
                json.append("{")
                    .append("\"seatNumber\":").append(seat.getSeatNumber()).append(",")
                    .append("\"status\":\"").append(status).append("\",")
                    .append("\"holderName\":\"").append(holder).append("\"")
                    .append("}");
            }
            json.append("]");

            sendJsonResponse(exchange, 200, json.toString());
        }
    }

    private abstract class BasePostApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String query = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String[] params = query.split("&");
                
                int seatNumber = -1;
                String userName = "Anonymous";
                String busId = "Express-101";
                String date = "2026-04-28";

                for (String param : params) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        if ("seatNumber".equals(pair[0])) {
                            try { seatNumber = Integer.parseInt(pair[1]); } catch (Exception ignored) {}
                        } else if ("userName".equals(pair[0])) {
                            userName = java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                        } else if ("busId".equals(pair[0])) {
                            busId = pair[1];
                        } else if ("date".equals(pair[0])) {
                            date = pair[1];
                        }
                    }
                }

                if (seatNumber != -1) {
                    processPost(exchange, busId, date, seatNumber, userName);
                    return;
                }
            }
            sendJsonResponse(exchange, 400, "{\"error\":\"Invalid request\"}");
        }

        protected abstract void processPost(HttpExchange exchange, String busId, String date, int seatNumber, String userName) throws IOException;
    }

    private class BookApiHandler extends BasePostApiHandler {
        @Override
        protected void processPost(HttpExchange exchange, String busId, String date, int seatNumber, String userName) throws IOException {
            UserTask task = new UserTask(bookingSystem, busId, date, userName, seatNumber, false);
            executorService.submit(task);
            sendJsonResponse(exchange, 200, "{\"success\":true}");
        }
    }

    private class CancelApiHandler extends BasePostApiHandler {
        @Override
        protected void processPost(HttpExchange exchange, String busId, String date, int seatNumber, String userName) throws IOException {
            Bus bus = bookingSystem.getBus(busId);
            if (bus != null) {
                SeatManager sm = bus.getSeatManager(date);
                // Attempt to deselect (if SELECTED) or cancel (if BOOKED)
                boolean deselected = sm.deselectSeat(seatNumber, userName);
                boolean cancelled = sm.cancelBooking(seatNumber, userName);
                
                sendJsonResponse(exchange, 200, "{\"success\":" + (deselected || cancelled) + "}");
            } else {
                sendJsonResponse(exchange, 404, "{\"success\":false}");
            }
        }
    }

    private class ResetApiHandler extends BasePostApiHandler {
        @Override
        protected void processPost(HttpExchange exchange, String busId, String date, int seatNumber, String userName) throws IOException {
            Bus bus = bookingSystem.getBus(busId);
            if (bus != null) {
                bus.getSeatManager(date).resetAllSeats();
            }
            sendJsonResponse(exchange, 200, "{\"success\":true}");
        }
    }
}
