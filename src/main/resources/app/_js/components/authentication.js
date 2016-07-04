var React = require('react');
var Router = require('react-router');
var $ = require('jquery');

var DevConferencesClient = require('../client/client');

var Link = Router.Link;

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
                    <li><Link to="/favourites">Mes favoris</Link></li>
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
                            <b>Notifications <span className="label label-primary">{user.messages.length}</span></b>
                        );
                    } else {
                        return (
                            <span>Notifications</span>
                        );
                    }
                }.bind(this);
                return (
                    <li><Link to="/notifications">{text()}</Link></li>
                );
            } else {
                return null;
            }
        }.bind(this);

        var help = function() {
            return (
                <li><Link to="/help">Aide</Link></li>
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
            var title = null;
            if(this.state.user &&
               this.state.user.messages.length > 0) {
                className += " unread";
                title = this.state.user.messages.length + " nouvelle(s) notification(s)";
            }
            return (
                <Link to="/notifications" className={className}
                        title={title}></Link>
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
            </div>
        );
    }
});

module.exports = Authentication;