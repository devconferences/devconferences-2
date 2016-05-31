var React = require('react');
var Router = require('react-router');
var ReactBootstrap = require('react-bootstrap');
var $ = require('jquery');
var Ol = require('openlayers');

var CityLinkList = require('./city-link-list');
var TimelineEventList = require('./timeline-event-list');
var SearchBar = require('./search-bar');
var Minimap = require('./minimap');
var GoogleCalendar = require('./social/google-calendar');
var TwitterTimeline = require('./social/twitter-timeline');
var DevConferencesClient = require('../client/client');

var Grid = ReactBootstrap.Grid;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;

var Link = Router.Link;

var Home = React.createClass({

    getInitialState: function () {
        return {
            cities: []
        };
    },

    componentDidMount: function () {
        DevConferencesClient.cities().then(cities => this.setState({ cities: cities.data }));
    },

    searchBarUpdated: function(data) {
        console.log("event:");
        console.log(data.events);
        console.log("calendar:");
        console.log(data.calendar);
    },

    render: function () {
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
            features: this.state.cities.filter(
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
        return (
            <div className="container">
                <div className="text-center">
                    Annuaire des
                    <abbr title="Evénements se déroulant sur un ou plusieurs jours, généralement annuellement"> conférences </abbr>
                    et
                    <abbr title="Groupes d'utilisateurs se rencontrant généralement mensuellement"> communautés </abbr>
                    de développeurs en France.
                </div>

                <SearchBar onUpdate={this.searchBarUpdated} all={true}/>

                <div id="map" className="center-block"></div>

                <CityLinkList cities={this.state.cities}/>

                <Minimap cities={this.state.cities} />

                <Grid>
                    <Row>
                        <Col md={8} className="text-center">
                            <h2>Prochains événements</h2>

                            <p>Les prochains événements sont répertoriés ici.</p>

                            <div>
                                <TimelineEventList />
                            </div>
                        </Col>
                        <Col md={4} className="text-center">
                            <h2>Dernières infos</h2>

                            <p>
                                Via
                                <a href="https://twitter.com/devconferences"> @DevConferences</a>
                            </p>

                            <TwitterTimeline twitterId="devconferences" widgetId="546986135780851713" />
                        </Col>
                    </Row>
                </Grid>

            </div>
        )
    }
});

module.exports = Home;
