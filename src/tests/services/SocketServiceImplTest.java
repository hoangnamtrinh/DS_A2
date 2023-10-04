package tests.services;

import main.services.SocketService;
import main.services.SocketServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.net.Socket;

public class SocketServiceImplTest {
  private SocketService server;
  private Socket clientSocket;

  @BeforeEach
  public void setUp() {
    server = new SocketServiceImpl();
  }

  @AfterEach
  public void tearDown() throws IOException {
    if (clientSocket != null) {
      clientSocket.close();
    }
    server.closeServer();
  }

  @Test
  public void testStartServer() throws IOException {
    // Start the server on an available port
    server.startServer(0);

    // Verify that the server socket is not null
    assertNotNull(server.getServerSocket());

    // Verify that the server socket is bound to a port
    assertTrue(server.getServerSocket().isBound());

    // Verify that the server socket is not closed
    assertFalse(server.getServerSocket().isClosed());
  }

  @Test
  public void testAcceptConnectionFromClient() throws IOException {
    server.startServer(0);

    // Create a client socket that connects to the server
    clientSocket = new Socket("localhost", server.getServerSocket().getLocalPort());

    // Accept the connection from the client
    Socket acceptedSocket = server.acceptConnectionFromClient();

    // Verify that the accepted socket is not null
    assertNotNull(acceptedSocket);

    // Verify that the accepted socket is connected
    assertTrue(acceptedSocket.isConnected());
  }

  @Test
  public void testGetDataFromClient() throws IOException {
    server.startServer(0);
    clientSocket = new Socket("localhost", server.getServerSocket().getLocalPort());

    // Prepare a sample HTTP request
    String httpRequest = "GET /index.html HTTP/1.1\r\n" +
        "Host: localhost\r\n" +
        "Content-Length: 12\r\n\r\n" +
        "Hello, World";

    // Send the sample request from the client to the server
    OutputStream clientOut = clientSocket.getOutputStream();
    clientOut.write(httpRequest.getBytes());
    clientOut.flush();

    // Accept the connection and read data from the client
    Socket acceptedSocket = server.acceptConnectionFromClient();
    String requestData = server.getDataFromClient(acceptedSocket);

    // Verify that the received data matches the sent request
    assertEquals(httpRequest, requestData);
  }

  @Test
  public void testCloseServer() throws IOException {
    server.startServer(0);

    // Close the server and verify that it's closed
    server.closeServer();
    assertTrue(server.getServerSocket().isClosed());
  }

  @Test
  public void testGetDataFromClientWithEmptyBody() throws IOException {
    server.startServer(0);
    clientSocket = new Socket("localhost", server.getServerSocket().getLocalPort());

    // Prepare a sample HTTP request with an empty body
    String httpRequest = "GET /index.html HTTP/1.1\r\n" +
        "Host: localhost\r\n" +
        "Content-Length: 0\r\n\r\n";

    // Send the sample request from the client to the server
    OutputStream clientOut = clientSocket.getOutputStream();
    clientOut.write(httpRequest.getBytes());
    clientOut.flush();

    // Accept the connection and read data from the client
    Socket acceptedSocket = server.acceptConnectionFromClient();
    String requestData = server.getDataFromClient(acceptedSocket);

    // Verify that the received data matches the sent request
    assertEquals(httpRequest, requestData);
  }

  @Test
  public void testGetDataFromClientWithNoContentLengthHeader() throws IOException {
    server.startServer(0);
    clientSocket = new Socket("localhost", server.getServerSocket().getLocalPort());

    // Prepare a sample HTTP request without a Content-Length header
    String httpRequest = "GET /index.html HTTP/1.1\r\n" +
        "Host: localhost\r\n\r\n";

    // Send the sample request from the client to the server
    OutputStream clientOut = clientSocket.getOutputStream();
    clientOut.write(httpRequest.getBytes());
    clientOut.flush();

    // Accept the connection and read data from the client
    Socket acceptedSocket = server.acceptConnectionFromClient();
    String requestData = server.getDataFromClient(acceptedSocket);

    // Verify that the received data matches the sent request
    assertEquals(httpRequest, requestData);
  }
}
