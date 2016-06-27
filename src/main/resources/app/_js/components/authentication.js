var React = require('react');
var $ = require('jquery');

var FavouriteList = require('./favourite-list');
var NotificationList = require('./notification-list');
var DevConferencesClient = require('../client/client');

var Authentication = React.createClass({
    getInitialState: function () {
        return {
            user : undefined,
            clientId: undefined
        };
    },

    componentDidMount: function () {
        DevConferencesClient.auth.clientId().then(result => {
            this.setState({clientId:result.data});
        });

        DevConferencesClient.auth.user().then(result => {
            this.updateUser(result.data);
        });
        DevConferencesClient.auth.addListener(this.updateUser);
    },

    updateUser: function(user) {
        this.setState({user: user});
    },

    render: function () {
        if(!this.state.clientId){
            return <div></div>;
        }
        //// TODO : il y a probablement mieux à faire...
        var user = this.state.user;
        var href = user ? "/auth/disconnect" : "https://github.com/login/oauth/authorize?client_id="+this.state.clientId;
        var imageUrl = user ? user.avatarURL : "/img/github.png";
        var title = user ? user.login : "Connectez-vous avez Github";

        var connect = function() {
            if(user) {
                return null;
            } else {
                return (
                    <li><a href={href}>Connexion</a></li>
                );
            }
        };

        var disconnect = function() {
            if(user) {
                return (
                    <li><a href={href}>Déconnexion</a></li>
                );
            } else {
                return null;
            }
        };

        var favourites = function() {
            if(user) {
                return (
                    <li><a href="" data-toggle="modal" data-target="#favouriteModal">Mes favoris</a></li>
                );
            } else {
                return null;
            }
        }.bind(this);

        var notifications = function() {
            if(user) {
                var text = function() {
                    if(user.messages.length > 0) {
                        return (
                            <b>Notifications ({user.messages.length})</b>
                        );
                    } else {
                        return (
                            <span>Notifications</span>
                        );
                    }
                }.bind(this);
                return (
                    <li><a href="" data-toggle="modal" data-target="#notificationsModal">{text()}</a></li>
                );
            } else {
                return null;
            }
        }.bind(this);

        var help = function() {
            return (
                <li><a href="" data-toggle="modal" data-target="#helpModal">Aide</a></li>
            );
        };

        var separe = function() {
            if(user) {
                return (
                    <li role="separator" className="divider"></li>
                );
            } else {
            }
        }.bind(this);

        return (
            <div className="authentication dropdown">
                <a href="" className="dropdown-toggle" data-toggle="dropdown">
                    <img src={imageUrl}
                           title={title}
                           alt={title}
                           className="img-circle"
                           />
                </a>
                <ul className="dropdown-menu dropdown-menu-right">
                    {favourites()}
                    {notifications()}
                    {separe()}
                    {connect()}
                    {help()}
                    {disconnect()}
                </ul>
                <div className="modal fade" id="favouriteModal" role="dialog">
                    <div className="modal-dialog" role="document">
                        <div className="modal-content">
                            <div className="modal-body text-center">
                                <button type="button" className="close" data-dismiss="modal" aria-label="Close">
                                  <span aria-hidden="true">&times;</span>
                                </button>
                                <FavouriteList favourites={(this.state.user ? this.state.user.favourites : null)} />
                            </div>
                        </div>
                    </div>
                </div>
                <div className="modal fade" id="notificationsModal" role="dialog">
                    <div className="modal-dialog" role="document">
                        <div className="modal-content">
                            <div className="modal-header text-center">
                                <button type="button" className="close" data-dismiss="modal" aria-label="Close">
                                  <span aria-hidden="true">&times;</span>
                                </button>
                                <h2>Mes notifications</h2>
                            </div>
                            <div className="modal-body">
                                <NotificationList messages={(this.state.user ? this.state.user.messages : [])} />
                            </div>
                        </div>
                    </div>
                </div>
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
            </div>
        );
    }
});

module.exports = Authentication;