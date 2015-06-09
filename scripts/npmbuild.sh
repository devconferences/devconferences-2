#!/bin/sh

echo 'NPM Setup'

cat /home/bas/.nvm/nvm.sh
source /home/bas/.nvm/nvm.sh

echo 'NPM install deps'

npm install

echo 'NPM build'

npm run build
