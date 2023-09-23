package main.servers;

import org.json.JSONObject;
import main.helpers.JSONParser;
import main.services.SocketService;
import main.services.SocketServiceImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class ContentServer {
    private final String contentServerId;
    private final SocketService socketService;
    private JSONObject data;

    /**
     * Constructor for the ContentServer class.
     *
     * @param socketService The SocketService implementation to use.
     */
    public ContentServer(SocketService socketService) {
        this.contentServerId = UUID.randomUUID().toString();
        this.socketService = socketService;
    }

    public static void main(String[] args) {
        ContentServer contentServer = new ContentServer(new SocketServiceImpl());
        try {
            contentServer.start(args);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Start the ContentServer with the provided arguments.
     *
     * @param args Command-line arguments.
     * @throws Exception If an error occurs during startup.
     */
    public void start(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException(
                    "Insufficient arguments. Usage: ContentServer <serverName>:<portNumber> <filePath>");
        }

        String[] serverNameAndPort = args[0].split(":");
        String serverName = serverNameAndPort[0];
        int portNumber = Integer.parseInt(serverNameAndPort[1]);
        String filePath = args[1];

        if (!isDoneReadingData(filePath)) {
            throw new IOException("Unable to load weather data from file: " + filePath);
        }

        sendPutRequest(serverName, portNumber);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            socketService.closeClient();
        }));
    }

    /**
     * Get the weather data.
     *
     * @return The weather data as a JSON object.
     */
    public JSONObject getWeatherData() {
        return data;
    }

    /**
     * Check if reading weather data from a file is completed.
     *
     * @param filePath The path to the file containing weather data.
     * @return True if reading is done, false otherwise.
     * @throws IOException If an error occurs while reading the file.
     */
    public boolean isDoneReadingData(String filePath) throws IOException {
        String fileContent = readFile(filePath);
        data = JSONParser.textToJson(fileContent);
        return true;
    }

    /**
     * Send a PUT request with weather data to the specified server.
     *
     * @param serverName The server's name.
     * @param portNumber The server's port number.
     */
    public void sendPutRequest(String serverName, int portNumber) {
        try {
            JSONObject jsonData = new JSONObject(data.toString());
            jsonData.put("ServerID", contentServerId);

            String putRequest = buildPutRequest(serverName, jsonData);

            String response = socketService.sendDataToServer(serverName, portNumber, putRequest);

            if (response != null && (response.contains("200 OK") || response.contains("201 OK"))) {
                System.out.println("Weather data sent to server.");
            } else {
                System.err.println("Error when sending weather data to server: " + response);
            }
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    /**
     * Read the content of a file specified by its file path.
     *
     * @param filePath The path to the file to read.
     * @return The content of the file as a string.
     * @throws IOException If an error occurs while reading the file.
     */
    private String readFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        byte[] fileBytes = Files.readAllBytes(path);
        return new String(fileBytes);
    }

    /**
     * Build a PUT request string for uploading weather data to the server.
     *
     * @param serverName The server's name.
     * @param jsonData   The weather data as a JSON object.
     * @return The PUT request string.
     */
    private String buildPutRequest(String serverName, JSONObject jsonData) {
        return String.format("PUT /uploadData HTTP/1.1\r\n" +
                "ServerID: %s\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: %d\r\n" +
                "\r\n" +
                "%s", contentServerId, jsonData.toString().length(), jsonData);
    }
}
