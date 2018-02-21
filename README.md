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
in `application.conf`.

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

Development
------

### Prerequisites

- You need a working postgres installation.  On Linux this is normally as simple as `apt-get install postgresql` or `yum install postgresql`.  On Mac it's best to install Homebrew, then you can run `brew install postgresql`
- You need an installation of node.js to build the frontend.  It's easiest to first install the Node Version Manager, nvm, and then use this to install node: `nvm install 8.1.3`
- You need a version 1.8+ JDK.  On  Linux this is normally as simple as `apt-get install openjdk-8-jdk` or the yum equivalent
- You need to set up the test database before the tests will work: `sudo -u postgres ./scripts/create_dev_db.sh` (**Note**: if installing through homebrew, postgres runs as the current user so the `sudo -u postgres` part is not required)

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
- **Note** - react-multistep is not properly webpack compliant and needs to be transpiled seperately.  This is automatically done when you run `npm run compile` or `npm run dev`, by running the `frontend/scripts/fix-multistep` script.  If you see errors relating to multistep, then try deleting `frontend/node_modules/react-multistep`, reinstalling with `yarn install` and running this script manually
