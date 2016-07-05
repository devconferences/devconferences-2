var React = require('react');
var ReactBootstrap = require('react-bootstrap');

var CityLink = require('./city-link');

var CityLinkList = React.createClass({

    render: function () {
        var renderCity = function (city) {
            return (
                <CityLink key={city.id} city={city} query={this.props.query}/>
            );
        }.bind(this);
        if(this.props.cities) {
            return (
                <div className="text-center visible-xs-block">
                        { this.props.cities.map(renderCity) }
                </div>
            );
        } else {
            return null;
        }
    }

});

module.exports = CityLinkList;