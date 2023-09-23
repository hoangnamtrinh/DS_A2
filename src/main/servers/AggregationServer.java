package main.servers;

import org.json.JSONObject;
import main.helpers.LamportClock;
import main.services.SocketService;
import main.services.SocketServiceImpl;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

/**
 * Represents an aggregation server for collecting and processing weather data.
 */
public class AggregationServer {
    private static final int DEFAULT_PORT = 4567;
    private volatile boolean isShutdown = false;
    private SocketService socketService;
    private LamportClock lamportClock;
    private Thread clientThread;
    private Map<String, PriorityQueue<WeatherData>> weatherDataMap = new ConcurrentHashMap<>();
    private LinkedBlockingQueue<Socket> requestQueue = new LinkedBlockingQueue<>();

    public static void main(String[] args) {
        int port = parsePortFromArgs(args);
        SocketService socketService = new SocketServiceImpl();
        AggregationServer aggregationServer = new AggregationServer(socketService);
        aggregationServer.start(port);
    }

    public AggregationServer(SocketService socketService) {
        this.socketService = socketService;
        this.lamportClock = new LamportClock();
    }

    /**
     * Starts the aggregation server on the specified port.
     *
     * @param portNumber The port number on which the server should listen.
     */
    public void start(int portNumber) {
        // Start server
        printMessage("Starting server...");
        try {
            socketService.startServer(portNumber);
        } catch (IOException e) {
            System.err.println("IO Error: An IO Exception occurred - " + e.getMessage());
        }

        // Start shutdown watch mode
        printMessage("Type 'SHUTDOWN' to stop the server.");
        Thread monitorThread = new Thread(this::watchForShutdown);
        monitorThread.start();

        // Start new threads to handle client requests
        printMessage("Server starting client threads...");
        clientThread = new Thread(this::acceptClient);
        clientThread.start();
        processClientRequests();
    }

    /**
     * Monitors the console for a "SHUTDOWN" command and triggers the server
     * shutdown when detected.
     */
    private void watchForShutdown() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (!isShutdown && scanner.hasNextLine()) {
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("SHUTDOWN")) {
                    shutdownServer();
                    break;
                }
            }
        }
    }

    /**
     * Accepts incoming client connections and adds them to a queue for processing.
     */
    private void acceptClient() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = socketService.acceptConnection();
                if (clientSocket != null) {
                    printMessage("Client connected " + clientSocket);
                    requestQueue.put(clientSocket);
                }
            } catch (IOException e) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                System.err.println("IO Error: An IO Exception occurred - " + e.getMessage());
            } catch (InterruptedException e) {
                System.err.println("Interupted Error: An Interrupted Exception occurred - " + e.getMessage());
            }
        }
    }

    /**
     * Processes client requests by dequeuing client sockets, handling their
     * requests, and sending responses.
     * This method continuously checks for incoming client connections in the
     * request queue, processes their
     * requests, and sends back responses until the server is shut down.
     */
    private void processClientRequests() {
        try {
            while (!isShutdown) {
                Socket clientSocket = requestQueue.poll(10, TimeUnit.MILLISECONDS);
                if (clientSocket != null) {
                    // Handle client request and send back the response
                    String requestData = socketService.getDataFromClient(clientSocket);
                    // printMessage("Client Request: " + requestData);
                    if (requestData != null) {
                        String responseData = handleClientRequest(requestData);
                        socketService.sendResponseToClient(responseData, clientSocket);
                    }
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("IO Error: An IO Exception occurred - " + e.getMessage());
        } finally {
            socketService.closeServer();
        }
    }

    /**
     * Adds weather data to the server's data store.
     *
     * @param weatherDataJSON The JSON representation of weather data to be added.
     * @param lamportTime     The Lamport timestamp associated with the data.
     * @param serverId        The ID of the server sending the data.
     * @return true if the data is successfully added, false otherwise.
     */
    public boolean storeData(JSONObject weatherDataJSON, int lamportTime, String serverId) {
        String stationId = extractStationId(weatherDataJSON);
        if (stationId == null || stationId.isEmpty()) {
            return false;
        }
        WeatherData newData = new WeatherData(weatherDataJSON, lamportTime, serverId);
        weatherDataMap.computeIfAbsent(stationId, k -> new PriorityQueue<>()).add(newData);
        return true;
    }

    /**
     * Extracts the station ID from the provided weather data JSON.
     *
     * @param weatherDataJSON The JSON representation of weather data.
     * @return The extracted station ID or null if not found.
     */
    private String extractStationId(JSONObject weatherDataJSON) {
        return weatherDataJSON.optString("id", null);
    }

    /**
     * Handles incoming client requests by parsing the request type and delegating
     * to appropriate handlers.
     *
     * @param requestData The raw HTTP request data received from the client.
     * @return The HTTP response to be sent back to the client.
     */
    public String handleClientRequest(String requestData) {
        String[] lines = requestData.split("\r\n");
        String requestType = getRequestType(lines);

        Map<String, String> headers = parseHeaders(lines);
        String content = extractContent(lines);

        if (requestType.equalsIgnoreCase("GET")) {
            return handleGetRequest(headers);
        } else if (requestType.equalsIgnoreCase("PUT")) {
            return handlePutRequest(headers, content);
        } else {
            return "400 Bad Request";
        }
    }

    /**
     * Extracts the HTTP request type (e.g., GET or PUT) from the request lines.
     *
     * @param lines The lines of the HTTP request.
     * @return The HTTP request type (GET or PUT).
     */
    private String getRequestType(String[] lines) {
        return lines[0].split(" ")[0].trim();
    }

    /**
     * Parses HTTP headers from the request lines and stores them in a map.
     *
     * @param lines The lines of the HTTP request.
     * @return A map containing parsed HTTP headers.
     */
    private Map<String, String> parseHeaders(String[] lines) {
        Map<String, String> headers = new HashMap<>();
        boolean readingContent = false;

        for (int i = 1; i < lines.length; i++) {
            if (!readingContent) {
                if (lines[i].isEmpty()) {
                    readingContent = true;
                } else {
                    String[] headerParts = lines[i].split(": ", 2);
                    headers.put(headerParts[0], headerParts[1]);
                }
            }
        }
        return headers;
    }

    /**
     * Extracts the content/body of the HTTP request.
     *
     * @param lines The lines of the HTTP request.
     * @return The content/body of the HTTP request.
     */
    private String extractContent(String[] lines) {
        StringBuilder contentBuilder = new StringBuilder();
        boolean readingContent = false;

        for (int i = 1; i < lines.length; i++) {
            if (readingContent) {
                contentBuilder.append(lines[i]);
            } else if (lines[i].isEmpty()) {
                readingContent = true;
            }
        }
        return contentBuilder.toString();
    }

    /**
     * Handles GET requests by returning weather data based on the Lamport timestamp
     * and station ID.
     *
     * @param headers The HTTP headers of the GET request.
     * @return The HTTP response containing weather data or an error message.
     */
    private String handleGetRequest(Map<String, String> headers) {
        int lamportTime = Integer.parseInt(headers.getOrDefault("LamportClock", "-1"));
        lamportClock.receiveLCTime(lamportTime);

        String stationId = headers.get("StationID");
        if (stationId == null || stationId.isEmpty()) {
            Optional<String> optionalStationId = weatherDataMap.keySet().stream().findFirst();
            if (optionalStationId.isPresent()) {
                stationId = optionalStationId.get();
            } else {
                return "404 Not Found";
            }
        }

        PriorityQueue<WeatherData> weatherDataQueue = weatherDataMap.get(stationId);
        if (weatherDataQueue == null || weatherDataQueue.isEmpty()) {
            return "404 Not Found";
        }

        Optional<WeatherData> dataStream = weatherDataQueue.stream()
                .filter(data -> data.getTimestamp() <= lamportTime)
                .findFirst();

        if (dataStream.isPresent()) {
            return "404 Not Found";
        }

        return dataStream.get().getWeatherData().toString();
    }

    /**
     * Handles PUT requests by adding weather data to the server's data store.
     *
     * @param headers The HTTP headers of the PUT request.
     * @param content The content/body of the PUT request containing weather data.
     * @return The HTTP response indicating the success or failure of the operation.
     */
    private String handlePutRequest(Map<String, String> headers, String content) {
        int lamportTime = Integer.parseInt(headers.getOrDefault("LamportClock", "0"));
        lamportClock.receiveLCTime(lamportTime);

        String serverId = headers.get("ServerID");
        if (serverId == null || serverId.isEmpty()) {
            return "400 Bad Request";
        }

        JSONObject weatherDataJSON;
        try {
            weatherDataJSON = new JSONObject(content);
        } catch (Exception e) {
            return "400 Bad Request";
        }

        if (storeData(weatherDataJSON, lamportTime, serverId)) {
            return "200 OK";
        } else {
            return "400 Bad Request";
        }
    }

    /**
     * Shuts down the server gracefully by setting the shutdown flag and
     * interrupting the client thread.
     */
    private void shutdownServer() {
        this.isShutdown = true;
        if (clientThread != null) {
            clientThread.interrupt();
        }
        printMessage("Shutting down server...");
    }

    /**
     * Parses the port number from command-line arguments or uses the default port
     * if none is provided.
     *
     * @param args The command-line arguments provided when starting the server.
     * @return The parsed port number.
     */
    private static int parsePortFromArgs(String[] args) {
        if (args.length > 0) {
            return Integer.parseInt(args[0]);
        }
        return DEFAULT_PORT;
    }

    /**
     * Prints a message to the console.
     *
     * @param message The message to be printed.
     */
    private void printMessage(String message) {
        System.out.println(message);
    }
}

class WeatherData implements Comparable<WeatherData> {
    // Weather data as a JSON object
    private JSONObject weatherData;

    // Lamport clock time associated with the weather data
    private int timestamp;

    // ID of the server from which the data is received
    private String serverId;

    public WeatherData(JSONObject data, int timestamp, String serverId) {
        this.weatherData = data;
        this.timestamp = timestamp;
        this.serverId = serverId;
    }

    // Getter method for retrieving the weather data
    public JSONObject getWeatherData() {
        return weatherData;
    }

    // Getter method for retrieving the Lamport clock time
    public int getTimestamp() {
        return timestamp;
    }

    // Getter method for retrieving the server ID
    public String getServerId() {
        return serverId;
    }

    // Comparison method required for implementing Comparable interface
    @Override
    public int compareTo(WeatherData other) {
        // Compare WeatherData objects based on their Lamport clock times
        return Integer.compare(this.timestamp, other.timestamp);
    }
}
