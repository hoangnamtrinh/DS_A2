package main.services;

import java.io.IOException;
import java.net.Socket;

/**
 * The SocketService interface defines a contract for socket-related operations,
 * including starting a server, accepting connections, sending and receiving
 * data,
 * and managing client and server sockets.
 */
public interface SocketService {
    /**
     * Starts the server on the specified port.
     *
     * @param portNumber The port number on which to start the server.
     * @throws IOException
     */
    void startServer(int portNumber) throws IOException;

    /**
     * Accepts a connection from a client.
     *
     * @return A Socket representing the client connection.
     * @throws IOException If an I/O error occurs while accepting the connection.
     */
    Socket acceptConnection() throws IOException;

    /**
     * Receives data from a client.
     *
     * @param clientSocket The Socket representing the client connection.
     * @return The received data as a string.
     * @throws IOException
     */
    String getDataFromClient(Socket clientSocket) throws IOException;

    /**
     * Sends a response to a client.
     *
     * @param response     The response to send.
     * @param clientSocket The Socket representing the client connection.
     * @throws IOException
     */
    void sendResponseToClient(String response, Socket clientSocket) throws IOException;

    /**
     * Sends data to a server and receives a response.
     *
     * @param serverName The name of the remote server.
     * @param portNumber The port number of the remote server.
     * @param data       The data to send to the server.
     * @return The received response as a string.
     * @throws IOException
     */
    String sendDataToServer(String serverName, int portNumber, String data) throws IOException;

    /**
     * Closes the client socket and associated resources.
     */
    void closeClient();

    /**
     * Closes the server socket.
     */
    void closeServer();
}
