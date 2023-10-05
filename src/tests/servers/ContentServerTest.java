package tests.servers;

import main.servers.ContentServer;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.IOException;

public class ContentServerTest {
  private ContentServer contentServer;

  @Before
  public void setUp() {
    contentServer = new ContentServer();
  }

  @After
  public void tearDown() {
    contentServer = null;
  }

  @Test
  public void testIsDoneReadingData() throws Exception {
    // Assuming you have a sample JSON file for testing
    String filePath = "weather1.txt";

    boolean result = contentServer.isDoneReadingData(filePath);

    assertTrue(result);
    assertNotNull(contentServer.getWeatherData());
  }

  @Test(expected = IOException.class)
  public void testIsDoneReadingDataWithInvalidFile() throws Exception {
    // Provide an invalid file path to trigger an IOException
    String invalidFilePath = "invalid_file.json";
    contentServer.isDoneReadingData(invalidFilePath);
  }

  @Test
  public void testBuildPutRequest() {
    JSONObject jsonData = new JSONObject();
    jsonData.put("key1", "value1");

    int timeStamp = 1;
    String request = contentServer.buildPutRequest("test", jsonData, timeStamp);

    assertTrue(request.contains("PUT /uploadData HTTP/1.1"));
    assertTrue(request.contains("ServerId:"));
    assertTrue(request.contains("LamportClock: 1"));
    assertTrue(request.contains("Content-Type: application/json"));
    assertTrue(request.contains("Content-Length: " + jsonData.toString().length()));
    assertTrue(request.contains(jsonData.toString()));
  }

  @Test
    public void testBuildPutRequestWithEmptyData() {
        JSONObject emptyData = new JSONObject();
        int timeStamp = 0; // Timestamp is set to zero

        String request = contentServer.buildPutRequest("test", emptyData, timeStamp);

        assertTrue(request.contains("PUT /uploadData HTTP/1.1"));
        assertTrue(request.contains("ServerId:"));
        assertTrue(request.contains("LamportClock: 0"));
        assertTrue(request.contains("Content-Type: application/json"));
        assertTrue(request.contains("Content-Length: 2")); // The empty JSON object is just "{}"
        assertTrue(request.contains("{}"));
    }

    @Test
    public void testBuildPutRequestWithLongData() {
        JSONObject longData = new JSONObject();
        StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longValue.append("a"); // Create a long string value
        }
        longData.put("key1", longValue.toString());
        int timeStamp = 9999;

        String request = contentServer.buildPutRequest("test", longData, timeStamp);

        assertTrue(request.contains("PUT /uploadData HTTP/1.1"));
        assertTrue(request.contains("ServerId:"));
        assertTrue(request.contains("LamportClock: 9999"));
        assertTrue(request.contains("Content-Type: application/json"));
        assertTrue(request.contains("Content-Length: " + longData.toString().length()));
        assertTrue(request.contains(longData.toString()));
    }

    @Test
    public void testBuildPutRequestWithSpecialCharacters() {
        JSONObject specialData = new JSONObject();
        specialData.put("key1", "Special Characters: !@#$%^&*()_+");

        int timeStamp = 12345;

        String request = contentServer.buildPutRequest("test", specialData, timeStamp);

        assertTrue(request.contains("PUT /uploadData HTTP/1.1"));
        assertTrue(request.contains("ServerId:"));
        assertTrue(request.contains("LamportClock: 12345"));
        assertTrue(request.contains("Content-Type: application/json"));
        assertTrue(request.contains("Content-Length: " + specialData.toString().length()));
        assertTrue(request.contains(specialData.toString()));
    }
}
