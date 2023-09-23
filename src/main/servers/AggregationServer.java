package main.servers;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import main.helpers.LamportClock;
import main.services.SocketService;
import main.services.SocketServiceImpl;
import org.json.JSONObject;

/**
 * Represents an aggregation server for collecting and processing weather data.
 */
public class AggregationServer {
    private static final int DEFAULT_PORT = 4567;
    private SocketService socketService;
    private LamportClock lamportClock;
    private Thread clientThread;
    // This represents a data structure that maps stationId with a priority queue of
    // weather data. Priority queue is used here so that weather data with the
    // latest timestamp will always be at top. ConcurrentHashMa
    private Map<String, PriorityQueue<WeatherData>> weatherDataMap = new HashMap<>();
    private LinkedBlockingQueue<Socket> requestQueue = new LinkedBlockingQueue<>();
    private String mostRecentStationId;
    private int latestPutTimestamp = -1;

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

        // Start new threads to handle client requests
        printMessage("Server accepting connections...");
        clientThread = new Thread(this::acceptClient);
        clientThread.start();
        handleClient();
    }

    /**
     * Accepts incoming client connections and adds them to a queue for processing.
     */
    private void acceptClient() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = socketService.acceptConnectionFromClient();
                if (clientSocket != null) {
                    printMessage("New client connected.");
                    requestQueue.put(clientSocket);
                    socketService.sendResponseToClient(String.valueOf(this.lamportClock.getCurrentTime()),
                            clientSocket);
                }
            } catch (IOException e) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                System.err.println("IO Error: An IO Exception occurred - " + e.getMessage());
            } catch (InterruptedException e) {
                System.err.println("Itterupted Error: An Interrupted Exception occurred - " + e.getMessage());
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
    private void handleClient() {
        try {
            while (true) {
                Socket clientSocket = requestQueue.poll(2000, TimeUnit.MILLISECONDS);
                if (clientSocket != null) {
                    // Handle client request and send back the response
                    String requestData = socketService.getDataFromClient(clientSocket);
                    printMessage(requestData);
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
            System.err.println("IO Error: An IO Exception occurred. " + e.getMessage());
        } finally {
            socketService.closeServer();
        }
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
        String type = getRequestType(lines);

        Map<String, String> headers = parseHeaders(lines);
        String content = extractContent(lines);

        if (type.equalsIgnoreCase("GET")) {
            return handleGetRequest(headers);
        } else if (type.equalsIgnoreCase("PUT")) {
            return handlePutRequest(headers, content);
        } else {
            return "400 Bad Request";
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
    public boolean storeWeatherData(JSONObject weatherDataJSON, int lamportTime, String serverId) {
        String stationId = extractStationId(weatherDataJSON);
        System.out.println("StationId test: " + stationId);
        if (stationId == null || stationId.isEmpty()) {
            return false;
        }

        // Update latest station id if this is the latest put request
        if (lamportTime > latestPutTimestamp) {
            mostRecentStationId = stationId;
        }
        WeatherData weatherData = new WeatherData(weatherDataJSON, lamportTime, serverId);
        // Check if the weatherDataMap contains an entry for the stationId.
        if (!weatherDataMap.containsKey(stationId)) {
            // If not, create a new PriorityQueue for the stationId.
            PriorityQueue<WeatherData> stationQueue = new PriorityQueue<>();
            weatherDataMap.put(stationId, stationQueue);
        }

        // Add the weatherData to the PriorityQueue for the stationId.
        weatherDataMap.get(stationId).add(weatherData);

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
        boolean isReading = false;

        for (int i = 1; i < lines.length; i++) {
            if (!isReading) {
                if (lines[i].isEmpty()) {
                    isReading = true;
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
        boolean isReading = false;

        for (int i = 1; i < lines.length; i++) {
            if (isReading) {
                contentBuilder.append(lines[i]);
            } else if (lines[i].isEmpty()) {
                isReading = true;
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
        int lamportTime = Integer.parseInt(headers.getOrDefault("LamportClock", "0"));
        lamportClock.receiveLCTime(lamportTime);

        String stationId = headers.get("StationId");
        // If client didn't specify a stationId, the most recent station id will be used
        if (stationId == null || stationId.isEmpty() && mostRecentStationId != null) {
            stationId = mostRecentStationId;
        }

        PriorityQueue<WeatherData> weatherDataQueue = weatherDataMap.get(stationId);
        if (weatherDataQueue == null || weatherDataQueue.isEmpty()) {
            return "404 Data Not Found";
        }

        Optional<WeatherData> weatherData = weatherDataQueue.stream()
                .filter(data -> data.getTimestamp() <= lamportTime)
                .findFirst();

        if (!weatherData.isPresent()) {
            return "404 Data Not Found";
        }

        return weatherData.get().getWeatherData().toString();
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
        System.out.println("Tesst LP" + lamportTime);
        lamportClock.receiveLCTime(lamportTime);

        String serverId = headers.get("ServerId");
        if (serverId == null || serverId.isEmpty()) {
            return "400 Null ServerId";
        }

        JSONObject weatherDataJSON;
        try {
            weatherDataJSON = new JSONObject(content);
        } catch (Exception e) {
            return "400 JSON Error";
        }

        if (storeWeatherData(weatherDataJSON, lamportTime, serverId)) {
            return "200 OK";
        } else {
            return "400 Null StationId";
        }
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
