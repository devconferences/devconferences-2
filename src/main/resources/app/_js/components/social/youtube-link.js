var React = require('react');
var ReactBootstrap = require('react-bootstrap');

var Glyphicon = ReactBootstrap.Glyphicon;

var YoutubeLink = React.createClass({

    render: function () {
        var url = "https://www.youtube.com/channel/" + this.props.youtube.channel;
        return (

            <i className="fa fa-youtube-play">
                <a href={url}> {this.props.youtube.name}</a>
            </i>
        )
    }
});

module.exports = YoutubeLink;