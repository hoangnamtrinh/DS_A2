package tests.servers;

import main.servers.AggregationServer;
import main.services.SocketServiceImpl;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

public class AggregationServerTest {
  private AggregationServer aggregationServer;

  @Before
  public void setUp() {
    aggregationServer = new AggregationServer(new SocketServiceImpl());
  }

  @After
  public void tearDown() {
    aggregationServer = null;
  }

  @Test
  public void testHandlePutRequestWithValidData() {
    String validRequest = "PUT /uploadData HTTP/1.1\r\n" +
        "ServerId: TestServer\r\n" +
        "LamportClock: 12345\r\n" +
        "Content-Type: application/json\r\n" +
        "Content-Length: 19\r\n" +
        "\r\n" +
        "{\"id\":\"station123\"}";

    String response = aggregationServer.handleClientRequest(validRequest);

    assertEquals("200 OK", response);
  }

  @Test
  public void testHandlePutRequestWithInvalidData() {
    String invalidRequest = "PUT /uploadData HTTP/1.1\r\n" +
        "ServerId: TestServer\r\n" +
        "LamportClock: 12345\r\n" +
        "Content-Type: application/json\r\n" +
        "Content-Length: 19\r\n" +
        "\r\n" +
        "Invalid JSON Data";

    String response = aggregationServer.handleClientRequest(invalidRequest);

    assertEquals("400 JSON Error", response);
  }

  @Test
  public void testHandlePutRequestWithNullServerId() {
    String requestWithNullServerId = "PUT /uploadData HTTP/1.1\r\n" +
        "LamportClock: 12345\r\n" +
        "Content-Type: application/json\r\n" +
        "Content-Length: 19\r\n" +
        "\r\n" +
        "{\"id\":\"station123\"}";

    String response = aggregationServer.handleClientRequest(requestWithNullServerId);

    assertEquals("400 Null ServerId", response);
  }

  @Test
  public void testHandleGetRequestWithValidData() {
    // Assuming you have a valid GET request in the correct format
    String validGetRequest = "GET /getWeatherData HTTP/1.1\r\n" +
        "LamportClock: 2\r\n" +
        "StationId: station123\r\n" +
        "\r\n";

    JSONObject weatherData = new JSONObject();
    weatherData.put("id", "station123");
    weatherData.put("temperature", 25.0);
    aggregationServer.storeWeatherData(weatherData, 1, "TestServer");
    Map<String, Long> serverTimestampMap = new HashMap<>();
    serverTimestampMap.put("TestServer", System.currentTimeMillis() - 10);
    aggregationServer.setServerTimestamMap(serverTimestampMap);

    String response = aggregationServer.handleClientRequest(validGetRequest);
    assertEquals("{\"temperature\":25,\"id\":\"station123\"}", response);
  }

  @Test
  public void testHandleGetRequestWithInvalidStationId() {
    // Assuming you have a valid GET request in the correct format with an invalid
    // StationId
    String invalidStationIdRequest = "GET /getWeatherData HTTP/1.1\r\n" +
        "LamportClock: 12345\r\n" +
        "StationId: invalidStation\r\n" +
        "\r\n";

    String response = aggregationServer.handleClientRequest(invalidStationIdRequest);

    assertEquals("404 Data Not Found", response);
  }

  @Test
  public void testHandleMultiplePutRequests() {
    String validRequest1 = "PUT /uploadData HTTP/1.1\r\n" +
        "ServerId: TestServer\r\n" +
        "LamportClock: 12345\r\n" +
        "Content-Type: application/json\r\n" +
        "Content-Length: 19\r\n" +
        "\r\n" +
        "{\"id\":\"station123\"}";

    String validRequest2 = "PUT /uploadData HTTP/1.1\r\n" +
        "ServerId: TestServer\r\n" +
        "LamportClock: 12346\r\n" +
        "Content-Type: application/json\r\n" +
        "Content-Length: 19\r\n" +
        "\r\n" +
        "{\"id\":\"station123\"}";

    String response1 = aggregationServer.handleClientRequest(validRequest1);
    String response2 = aggregationServer.handleClientRequest(validRequest2);

    assertEquals("200 OK", response1);
    assertEquals("200 OK", response2);
  }

  @Test
  public void testHandleGetRequestWithNoWeatherData() {
    // Assuming you have a valid GET request in the correct format
    String validGetRequest = "GET /getWeatherData HTTP/1.1\r\n" +
        "LamportClock: 2\r\n" +
        "StationId: station123\r\n" +
        "\r\n";

    String response = aggregationServer.handleClientRequest(validGetRequest);

    assertEquals("404 Data Not Found", response);
  }

  @Test
  public void testHandleGetRequestWithNoStationId() {
    // Set up most recent station id and its data
    aggregationServer.setMostRecentStationId("TestStationId");
    JSONObject weatherData = new JSONObject();
    weatherData.put("id", "station123");
    weatherData.put("temperature", 25.0);
    aggregationServer.storeWeatherData(weatherData, 1, "TestServer");
    Map<String, Long> serverTimestampMap = new HashMap<>();
    serverTimestampMap.put("TestServer", System.currentTimeMillis() - 10);
    aggregationServer.setServerTimestamMap(serverTimestampMap);

    String getRequestWithoutStationId = "GET /getWeatherData HTTP/1.1\r\n" +
        "LamportClock: 2\r\n" +
        "\r\n";

    String response = aggregationServer.handleClientRequest(getRequestWithoutStationId);

    // The mostRecentStationId should be used, so the response should be based on
    // that.
    assertEquals("{\"temperature\":25,\"id\":\"station123\"}", response);
  }

  @Test
  public void testHandleGetRequestWithOldLamportClock() {
    // Assuming you have a valid GET request in the correct format
    String validGetRequest = "GET /getWeatherData HTTP/1.1\r\n" +
        "LamportClock: 1\r\n" +
        "StationId: station123\r\n" +
        "\r\n";

    // Assuming that the serverTimestampMap has a recent entry for "TestServer"
    Map<String, Long> serverTimestampMap = new HashMap<>();
    serverTimestampMap.put("TestServer", System.currentTimeMillis() - 10);
    aggregationServer.setServerTimestamMap(serverTimestampMap);

    String response = aggregationServer.handleClientRequest(validGetRequest);

    assertEquals("404 Data Not Found", response);
  }

  @Test
  public void testHandleConcurrentPutRequests() {
    // Simulate two concurrent PUT requests with different LamportClock values
    String request1 = "PUT /uploadData HTTP/1.1\r\n" +
        "ServerId: Server1\r\n" +
        "LamportClock: 1\r\n" +
        "Content-Type: application/json\r\n" +
        "Content-Length: 19\r\n" +
        "\r\n" +
        "{\"id\":\"station123\"}";

    String request2 = "PUT /uploadData HTTP/1.1\r\n" +
        "ServerId: Server2\r\n" +
        "LamportClock: 2\r\n" +
        "Content-Type: application/json\r\n" +
        "Content-Length: 19\r\n" +
        "\r\n" +
        "{\"id\":\"station123\"}";

    // Execute both requests in separate threads
    Thread thread1 = new Thread(() -> {
      String response1 = aggregationServer.handleClientRequest(request1);
      assertEquals("200 OK", response1);
    });

    Thread thread2 = new Thread(() -> {
      String response2 = aggregationServer.handleClientRequest(request2);
      assertEquals("200 OK", response2);
    });

    thread1.start();
    thread2.start();

    try {
      thread1.join();
      thread2.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testHandleGetRequestWithNoLamportClock() {
    // Assuming you have a valid GET request in the correct format but without a
    // LamportClock header
    String getRequestWithoutLamportClock = "GET /getWeatherData HTTP/1.1\r\n" +
        "StationId: station123\r\n" +
        "\r\n";

    String response = aggregationServer.handleClientRequest(getRequestWithoutLamportClock);

    assertEquals("404 Data Not Found", response);
  }

}
