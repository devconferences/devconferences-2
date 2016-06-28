var React = require('react');

var Help = React.createClass({
    render: function() {
        return (
            <div className="modal fade" id="helpModal" role="dialog">
                <div className="modal-dialog" role="document">
                    <div className="modal-content">
                        <div className="modal-body text-center">
                            <button type="button" className="close" data-dismiss="modal" aria-label="Close">
                              <span aria-hidden="true">&times;</span>
                            </button>
                            <h3>Bienvenue sur Dev Conferences !</h3>
                            <p>
                                Dev Conferences rassemble les conférences, ainsi que les communautés de développeurs
                                dans toute la France.
                            </p>
                            <p>
                                Vous êtes actuellement dans une ville, et vous voulez savoir ce qu'il s'y passe ?
                                Cliquez sur la carte !
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
                                Une conférence ou une communauté manque sur le site ?<br/>Un événement à venir n'est
                                pas répertorié ?<br/>Des suggestions, des bugs à nous faire parvenir ?<br/>Visitez
                                notre <a href="//github.com/devconferences/devconferences-2">dépôt Github</a> !
                            </p>
                            <p>
                                Bonne visite !
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = Help;