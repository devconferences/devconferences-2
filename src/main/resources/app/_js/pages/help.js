var React = require('react');
var DocumentTitle = require('react-document-title');

var Help = React.createClass({
    render: function() {
        return (
            <DocumentTitle title="Dev Conferences - Aide">
                <div className="container text-justify">
                    <h2 className="text-center">Bienvenue sur Dev Conferences !</h2>
                    <p>
                        Dev Conferences rassemble les conférences, ainsi que les communautés de développeurs
                        dans toute la France.
                    </p>
                    <p>
                        Vous êtes actuellement dans une ville, et vous voulez savoir ce qu'il s'y passe ?
                        Cliquez sur la carte de l'accueil !
                    </p>
                    <p>
                        Vous êtes prêts à bouger ? Effectuez une recherche sur un thème pour avoir une liste
                        complète sur toute la France !
                    </p>
                    <p>
                        Vous voulez être informé des dernières modifications sur un événement, sur ce qu'il
                        se passe dans votre ville, ou tout simplement être au courant des événements sur un
                        thème ? Connectez-vous via Github, et vous pourrez créer vos favoris ! Une
                        notification sera envoyée pour toute création ou modification pouvant
                        vous intéresser.
                    </p>
                    <p>
                        Une conférence ou une communauté manque sur le site ?<br/>
                        Un événement à venir n'est pas répertorié ?<br/>
                        Des suggestions, des bugs à nous faire parvenir ?<br/>
                        Visitez notre <a href="https://github.com/devconferences/devconferences-2">dépôt Github</a> !
                    </p>
                    <p className="text-center">
                        <i>Bonne visite !</i>
                    </p>
                </div>
            </DocumentTitle>
        );
    }
});

module.exports = Help;