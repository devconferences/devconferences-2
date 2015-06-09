#!/bin/sh

echo 'NPM Setup'

source /home/bas/.nvm/nvm.sh
nvm use 0.10

echo 'NPM install deps'

npm install

echo 'NPM build'

npm run build
