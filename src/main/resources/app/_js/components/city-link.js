var React = require('react');
var Router = require('react-router');

var Link = Router.Link;

var CityLink = React.createClass({
    render: function () {
        return (
            <Link to="city" params={{id: this.props.city.id}} className='btn btn-primary btn-block btn-city'>
                {this.props.city.name}-{this.props.city.count}
            </Link>
        );
    }
});

module.exports = CityLink;