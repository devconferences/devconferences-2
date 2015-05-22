var React = require('react');
var ReactBootstrap = require('react-bootstrap');

var Button = ReactBootstrap.Button;

var City = React.createClass({
    render: function () {
        return (
            <Button bsStyle='primary' className='btn-block btn-city' href={'/v1/' + this.props.name}>{this.props.name}</Button>
        )
    }
});

module.exports = City;