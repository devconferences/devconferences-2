import {Router, browserHistory} from 'react-router';

var React = require('react');
var ReactDOM = require('react-dom');
var ol = require('openlayers');
var $ = require('jquery');

var CityLink = require('./city-link');

var Minimap = React.createClass({
    contextTypes: {
        router: React.PropTypes.object.isRequired
    },

    getInitialState: function() {
        return {
            minimapText: "Choisissez une ville sur la carte.",
            linkHovered: false
        }
    },

    componentDidMount: function() {
        this.clearMapNode();
    },

    componentDidUpdate: function() {
        this.clearMapNode();
    },

    clearMapNode: function() {
        // Because OSM APPEND maps instead of replace, we need to remove #map children before render a new map
        var mapNode = ReactDOM.findDOMNode(this.refs.map);
        while (mapNode.lastChild) {
            mapNode.removeChild(mapNode.lastChild);
        }

        // Begin OSM Map Config
        var cityMarker = function(city) {
            var iconFeature = new ol.Feature({
                geometry: new ol.geom.Point(ol.proj.fromLonLat([city.location.lon, city.location.lat])),
                name: city.name,
                count: city.count
            });

            iconFeature.setStyle(new ol.style.Style({
                image: new ol.style.Circle({
                    radius: 6,
                    fill: new ol.style.Fill({
                        color: [50, 128, 192]
                    })
                }),
                text: new ol.style.Text({
                    text: (city.name),
                    font: "bold 10px \"Open Sans\", sans-serif",
                    offsetY: -12
                })
            }));

            return iconFeature;
        }

        var vectorSource = new ol.source.Vector({
            features: this.props.cities.filter(
                function(city) {return city.location != null}
            ).map(cityMarker)
        });

        var vectorLayer = new ol.layer.Vector({
            source: vectorSource
        });
        var map = new ol.Map({
            target: "map",
            layers: [
                new ol.layer.Tile({source: new ol.source.OSM()}),
                vectorLayer
            ],
            controls: ol.control.defaults({
                attributionOptions: ({
                    collapsible: false
                })
            }),
            view: new ol.View({
                center: ol.proj.fromLonLat([2.367, 46.500]),
                zoom: 5.3
            })
        });

        // handle popup behaviour
        var citySelected = document.getElementById('citySelected');
        var cityCount = document.getElementById('cityCount');

        // display popup on click
        map.on('click', function(evt) {
            var feature = map.forEachFeatureAtPixel(evt.pixel,
            function(feature) {
                  return feature;
            });
            if (feature) {
                this.context.router.push("/city/" + feature.get('name'));
            }
        }.bind(this));

        // change mouse cursor when over marker
        map.on('pointermove', function(e) {
            if (e.dragging) {
                return;
            }
            var pixel = map.getEventPixel(e.originalEvent);
            var hit = map.hasFeatureAtPixel(pixel);
            map.getTargetElement().style.cursor = hit ? 'pointer' : '';

            if(hit) {
                map.forEachFeatureAtPixel(pixel,
                function(feature) {
                      citySelected.innerHTML = feature.get('name');
                      cityCount.innerHTML = feature.get('count');
                });
            } else {
                  citySelected.innerHTML = "Choisissez une ville sur la carte.";
                  cityCount.innerHTML = null;
            }
        });
        // End OSM Map
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

        var linkNotLocatedCity = function(city) {
            if(city.location != null) {
                return null;
            } else {
                return (
                    <CityLink key={city.id} city={city}/>
                );
            }
        };
        return (
            <div className="minimap text-center hidden-xs">
                <h2>
                    Villes répertoriées
                </h2>
                <p className="city">
                    <span className="label btn-primary label-primary"><span id="citySelected">{this.state.minimapText}</span> <span id="cityCount" className="badge"></span></span>
                </p>

                <div className="wrapper-map">
                    <div ref="map" id="map" className="center-block"></div>
                </div>

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