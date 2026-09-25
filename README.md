# Early HTTP/1.1 Assignment: Build a Calculator That Stays on the Line

**Due before Session 7**

## The Task

Write a server in **any language, with no framework, just using a socket**.

### Feature Set

Here is the complete expected feature set. You could write it in an afternoon, and the arithmetic is not the point. 

> **Note:** Do not read `02-http11/server11.py`

```http
GET /add?a=2&b=3    -> 200  5
GET /sub?a=10&b=4   -> 200  6
GET /mul?a=6&b=7    -> 200  42
GET /div?a=9&b=3    -> 200  3

GET /div?a=1&b=0    -> 400
GET /add?a=x&b=3    -> 400
GET /pow?a=2&b=8    -> 404
POST /add           -> 405
GET /add (no Host)  -> 400
```

## How It Will Be Marked

- **One socket.**
- **Every request.**

```python
s = socket.create_connection(("localhost", 8060))

GET /add?a=2&b=3  -> 200 5
GET /sub?a=10&b=4 -> 200 6
GET /mul?a=6&b=7  -> 200 42
GET /div?a=1&b=0  -> 400
GET /pow?a=2&b=8  -> 404
POST /add         -> 405

socket still open: True
```

**Goal:** 1 TCP handshake, 6 responses.

*(If the socket dies before the tests are done, the test fails.)*

## The Hard Part

Keeping the connection open forces a question HTTP/1.0 never asked you: **where does this request end and the next one begin?** 

While you were hanging up, the answer was "at EOF", for free. Now you must consume exactly `Content-Length` bytes and not one more — byte `n+1` belongs to somebody else.

### Optional Stretch Goals

- Honour `Connection: close`
- Implement an idle timeout you can defend
- Support chunked encoding
- Take all six requests at once and answer in order (pipelining)



