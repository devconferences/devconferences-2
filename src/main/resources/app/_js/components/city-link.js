var React = require('react');
var ReactBootstrap = require('react-bootstrap');

var Button = ReactBootstrap.Button;

var CityLink = React.createClass({
    render: function () {
        return (
            <Button bsStyle='primary' className='btn-block btn-city' href={'#/city/' + this.props.city.id}>
                {this.props.city.name}
            </Button>
        )
    }
});

module.exports = CityLink;