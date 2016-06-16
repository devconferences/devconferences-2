var React = require('react');
var Router = require('react-router');
var ReactBootstrap = require('react-bootstrap');
var $ = require('jquery');

var CityLinkList = require('./city-link-list');
var TimelineEventList = require('./timeline-event-list');
var SearchBar = require('./search-bar');
var Minimap = require('./minimap');
var FavouriteList = require('./favourite-list');
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
            cities: [],
            calendar: {
                query: null,
                hits: []
            },
            user: null
        };
    },

    componentDidMount: function() {
        DevConferencesClient.auth.user().then(result => {
            this.updateUser(result.data);
        });
        DevConferencesClient.auth.addListener(this.updateUser);
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
            <div className="container text-center">
                <SearchBar ref="searchBar" favourites={(this.state.user ? this.state.user.favourites : null)} onUpdate={this.searchBarUpdated} all={true} limit={10} searchType={new SearchBar().ALL} allDataWhenEmpty={true}/>

                <CityLinkList cities={this.state.cities} query={this.state.calendar.query}/>

                <Grid className="no-fixed-container">
                    <Row>
                        <Col lg={7} className="text-center">
                            <h2>Prochains événements</h2>

                            <p>Les prochains événements sont répertoriés ici.</p>

                            <TimelineEventList calendar={this.state.calendar} moreUpcomingEvents={this.moreUpcomingEvents} favourites={(this.state.user ? this.state.user.favourites : null)}/>
                        </Col>
                        <Col lg={5} className="text-center">
                            <Minimap cities={this.state.cities} query={this.state.calendar.query}/>

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
