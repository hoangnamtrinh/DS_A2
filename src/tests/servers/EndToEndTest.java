package tests.servers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import main.servers.ContentServer;
import main.servers.GETClient;
import main.services.SocketServiceImpl;
import main.helpers.GETClientDataResponse;
import main.servers.AggregationServer;

public class EndToEndTest {
  private static AggregationServer aggregationServer;
  private static ContentServer contentServer;
  private static GETClient getClient;
  private static PrintStream originalOut;
  private static ByteArrayOutputStream outputStreamCaptor;

  @BeforeAll
  public static void setup() {
    // Supress System.out.println
    // Save the original System.out
    originalOut = System.out;

    // Create a ByteArrayOutputStream to capture the output
    outputStreamCaptor = new ByteArrayOutputStream();

    // Set System.out to use the outputStreamCaptor
    System.setOut(new PrintStream(outputStreamCaptor));

    // Start the AggregationServer in a separate thread
    Thread serverThread = new Thread(() -> {
      aggregationServer = new AggregationServer(new SocketServiceImpl());
      try {
        aggregationServer.start(1234);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    serverThread.start();

    contentServer = new ContentServer();
    getClient = new GETClient();

  }

  @AfterAll
  public static void teardown() {
    // Stop the AggregationServer
    aggregationServer.stopServer();
    aggregationServer = null;
    contentServer = null;
    getClient = null;

    // Reset System.out.println
    System.setOut(originalOut);
  }

  @Test
  public void testIntegrationWithWeather1() {
    try {
      // Simulate sending data from ContentServer to AggregationServer
      String[] contentServerArgs = { "localhost:1234", "weather1.txt" };
      contentServer.start(contentServerArgs);

      // Simulate a GET request from GETClient to AggregationServer
      GETClientDataResponse response = getClient.start("localhost", 1234, "IDS60901");

      assertFalse(response.isError());
      assertEquals("IDS60901", response.getData().getString("id"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testIntegrationWithWeather2() {
    try {
      // Simulate sending data from ContentServer to AggregationServer
      String[] contentServerArgs = { "localhost:1234", "weather2.txt" };
      contentServer.start(contentServerArgs);

      // Simulate a GET request from GETClient to AggregationServer
      GETClientDataResponse response = getClient.start("localhost", 1234, "IDS60902");

      assertFalse(response.isError());
      assertEquals("IDS60902", response.getData().getString("id"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testIntegrationMultipleClients() {
    try {
      // Simulate sending data from ContentServer to AggregationServer
      String[] contentServerArgs1 = { "localhost:1234", "weather1.txt" };
      String[] contentServerArgs2 = { "localhost:1234", "weather2.txt" };
      contentServer.start(contentServerArgs1);
      contentServer.start(contentServerArgs2);

      // Simulate a GET request from GETClient to AggregationServer
      GETClientDataResponse response1 = getClient.start("localhost", 1234, "IDS60901");
      GETClientDataResponse response2 = getClient.start("localhost", 1234, "IDS60902");

      assertFalse(response1.isError());
      assertFalse(response2.isError());
      assertEquals("IDS60901", response1.getData().getString("id"));
      assertEquals("IDS60902", response2.getData().getString("id"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testIntegrationNoDataFound() throws Exception {
    // Simulate a GET request from GETClient to AggregationServer
    GETClientDataResponse response = getClient.start("localhost", 1234, "IDS60903");
    assertTrue(response.isError());
    assertEquals("404 Data Not Found\n", response.getErrorMessage());
  }

  @Test
  public void testIntegrationNoStationId() {
    try {
      // Simulate sending data from ContentServer to AggregationServer
      String[] contentServerArgs = { "localhost:1234", "weather2.txt" };
      contentServer.start(contentServerArgs);

      // Simulate a GET request from GETClient to AggregationServer
      GETClientDataResponse response = getClient.start("localhost", 1234, null);

      assertFalse(response.isError());
      assertEquals("IDS60902", response.getData().getString("id"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
