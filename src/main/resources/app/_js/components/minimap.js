var React = require('react');
var ReactDOM = require('react-dom');
var Ol = require('openlayers');

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
        var cityMarker = function(city) {
            var iconFeature = new Ol.Feature({
                geometry: new Ol.geom.Point(Ol.proj.fromLonLat([city.location.lon, city.location.lat])),
                name: city.name,
                population: 4000,
                rainfall: 500
            });

            return iconFeature;
        }

        var vectorSource = new Ol.source.Vector({
            features: this.props.cities.filter(
                function(city) {return city.location != null}
            ).map(cityMarker)
        });

        var vectorLayer = new Ol.layer.Vector({
            source: vectorSource
        });
        var map = new Ol.Map({
            target: "map",
            layers: [
                new Ol.layer.Tile({source: new Ol.source.OSM()}),
                vectorLayer
            ],
            view: new Ol.View({
                center: Ol.proj.fromLonLat([2.367, 46.500]),
                zoom: 5.8
            })
        });
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
            <div className="minimap text-center hidden-xs">

                <div id="map" className="center-block"></div>

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