package tests.helpers;

import org.json.JSONObject;
import main.helpers.JSONParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JSONParserTest {

  @Test
  public void testTextToJsonValidInput() {
    String input = "name: John\nage: 30";
    JSONObject json = JSONParser.textToJson(input);

    assertNotNull(json);
    assertEquals("John", json.getString("name"));
    assertEquals(30, json.getInt("age"));
  }

  @Test
  public void testTextToJsonInvalidInput() {
    assertThrows(IllegalArgumentException.class, () -> {
      JSONParser.textToJson(null);
    });

    assertThrows(IllegalArgumentException.class, () -> {
      JSONParser.textToJson("invalid input");
    });
  }

  @Test
  public void testTextToJsonEmptyInput() {
    String input = "";
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
      JSONParser.textToJson(input);
    });

    assertEquals("Invalid key-value format.", exception.getMessage());
  }

  @Test
  public void testTextToJsonValidInputWithSpaces() {
    String input = "   name  :  John  \nage   :  30  ";
    JSONObject json = JSONParser.textToJson(input);

    assertNotNull(json);
    assertEquals("John", json.getString("name"));
    assertEquals(30, json.getInt("age"));
  }

  @Test
  public void testJsonToTextWithTypes() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("name", "John");
    jsonObject.put("age", 30);

    String result = JSONParser.jsonToText(jsonObject, true, true);

    assertTrue(result.contains("String(\"John\")"));
    assertTrue(result.contains("Integer(30)"));
  }

  @Test
  public void testJsonToTextWithoutTypes() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("name", "John");
    jsonObject.put("age", 30);

    String result = JSONParser.jsonToText(jsonObject, false, true);

    assertTrue(result.contains("\"John\""));
    assertTrue(result.contains("30"));
  }

  @Test
  public void testJsonToTextNoPrettyPrint() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("name", "John");
    jsonObject.put("age", 30);

    String result = JSONParser.jsonToText(jsonObject, true, false);

    assertFalse(result.contains("\n"));
    assertFalse(result.endsWith(","));
  }

  @Test
  public void testJsonToTextEmptyJson() {
    JSONObject jsonObject = new JSONObject();

    String result = JSONParser.jsonToText(jsonObject, true, true);

    assertEquals("", result);
  }

  @Test
  public void testJsonToTextNestedJson() {
    JSONObject innerJson = new JSONObject();
    innerJson.put("city", "New York");
    innerJson.put("country", "USA");

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("name", "John");
    jsonObject.put("location", innerJson);

    String result = JSONParser.jsonToText(jsonObject, false, true);
    assertTrue(result.contains("location"));
  }

  @Test
  public void testJsonToTextNullInput() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
      JSONParser.jsonToText(null, true, true);
    });

    assertEquals("Null input given.", exception.getMessage());
  }
}
