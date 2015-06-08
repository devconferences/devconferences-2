var React = require('react');
var $ = require('jquery');

var Authentication = React.createClass({
    getInitialState: function () {
        return {
            clientId: ""
        };
    },
    componentDidMount: function () {
        var url = '/auth/client-id';
        $.ajax({
            url: url,
            dataType: 'text',
            cache: false,
            success: function (data) {
                console.log(data);
                this.setState({clientId: data});
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(url, status, err.toString());
            }.bind(this)
        });
    },
    render: function () {
        // TODO : il y a probablement mieux à faire...
        var user = document.cookie.split(";")
            .map(function (entry) {return entry.trim().split("=")})
            .filter(function (entry) {return entry[0] === "user"})
            .map(function (entry) {return JSON.parse(entry[1])})
            .reduce(function (a, b) {return a === undefined ? b : a;}, undefined);


        var href = user ? "/auth/disconnect" : "https://github.com/login/oauth/authorize?client_id="+this.state.clientId;
        var imageUrl = user ? user.avatarURL : "https://www.clever-cloud.com/assets/img/github-icon.svg";
        var title = user ? user.login : "Connectez-vous avez Github";

        return (
            <div className="authentication">
                <a href={href}>
                    <image src={imageUrl}
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