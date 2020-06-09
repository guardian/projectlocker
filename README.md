Projectlocker
======

Projectlocker is a database interface for storing, backing up and creating new projects from templates.

## Installation

Projectlocker uses a CI build system, at http://circleci.com/gh/fredex42/projectlocker.  RPMs are built for every commit, 
and it should be installed from said RPM.

## Session cookie setup

Logins are persisted by using session cookies. This is controlled by the `play.http.session` section of `application.conf`.

When deploying, you should ensure that the `domain = ` setting is configured to be the domain within which you are deploying,
to prevent cookie theft.  It's also recommended to serve via https and set `secure = true` (but this could be problematic if you're
only implementing https to the loadbalancer)

## Authentication Setup

Projectlocker is intended to run against an ldap-based authentication system, such as Active Directory. This is configured
in `application.conf` but it can be turned off during development.

### ldaps

Secure ldap is recommended, as it not only encrypts the connection but protects against man-in-the-middle attacks.
In order to configure this, you will need to have a copy of the server's certificate and to create a trust store with it.
If your certificate is called `certificate.cer`, then the following commands will create a keystore:

```
$ mkdir -p /usr/share/projectlocker/conf
$ keytool -import -keystore /usr/share/projectlocker/conf/keystore.jks -file certificate.cer
[keytool will prompt for a secure passphrase for the keystore and confirmation to add the cert]
```

`keytool` should be provided by your java runtime environment.

In order to configure this, you need to adjust the `ldap` section in `application.conf`:

```
  ldapProtocol = "ldaps"
  ldapUseKeystore = true
  ldapPort = 636
  ldapHost0 = "adhost1.myorg.int"
  ldapHost1 = "adhost2.myorg.int"
  serverAddresses = ["adhost1.myorg.int","adhost2.myorg.int"]
  serverPorts = [ldapPort,ldapPort]
  bindDN = "aduser"
  bindPass = "adpassword"
  poolSize = 3
  roleBaseDN = "DC=myorg,DC=com"
  userBaseDN = "DC=myorg,DC=com"
  trustStore = "/usr/share/projectlocker/conf/keystore.jks"
  trustStorePass = "YourPassphraseHere"
  trustStoreType = "JKS"
  ldapCacheDuration = 600
  acg1 = "acg-name-1"
```

Replace `adhost*.myorg.int` with the names of your AD servers, `aduser` and `adpassword` with the username and password
to log into AD, and your DNs in `roleBaseDN` and `userBaseDN`.

### ldap

Plain unencrypted ldap can also be used, but is discouraged.  No keystore is needed, simply configure the `application.conf`
as above but use `ldapProtocol = "ldap"` and `ldapPort = 336` instead.

### none

Authentication can be disabled, if you are working on development without access to an ldap server.  Simply set
`ldapProtocol = "none"` in `application.conf`.  This will treat any session to be logged in with a username of `noldap`.

Fairly obviously, don't deploy the system like this!


Development
------

### Prerequisites

- You need a working postgres installation. You could install postgres using a package manager, or you can quickly run a Dockerised version by
running the `setup_docker_postgres.sh` script in `scripts`.
- You need an installation of node.js to build the frontend.  It's easiest to first install the Node Version Manager, nvm, and then use this to install node: `nvm install 8.1.3`
- You need a version 1.8+ JDK.  On  Linux this is normally as simple as `apt-get install openjdk-8-jdk` or the yum equivalent
- If you are not using the postgres docker image, you will need to set up the test database before the tests will work:
 `sudo -u postgres ./scripts/create_dev_db.sh` (**Note**: if installing through homebrew, postgres runs as the current 
 user so the `sudo -u postgres` part is not required)

### Backend

- The backend is a Scala play project.  IDEs like Intellij IDEA can load this directly and have all of the tools needed to build and test.
- If you want to do this from the terminal, you'll need to have the Scala Built tool installed: `wget https://dl.bintray.com/sbt/debian/sbt-0.13.12.deb && sudo dpkg -i sbt-0.13.12.deb`
- You can then run the backend tests: `sbt test`.  **Note**: after each invokation of the backend tests, you should run `scripts/blank_test_db.sh` to reset the tests database so that the next invokation will run correctly.
- If the tests fail, check that you have set up the projectlocker_test database properly (see previous section, and also check `circle.yml` to see how the CI environment does it)

### Frontend

- The frontend is written in ES2016 javascript using React JSX.  To run in a browser, it's transpiled with Babel and bundled with Webpack
- I'd recommend installing yarn to manage javascript dependencies: see `https://yarnpkg.com/lang/en/docs/install/`
- Set up the tools like so: `cd frontend; yarn install` (or `cd frontend; npm install` if you don't like yarn)
- Tests can be run via `npm test`
- To just build the frontend, you can run `npm run compile` from the frontend directory
- To develop the frontend, you should run `npm run dev` from the frontend directory.  This doesn't terminate, it asks Webpack to stay running and check the source files for modifications and automatically rebuild for you

### Running the backend tests

The backend tests can be run with sbt, but since they depend on having a test database (`projectlocker-test`) in a specific state to start
and on the akka cluster configuration being suitable, they can be a pain to get working.  If you are having trouble getting `sbt test` to pass,
make sure of the following:
- set `ldap.ldapProtocol` to `"ldaps"` in the config (this will fix errors around "expected "testuser", got "noldap")
- set `akka.remote.netty.tcp.port` to `0` in order to bind to a random available port (this will fix guice provisioning errors,
 but can stop the app running properly in dev mode)
- run the database with `scripts/setup_docker_postgres.sh`.  This will automatically set up projectlocker_test for you.
- reset the state of the database before each test run. The simplest way to do this is to use `scripts/setup_docker_postgres.sh`
and use CTRL-C to exit the database and re-run it to set up the environment again before each run

### Signing requests for server->server interactions

Projectlocker supports HMAC signing of requests for server-server actions.
In order to use this, you must:

- provide a base64 encoded SHA-384 checksum of your request's content in a header called `X-Sha384-Checksum`
- ensure that an HTTP date is present in a header called `Date`
- ensure that the length of your body content is present in a header called `Content-Length`. If there is no body then this value should be 0.
- provide a signature in a header called 'Authorization'.  This should be of the form `{uid}:{auth}`, where {uid} is a user-provided
identifier of the client and {auth} is the signature

The signature should be calculated like this:

- make a string of the contents of the Date, Content-Length and Checksum headers separated by newlines followed by the
 request method and URI path (not query parts) also separated by newlines.
- use the server's shared secret to calculate an SHA-384 digest of this string, and base64 encode it
- the server performs the same calculation (in `auth/HMAC.scala`) and if the two signatures match then you are in.
- if you have troubles, turn on debug at the server end to check the string_to_sign and digests

There is a working example of how to do this in Python in `scripts/test_hmac_auth.py`