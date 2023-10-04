package tests.servers;

import main.helpers.GETClientDataResponse;
import main.servers.GETClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.Socket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GETClientTest {
  private GETClient getClient;
  private ByteArrayInputStream inputStream;
  private ByteArrayOutputStream outputStream;
  private SocketStub socket;
  

  @Before
  public void setUp() {
    getClient = new GETClient();
    outputStream = new ByteArrayOutputStream();
    socket = new SocketStub();
    socket.setInputStream(inputStream);
    socket.setOutputStream(outputStream);
    getClient.clientSocket = socket;
  }

  @After
  public void tearDown() {
    getClient = null;
  }

  @Test
  public void testBuildGetRequest() {
    String serverId = "server123";
    int currentTime = 12345;
    String stationId = "station456";

    String getRequest = getClient.buildGetRequest(serverId, currentTime, stationId);

    String expectedRequest = "GET /weather.json HTTP/1.1\r\n" +
        "ServerId: server123\r\n" +
        "LamportClock: 12345\r\n" +
        "StationId: station456\r\n" +
        "\r\n";

    assertEquals(expectedRequest, getRequest);
  }

  @Test
  public void testCreateErrorResponse() {
    String errorMessage = "Error message";
    GETClientDataResponse response = getClient.createErrorResponse(errorMessage);
    assertNotNull(response);
    assertEquals(errorMessage, response.getErrorMessage());
    assertEquals(true, response.isError());
  }

  @Test
  public void testParseJsonResponse() throws JSONException {
    String jsonStr = "{\"key\": \"value\"}";
    JSONObject jsonObject = getClient.parseJsonResponse(jsonStr);
    assertNotNull(jsonObject);
    assertEquals("value", jsonObject.getString("key"));
  }

  @Test
  public void testCreateSuccessResponse() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("key", "value");

    GETClientDataResponse response = getClient.createSuccessResponse(jsonObject);
    assertNotNull(response);
    assertEquals(jsonObject.toString(), response.getData().toString());
    assertEquals(false, response.isError());
  }

  @Test
  public void testReceiveResponseFromServer() throws IOException {
    String responseString = "Line 1\nLine 2\n";
    inputStream = new ByteArrayInputStream(responseString.getBytes());
    getClient.in = new BufferedReader(new InputStreamReader(inputStream));
    socket.setInputStream(inputStream);

    String responseStr = getClient.receiveResponseFromServer(getClient.in);

    String expectedResponse = "Line 1\nLine 2\n";
    assertEquals(expectedResponse, responseStr);
  }

  @Test
  public void testCloseClient() {
    getClient.closeClient(getClient.in, getClient.out, getClient.clientSocket);
  }

  // @Test
  // public void testRetrieveWeatherData_SuccessfulResponse() throws IOException, JSONException {
  //   // Mock server response
  //   String serverResponse = "200 OK\n" +
  //       "Content-Type: application/json\n" +
  //       "\n" +
  //       "{\"temperature\": 25.5, \"humidity\": 60.0}";

  //   inputStream = new ByteArrayInputStream(serverResponse.getBytes());
  //   getClient.in = new BufferedReader(new InputStreamReader(inputStream));
  //   socket.setInputStream(inputStream);

  //   GETClientDataResponse response = getClient.retrieveWeatherData("server", 8080, "station");

  //   // Check if the response is not null and contains the expected data
  //   assertNotNull(response);
  //   assertEquals(false, response.isError());
  //   JSONObject data = response.getData();
  //   assertNotNull(data);
  //   assertEquals(25.5, data.getDouble("temperature"), 0.01);
  //   assertEquals(60.0, data.getDouble("humidity"), 0.01);
  // }

  // @Test
  // public void testRetrieveWeatherData_ErrorResponse() throws IOException {
  //   // Mock server response with a 404 error
  //   String serverResponse = "404 Not Found\n" +
  //       "Content-Type: text/plain\n" +
  //       "\n" +
  //       "Resource not found.";

  //   inputStream = new ByteArrayInputStream(serverResponse.getBytes());
  //   getClient.in = new BufferedReader(new InputStreamReader(inputStream));
  //   socket.setInputStream(inputStream);

  //   GETClientDataResponse response = getClient.retrieveWeatherData("server", 8080, "station");

  //   // Check if the response is an error response
  //   assertNotNull(response);
  //   assertEquals(true, response.isError());
  //   assertEquals("404 Not Found\nResource not found.", response.getErrorMessage());
  // }

  @Test
  public void testMain_MethodWithInvalidArguments() {
    // Test the main method with insufficient arguments
    ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();
    System.setOut(new PrintStream(consoleOutput));

    GETClient.main(new String[] {});

    // Check if the program prints an error message
    String expectedErrorMessage = "Insufficient arguments. Usage: GETClient <serverName>:<portNumber> [<stationId>]\n";
    assertEquals(expectedErrorMessage, consoleOutput.toString());
  }

  private class SocketStub extends Socket {
    private InputStream inputStream;
    private OutputStream outputStream;

    public void setInputStream(InputStream inputStream) {
      this.inputStream = inputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
      this.outputStream = outputStream;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return inputStream;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      return outputStream;
    }
  }
}
