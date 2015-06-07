var React = require('react');

var Authentication = React.createClass({

    render: function () {
        // TODO : il y a probablement mieux à faire...
        var user = document.cookie.split(";")
            .map(function (entry) {return entry.trim().split("=")})
            .filter(function (entry) {return entry[0] === "user"})
            .map(function (entry) {return JSON.parse(entry[1])})
            .reduce(function (a, b) {return a === undefined ? b : a;}, undefined);

        var href = user ? "#" : "https://github.com/login/oauth/authorize?client_id=9a8a7843de53c0561a73";
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