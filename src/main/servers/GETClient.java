package main.servers;

import main.services.SocketService;
import main.services.SocketServiceImpl;
import main.helpers.LamportClock;
import main.helpers.JSONParser;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.UUID;

public class GETClient {
    private final String serverID;
    private final SocketService socketService;
    private final LamportClock lamportClock;

    public GETClient(SocketService socketService) {
        this.serverID = UUID.randomUUID().toString();
        this.socketService = socketService;
        this.lamportClock = new LamportClock();
    }

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.out.println("Insufficient arguments. Usage: GETClient <serverName>:<portNumber> <stationId>");
                return;
            }

            // Split the first argument into serverName and portNumber and parse stationId
            String[] serverNameAndPort = args[0].split(":");
            String serverName = serverNameAndPort[0];
            int portNumber = Integer.parseInt(serverNameAndPort[1]);
            String stationId = null;
            if (args.length == 3) {
                stationId = args[2];
            }

            // Initialise client, send a request to the aggregation server and display the
            // response
            SocketService socketService = new SocketServiceImpl();
            GETClient client = new GETClient(socketService);
            GETClientDataResponse response = client.retrieveWeatherData(serverName, portNumber, stationId);
            client.processResponse(response);

            socketService.closeClient();
        } catch (IOException e) {
            System.err.println("IO Error: An IO Exception occurred - " + e.getMessage());
        } catch (JSONException e) {
            System.err.println("JSON Error: An error occurred while processing JSON data - " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Number Format Error: Invalid port number - " + e.getMessage());
        }
    }

    public void processResponse(GETClientDataResponse response) {
        // Check if the response is null, indicating no response received from the
        // server
        if (response == null) {
            System.out.println("Error: No response received from the server.");
            return;
        }

        if (response.isError()) {
            System.out.println("Server Error: " + response.getErrorMessage());
        } else {
            // Convert JSON data to text and display it line by line
            String weatherDataText = JSONParser.jsonToText(response.getData(), false, true);
            String[] lines = weatherDataText.split("\n");
            for (String line : lines) {
                System.out.println(line);
            }
        }
    }

    public GETClientDataResponse retrieveWeatherData(String serverName, int portNumber, String stationId)
            throws IOException, JSONException {
        int currentTime = lamportClock.sendLCTime();
        String getRequest = buildGetRequest(this.serverID, currentTime, stationId);

        String responseStr = socketService.sendDataToServer(serverName, portNumber, getRequest);

        if (responseStr.startsWith("500")) {
            return createErrorResponse(
                    "Server Error: The server encountered an internal issue while processing the request.");
        }

        JSONObject jsonObject = parseJsonResponse(responseStr);
        updateLamportClock(jsonObject);

        return createSuccessResponse(jsonObject);
    }

    private String buildGetRequest(String serverId, int currentTime, String stationId) {
        StringBuilder getRequestBuilder = new StringBuilder();
        getRequestBuilder.append("GET /weather.json HTTP/1.1\r\n");
        getRequestBuilder.append("ServerID: ").append(serverId).append("\r\n");
        getRequestBuilder.append("LamportClock: ").append(currentTime).append("\r\n");

        if (stationId != null) {
            getRequestBuilder.append("StationID: ").append(stationId).append("\r\n");
        }

        getRequestBuilder.append("\r\n");
        return getRequestBuilder.toString();
    }

    private GETClientDataResponse createErrorResponse(String errorMessage) {
        return GETClientDataResponse.error(errorMessage);
    }

    private JSONObject parseJsonResponse(String responseStr) throws JSONException {
        return new JSONObject(new JSONTokener(responseStr));
    }

    private void updateLamportClock(JSONObject jsonObject) {
        if (jsonObject.has("LamportClock")) {
            int receivedLamportClock = jsonObject.getInt("LamportClock");
            lamportClock.receiveLCTime(receivedLamportClock);
        }
    }

    private GETClientDataResponse createSuccessResponse(JSONObject jsonObject) {
        return GETClientDataResponse.success(jsonObject);
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
