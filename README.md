# DevConferences - version 2

[![Join the chat at https://gitter.im/devconferences/devconferences-2](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/devconferences/devconferences-2?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Repository pour la version 2 de DevConferences.org

## Dev

Après le clone du repo, lancer 

```
$ npm install
```

Puis pour builder le front en mode `watch`, lancer 

```
$ npm start
```

## Build & Deploy

Pour builder la partie ReactJS : `npm run build`

Le déploiement se fait automatique sur Clever Cloud lorsque du code est pushé sur le master : [devconferences.cleverapps.io](http://devconferences.cleverapps.io)

## Data

- Ajout / édition de communautés ou de conférences (informations) : [ici](https://github.com/devconferences/devconferences-2/tree/master/src/main/resources/events)
- Ajout / édition d'événements (agenda) : [ici](https://github.com/devconferences/devconferences-2/tree/master/src/main/resources/calendar)

## TODO list

- [ ] ajouter des tags (ex : jug, java, php, gdg ...)
- [ ] ajouter la possibilité d'effectuer des recherches
- [ ] ajouter des liens vers les chaînes vidéo (youtube, parleys ...) des conférences
- [ ] gérér des agendas pour chacune des villes
- [ ] automatiser l'import d'événements depuis meetup.com
- [ ] automatiser l'import d'événements depuis lanyrd.com
- [ ] automatiser l'envoi de tweets pour annoncer les prochaines sessions (via @devconferences)
- [ ] faciliter la contribution de nouvelles conférences/communautés (formulaire + authentification github par exemple)
- [ ] permettre la souscription à une news-letter
- [ ] permettre l'envoi de notifications (par ville, par thème / catégorie)
- [ ] fournir des statistiques sur les conférences
- [ ] ajouter une carte type google-map
- [ ] lister les CFP / s'abonner aux CFP
