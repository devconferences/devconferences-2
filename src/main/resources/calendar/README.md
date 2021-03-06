# Edition des événements

Ici se trouve les différentes événements référencés sur DevConferences.
Si vous voulez ajouter ou modifier un événement (appelés CalendarEvent pour la
suite), voici la procédure à suivre.

**Remarque :** Si l'identifiant Meetup est renseigné pour un Event, les événements à venir
du compte sont automatiquement importés. Il n'est donc pas nécessaire d'ajouter de fichiers.

## Structure du dossier

Chaque CalendarEvent est dans une arborescence de type "year/month/file_azerty.json".
Un CalendarEvent est représenté par un fichier JSON.

Par exemple, la Devoxx FR 2016, qui s'est déroulé en avril 2016, est représenté par le fichier
`2016/04/devoxxfr2016.json`

## Convention

Par souci d'uniformisation des CalendarEvents, merci de respecter les conventions suivantes :

- Les dossiers de mois SONT avec 2 chiffres, donc janvier correspond à `01`.
- Les noms des fichiers sont en minuscule. Seul les lettres non accentuées
et les chiffres sont autorisées. Le fichier DOIT correspondre à l'`id` de l'Event.
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

Pour vérifier TOUS les CalendarEvents, il faut faire une compilation classique, et lancer le
main de `org.devconferences.Main` avec l'option `-DONLY_CHECK_CALENDAR=true`.

Par exemple, en ligne de commande :
```
mvn clean package exec:java -DONLY_CHECK_CALENDAR=true
```

Cela lèvera une RuntimeException si un fichier est corrompu.

### Modification d'un CalendarEvent

Pour modifier un CalendarEvent, rien de plus simple : il suffit de modifier le fichiers JSON
correspondant, puis `add commit push` sur votre fork.

### Création d'un CalendarEvent

Pour ajouter un CalendarEvent, c'est pas plus compliqué : copiez le fichier `calendar.json.example`,
modifiez les attributs nécessaires, puis `add commit push` sur votre fork. Attention à la
convention de nommage !

### Création du Pull Request

Enfin, créez une Pull Request sur le projet, avec le tag `calendar modification`. Une petite explication
est toujours la bienvenue !

## Liste des attributs des fichiers JSON

### Attributs obligatoires

#### `id`

L'identifiant du CalendarEvent.

L'identifiant DOIT être :

- unique parmi tous les CalendarEvents
- tout en minuscule, avec éventuellement des chiffres

Le nom du fichier DOIT être le même que l'identifiant.

#### `name`

Le nom du CalendarEvent.

#### `description`

Une rapide description du CalendarEvent.

#### `date`

La date du CalendarEvent.

Il s'agit d'un timestamp en millisecondes.

### Attributs recommandés

#### `url`

Le site web du CalendarEvent.

### Attributs optionnels

#### `duration`

La durée du CalendarEvent.

Comme pour `date`, il s'agit d'un timestamp en millisecondes.

#### `organizer.*`

Les informations concernant l'organisateur.

Il contient les sous-champs suivants :

##### `organizer.name`

Le nom de l'organisateur du CalendarEvent.

##### `organizer.url`

Le site de l'organisateur du CalendarEvent.

`organizer.name` doit être renseigné, sinon le lien ne s'affichera pas.

#### `location.*`

Le lieu du CalendarEvent.

Soit `location` n'existe pas, soit TOUS les champs suivants sont renseignés :

##### `location.city`

La ville où se trouve le CalendarEvent.

##### `location.address`

L'adresse du CalendarEvent.

Ici, il faut juste mettre la rue, pas besoin de mettre la ville.

##### `location.name`

Le nom du lieu associé à l'adresse.

##### `location.gps`

La géolocalisation du CalendarEvent.

Cela permet de créer un lien vers Google Maps pour afficher un plan.

#### `cfp.*`

Les informations concernant le Call For Paper du CalendarEvent.

Soit `cfp` n'existe pas, soit TOUS les champs suivants sont renseignés :

##### `cfp.url`

Lien vers la page permettant d'afficher plus d'informations oude soumettre un sujet.

##### `cfp.dateSubmission`

Date butoir pour soumettre un sujet.

Comme toutes les dates, il s'agit d'un timestamp en millisecondes.

