var React = require('react');
var ReactDOM = require('react-dom');
var CityLink = require('./city-link');

var Minimap = React.createClass({
    getInitialState: function() {
        return {
            minimapText: "Choisissez une ville sur la carte.",
            linkHovered: false
        }
    },
    setMinimapText: function(e) {
        this.setState({
            minimapText: e.target.parentNode.attributes["title"].value || "...",
            linkHovered: true
        })
    },
    resetMinimapText: function(e) {
        this.setState({
            minimapText: "Choisissez une ville sur la carte.",
            linkHovered: false
        });
    },

    render: function() {
        var parentThis = this;
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
                    <a onMouseEnter={parentThis.setMinimapText} onMouseLeave={parentThis.resetMinimapText} key={city.id} xlinkHref={"city/" + city.name} title={city.name + " (" + city.count + ")"}>
                        <ellipse fill="#337AB7" cx={cx + ""} cy={cy + ""} rx="5" ry="5"/>
                    </a>
                );
            } else {
                return null;
            }
        };
        var minimapTextRender = function(text, isHover) {
            if(isHover) {
                return (
                    <span className="label label-primary">{text}</span>
                );
            } else {
                return (
                    <span className="label label-default">{text}</span>
                );
            }
        };
        var linkNotLocatedCity = function(city) {
            if(city.location != null) {
                return null;
            } else {
                return (
                    <CityLink key={city.id} city={city}/>
                );
            }
        }
        return (
            <div className="minimap text-center">
                <p className="city">
                    {minimapTextRender(this.state.minimapText, this.state.linkHovered)}
                </p>
                <svg width="600" height="577">
                    <image xlinkHref="/img/france_map.svg" width="600" height="577"/>
                    {this.props.cities.map(linkToCity)}
                </svg>
                <div>
                    <p className="city">
                        <span className="label label-default">Ville(s) non affichée(s) :</span>
                    </p>
                    <p>
                        {this.props.cities.map(linkNotLocatedCity)}
                    </p>
                </div>
            </div>
        );
    }
});

module.exports = Minimap;