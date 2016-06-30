var React = require('react');
var ReactBootstrap = require('react-bootstrap');
var Router = require('react-router');

var Grid = ReactBootstrap.Grid;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;
var Link = Router.Link;

var FavouriteList = React.createClass({
    render: function () {
        var favouritesTags = function() {
            var tagLink = function(tag) {
                var linkToURL = "/search/" + tag;
                return (
                    <li key={tag}><Link to={linkToURL}>{tag}</Link></li>
                );
            };
            if(this.props.favourites.tags.length > 0) {
                return (
                    <ul>
                        {this.props.favourites.tags.map(tagLink)}
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

            if(this.props.favourites.cities.length > 0) {
                return (
                    <ul>
                        {this.props.favourites.cities.map(tagsLink)}
                    </ul>
                );
            } else {
                return (<p>Pas de ville favorite.</p>);
            }
        }.bind(this);

        if(this.props.favourites) {
            return (
                <div className="favourite-list">
                    <div>
                        <h2>Mes favoris</h2>
                    </div>
                    <div>
                        <Grid className="favourite-grid">
                            <Row>
                                <Col md={4}>
                                    <h3>Recherche</h3>
                                    {favouritesTags()}
                                </Col>
                                <Col md={4}>
                                    <h3>Ville + filtre</h3>

                                    {favouritesCities()}
                                </Col>
                                <Col md={4}>
                                    <h3><Link to="/favourites/conference">Conférences ({this.props.favourites.conferences.length})</Link></h3>

                                    <h3><Link to="/favourites/community">Communautés ({this.props.favourites.communities.length})</Link></h3>

                                    <h3><Link to="/favourites/calendar">Événements ({this.props.favourites.upcomingEvents.length})</Link></h3>
                                </Col>
                            </Row>
                        </Grid>
                    </div>
                </div>
            );
        } else {
            return null;
        }
    }

});

module.exports = FavouriteList;