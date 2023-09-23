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
- `json-20230227.jar` library (included in the `lib` directory)

## Project Structure

The project is organized into the following directories:

- `src/main`: Contains the Java source code for the aggregation server, content server, GET client, and helper classes.
- `lib`: Contains the `json-20230227.jar` library required for JSON parsing.
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
```

## Cleaning Up

To clean the project and remove generated files, run:

```shell
make clean
```

## Important Notes

- Make sure to have the `json-20230227.jar` library in the `lib` directory.
- Port numbers and server configurations are specified within the Java code and may need adjustments based on your requirements.

## Authors

- Name: Hoang Nam Trinh
- Student Id: A1807377

# Testing Strategy

Testing is a crucial part of ensuring the reliability and correctness of the Weather Data Distribution System. The project encompasses multiple components, including the Aggregation Server, Content Server, and GET Client, each of which needs to be thoroughly tested. Here is the testing strategy for different aspects of the project:

## 1. Unit Testing

### a. Individual Component Testing

Each component of the system (Aggregation Server, Content Server, and GET Client) should undergo comprehensive unit testing. Unit tests are designed to evaluate the functionality of individual classes or methods in isolation.

- **Aggregation Server Unit Tests:** Test the methods and functionalities of the Aggregation Server class. Ensure that it can correctly handle incoming requests from content servers and clients. Verify that it correctly maintains its internal data structures.

- **Content Server Unit Tests:** Test the methods and functionalities of the Content Server class. Verify that it can read weather data from files and respond to requests from aggregation servers. Ensure that it handles invalid input gracefully.

- **GET Client Unit Tests:** Test the methods and functionalities of the GET Client class. Ensure that it correctly constructs GET requests, processes responses from aggregation servers, and handles errors.

### b. Helper Class Testing

The project includes helper classes like `JSONParser` and `LamportClock`. These classes should also be thoroughly unit tested to ensure they perform their intended functions correctly.

## 2. Integration Testing

Integration testing focuses on verifying the interactions between different components of the system. In this case, it involves testing the communication and data exchange between the Aggregation Server, Content Servers, and GET Clients.

- **Aggregation Server-Content Server Integration:** Test the integration between the Aggregation Server and Content Servers. Ensure that the Aggregation Server can successfully retrieve weather data from Content Servers and handle multiple simultaneous connections.

- **Aggregation Server-GET Client Integration:** Test the interaction between the Aggregation Server and GET Clients. Verify that the Aggregation Server responds to GET requests correctly and delivers the requested weather data.

- **Content Server-GET Client Integration:** Test the GET Client's ability to connect to Content Servers, request weather data, and process the responses.

## 3. End-to-End Testing

End-to-end testing evaluates the entire system's functionality as a whole, simulating real-world scenarios. This testing ensures that the system operates correctly from the perspective of an external user or client.

- **End-to-End Scenario Testing:** Create test scenarios that mimic the actual usage of the Weather Data Distribution System. This includes starting Content Servers, Aggregation Servers, and GET Clients, and then verifying that weather data is correctly distributed and retrieved.

- **Load Testing:** Simulate a high volume of requests to assess how the system performs under heavy load. Ensure that the system remains responsive and that there are no memory leaks or performance bottlenecks.

- **Error Handling Testing:** Test the system's ability to handle errors gracefully. Introduce scenarios such as network failures, invalid requests, or unexpected server behavior to validate error-handling mechanisms.
