import java.io.*;
import java.net.*;
import java.nio.file.*;

public class bserve {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java bserve <www-root> <port>");
            System.exit(1);
        }

        String rootDir = args[0];
        int port = Integer.parseInt(args[1]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Binary HTTP Server listening on port " + port);
            while (true) {
                Socket client = serverSocket.accept();
                new Thread(() -> handleConnection(client, rootDir)).start();
            }
        }
    }

    private static void handleConnection(Socket client, String rootDir) {
        try {
            DataInputStream in = new DataInputStream(client.getInputStream());
            DataOutputStream out = new DataOutputStream(client.getOutputStream());

            while (!client.isClosed()) {
                Protocol.Frame reqFrame = null;
                try {
                    reqFrame = Protocol.readFrame(in);
                } catch (EOFException e) {
                    break; // Connection closed normally
                } catch (Exception e) {
                    sendError(out, 400, "Bad Request");
                    continue;
                }

                if (reqFrame.type != Protocol.TYPE_REQUEST) {
                    // Unknown frame type - skip cleanly.
                    // The payload was already consumed by readFrame based on length.
                    // So we are still aligned for the next frame.
                    System.out.println("Skipped unknown frame type: " + reqFrame.type);
                    continue;
                }

                String path = reqFrame.headers.get("Path");
                if (path == null) {
                    sendError(out, 400, "Missing Path Header");
                    continue;
                }

                if (path.equals("/")) {
                    path = "/index.html";
                }

                Path filePath = Paths.get(rootDir, path).normalize();
                if (!filePath.startsWith(Paths.get(rootDir).normalize())) {
                    sendError(out, 404, "Not Found");
                    continue;
                }

                File file = filePath.toFile();
                if (!file.exists() || file.isDirectory()) {
                    sendError(out, 404, "Not Found");
                    continue;
                }

                Protocol.Frame resFrame = new Protocol.Frame();
                resFrame.type = Protocol.TYPE_RESPONSE;
                resFrame.streamId = reqFrame.streamId;
                resFrame.headers.put("Status", "200");
                resFrame.headers.put("Content-Length", String.valueOf(file.length()));
                
                String contentType = "application/octet-stream";
                if (path.endsWith(".html")) contentType = "text/html";
                resFrame.headers.put("Content-Type", contentType);

                resFrame.body = Files.readAllBytes(filePath);

                Protocol.writeFrame(out, resFrame);
            }
        } catch (IOException e) {
            // Socket closed or error
        } finally {
            try {
                client.close();
            } catch (IOException e) {}
        }
    }

    private static void sendError(DataOutputStream out, int status, String msg) throws IOException {
        Protocol.Frame resFrame = new Protocol.Frame();
        resFrame.type = Protocol.TYPE_RESPONSE;
        resFrame.headers.put("Status", String.valueOf(status));
        resFrame.body = msg.getBytes("UTF-8");
        resFrame.headers.put("Content-Length", String.valueOf(resFrame.body.length));
        Protocol.writeFrame(out, resFrame);
    }
}
