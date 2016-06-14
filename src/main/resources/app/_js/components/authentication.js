var React = require('react');
var $ = require('jquery');

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
            this.setState({user:result.data});
        });
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

        return (
            <div className="authentication">
                <a href={href}>
                    <img src={imageUrl}
                           title={title}
                           alt={title}
                           className="img-circle"
                           height="50px" width="50px"/>
                </a>
            </div>
        );
    }
});

module.exports = Authentication;