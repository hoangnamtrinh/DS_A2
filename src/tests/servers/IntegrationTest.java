package tests.servers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import main.servers.ContentServer;
import main.servers.GETClient;
import main.services.SocketServiceImpl;
import main.helpers.GETClientDataResponse;
import main.servers.AggregationServer; // Import the AggregationServer class

public class IntegrationTest {
    private static AggregationServer aggregationServer; // Use AggregationServer
    private static ContentServer contentServer;
    private static GETClient getClient;

    @BeforeAll
    public static void setup() {
        // Start the AggregationServer
        aggregationServer = new AggregationServer(new SocketServiceImpl()); // Assuming AggregationServer is the correct class
        try {
            aggregationServer.start(8080); // Assuming the server is running on port 8080
        } catch (Exception e) {
            e.printStackTrace();
        }

        contentServer = new ContentServer();
        getClient = new GETClient();
    }

    @AfterAll
    public static void teardown() {
        // Stop the AggregationServer
        aggregationServer.stopServer();
    }

    @Test
    public void testIntegration() {
        try {
            // Simulate sending data from ContentServer to AggregationServer
            String[] contentServerArgs = {"localhost:8080", "weather1.txt"};
            contentServer.start(contentServerArgs);

            // Simulate a GET request from GETClient to AggregationServer
            // String[] getClientArgs = {"localhost:8080"};
            GETClientDataResponse response = getClient.retrieveWeatherDataWithRetry("localhost", 8080, "ID60901");

            // Perform assertions on the response or other test scenarios
            assertEquals("200", response);
            // Add more assertions as needed
        } catch (Exception e) {
            // Handle exceptions if necessary
            e.printStackTrace();
        }
    }
}
