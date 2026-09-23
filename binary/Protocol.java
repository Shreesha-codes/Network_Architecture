import java.io.*;
import java.util.*;

public class Protocol {

    public static final byte TYPE_REQUEST = 0x01;
    public static final byte TYPE_RESPONSE = 0x02;

    public static final Map<String, Byte> HEADER_TO_ID = new HashMap<>();
    public static final Map<Byte, String> ID_TO_HEADER = new HashMap<>();

    static {
        String[] predefined = {
            "Method", "Path", "Status", "Content-Type", "Content-Length", 
            "Host", "User-Agent", "Accept", "Connection", "Server"
        };
        for (int i = 0; i < predefined.length; i++) {
            byte id = (byte) (i + 1);
            HEADER_TO_ID.put(predefined[i], id);
            ID_TO_HEADER.put(id, predefined[i]);
        }
    }

    public static class Frame {
        public int length;
        public byte type;
        public byte flags;
        public int streamId;
        public byte[] payload;

        public Map<String, String> headers = new HashMap<>();
        public byte[] body = new byte[0];
    }

    public static Frame readFrame(DataInputStream in) throws IOException {
        byte[] lenBytes = new byte[3];
        in.readFully(lenBytes);
        int length = ((lenBytes[0] & 0xFF) << 16) | ((lenBytes[1] & 0xFF) << 8) | (lenBytes[2] & 0xFF);
        
        byte type = in.readByte();
        byte flags = in.readByte();
        int streamId = in.readInt() & 0x7FFFFFFF;

        byte[] payload = new byte[length];
        in.readFully(payload);

        Frame frame = new Frame();
        frame.length = length;
        frame.type = type;
        frame.flags = flags;
        frame.streamId = streamId;
        frame.payload = payload;

        if (type == TYPE_REQUEST || type == TYPE_RESPONSE) {
            parsePayload(frame);
        }

        return frame;
    }

    public static void writeFrame(DataOutputStream out, Frame frame) throws IOException {
        byte[] payload = serializePayload(frame);
        frame.length = payload.length;

        out.write((frame.length >> 16) & 0xFF);
        out.write((frame.length >> 8) & 0xFF);
        out.write(frame.length & 0xFF);
        out.writeByte(frame.type);
        out.writeByte(frame.flags);
        out.writeInt(frame.streamId & 0x7FFFFFFF);
        out.write(payload);
        out.flush();
    }

    private static void parsePayload(Frame frame) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(frame.payload);
        DataInputStream din = new DataInputStream(bais);

        while (din.available() > 0) {
            byte id = din.readByte();
            if ((id & 0xFF) == 0xFF) {
                // End of headers
                int remaining = din.available();
                frame.body = new byte[remaining];
                din.readFully(frame.body);
                break;
            }

            String name;
            if (id == 0x00) {
                int nameLen = din.readByte() & 0xFF;
                byte[] nameBytes = new byte[nameLen];
                din.readFully(nameBytes);
                name = new String(nameBytes, "UTF-8");
            } else {
                name = ID_TO_HEADER.get(id);
                if (name == null) name = "Unknown-" + id;
            }

            int valLen = din.readUnsignedShort();
            byte[] valBytes = new byte[valLen];
            din.readFully(valBytes);
            String value = new String(valBytes, "UTF-8");

            frame.headers.put(name, value);
        }
    }

    private static byte[] serializePayload(Frame frame) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(baos);

        for (Map.Entry<String, String> entry : frame.headers.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            Byte id = HEADER_TO_ID.get(name);
            if (id != null) {
                dout.writeByte(id);
            } else {
                dout.writeByte(0x00);
                byte[] nameBytes = name.getBytes("UTF-8");
                dout.writeByte(nameBytes.length);
                dout.write(nameBytes);
            }

            byte[] valBytes = value.getBytes("UTF-8");
            dout.writeShort(valBytes.length);
            dout.write(valBytes);
        }

        dout.writeByte(0xFF); // End of headers
        if (frame.body != null && frame.body.length > 0) {
            dout.write(frame.body);
        }

        return baos.toByteArray();
    }
    
    public static void printHexDump(String label, Frame f) {
        System.out.println("--- " + label + " HEXDUMP ---");
        System.out.printf("Length: %d, Type: 0x%02X, Flags: 0x%02X, Stream: %d\n", f.length, f.type, f.flags, f.streamId);
        
        byte[] header = new byte[9];
        header[0] = (byte) ((f.length >> 16) & 0xFF);
        header[1] = (byte) ((f.length >> 8) & 0xFF);
        header[2] = (byte) (f.length & 0xFF);
        header[3] = f.type;
        header[4] = f.flags;
        header[5] = (byte) ((f.streamId >> 24) & 0xFF);
        header[6] = (byte) ((f.streamId >> 16) & 0xFF);
        header[7] = (byte) ((f.streamId >> 8) & 0xFF);
        header[8] = (byte) (f.streamId & 0xFF);
        
        System.out.println("Header Bytes:");
        dumpBytes(header);
        
        if (f.payload != null && f.payload.length > 0) {
            System.out.println("Payload Bytes:");
            dumpBytes(f.payload);
        }
        System.out.println("-------------------------");
    }

    private static void dumpBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; i += 16) {
            System.out.printf("%04X: ", i);
            for (int j = 0; j < 16; j++) {
                if (i + j < bytes.length) {
                    System.out.printf("%02X ", bytes[i + j]);
                } else {
                    System.out.print("   ");
                }
            }
            System.out.print("  ");
            for (int j = 0; j < 16; j++) {
                if (i + j < bytes.length) {
                    char c = (char) bytes[i + j];
                    if (c >= 32 && c <= 126) {
                        System.out.print(c);
                    } else {
                        System.out.print(".");
                    }
                }
            }
            System.out.println();
        }
    }
}
