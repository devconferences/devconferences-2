var React = require('react');
var ReactBootstrap = require('react-bootstrap');

var Glyphicon = ReactBootstrap.Glyphicon;

var ParleysLink = React.createClass({

    render: function () {
        var url = "https://www.parleys.com/channel/" + this.props.channel;
        return (

            <i className="fa fa-video-camera">
                <a href={url}> {url}</a>
            </i>
        )
    }
});

module.exports = ParleysLink;