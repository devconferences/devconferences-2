var React = require('react');
var ReactBootstrap = require('react-bootstrap');
var DocumentTitle = require('react-document-title');
var Router = require('react-router');

var DevConferencesClient = require('../client/client');

var Grid = ReactBootstrap.Grid;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;
var Link = Router.Link;

var FavouriteList = React.createClass({
    getInitialState: function() {
        return {
            user: null
        };
    },

    componentDidMount: function() {
        DevConferencesClient.auth.user().then(result => {
            this.updateUser(result.data);
        });
        DevConferencesClient.auth.addListener(this.updateUser);
    },

    componentWillUnmount: function() {
        DevConferencesClient.auth.removeListener(this.updateUser);
    },

    updateUser: function(user) {
        this.setState({
            user: user
        });
    },

    render: function () {
        var favouritesTags = function() {
            var tagLink = function(tag) {
                var linkToURL = "/search/" + tag;
                return (
                    <li key={tag}><Link to={linkToURL}>{tag}</Link></li>
                );
            };
            if(this.state.user.favourites.tags.length > 0) {
                return (
                    <ul>
                        {this.state.user.favourites.tags.map(tagLink)}
                    </ul>
                );
            } else {
                return (<p>Pas de recherche favorite.</p>);
            }
        }.bind(this);

        var favouritesCities = function() {
            var cityLink = function(city) {
                var linkToURL = "/city/" + city;
                return (
                    <li key={city}><Link to={linkToURL}>{city}</Link></li>
                );
            };

            if(this.state.user.favourites.cities.length > 0) {
                return (
                    <ul>
                        {this.state.user.favourites.cities.map(cityLink)}
                    </ul>
                );
            } else {
                return (<p>Pas de ville favorite.</p>);
            }
        }.bind(this);

        if(this.state.user) {
            return (
                <DocumentTitle title="Dev Conferences - Mes favoris">
                    <div className="container text-center favourite-list">
                        <div>
                            <h1>Mes favoris</h1>
                        </div>
                        <div>
                            <Grid className="no-fixed-container favourite-grid">
                                <Row>
                                    <Col md={6}>
                                        <h3>Recherche</h3>
                                        {favouritesTags()}
                                    </Col>
                                    <Col md={6}>
                                        <h3>Ville + filtre</h3>
                                        {favouritesCities()}
                                    </Col>
                                </Row>
                            </Grid>
                            <h3><Link to="/favourites/conference">Conférences ({this.state.user.favourites.conferences.length})</Link></h3>

                            <h3><Link to="/favourites/community">Communautés ({this.state.user.favourites.communities.length})</Link></h3>

                            <h3><Link to="/favourites/calendar">Événements ({this.state.user.favourites.upcomingEvents.length})</Link></h3>
                        </div>
                    </div>
                </DocumentTitle>
            );
        } else {
            return (
                <div className="container text-center">
                    Chargement...
                </div>
            );
        }
    }

});

module.exports = FavouriteList;