# Edition des conférences et des communautés

Ici se trouve les différentes conférences et communautés référencées sur DevConferences.
Si vous voulez ajouter ou modifier une conférence / communauté (appelés Event pour la
suite), voici la procédure à suivre.

## Structure du dossier

Chaque Event est contenu dans un dossier qui correspond à sa ville. Un Event est représenté
par un fichier JSON.

Par exemple, le BreizhCamp, se déroulant à Rennes, est représenté par le fichier
`Rennes/breizhcamp.json`

## Convention

Par souci d'uniformisation des Events, merci de respecter les conventions suivantes :

- Les noms de dossiers des villes sont en Majuscules Pour Chaque Mot. Les espaces, les accents
et les tirets sont bien sûr autorisés.
- Les noms des fichiers sont en minuscule. Seul les lettres non accentuées et les chiffres
sont autorisées. Le nom DOIT correspondre à l'`id` de l'Event.
- L'`id` DOIT être unique parmi tous les Events.
- Merci de respecter la mise en forme des fichiers (notamment l'indentation avec 4 espaces,
et un seul attribut par ligne), par souci de lisibilité pour tous.

## Procédure

Tout d'abord, il est nécessaire de fork le projet, et d'installer les dépendances, ce
qui vous permettra notamment de tester le jeu de données en local. Cependant, pensez à
lancer `mvn clean` entre chaque compilation, sinon les anciens fichiers JSON peuvent
être utilisé lors de la création du noeud Elastic, et qui peut entraîner des bugs.

Voir [le README du dépôt](http://www.github.com/devconferences/devconferences-2) pour
les installations nécessaires.

### Vérification du jeu de données

Pour vérifier TOUS les Events, il faut faire une compilation classique, et lancer le
main de `org.devconferences.Main` avec l'option `-DONLY_CHECK_EVENTS=true`.

Par exemple, en ligne de commande :
```
mvn clean package exec:java -DONLY_CHECK_EVENTS=true
```

Cela lèvera une RuntimeException si un fichier est corrompu.

### Modification d'un Event

Pour modifier un Event, rien de plus simple : il suffit de modifier le fichiers JSON
correspondant, puis `add commit push` sur votre fork.

### Création d'un Event

Pour ajouter un Event, c'est pas plus compliqué : copiez le fichier `event.json.example`,
modifiez les attributs nécessaires, puis `add commit push` sur votre fork. Attention à la
convention de nommage !

### Création du Pull Request

Enfin, créez une Pull Request sur le projet, avec le tag `event modification`. Une petite explication
est toujours la bienvenue ! Travis vérifiera alors la PR, avant qu'un administrateur ne valide
ou non la PR.

## Liste des attributs des fichiers JSON

### Attributs obligatoires

#### `id`

L'identifiant de l'Event.

L'identifiant DOIT être :

- unique parmi tous les Events
- tout en minuscule, avec éventuellement des chiffres

Le nom du fichier DOIT être le même que l'identifiant.

#### `type`

Le type de l'Event.

2 valeurs possibles :

- `COMMUNITY`, pour une communauté
- `CONFERENCE`, pour une conférence

#### `name`

Le nom de l'Event.

#### `description`

Une rapide description de l'Event.

### Attributs recommandés

#### `website`

Le site web de l'Event.

#### `avatar`

L'avatar de l'Event.

Il s'agit de l'URL vers cette image. Une image par défaut est mise si cet attribut
n'est pas édité.

### Attributs optionnels

#### `facebook`

La page Facebook de l'event.

Il s'agit du suffixe à `facebook.com`. Cela marche donc aussi bien pour une page
que pour un groupe (laissez juste le `groups/` dans ce cas)

#### `twitter`

Le compte Twitter de l'Event.

#### `meetup`

Le compte Meetup de l'Event.

Si cet attribut est ajouté, DevConferences affichera alors le prochain événement
du compte.

#### `tags`

Les différents tags de l'Event.

Cela permet de regrouper les différents Events par thème (`java`, `jug`, `gdg`, ...),
notamment lors de la recherche. Il s'agit d'un tableau.

#### `location`

La localisation de l'Event.

Cet attribut n'est pas encore géré, mais c'est prévu dans un futur proche...
