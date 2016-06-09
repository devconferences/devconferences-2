var React = require('react');
var Router = require('react-router');

var Link = Router.Link;

var CityLink = React.createClass({
    render: function () {
        return (
            <Link to={`/city/${this.props.city.id}`} className='btn btn-primary btn-city'
                    title={this.props.city.totalConference + " conférence(s), " +
                           this.props.city.totalCommunity + " communauté(s), " +
                           this.props.city.totalCalendar + " événement(s)."}>
                {this.props.city.name} <span className="badge">{this.props.city.count}</span>
            </Link>
        );
    }
});

module.exports = CityLink;