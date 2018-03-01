Authenticating Desktop Apps
======================

Projectlocker has two options to authenticate - user-based and server based.  I would recommend user-based login for desktop apps.

##User-based login

Projectlocker uses session-based authentication for users, relying on cookies.  This is pretty simple in a web browser 
as the browser does most of the work for you; in a desktop app you will need to store the cookie securely and provide
it in the request headers of each request that you make to the server.

###How to log in

The first step is to make an HTTP POST request to `/api/login` (in `app/controllers/Application.scala`). 
You will need to include a JSON document in the request body (with appropriate `Content-Type` set in the headers) 
supplying the user credentials:

```
{
  "username": "joe_bloggs",
  "password": "somethingR3allyl0ng@ndC0mp1ex"
}
```

This is secure so long as you **only communicate with the server over https**.

If your login fails, then the server will respond with a `403 Forbidden` response and you should reflect this to the
user.

If your login is successful, then the server will respond with a `200 OK` response and the following content:

```
{
  "status": "ok",
  "detail": "Logged in",
  "uid": "joe_bloggs"
}
```

where uid is the recognised username.

The response will also contain a cookie, called `projectlocker_session` (the exact name is configurable in `conf/application.conf`)
This should be treated as a session token, and stored securely.  It consists of several sets of base64-encoded data,
one of which is a signature that validates that it was indeed created by the server in question and so prevents tampering.

This cookie should be presented back to the server as a cookie with each subsequent request in order to validate the user.
Since it does not change between requests it should only be held in memory or stored securely

###How to check that a session cookie is valid

In order to check if you have a valid login session, you can make an HTTP GET request to `/api/isLoggedIn`.  If you present
a valid session cookie, the server will respond with a `200 OK` response and the following request body:

```
{
  "status": "ok",
  "uid": "name_of_logged_in_user"
}
```

If the cookie is not valid, or no cookie is presented, then a `403 Forbidden` response is returned. 

###Session expiry timeout

At the moment, there is no limit to the session length, but this will probably change.  This document will be updated
with the responses to expect when a session has expired.

###Logging out

To log out, simply invalidate the session.  This can be done by making an authenticated POST request to `/api/logout`.

