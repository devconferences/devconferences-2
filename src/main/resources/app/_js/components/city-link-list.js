var React = require('react');
var ReactBootstrap = require('react-bootstrap');

var CityLink = require('./city-link');

var CityLinkList = React.createClass({

    render: function () {
        var renderCity = function (city) {
            return (
                <CityLink key={city.id} city={city} />
            );
        };
        return (
            <div className="text-center">
                    { this.props.cities.map(renderCity) }
            </div>
        );
    }

});

module.exports = CityLinkList;