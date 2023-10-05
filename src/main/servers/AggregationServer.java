package main.servers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import main.helpers.LamportClock;
import main.services.SocketService;
import main.services.SocketServiceImpl;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Represents an aggregation server for collecting and processing weather data.
 */
public class AggregationServer {
    private static final int DEFAULT_PORT = 4567;
    private SocketService socketService;
    private LamportClock lamportClock;
    private Thread clientThread;

    /**
     * This represents a data structure that maps stationId with a priority queue of
     * weather data. Priority queue is used here so that weather data with the
     * latest timestamp will always be at top.
     */
    private Map<String, PriorityQueue<WeatherData>> weatherDataMap = new HashMap<>();

    /**
     * This represents a data structure that maps a content serverId with the last
     * time it sent a put request to the aggregation server.
     */
    private Map<String, Long> serverTimestampMap = new HashMap<>();

    private LinkedBlockingQueue<Socket> requestQueue = new LinkedBlockingQueue<>();
    private String mostRecentStationId;
    private int latestPutTimestamp = -1;

    public static void main(String[] args) {
        int saveIntervalMillis = 15 * 1000; // 15 seconds;
        int port = parsePortFromArgs(args);
        SocketService socketService = new SocketServiceImpl();
        AggregationServer aggregationServer = new AggregationServer(socketService);

        // Get the path to data.json
        String currentDirectory = System.getProperty("user.dir");
        String filePath = currentDirectory + File.separator + "data.json";

        // Start the state-saving thread
        StateSaverThread stateSaverThread = new StateSaverThread(aggregationServer,
                filePath, saveIntervalMillis);
        stateSaverThread.start();
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
        if (stationId == null || stationId.isEmpty()) {
            return false;
        }

        // Update latest station id if this is the latest put request
        if (lamportTime > latestPutTimestamp) {
            mostRecentStationId = stationId;
            latestPutTimestamp = lamportTime;
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

        // Make sure that only data from server that comminicated within the last 30
        // seconds is taken into account.
        Optional<WeatherData> weatherData = weatherDataQueue.stream()
                .filter(data -> data.getTimestamp() <= lamportTime
                        && (serverTimestampMap.get(data.getServerId()) != null
                                && serverTimestampMap.get(data.getServerId()) >= System.currentTimeMillis() - 30000))
                .findFirst();

        if (!weatherData.isPresent()) {
            return "404 Data Not Found 1";
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

        serverTimestampMap.put(serverId, System.currentTimeMillis());
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
     * Stops the aggregation server.
     */
    public void stopServer() {
        printMessage("Stopping server...");

        // Interrupt the client thread to stop accepting new connections
        clientThread.interrupt();

        // Close the server socket to stop accepting new connections
        socketService.closeServer();

        // Optionally, you can wait for the client thread to finish
        try {
            clientThread.join();
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for the client thread to finish.");
        }

        // Optionally, you can perform any cleanup or shutdown operations here

        printMessage("Server stopped.");
    }

    /**
     * Prints a message to the console.
     *
     * @param message The message to be printed.
     */
    private void printMessage(String message) {
        System.out.println(message);
    }

    // Getter and Setter methods for state variables
    public Map<String, PriorityQueue<WeatherData>> getWeatherDataMap() {
        return weatherDataMap;
    }

    public void setWeatherDataMap(Map<String, PriorityQueue<WeatherData>> weatherDataMap) {
        this.weatherDataMap = weatherDataMap;
    }

    public LinkedBlockingQueue<Socket> getRequestQueue() {
        return requestQueue;
    }

    public void setRequestQueue(LinkedBlockingQueue<Socket> requestQueue) {
        this.requestQueue = requestQueue;
    }

    public String getMostRecentStationId() {
        return mostRecentStationId;
    }

    public void setMostRecentStationId(String mostRecentStationId) {
        this.mostRecentStationId = mostRecentStationId;
    }

    public int getLatestPutTimestamp() {
        return latestPutTimestamp;
    }

    public void setLatestPutTimestamp(int latestPutTimestamp) {
        this.latestPutTimestamp = latestPutTimestamp;
    }

    public Map<String, Long> getServerTimestamMap() {
        return serverTimestampMap;
    }

    public void setServerTimestamMap(Map<String, Long> serverTimestampMap) {
        this.serverTimestampMap = serverTimestampMap;
    }

    public int getLamportTime() {
        return this.lamportClock.getCurrentTime();
    }

    public void setLamportTime(int time) {
        this.lamportClock.setCurrentTime(time);
    }
}

/**
 * This data structure provides a way to compare weather data based on their
 * lamport clock timestamp so that we can use it in a priority queue.
 */
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

class StateSaverThread extends Thread {
    private ServerStateHandler stateHandler;
    private String filePath;
    private long saveIntervalMillis;

    public StateSaverThread(AggregationServer server, String filePath, long saveIntervalMillis) {
        this.stateHandler = new ServerStateHandler(server);
        this.filePath = filePath;
        this.saveIntervalMillis = saveIntervalMillis;
    }

    /**
     * Runs the StateSaverThread, loading server state and periodically saving it.
     */
    @Override
    public void run() {
        try {
            stateHandler.loadServerState(filePath);
            System.out.println("Sever state loaded.\n");
        } catch (Exception e) {
            File file = new File(filePath);
            if (file.exists()) {
                System.out.println("Can't load server data.");
            } else {
                System.out.println("File does not exist at the specified path.");
            }
        }
        while (true) {
            stateHandler.saveServerState(filePath);
            try {
                Thread.sleep(saveIntervalMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class ServerStateHandler {
    private AggregationServer server;

    public ServerStateHandler(AggregationServer server) {
        this.server = server;
    }

    public AggregationServer getServer() {
        return this.server;
    }

    /**
     * Saves the server state to a JSON file.
     *
     * @param filePath The path to the file where the state will be saved.
     */
    public void saveServerState(String filePath) {
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            JSONObject stateJson = new JSONObject();
            Map<String, PriorityQueue<WeatherData>> weatherData = new HashMap<>(server.getWeatherDataMap());
            WeatherDataWriter weatherDataWriter = new WeatherDataWriter(weatherData);
            JSONObject weatherObject = weatherDataWriter.writeWeatherDataToObject();
            JSONObject serverTimestampMapObject = new JSONObject(server.getServerTimestamMap());

            stateJson.put("weatherDataMap", weatherObject);
            stateJson.put("mostRecentStationId", server.getMostRecentStationId());
            stateJson.put("latestPutTimestamp", server.getLatestPutTimestamp());
            stateJson.put("lamportTime", server.getLamportTime());
            stateJson.put("serverTimestampMap", serverTimestampMapObject);

            fileWriter.write(stateJson.toString());
            System.out.println("Server state saved to data.json file.\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the server state from a JSON file.
     *
     * @param fileName The name of the file from which to load the state.
     */
    public void loadServerState(String fileName) {
        String filePath = "data.json";
        try {
            // Read JSON data from the file as a string
            String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));

            // Parse the JSON string into a JSONObject
            JSONObject jsonObject = new JSONObject(jsonContent);

            // Extract the values
            int latestPutTimestamp = jsonObject.getInt("latestPutTimestamp");
            String mostRecentStationId = jsonObject.getString("mostRecentStationId");
            int lamportTime = jsonObject.getInt("lamportTime");
            JSONObject serverTimestampMap = jsonObject.getJSONObject("serverTimestampMap");
            JSONObject weatherDataMap = jsonObject.getJSONObject("weatherDataMap");

            Map<String, PriorityQueue<WeatherData>> deserialisedWeatherDataMap = new HashMap<>();
            for (String stationId : weatherDataMap.keySet()) {
                JSONArray stationData = weatherDataMap.getJSONArray(stationId);
                PriorityQueue<WeatherData> weatherDataQueue = new PriorityQueue<>();

                // Iterate through the JSON array to reconstruct the priority queue
                for (int i = 0; i < stationData.length(); i++) {
                    JSONObject weatherJson = stationData.getJSONObject(i);
                    int timestamp = weatherJson.getInt("timestamp");
                    weatherJson.remove("timestamp");
                    String serverId = weatherJson.getString("ServerId");
                    WeatherData weatherData = new WeatherData(weatherJson, timestamp, serverId);
                    weatherDataQueue.offer(weatherData);
                }

                // Add the reconstructed data to the map
                deserialisedWeatherDataMap.put(stationId, weatherDataQueue);
            }

            // Create a HashMap to store the JSON object content
            Map<String, Long> deserialisedServerTimestampMap = new HashMap<>();

            // Use a for loop to iterate through the keys
            for (String key : serverTimestampMap.keySet()) {
                Long value = serverTimestampMap.getLong(key);
                deserialisedServerTimestampMap.put(key, value);
            }
            server.setLatestPutTimestamp(latestPutTimestamp);
            server.setMostRecentStationId(mostRecentStationId);
            server.setLamportTime(lamportTime);
            server.setWeatherDataMap(deserialisedWeatherDataMap);
            server.setServerTimestamMap(deserialisedServerTimestampMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/**
 * A helper class for writing WeatherData to a JSON object.
 */
class WeatherDataWriter {

    private Map<String, PriorityQueue<WeatherData>> data;

    public WeatherDataWriter(Map<String, PriorityQueue<WeatherData>> weatherDataMap) {
        this.data = weatherDataMap;
    }

    /**
     * Writes weather data to a JSON object.
     *
     * @return The JSON object representing the weather data.
     */
    public JSONObject writeWeatherDataToObject() {
        // Create a JSONObject to store the data
        JSONObject jsonData = new JSONObject();

        // Iterate through the map entries
        for (Map.Entry<String, PriorityQueue<WeatherData>> entry : data.entrySet()) {
            String stationId = entry.getKey();
            PriorityQueue<WeatherData> weatherDataQueue = new PriorityQueue<WeatherData>(entry.getValue());

            // Create a JSONArray to store weather data for this server
            JSONArray serverDataArray = new JSONArray();

            // Iterate through the PriorityQueue and add each WeatherData entry to the array
            while (!weatherDataQueue.isEmpty()) {
                WeatherData weatherData = weatherDataQueue.poll();
                JSONObject weatherJson = weatherData.getWeatherData();
                int timestamp = weatherData.getTimestamp();
                weatherJson.put("timestamp", timestamp);
                serverDataArray.put(weatherJson);
            }

            // Add the server data array to the JSON object with the server ID as the key
            jsonData.put(stationId, serverDataArray);
        }
        return jsonData;
    }
}
