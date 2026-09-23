import java.io.*;
import java.net.*;

public class bcurl {
    public static void main(String[] args) {
        boolean verbose = false;
        String target = null;

        for (String arg : args) {
            if (arg.equals("-v")) {
                verbose = true;
            } else {
                target = arg;
            }
        }

        if (target == null) {
            System.err.println("Usage: java bcurl [-v] host:port/path");
            System.exit(1);
        }

        String host = "localhost";
        int port = 80;
        String path = "/";

        int slashIdx = target.indexOf("/");
        if (slashIdx != -1) {
            path = target.substring(slashIdx);
            target = target.substring(0, slashIdx);
        }

        int colonIdx = target.indexOf(":");
        if (colonIdx != -1) {
            host = target.substring(0, colonIdx);
            port = Integer.parseInt(target.substring(colonIdx + 1));
        } else {
            host = target;
        }

        try (Socket socket = new Socket(host, port)) {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            Protocol.Frame reqFrame = new Protocol.Frame();
            reqFrame.type = Protocol.TYPE_REQUEST;
            reqFrame.headers.put("Method", "GET");
            reqFrame.headers.put("Path", path);
            reqFrame.headers.put("Host", host);
            
            Protocol.writeFrame(out, reqFrame);
            
            if (verbose) {
                // To get the exact payload for hexdumping, we can create a temporary frame representation
                // Or better, re-parse our own serialized payload for accuracy in printing
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                Protocol.writeFrame(dos, reqFrame);
                
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
                Protocol.Frame printedReq = Protocol.readFrame(dis);
                Protocol.printHexDump("CLIENT SEND (REQUEST)", printedReq);
            }

            Protocol.Frame resFrame = Protocol.readFrame(in);
            
            if (verbose) {
                Protocol.printHexDump("CLIENT RECEIVE (RESPONSE)", resFrame);
            }

            String statusStr = resFrame.headers.get("Status");
            int status = statusStr != null ? Integer.parseInt(statusStr) : 500;

            if (resFrame.body != null && resFrame.body.length > 0) {
                System.out.write(resFrame.body);
                System.out.println();
            }

            if (status >= 400) {
                System.exit(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
