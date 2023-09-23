package main.servers;

import main.helpers.JSONParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * The GETClient class represents a client that sends a GET request to a server and processes the response.
 */
public class GETClient {
    private final String serverID;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public GETClient() {
        this.serverID = UUID.randomUUID().toString();
    }

    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                System.out.println("Insufficient arguments. Usage: GETClient <serverName>:<portNumber> [<stationId>]");
                return;
            }

            // Split the first argument into serverName and portNumber and parse stationId
            String[] serverNameAndPort = args[0].split(":");
            String serverName = serverNameAndPort[0];
            int portNumber = Integer.parseInt(serverNameAndPort[1]);
            String stationId = null;
            if (args.length == 2) {
                stationId = args[1];
            }

            // Initialise client, send a request to the aggregation server and display the
            // response
            GETClient client = new GETClient();
            GETClientDataResponse response = client.retrieveWeatherData(serverName, portNumber, stationId);
            client.processResponse(response);

            // socketService.closeClient();
        } catch (IOException e) {
            System.err.println("IO Error: An IO Exception occurred - " + e.getMessage());
        } catch (JSONException e) {
            System.err.println("JSON Error: An error occurred while processing JSON data - " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Number Format Error: Invalid port number - " + e.getMessage());
        }
    }

    /**
     * Processes the response received from the server.
     *
     * @param response The response data to be processed.
     */
    public void processResponse(GETClientDataResponse response) {
        // Check if the response is null, indicating no response received from the
        // server
        if (response == null) {
            System.out.println("Error: No response received from the server.");
            return;
        }

        if (response.isError()) {
            System.err.println(response.getErrorMessage());
        } else {
            // Convert JSON data to text and display it line by line
            String weatherDataText = JSONParser.jsonToText(response.getData(), false, true);
            String[] lines = weatherDataText.split("\n");
            for (String line : lines) {
                System.out.println(line);
            }
        }
    }

    /**
     * Retrieves weather data from the server.
     *
     * @param serverName The name of the server.
     * @param portNumber The port number of the server.
     * @param stationId  The station ID (optional).
     * @return A GETClientDataResponse object representing the server's response.
     * @throws IOException   If an I/O error occurs.
     * @throws JSONException If a JSON error occurs.
     */
    public GETClientDataResponse retrieveWeatherData(String serverName, int portNumber, String stationId)
            throws IOException, JSONException {
        // Create a client socket and initialize input and output streams.
        clientSocket = new Socket(serverName, portNumber);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String serverLC = in.readLine();
        int timestamp = Integer.parseInt(serverLC);
        String getRequest = buildGetRequest(this.serverID, timestamp, stationId);

        // Send request to the server
        out.println(getRequest);

        String responseStr = receiveResponseFromServer(in);

        if (responseStr.startsWith("400") || responseStr.startsWith("404") || responseStr.startsWith("500")) {
            return createErrorResponse(responseStr);
        }

        JSONObject jsonObject = parseJsonResponse(responseStr);
        closeClient(in, out, clientSocket);
        return createSuccessResponse(jsonObject);
    }

    /**
     * Builds a GET request message to send to the server.
     *
     * @param serverId    The server ID.
     * @param currentTime The current Lamport clock timestamp.
     * @param stationId   The station ID (optional).
     * @return The formatted GET request message.
     */
    private String buildGetRequest(String serverId, int currentTime, String stationId) {
        StringBuilder getRequestBuilder = new StringBuilder();
        getRequestBuilder.append("GET /weather.json HTTP/1.1\r\n");
        getRequestBuilder.append("ServerId: ").append(serverId).append("\r\n");
        getRequestBuilder.append("LamportClock: ").append(currentTime).append("\r\n");

        if (stationId != null) {
            getRequestBuilder.append("StationId: ").append(stationId).append("\r\n");
        }

        getRequestBuilder.append("\r\n");
        return getRequestBuilder.toString();
    }

    /**
     * Creates a GETClientDataResponse object representing an error response.
     *
     * @param errorMessage The error message.
     * @return A GETClientDataResponse object with isError set to true.
     */
    private GETClientDataResponse createErrorResponse(String errorMessage) {
        return GETClientDataResponse.error(errorMessage);
    }

    /**
     * Parses a JSON response string into a JSONObject.
     *
     * @param responseStr The JSON response string.
     * @return The parsed JSONObject.
     * @throws JSONException If a JSON error occurs during parsing.
     */
    private JSONObject parseJsonResponse(String responseStr) throws JSONException {
        return new JSONObject(new JSONTokener(responseStr));
    }

    /**
     * Creates a GETClientDataResponse object representing a success response.
     *
     * @param jsonObject The JSON data.
     * @return A GETClientDataResponse object with isError set to false.
     */
    private GETClientDataResponse createSuccessResponse(JSONObject jsonObject) {
        return GETClientDataResponse.success(jsonObject);
    }

    /**
     * Receives and reads the response from the server.
     *
     * @param in The BufferedReader for reading the server's response.
     * @return The response string received from the server.
     * @throws IOException If an I/O error occurs.
     */
    public String receiveResponseFromServer(BufferedReader in) throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            response.append(line).append("\n");
        }

        return response.toString();
    }

    /**
     * Closes the client's input, output streams, and socket.
     *
     * @param in          The BufferedReader for input.
     * @param out         The PrintWriter for output.
     * @param clientSocket The client socket to be closed.
     */
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

class GETClientDataResponse {
    private final boolean isError;
    private final String errorMessage;
    private final JSONObject data;

    private GETClientDataResponse(boolean isError, String errorMessage, JSONObject data) {
        this.isError = isError;
        this.errorMessage = errorMessage;
        this.data = data;
    }

    public static GETClientDataResponse error(String errorMessage) {
        return new GETClientDataResponse(true, errorMessage, null);
    }

    public static GETClientDataResponse success(JSONObject data) {
        return new GETClientDataResponse(false, null, data);
    }

    public boolean isError() {
        return isError;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public JSONObject getData() {
        return data;
    }
}
