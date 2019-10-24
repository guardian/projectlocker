#!/usr/bin/env bash -e

curl https://raw.githubusercontent.com/creationix/nvm/v0.34.0/install.sh | bash
source ~/.bashrc
nvm install 12.13.0
curl -o- -L https://yarnpkg.com/install.sh | bash
