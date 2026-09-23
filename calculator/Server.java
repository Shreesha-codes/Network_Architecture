import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Server {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Calculator Server listening on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        }
    }

    private static void handleClient(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            while (!socket.isClosed()) {
                Request req = parseRequest(in);
                if (req == null) {
                    break;
                }

                if (!req.hasHostHeader) {
                    sendResponse(out, 400, "Bad Request", "Missing Host Header");
                    continue;
                }

                if (!req.method.equals("GET")) {
                    sendResponse(out, 405, "Method Not Allowed", "");
                    continue;
                }

                String[] pathParts = req.path.split("\\?");
                String endpoint = pathParts[0];
                Map<String, String> queryParams = new HashMap<>();

                if (pathParts.length > 1) {
                    String[] pairs = pathParts[1].split("&");
                    for (String pair : pairs) {
                        String[] kv = pair.split("=");
                        if (kv.length == 2) {
                            queryParams.put(kv[0], kv[1]);
                        }
                    }
                }

                if (!endpoint.equals("/add") && !endpoint.equals("/sub") && !endpoint.equals("/mul")
                        && !endpoint.equals("/div")) {
                    sendResponse(out, 404, "Not Found", "");
                    continue;
                }

                try {
                    if (!queryParams.containsKey("a") || !queryParams.containsKey("b")) {
                        sendResponse(out, 400, "Bad Request", "Missing parameters");
                        continue;
                    }

                    int a = Integer.parseInt(queryParams.get("a"));
                    int b = Integer.parseInt(queryParams.get("b"));
                    int result = 0;

                    switch (endpoint) {
                        case "/add":
                            result = a + b;
                            break;
                        case "/sub":
                            result = a - b;
                            break;
                        case "/mul":
                            result = a * b;
                            break;
                        case "/div":
                            if (b == 0) {
                                sendResponse(out, 400, "Bad Request", "you cant devide by zero");
                                continue;
                            }
                            result = a / b;
                            break;
                    }
                    sendResponse(out, 200, "OK", String.valueOf(result));

                } catch (NumberFormatException e) {
                    sendResponse(out, 400, "Bad Request", "Invalid number format");
                }
            }
        } catch (IOException e) {
            // Connection closed or broken pipe
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private static Request parseRequest(InputStream in) throws IOException {
        ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
        int prev = -1, curr = -1, prev2 = -1, prev3 = -1;
        boolean foundEnd = false;

        while ((curr = in.read()) != -1) {
            headerBuffer.write(curr);
            if (prev3 == '\r' && prev2 == '\n' && prev == '\r' && curr == '\n') {
                foundEnd = true;
                break;
            }
            prev3 = prev2;
            prev2 = prev;
            prev = curr;
        }

        if (headerBuffer.size() == 0)
            return null;
        if (!foundEnd)
            return null;

        String headerString = new String(headerBuffer.toByteArray(), "UTF-8");
        String[] lines = headerString.split("\r\n");
        if (lines.length == 0)
            return null;

        String[] requestLine = lines[0].split(" ");
        if (requestLine.length < 3)
            return null;

        Request req = new Request();
        req.method = requestLine[0];
        req.path = requestLine[1];

        int contentLength = 0;

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.toLowerCase().startsWith("host:")) {
                req.hasHostHeader = true;
            }
            if (line.toLowerCase().startsWith("content-length:")) {
                try {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }

        if (contentLength > 0) {
            byte[] body = new byte[contentLength];
            int read = 0;
            while (read < contentLength) {
                int count = in.read(body, read, contentLength - read);
                if (count == -1)
                    break;
                read += count;
            }
        }

        return req;
    }

    private static void sendResponse(OutputStream out, int statusCode, String statusText, String body)
            throws IOException {
        
        String finalBody = body;
        if (statusCode >= 400) {
            finalBody = "<h1>" + statusCode + " " + statusText + "</h1><p>" + body + "</p>";
        }

        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n");
        response.append("Content-Length: ").append(finalBody.length()).append("\r\n");
        response.append("Content-Type: text/html\r\n");
        response.append("Connection: keep-alive\r\n");
        response.append("\r\n");
        response.append(finalBody);

        out.write(response.toString().getBytes("UTF-8"));
        out.flush();
    }

    static class Request {
        String method;
        String path;
        boolean hasHostHeader;
    }
}
