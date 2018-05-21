#!/usr/bin/env bash -e

curl https://raw.githubusercontent.com/creationix/nvm/v0.33.11/install.sh | bash
source ~/.bashrc
nvm install 8.1.3
curl -o- -L https://yarnpkg.com/install.sh | bash
