var React = require('react');
var ReactBootstrap = require('react-bootstrap');

var Button = ReactBootstrap.Button;

var ConferenceAnchor = React.createClass({
    render: function () {
        // FIXME anchors do not work because of ReactRouter
        return (
            <Button bsStyle='primary' href={'#' + this.props.conference.id}>
                {this.props.conference.name}
            </Button>
        )
    }
});

module.exports = ConferenceAnchor;