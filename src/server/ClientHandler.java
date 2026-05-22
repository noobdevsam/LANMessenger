package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String authenticatedUser = null;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            while (true) {
                String payload = in.readUTF();
                System.out.println("[SERVER DEBUG] Incoming Raw Payload: " + payload);

                if (payload.startsWith("CMD_LOGIN:")) {
                    handleLoginProtocol(payload);
                } else if (payload.startsWith("CMD_MSG:")) {
                    handleMessageRouting(payload);
                }
            }
        } catch (IOException e) {
            System.out.println("[SERVER LOG] Connection dropped or terminated for user: " + authenticatedUser);
        } finally {
            cleanTerminateSession();
        }
    }

    private void handleLoginProtocol(String payload) throws IOException {
        String[] parts = payload.split(":", 2);
        if (parts.length >= 2) {
            String user = parts[1].trim();
            this.authenticatedUser = user;
            ServerMain.activeClients.put(user, this);
            out.writeUTF("CMD_LOGIN_SUCCESS");
            System.out.println("[SERVER LOG] Session authenticated and bound for: " + user);
            ServerMain.broadcastUserList();
        }
    }

    private void handleMessageRouting(String payload) {
        String[] parts = payload.split(":", 4);
        if (parts.length == 4) {
            String targetReceiver = parts[2];
            ClientHandler receiverHandler = ServerMain.activeClients.get(targetReceiver);
            if (receiverHandler != null) {
                receiverHandler.sendRawPayload(payload);
            }
            this.sendRawPayload(payload); 
        }
    }

    public void sendRawPayload(String payload) {
        try {
            out.writeUTF(payload);
            out.flush();
        } catch (IOException e) {
            System.err.println("[SERVER ERROR] Failed downstream emit to " + authenticatedUser + " : " + e.getMessage());
        }
    }

    private void cleanTerminateSession() {
        if (authenticatedUser != null) {
            ServerMain.activeClients.remove(authenticatedUser);
            System.out.println("[SERVER LOG] Purged live session reference for: " + authenticatedUser);
            ServerMain.broadcastUserList();
        }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[SERVER ERROR] Error closing socket: " + e.getMessage());
        }
    }
}