var React = require('react');
var Router = require('react-router');
var ReactBootstrap = require('react-bootstrap');
var $ = require('jquery');

var CityLinkList = require('./city-link-list');
var TimelineEventList = require('./timeline-event-list');
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

    render: function () {
        return (
            <div className="container">
                <div className="text-center">
                    Annuaire des
                    <abbr title="Evénements se déroulant sur un ou plusieurs jours, généralement annuellement"> conférences </abbr>
                    et
                    <abbr title="Groupes d'utilisateurs se rencontrant généralement mensuellement"> communautés </abbr>
                    de développeurs en France.
                </div>

                <Link to="search" className='btn btn-primary btn-block btn-city'>
                    Recherche
                </Link>

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
