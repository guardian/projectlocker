## Build/Test environment setup

This project uses TeamCity (but was ported from circleci) to build.
This directory contains the build configuration (`config.yml`) which references
a customised Centos Docker image containing the right tools to build the project.

This image is built with the included `Dockerfile`, which uses the scripts held in `containerscripts` to set up the image to build the project.

In order to build the image, first you must make sure that you have Docker installed.  Then, 

```$bash
$ cd .circleci
$ docker build -t {your-repo}/projectlockerbuild:latest .
$ docker run --rm -it {your-repo}/projectlockerbuild:latest
[check that it is working how you expect]
$ docker push {your-repo}/projectlockerbuild:latest
```

Once you have done this, edit each occurrence of `docker: - image:` in config.yml to use the newly built image - there is more than one occurrence.
CircleCI downloads the image each time a new container is initialised.

For more information, see https://circleci.com/docs/2.0/containers/