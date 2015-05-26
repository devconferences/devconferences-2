var React = require('react');
var ReactBootstrap = require('react-bootstrap');

var Button = ReactBootstrap.Button;

var EventAnchor = React.createClass({
    render: function () {
        // FIXME anchors do not work because of ReactRouter
        return (
            <Button bsStyle='primary' href={'#' + this.props.event.id}>
                {this.props.event.name}
            </Button>
        )
    }
});

module.exports = EventAnchor;