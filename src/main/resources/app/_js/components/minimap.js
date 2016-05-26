var React = require('react');

var Minimap = React.createClass({

    render: function() {
        var linkToCity = function(city) {
            /*
             * Equirectangular projection :
             * North : 51.5500
             * South : 40.9982
             * East : 9.9675
             * West : -5.8125
             *
             * With pic of 2,000px * 1,922px (the original) :
             * cx = (51.55 - ${lon}) / 0.005490
             * cy = (5.8125 + ${lat}) / 0.007890
             */
            if(city.location) {
                var zoom = 600 / 2000;
                var cy = parseInt((51.55 - city.location.lat) / (0.005490 / zoom));
                var cx = parseInt((5.8125 + city.location.lon) / (0.007890 / zoom));
                return (
                    <a key={city.id} xlinkHref={"city/" + city.name} title={city.name + "(" + city.count + ")"}>
                        <ellipse fill="#337AB7" cx={cx + ""} cy={cy + ""} rx="5" ry="5"/>
                    </a>
                );
            } else {
                return null;
            }
        }
        return (
            <div className="text-center">
                <svg width="600" height="577">
                    <image xlinkHref="/img/france_map.svg" width="600" height="577"/>
                    {this.props.cities.map(linkToCity)}
                </svg>
            </div>
        );
    }
});

module.exports = Minimap;