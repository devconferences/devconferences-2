var React = require('react');
var ReactBootstrap = require('react-bootstrap');

var Glyphicon = ReactBootstrap.Glyphicon;

var WebsiteLink = React.createClass({

    render: function () {
        var url = this.props.url;
        return (
            <Glyphicon glyph='home'>
                <a href={url}> {url}</a>
            </Glyphicon>
        )
    }
});

module.exports = WebsiteLink;