package main.servers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import main.helpers.JSONParser;
import main.services.SocketService;
import main.services.SocketServiceImpl;
import org.json.JSONObject;

public class ContentServer {
    private final String contentServerId;
    private JSONObject data;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    /**
     * Constructor for the ContentServer class.
     *
     * @param socketService The SocketService implementation to use.
     */
    public ContentServer(SocketService socketService) {
        this.contentServerId = UUID.randomUUID().toString();
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

        sendPutRequestWithRetry(serverName, portNumber);
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
     * Send a PUT request with weather data to the specified server with retry.
     *
     * @param serverName The server's name.
     * @param portNumber The server's port number.
     */
    public void sendPutRequestWithRetry(String serverName, int portNumber) {
        int maxRetries = 3; // Maximum number of retry attempts
        int retryIntervalMillis = 15000; // 15 seconds in milliseconds

        for (int retryCount = 0; retryCount < maxRetries; retryCount++) {
            try {
                JSONObject jsonData = new JSONObject(data.toString());
                jsonData.put("ServerId", contentServerId);

                clientSocket = new Socket(serverName, portNumber);
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String serverLC = in.readLine();
                int timestamp = Integer.parseInt(serverLC);

                // Send request to the server
                String putRequest = buildPutRequest(serverName, jsonData, timestamp);
                out.println(putRequest);

                String response = receiveResponseFromServer(in);

                if (response != null && (response.contains("200 OK") || response.contains("201 OK"))) {
                    System.out.println("Weather data sent to server.");
                    return; // Successful response, no need to retry
                } else {
                    System.err.println("Error when sending weather data to server: " + response);
                }

                closeClient(in, out, clientSocket);
            } catch (IOException e) {
                System.err.println("Server error: " + e.getMessage());
            }

            if (retryCount < maxRetries - 1) {
                System.out.println("Retrying in 15 seconds...");
                try {
                    Thread.sleep(retryIntervalMillis);
                } catch (InterruptedException e) {
                    System.err.println("Retry delay interrupted.");
                }
            }
        }

        System.err.println("Failed to send weather data after " + maxRetries + " retries.");
    }

    private String receiveResponseFromServer(BufferedReader in) throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            response.append(line).append("\n");
        }

        return response.toString();
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
    private String buildPutRequest(String serverName, JSONObject jsonData, int timeStamp) {
        return String.format("PUT /uploadData HTTP/1.1\r\n" +
                "ServerId: %s\r\n" +
                "LamportClock: %d\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: %d\r\n" +
                "\r\n" +
                "%s", contentServerId, timeStamp, jsonData.toString().length(), jsonData);
    }

    public void closeClient(BufferedReader in, PrintWriter out, Socket clienSocket) {
        try {
            // Close input, output streams, and the client socket.
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (clientSocket != null)
                clientSocket.close();
        } catch (IOException e) {
            System.err.println("IO Error: An IO Exception occurred - " + e.getMessage());
        }
    }
}
