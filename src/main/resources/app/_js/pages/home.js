var React = require('react');
var DocumentTitle = require('react-document-title');
var Router = require('react-router');
var ReactBootstrap = require('react-bootstrap');
var $ = require('jquery');

var CityLinkList = require('../components/city-link-list');
var TimelineEventList = require('../components/timeline-event-list');
var SearchBar = require('../components/search-bar');
var Minimap = require('../components/minimap');
var GoogleCalendar = require('../components/social/google-calendar');
var TwitterTimeline = require('../components/social/twitter-timeline');
var DevConferencesClient = require('../client/client');

var Grid = ReactBootstrap.Grid;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;

var Link = Router.Link;

var Home = React.createClass({

    getInitialState: function () {
        return {
            cities: [],
            calendar: {
                query: null,
                hits: null
            },
            user: null
        };
    },

    componentDidMount: function() {
        DevConferencesClient.auth.user().then(result => {
            if(result != undefined) {
                this.updateUser(result.data);
            }
        });
        DevConferencesClient.auth.addListener(this.updateUser);
    },

    componentWillUnmount: function() {
        DevConferencesClient.auth.removeListener(this.updateUser);
    },

    updateUser: function(user) {
        this.setState({user: user});
    },

    searchBarUpdated: function(data) {
        this.setState({
            cities: data.cities,
            calendar: data.calendar
        });
    },

    moreUpcomingEvents: function(limit) {
        DevConferencesClient.searchCalendar(this.state.calendar.query, 1, limit).then( result => {
            this.setState({
                calendar: result.data
            });
        });
    },

    render: function () {
        return (
            <DocumentTitle title="Dev Conferences - Accueil">
                <div className="container text-center">
                    <SearchBar ref="searchBar" favourites={(this.state.user ? this.state.user.favourites : null)} onUpdate={this.searchBarUpdated} all={true} limit={10} searchType={new SearchBar().HOME} allDataWhenEmpty={true}/>

                    <Grid className="no-fixed-container">
                        <Row>
                            <Col lg={7} className="text-center">
                                <h2>Prochains événements</h2>

                                <p>Les prochains événements sont répertoriés ici.</p>

                                <TimelineEventList calendar={this.state.calendar} moreUpcomingEvents={this.moreUpcomingEvents} favourites={(this.state.user ? this.state.user.favourites : null)}/>
                            </Col>
                            <Col lg={5} className="text-center">
                                <h2>Villes répertoriées</h2>

                                <Minimap cities={this.state.cities} query={this.state.calendar.query}/>
                                <CityLinkList cities={this.state.cities} query={this.state.calendar.query}/>

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
            </DocumentTitle>
        )
    }
});

module.exports = Home;
