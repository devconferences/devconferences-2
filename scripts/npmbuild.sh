#!/bin/sh

echo 'NPM Setup'

source /home/bas/.nvm/nvm.sh

echo 'NPM install deps'

npm install

echo 'NPM build'

npm run build
