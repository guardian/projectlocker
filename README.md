#Projectlocker

Projectlocker is a database interface for storing, backing up and creating new projects from templates.

## Installation

Projectlocker uses a CI build system, at http://circleci.com/gh/fredex42/projectlocker.  RPMs are built for every commit, 
and it should be installed from said RPM.

## Development

###Prerequisites

- You need a working postgres installation.  On Linux this is normally as simple as `apt-get install postgresql` or `yum install postgresql`.  On Mac it's best to install Homebrew, then you can run `brew install postgresql`
- You need an installation of node.js to build the frontend.  It's easiest to first install the Node Version Manager, nvm, and then use this to install node: `nvm install 8.1.3`
- You need a version 1.8+ JDK.  On  Linux this is normally as simple as `apt-get install openjdk-8-jdk` or the yum equivalent
- You need to set up the test database before the tests will work: `sudo -u postgres ./scripts/create_dev_db.sh` (**Note**: if installing through homebrew, postgres runs as the current user so the `sudo -u postgres` part is not required)

###Backend

- The backend is a Scala play project.  IDEs like Intellij IDEA can load this directly and have all of the tools needed to build and test.
- If you want to do this from the terminal, you'll need to have the Scala Built tool installed: `wget https://dl.bintray.com/sbt/debian/sbt-0.13.12.deb && sudo dpkg -i sbt-0.13.12.deb`
- You can then run the backend tests: `sbt test`.  **Note**: after each invokation of the backend tests, you should run `scripts/blank_test_db.sh` to reset the tests database so that the next invokation will run correctly.
- If the tests fail, check that you have set up the projectlocker_test database properly (see previous section, and also check `circle.yml` to see how the CI environment does it)

###Frontend

- The frontend is written in ES2016 javascript using React JSX.  To run in a browser, it's transpiled with Babel and bundled with Webpack
- Set up the tools like so: `cd frontend; yarn install` (or `cd frontend; npm install` if you don't like yarn)
- Tests can be run via `npm test`
- To just build the frontend, you can run `npm run compile` from the frontend directory
- To develop the frontend, you should run `npm run dev` from the frontend directory.  This doesn't terminate, it asks Webpack to stay running and check the source files for modifications and automatically rebuild for you
- **Note** - react-multistep is not properly webpack compliant and needs to be transpiled seperately.  This is automatically done when you run `npm run compile` or `npm run dev`, by running the `frontend/scripts/fix-multistep` script.  If you see errors relating to multistep, then try deleting `frontend/node_modules/react-multistep`, reinstalling with `yarn install` and running this script manually
