var React = require('react');
var Router = require('react-router');

var Link = Router.Link;

var CityLink = React.createClass({
    render: function () {
        return (
            <Link to="city" params={{id: this.props.city.id}} className='btn btn-primary btn-city'>
                {this.props.city.name} <span className="badge">{this.props.city.count}</span>
            </Link>
        );
    }
});

module.exports = CityLink;