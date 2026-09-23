EARLY HTTP/1.1 ASSIGNMENT DUE BEFORE SESSION 7
Build a calculator that stays on the line

The task
any language, no framework, just a socket

GET /add?a=2&b=3
-> 200
5

GET /sub?a=10&b=4
-> 200
6

GET /mul?a=6&b=7
-> 200
42

GET /div?a=9&b=3
-> 200
3

GET /div?a=1&b=0
-> 400

GET /add?a=x&b=3
-> 400

GET /pow?a=2&b=8
-> 404

POST /add
-> 405

GET /add (no Host)
-> 400

That is the entire feature set. You could write it in an afternoon, and the arithmetic is not the point.

Don't read 02-http11/server11.py

How I will mark it
one socket. every request.

socket.create_connection(("localhost", 8060))
GET /add?a=2&b=3 -> 200 5
GET /sub?a=10&b=4 -> 200 6
GET /mul?a=6&b=7 -> 200 42
GET /div?a=1&b=0 -> 400
GET /pow?a=2&b=8 -> 404
POST /add -> 405

socket still open: True

1 TCP handshake, 6 responses

If the socket dies before I am done,

The part that is actually hard. Keeping the connection open forces a question HTTP/1.0 never asked you: where does this request end and the next one begin? While you were hanging up, the answer was "at EOF", for free. Now you must consume exactly Content-Length bytes and not one more byte n+1 belongs to somebody else.

Stretch, all optional: honour Connection: close an idle timeout you can defend chunked encoding take all six at once and answer in order, which is pipelining..