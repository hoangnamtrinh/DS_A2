package main.services;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class SocketServiceImpl implements SocketService {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    @Override
    public void startServer(int portNumber) throws IOException {
        // Initialize the server socket and bind it to the specified port.
        serverSocket = new ServerSocket(portNumber);
    }

    @Override
    public Socket acceptConnection() throws IOException {
        if (serverSocket == null) {
            throw new IOException("Server socket is not initialized.");
        }

        // Set a timeout for accepting incoming connections.
        serverSocket.setSoTimeout(5000);
        try {
            // Wait for and accept an incoming connection.
            return serverSocket.accept();
        } catch (SocketTimeoutException e) {
            if (Thread.currentThread().isInterrupted()) {
                throw new IOException("Thread was interrupted while waiting for a connection.");
            }
            return null;
        }
    }

    @Override
    public String getDataFromClient(Socket clientSocket) throws IOException {
        StringBuilder requestBuilder = new StringBuilder();
        InputStream input = clientSocket.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(input));

        // Read HTTP headers and retrieve the content length if present.
        int contentLength = readHeaders(in, requestBuilder);

        if (contentLength > 0) {
            // Read the HTTP body based on the content length.
            readBody(in, requestBuilder, contentLength);
        }

        return requestBuilder.toString();
    }

    private int readHeaders(BufferedReader in, StringBuilder requestBuilder) throws IOException {
        String line;
        int contentLength = 0;
        boolean isHeader = true;

        while (isHeader && (line = in.readLine()) != null) {
            if (line.startsWith("Content-Length: ")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }

            requestBuilder.append(line).append("\r\n");

            if (line.isEmpty()) {
                isHeader = false;
            }
        }

        return contentLength;
    }

    private void readBody(BufferedReader in, StringBuilder requestBuilder, int contentLength) throws IOException {
        char[] bodyChars = new char[contentLength];
        in.read(bodyChars, 0, contentLength);
        requestBuilder.append(bodyChars);
    }

    @Override
    public void sendResponseToClient(String response, Socket clientSocket) throws IOException {
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        out.println(response);
    }

    @Override
    public String sendDataToServer(String serverName, int portNumber, String data) throws IOException {
        if (clientSocket == null || clientSocket.isClosed()) {
            // Create a client socket and initialize input and output streams.
            clientSocket = new Socket(serverName, portNumber);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }

        // Send data to the server.
        out.println(data);

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            response.append(line).append("\n");
        }

        return response.toString();
    }

    @Override
    public void closeClient() {
        try {
            // Close input, output streams, and the client socket.
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (clientSocket != null)
                clientSocket.close();
        } catch (IOException e) {
            System.err.println("IO Error: An IO Exception occurred - " + e.getMessage());
        }
    }

    @Override
    public void closeServer() {
        try {
            // Close the server socket.
            if (serverSocket != null)
                serverSocket.close();
        } catch (IOException e) {
            System.err.println("IO Error: An IO Exception occurred - " + e.getMessage());
        }
    }
}
