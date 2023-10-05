# Weather Data Distribution System

## Overview

The Weather Data Distribution System is a Java-based project designed to facilitate the distribution of weather data among various components of a distributed system. It consists of three main components:

1. **Aggregation Server:** This server collects weather data from multiple content servers and provides a unified interface for clients to retrieve weather information.

2. **Content Server:** Content servers store and serve weather data. They are responsible for reading weather data from files and making it available to aggregation servers upon request.

3. **GET Client:** GET clients are clients that retrieve weather data from the aggregation server. Clients can specify station IDs to request specific weather data.

The system uses a Lamport logical clock for tracking events in the distributed system, ensuring the correct order of events across different components.

## Prerequisites

To run this project, you need the following prerequisites:

- Java Development Kit (JDK)
- Make utility (for building and running the project using the provided Makefile)
- `json-20230227.jar`, `junit-jupiter-5.10.0.jar`, `junit-hupiter-api5.10.0.jar` and `junit-platform-console-standalone-1.5.2.jar` library (included in the `lib` directory)

## Project Structure

The project is organized into the following directories:

- `src/main`: Contains the Java source code for the aggregation server, content server, GET client, and helper classes.
- `src/tests`: Contains the Java source code for testing using JUnit.
- `lib`: Contains the libraries required for JSON parsing and testing.
- `bin`: This directory is created during the build process and contains compiled Java class files.
- `weather1.txt` and `weather2.txt`: Sample weather data files used by content servers.

## Building the Project

To build the project, open your terminal and navigate to the project directory. Then, run the following command:

```shell
make all
```

This command will compile the Java source files and create the necessary class files in the `bin` directory.

## Running the Components

You can run the project components using the provided Makefile targets. Here are some examples:

- **Aggregation Server:**

```shell
make aggregation
```

- **Content Servers:**

```shell
make content1  # Starts content server 1
make content2  # Starts content server 2
```

- **GET Clients:**

```shell
make client1  # GET client with station ID IDS60901
make client2  # GET client with station ID IDS60902
make client3  # GET client without stationId, this should return the latest weather data
```

## Cleaning Up

To clean the project and remove generated files, run:

```shell
make clean
```

## Important Notes

- Port numbers and server configurations are specified within the Java code and may need adjustments based on your requirements.

## Implementation

In this section, I will describe the methodology employed for implementing key system features.

### Lamport Clock

The Lamport Clock functionality has been encapsulated within a distinct class and integrated into the Aggregation Server. To enhance clarity and synchronization across various system components, a single Lamport Clock system is utilized within the Aggregation Server. To maintain synchronization, both the GetClient and AggregationServer must request the current Lamport Clock time from the Aggregation Server prior to initiating any get/put requests. Subsequently, they incorporate this timestamp into their requests. Aggregation then processes these requests based on the timestamp, ensuring consistent logic among all clients.

### Retry Mechanism on Errors

This feature is realized by implementing a default retry policy involving three retries, each scheduled 15 seconds apart. Consequently, if the Aggregation Server is temporarily unavailable, any other servers attempting to communicate with it will automatically retry their requests every 15 seconds. After three unsuccessful retries, if the server remains unresponsive, clients will terminate their communication attempts.

### Expired Data Handling in the Aggregation Server

The Aggregation Server's mechanism for expunging expired data adopts a soft deletion approach. Instead of permanently removing expired data (hard delete), it is retained but marked as unavailable. This functionality is facilitated through a data structure that associates a serverId with the timestamp of its last communication with the aggregation system (measured in milliseconds). When a client submits a data retrieval request, the system not only checks the storage for the requested data but also verifies the timestamp to ensure that the returned data is not expired.

### Ensuring Fault Tolerance

The `StateSaverThread` class within the context of the `AggregationServer` plays a crucial role in guaranteeing the periodic preservation and retrieval of the server's vital state. This state encompasses critical data required for the uninterrupted operation of the server, such as the `weatherDataMap`, a hash map associating `stationId` with its corresponding priority queue of weather data. This priority queue ensures that the weather data with the most recent timestamp is readily accessible. Additionally, other essential data includes the `serverTimestampMap`, which tracks the last communication times of servers with the aggregation server, the `mostRecentStationId`, and the `latestPutTimestamp`.

Once the `StateSaverThread` is in operation, it seamlessly manages the loading of the Aggregation Server's state from a designated file named `data.json`. Furthermore, it takes responsibility for periodically saving the server state at predefined intervals. The thread continues to execute indefinitely, consistently preserving the server state within the specified JSON file and efficiently reloading it when necessary.

The `StateSaverThread` class is intentionally engineered to enhance fault tolerance for `AggregationServer`. During startup, it proactively loads the server state from the JSON file. In cases where issues arise during the loading process, such as the absence of the file, the class handles these scenarios gracefully. This robust design ensures that the application can gracefully recover from unexpected failures and persistently operate with the most recent saved state, safeguarding data integrity and system reliability.

# Testing

Testing is a crucial part of ensuring the reliability and correctness of the Weather Data Distribution System. The project encompasses multiple components, including the Aggregation Server, Content Server, and GET Client, each of which needs to be thoroughly tested. Here is the testing strategy for different aspects of the project:

## Run tests

To run tests for the Weather Data Distribution System, follow these steps:

1. **Compile the Test Files:**

   First, compile the JUnit test files by running the following command:

   ```shell
   make test_compile
   ```

   This will compile all the test classes specified in the `TEST_SOURCES` variable.

2. **Run the Tests:**

   After compiling the test files, you can run the tests using the following command:

   ```shell
   make test_run
   ```

   This command will execute the JUnit tests using the `org.junit.platform.console.ConsoleLauncher`. It will run all tests in the `tests` package.

3. **Review Test Results:**

   The test results will be displayed in the terminal, showing which tests passed and which failed. You can review the output to identify any test failures or errors.

4. **Clean Up:**

   To clean up any generated files and compiled test classes, you can run the clean command:

   ```shell
   make clean
   ```

## Testing Strategy

### 1. Unit Testing

#### a. Individual Component Testing

Each component of the system (Aggregation Server, Content Server, and GET Client) should undergo comprehensive unit testing. Unit tests are designed to evaluate the functionality of individual classes or methods in isolation.

- **Aggregation Server Unit Tests:** Test the methods and functionalities of the Aggregation Server class. Ensure that it can correctly handle incoming requests from content servers and clients. Verify that it correctly maintains its internal data structures.

- **Content Server Unit Tests:** Test the methods and functionalities of the Content Server class. Verify that it can read weather data from files and respond to requests from aggregation servers. Ensure that it handles invalid input gracefully.

- **GET Client Unit Tests:** Test the methods and functionalities of the GET Client class. Ensure that it correctly constructs GET requests, processes responses from aggregation servers, and handles errors.

#### b. Helper Class Testing

The project includes helper classes like `JSONParser` and `LamportClock`. These classes should also be thoroughly unit tested to ensure they perform their intended functions correctly.


#### c. Edge Cases Testing

- Concurrency Tests: Evaluate the system's behavior under heavy concurrent loads. Test how it handles multiple requests from clients simultaneously

- Timeout and Latency Tests: Introduce network delays and timeouts to simulate real-world network conditions. Ensure that the system gracefully handles delays, retries, and timeouts without becoming unresponsive.

- Negative Tests: Test the system with intentionally malformed or incorrect inputs. Verify that it rejects invalid requests or inputs as expected, preventing potential security vulnerabilities or data corruption.

### 2. Integration Testing

- **Aggregation Server-Content Server Integration:** Test the integration between the Aggregation Server and Content Servers. Ensure that the Aggregation Server can successfully retrieve weather data from Content Servers and handle multiple simultaneous connections.

- **Aggregation Server-GET Client Integration:** Test the interaction between the Aggregation Server and GET Clients. Verify that the Aggregation Server responds to GET requests correctly and delivers the requested weather data.

- **Content Server-GET Client Integration:** Test the GET Client's ability to connect to Content Servers, request weather data, and process the responses.

### 3. End-to-End Testing

- **End-to-End Scenario Testing:** Create test scenarios that mimic the actual usage of the Weather Data Distribution System. This includes starting Content Servers, Aggregation Servers, and GET Clients, and then verifying that weather data is correctly distributed and retrieved.

- **Error Handling Testing:** Test the system's ability to handle errors gracefully.

### 4. Synchronization and fault testing

- Synchronization Testing: Verify that the system's synchronization mechanisms work correctly.

- Fault Tolerance Testing: Introduce faults or failures intentionally to assess how the system responds. 

- Data Integrity Testing: Validate the system's ability to maintain data integrity in the presence of unexpected faults or errors.

## Authors

- Name: Hoang Nam Trinh
- Student Id: A1807377