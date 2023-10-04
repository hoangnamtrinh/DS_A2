package main.helpers;

import org.json.JSONObject;

public class JSONParser {
    /**
     * Converts text to a JSON object.
     *
     * @param input The text to convert to JSON format.
     * @return The JSON object representing the input text.
     * @throws IllegalArgumentException If the input text is null or has an invalid
     *                                  format.
     */
    public static JSONObject textToJson(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Null input given.");
        }

        // Split the input text into key-value pairs
        String[] keyValuePairs = input.split("\n");
        JSONObject json = new JSONObject();

        for (String pair : keyValuePairs) {
            // Split each pair into key and value
            String[] keyValue = pair.split(":", 2);

            if (keyValue.length != 2) {
                throw new IllegalArgumentException("Invalid key-value format.");
            }

            // Trim leading and trailing spaces from key and value
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();

            Object processedValue = processValue(value);

            json.put(key, processedValue);
        }

        return json;
    }

    /**
     * Process the value according to its type.
     *
     * @param value The value to process.
     * @return The processed value.
     */
    private static Object processValue(String value) {
        // Check if the value is a boolean
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.valueOf(value);
        } else {
            try {
                // Try to parse the value as an integer
                return Integer.parseInt(value);
            } catch (NumberFormatException eInt) {
                try {
                    // Try to parse the value as a float
                    return Float.parseFloat(value);
                } catch (NumberFormatException eFloat) {
                    // If it's not an integer or float, keep it as a string
                    return value;
                }
            }
        }
    }

    /**
     * Converts a JSON object to a formatted text representation with options.
     *
     * @param jsonObject   The JSON object to convert to text.
     * @param includeTypes Include data types in the output.
     * @param prettyPrint  Enable pretty-printing with indentation.
     * @return The JSON object represented as formatted text.
     * @throws IllegalArgumentException If the input JSON object is null.
     */
    public static String jsonToText(JSONObject jsonObject, boolean includeTypes, boolean prettyPrint) {
        if (jsonObject == null) {
            throw new IllegalArgumentException("Null input given.");
        }

        StringBuilder stringBuilder = new StringBuilder();

        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);

            String valueStr;
            if (includeTypes) {
                valueStr = formatValueWithTypes(value);
            } else {
                valueStr = formatValueWithoutTypes(value);
            }

            stringBuilder.append(key).append(": ").append(valueStr);

            if (prettyPrint) {
                stringBuilder.append("\n");
            } else {
                stringBuilder.append(", ");
            }
        }

        return prettyPrint ? stringBuilder.toString() : stringBuilder.toString().replaceAll(", $", "");
    }

    private static String formatValueWithTypes(Object value) {
        if (value instanceof Number) {
            return value.getClass().getSimpleName() + "(" + value + ")";
        } else if (value instanceof String) {
            return "String(\"" + value + "\")";
        } else if (value instanceof Boolean) {
            return "Boolean(" + value + ")";
        } else if (value instanceof JSONObject) {
            return "JSONObject";
        } else {
            return "UnknownType";
        }
    }

    private static String formatValueWithoutTypes(Object value) {
        if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof String) {
            return "\"" + value + "\"";
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof JSONObject) {
            return "{...}";
        } else {
            return "Unknown";
        }
    }
}
