package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import ui.ChatFrame;

public class NetworkManager implements Runnable {
    private final String serverIP;
    private final int serverPort;
    private final ChatFrame uiBridge;
    private final String initialLoginUser;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public NetworkManager(String host, int port, ChatFrame uiBridge) {
        this(host, port, uiBridge, null);
    }

    public NetworkManager(String host, int port, ChatFrame uiBridge, String initialLoginUser) {
        this.serverIP = host;
        this.serverPort = port;
        this.uiBridge = uiBridge;
        this.initialLoginUser = initialLoginUser;
    }

    @Override
    public void run() {
        try {
            this.socket = new Socket(serverIP, serverPort);
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            // send login once connected if an initial user was provided
            if (initialLoginUser != null && !initialLoginUser.isEmpty()) {
                try {
                    out.writeUTF("CMD_LOGIN:" + initialLoginUser);
                    out.flush();
                } catch (IOException ioe) {
                    System.err.println("[CLIENT NET ERROR] Failed to send initial login: " + ioe.getMessage());
                }
            }

            while (true) {
                String dynamicPayload = in.readUTF();
                processIncomingCommand(dynamicPayload);
            }
        } catch (IOException e) {
            System.err.println("[CLIENT EXCEPTION] Drop out connection anomaly detected: " + e.getMessage());
        } finally {
            closeImplicitResources();
        }
    }

    private void processIncomingCommand(String rawPayload) {
        if (rawPayload.startsWith("CMD_USER_LIST:")) {
            String[] list = rawPayload.substring(14).split(",");
            uiBridge.updateRosterView(list);
        } else if (rawPayload.startsWith("CMD_MSG:")) {
            String[] tokens = rawPayload.split(":", 4);
            if (tokens.length == 4) {
                String sender = tokens[1];
                String text = tokens[3];
                uiBridge.appendStreamMessage(sender, text);
            }
        } else if (rawPayload.startsWith("CMD_FILE:")) {
            String[] tokens = rawPayload.split(":", 5);
            if (tokens.length == 5) {
                String sender = tokens[1];
                String filename = tokens[3];
                String base64 = tokens[4];
                uiBridge.receiveFile(sender, filename, base64);
            }
        }
    }

    public void sendRawPayload(String payload) {
        try {
            if (out != null) {
                out.writeUTF(payload);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("[CLIENT NET ERROR] Failed writing bytes to downstream socket channels.");
        }
    }

    private void closeImplicitResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}