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
                totalCommunity : city.totalCommunity,
                totalConference : city.totalConference,
                totalCalendar : city.totalCalendar
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

        var container = this.refs.mapPopup;
        var content = this.refs.mapPopupContent;

        var overlay = new ol.Overlay({
            element: container
        });

        var map = new ol.Map({
            target: "map",
            layers: [
                new ol.layer.Tile({source: new ol.source.OSM()}),
                vectorLayer
            ],
            overlays: [overlay],
            controls: ol.control.defaults({
                attributionOptions: ({
                    collapsible: false
                }),
                rotate: false,
                zoomOptions: ({
                    zoomInTipLabel: "Zoom +",
                    zoomOutTipLabel: "Zoom -"
                })
            }).extend([
                new ol.control.ScaleLine(),
                new ol.control.ZoomToExtent({
                    extent: [-607062, 5051361, 1099164, 6668810],
                    tipLabel: "Centrer sur la France",
                    label: "\uf0b2"
                })
            ]),
            view: new ol.View({
                center: ol.proj.fromLonLat([2.367, 46.500]),
                zoom: 5.3
            })
        });

        // handle popup behaviour
        var cityName = document.getElementById('mapCityName');
        var cityConference = document.getElementById('mapCityConference');
        var cityCommunity = document.getElementById('mapCityCommunity');
        var cityCalendar = document.getElementById('mapCityCalendar');

        // display popup on click
        map.on('click', function(evt) {
            var feature = map.forEachFeatureAtPixel(evt.pixel,
            function(feature) {
                  return feature;
            });
            if (feature) {
                this.context.router.push("/city/" + feature.get('name') + (this.props.query ? "/" + this.props.query : ""));
            }
        }.bind(this));

        // change mouse cursor when over marker
        map.on('pointermove', function(e) {
            if (e.dragging) {
                return;
            }
            var coordinate = e.coordinate;
            var pixel = map.getEventPixel(e.originalEvent);
            var hit = map.hasFeatureAtPixel(pixel);
            map.getTargetElement().style.cursor = hit ? 'pointer' : '';

            if(hit) {
                container.style.display = 'block';
                map.forEachFeatureAtPixel(pixel,
                function(feature) {
                    cityName.innerHTML = feature.get('name');
                    cityConference.innerHTML = "Conférences : " + feature.get('totalConference');
                    cityCommunity.innerHTML = "Communautés : " + feature.get('totalCommunity');
                    cityCalendar.innerHTML = "Événements : " + feature.get('totalCalendar');
                    overlay.setPosition(coordinate);
                });
            } else {
                container.style.display = 'none';
            }
        }.bind(this));
        // End OSM Map
    },

    render: function() {

        var linkNotLocatedCity = function(city) {
            if(city.location != null) {
                return null;
            } else {
                return (
                    <CityLink key={city.id} city={city} query={this.props.query}/>
                );
            }
        }.bind(this);
        return (
            <div className="minimap text-center hidden-xs">
                <h2>
                    Villes répertoriées
                </h2>
                <p>
                    Choisissez une ville sur la carte.
                </p>

                <div className="wrapper-map">
                    <div ref="map" id="map" className="center-block"></div>
                    <div ref="mapPopup" id="map-popup">
                        <div ref="mapPopupContent" id="map-popup-content">
                            <h4 id="mapCityName"></h4>
                            <div id="mapCityConference"></div>
                            <div id="mapCityCommunity"></div>
                            <div id="mapCityCalendar"></div>
                        </div>
                    </div>
                </div>

                <div>
                    <p>
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