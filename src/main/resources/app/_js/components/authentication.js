var React = require('react');
var $ = require('jquery');

var FavouriteList = require('./favourite-list');
var Help = require('./help');
var Notifications = require('./notifications');
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
            if(result != undefined) {
                this.updateUser(result.data);
            }
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

        var notificationStatus = function() {
            var className = "notification-status";
            if(this.state.user &&
               this.state.user.messages.length > 0) {
                className += " unread";
            }
            return (
                <span data-toggle="modal" data-target="#notificationsModal" className={className}></span>
            );
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
                {notificationStatus()}
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
                <Notifications messages={(this.state.user ? this.state.user.messages : [])}/>
                <Help />
            </div>
        );
    }
});

module.exports = Authentication;