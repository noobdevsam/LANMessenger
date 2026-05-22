package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMain {
    private static final int PORT = 8085;
    protected static final ConcurrentHashMap<String, ClientHandler> activeClients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("[SERVER LOG] Initializing Socket Engine on port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER LOG] Network Matrix Ready. Awaiting inbound Handshakes...");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER LOG] New raw socket connection from: " + clientSocket.getRemoteSocketAddress());
                
                ClientHandler handler = new ClientHandler(clientSocket);
                Thread clientThread = new Thread(handler);
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("[SERVER CRITICAL] Engine lifecycle crash: " + e.getMessage());
        }
    }

    public static void broadcastUserList() {
        String userListPayload = "CMD_USER_LIST:" + String.join(",", activeClients.keySet());
        for (ClientHandler handler : activeClients.values()) {
            handler.sendRawPayload(userListPayload);
        }
    }
}