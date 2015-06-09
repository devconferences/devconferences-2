#!/bin/sh

echo 'NPM Setup'

source /home/bas/.nvm/nvm.sh
nvm install stable
nvm use stable

echo 'NPM install deps'

npm install

echo 'NPM build'

npm run build
