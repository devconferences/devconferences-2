#!/bin/sh

echo 'NPM Setup'

source /home/bas/.nvm/nvm.sh
nvm use stable
nvm ls
nvm exec 0.10 node --version
nvm exec 0.10 npm --version

echo 'NPM install deps'

npm install

echo 'NPM build'

npm run build
