package main.services;

import java.io.IOException;
import java.net.ServerSocket;
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
    Socket acceptConnectionFromClient() throws IOException;

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
     * Closes the server socket.
     */
    void closeServer();

    ServerSocket getServerSocket();
}
