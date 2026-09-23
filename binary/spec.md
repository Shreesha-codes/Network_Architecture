# Binary HTTP Protocol Specification

## 1. Introduction
This document defines a custom, minimal binary HTTP protocol designed for efficiency and simplicity. The protocol is connection-oriented, running over a raw TCP socket, and supports multiplexing for future iterations via a Stream ID. 

## 2. Frame Structure
Every message in this protocol is composed of one or more frames. All frames begin with a fixed **9-byte** header, heavily inspired by the HTTP/2 specification.

### 2.1. Frame Header (9 bytes)
```
+-----------------------------------------------+
|                 Length (24)                   |
+---------------+---------------+---------------+
|   Type (8)    |   Flags (8)   |
+---------------+---------------+---------------+
|R|                 Stream Identifier (31)      |
+-----------------------------------------------+
```

* **Length (24 bits):** The length of the frame payload as an unsigned 24-bit integer. The maximum payload size is 16,777,215 bytes (~16 MB).
* **Type (8 bits):** Indicates the type of the frame.
    * `0x01` : REQUEST
    * `0x02` : RESPONSE
* **Flags (8 bits):** Boolean flags specific to the frame type. Currently unused (set to `0x00`).
* **Stream Identifier (31 bits):** Identifies the stream. The most significant bit (R) is reserved and MUST remain `0`. This avoids signed integer overflow issues in environments like Java that lack unsigned 32-bit integer types.

### 2.2. Forward Compatibility
If an endpoint receives a frame with an unrecognized `Type`, it **MUST** use the `Length` field to read and discard the payload bytes. This mechanism ensures that the protocol can be safely upgraded in version 2 with new frame types without breaking existing parsers.

## 3. Headers Encoding
Headers are encoded in the payload of `REQUEST` and `RESPONSE` frames. To optimize for size, common headers are numbered.

### 3.1. Predefined Headers
The following names are predefined and assigned a 1-byte ID (from `1` to `10`):
1. `Method`
2. `Path`
3. `Status`
4. `Content-Type`
5. `Content-Length`
6. `Host`
7. `User-Agent`
8. `Accept`
9. `Connection`
10. `Server`

**Encoding a Predefined Header:**
```
[1-byte ID] + [2-byte Value Length] + [Value Bytes]
```

### 3.2. Custom Headers
If a header is not in the predefined list, it is assigned the ID `0x00` and its name is length-prefixed.

**Encoding a Custom Header:**
```
[0x00] + [1-byte Name Length] + [Name Bytes] + [2-byte Value Length] + [Value Bytes]
```

### 3.3. End of Headers
The end of the headers block within a payload is indicated by a single `0xFF` byte. Any bytes following this marker within the payload belong to the body (e.g., file contents).

## 4. Frame Types

### 4.1. REQUEST Frame (`Type = 0x01`)
Sent by the client to request a resource. The payload contains encoded headers. 
* A `Path` header (ID `2`) is mandatory.
* Example sequence: `[0x01] (REQUEST type) -> [0x02] (Path ID) -> [0x00, 0x0B] (Length 11) -> "/index.html" -> [0xFF] (End of headers)`

### 4.2. RESPONSE Frame (`Type = 0x02`)
Sent by the server in reply to a request.
* A `Status` header (ID `3`) is mandatory.
* The body of the requested file is appended immediately after the `0xFF` end-of-headers marker.
* Example sequence: `[0x02] (RESPONSE type) -> [0x03] (Status ID) -> [0x00, 0x03] (Length 3) -> "200" -> [0xFF] (End of headers) -> [File Bytes...]`
