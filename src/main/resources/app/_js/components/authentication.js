var React = require('react');
var $ = require('jquery');

var Authentication = React.createClass({
    getInitialState: function () {
        return {
            user : undefined,
            clientId: undefined
        };
    },
    componentDidMount: function () {
        var clientIdURL = '/auth/client-id';
        $.ajax({
            url: clientIdURL,
            dataType: 'text',
            cache: true,
            success: function (data) {
                this.setState({clientId:data});
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(clientIdURL, status, err.toString());
            }.bind(this)
        });

        var connectedUserURL = '/auth/connected-user';
        $.ajax({
            url: connectedUserURL,
            dataType: 'json',
            cache: false,
            success: function (data) {
                console.log(data);
                this.setState({user: data});
            }.bind(this),
            error: function (xhr, status, err) {
                if(xhr.status === 404){
                    this.setState({user : undefined});
                }else{
                    console.error(connectedUserURL, status, err.toString());
                }
            }.bind(this)
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