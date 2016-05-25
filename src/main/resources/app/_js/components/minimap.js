var React = require('react');

var Minimap = React.createClass({

    render: function() {
        var linkToCity = function(city) {
            console.log(city);
            return (
                <a xlinkHref="city/Nantes" title="This is a link !">
                    <rect x="100" y="100" width="10" height="10"/>
                </a>
            )
        }
        return (
            <div className="text-center">
                <svg width="600" height="600">
                    <image xlinkHref="/img/france_map.svg" width="600px" height="600px"/>
                    {this.props.cities.map(linkToCity)}
                </svg>
            </div>
        );
    }
});

module.exports = Minimap;