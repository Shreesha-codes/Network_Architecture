EARLY HTTP/1.1 ASSIGNMENT DUE BEFORE SESSION 7[cite: 1]
Build a calculator that stays on the line[cite: 1]

The task[cite: 1]
any language, no framework, just a socket[cite: 1]

GET /add?a=2&b=3[cite: 1]
-> 200[cite: 1]
5[cite: 1]

GET /sub?a=10&b=4[cite: 1]
-> 200[cite: 1]
6[cite: 1]

GET /mul?a=6&b=7[cite: 1]
-> 200[cite: 1]
42[cite: 1]

GET /div?a=9&b=3[cite: 1]
-> 200[cite: 1]
3[cite: 1]

GET /div?a=1&b=0[cite: 1]
-> 400[cite: 1]

GET /add?a=x&b=3[cite: 1]
-> 400[cite: 1]

GET /pow?a=2&b=8[cite: 1]
-> 404[cite: 1]

POST /add[cite: 1]
-> 405[cite: 1]

GET /add (no Host)[cite: 1]
-> 400[cite: 1]

That is the entire feature set. You could write it in an afternoon, and the arithmetic is not the point.[cite: 1]

Don't read 02-http11/server11.py[cite: 1]

How I will mark it[cite: 1]
one socket. every request.[cite: 1]

socket.create_connection(("localhost", 8060))[cite: 1]
GET /add?a=2&b=3 -> 200 5[cite: 1]
GET /sub?a=10&b=4 -> 200 6[cite: 1]
GET /mul?a=6&b=7 -> 200 42[cite: 1]
GET /div?a=1&b=0 -> 400[cite: 1]
GET /pow?a=2&b=8 -> 404[cite: 1]
POST /add -> 405[cite: 1]

socket still open: True[cite: 1]

1 TCP handshake, 6 responses[cite: 1]

If the socket dies before I am done,[cite: 1]

The part that is actually hard. Keeping the connection open forces a question HTTP/1.0 never asked you: where does this request end and the next one begin? While you were hanging up, the answer was "at EOF", for free. Now you must consume exactly Content-Length bytes and not one more byte n+1 belongs to somebody else.[cite: 1]

Stretch, all optional: honour Connection: close an idle timeout you can defend chunked encoding take all six at once and answer in order, which is pipelining..[cite: 1]

COURSE PROJECT HTTP, IN BINARY[cite: 1]
TWO TRACKS, ONE PROTOCOL[cite: 1]
Now you write the spec[cite: 1]

Track 1 the server[cite: 1]
$ ./bserve ./www 9000[cite: 1]
accept a TCP connection[cite: 1]
read one binary request frame[cite: 1]
map the path to a file under a root[cite: 1]
reply: status, headers, the bytes[cite: 1]
404 if it is not there[cite: 1]
400 if the frame is malformed[cite: 1]
and keep the connection open[cite: 1]

Track 2- the client[cite: 1]
$./bcurl -v localhost:9000/index.html[cite: 1]
build the binary request frame[cite: 1]
read the response, body to stdout[cite: 1]
-v hexdumps every frame[cite: 1]
exit non-zero on 4xx / 5xx[cite: 1]
and never open a second connection[cite: 1]

The bit in the middle is the actual project[cite: 1]
A fixed-size frame header you pick the fields and the widths, and you defend them. HTTP/2 chose 24/8/8/31. Why?[cite: 1]
Headers: number the ten names you actually send, length-prefix the rest HPACK's first two mechanisms, in an evening.[cite: 1]
And one line you may not skip: a receiver meeting a frame type it does not know MUST skip it cleanly. That is how you leave room for a version 2.[cite: 1]

What you hand in[cite: 1]
1. The spec. Two pages. Enough for a stranger.[cite: 1]
2. Your program.[cite: 1]
3. An annotated hexdump of one complete request and response.[cite: 1]

If you cannot annotate your own bytes, the spec is not finished.[cite: 1]

In pairs: one server, one client, and the only thing that crosses between you is the spec. A client that only works against your own server is an implementation, not a protocol.[cite: 1]