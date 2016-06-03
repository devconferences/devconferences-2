var React = require('react');
var Router = require('react-router');
var ReactBootstrap = require('react-bootstrap');
var $ = require('jquery');

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
        DevConferencesClient.cities(null, true).then(cities => this.setState({ cities: cities.data }));
    },

    searchBarUpdated: function(data) {
        console.log("all:");
        console.log(data);
    },

    render: function () {

        return (
            <div className="container text-center">
                <div title="Cliquez pour affichez plus d'informations"  id="informations" className="panel panel-default"  data-toggle="collapse" data-target="#informationsCollapse">
                    <div>
                       <h3>
                            Bienvenue sur <em>Dev Conferences</em> !
                       </h3>
                    </div>
                    <div id="informationsCollapse" className="collapse">
                        <p>
                            Ce site rassemble les conférences, ainsi que les communautés de développeurs dans toute la France.
                        </p>
                        <p>
                            Vous êtes actuellement dans une ville, et vous voulez savoir ce qu'il s'y passe ? Cliquez sur la carte !
                        </p>
                        <p>
                            Vous êtes prêts à bouger ? Effectuez une recherche sur un thème pour avoir une liste complète sur toute la France !
                        </p>
                        <p>
                            Une conférence ou une communauté manque sur le site ?<br/>Un événement à venir n'est pas répertorié ?<br/>Des suggestions, des bugs à nous faire parvenir ?<br/>Visitez notre <a href="//github.com/devconferences/devconferences-2">dépôt Github</a> !
                        </p>
                        <p>
                            Bonne visite !
                        </p>
                    </div>
                </div>

                <SearchBar onUpdate={this.searchBarUpdated} all={true}/>

                <CityLinkList cities={this.state.cities}/>

                <Grid>
                    <Row>
                        <Col lg={7} className="text-center">
                            <h2>Prochains événements</h2>

                            <p>Les prochains événements sont répertoriés ici.</p>

                            <div>
                                <TimelineEventList />
                            </div>
                        </Col>
                        <Col lg={5} className="text-center">
                            <Minimap cities={this.state.cities} />

                            <hr/>

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
