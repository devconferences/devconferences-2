var React = require('react');
var ReactBootstrap = require('react-bootstrap');
var Router = require('react-router');

var Grid = ReactBootstrap.Grid;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;
var Link = Router.Link;

var FavouriteList = React.createClass({
    render: function () {
        var tagsLink = function(tag) {
            var linkToURL = "/search/" + tag;
            return (
                <li key={tag}><Link to={linkToURL}>{tag}</Link></li>
            );
        };
        var cityLink = function(city) {
            var linkToURL = "/city/" + city;
            return (
                <li key={city}><Link to={linkToURL}>{city}</Link></li>
            );
        }
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
                                    <ul>
                                        {this.props.favourites.tags.map(tagsLink)}
                                    </ul>
                                </Col>
                                <Col md={4}>
                                    <h3>Ville + filtre</h3>
                                    <ul>
                                        {this.props.favourites.cities.map(cityLink)}
                                    </ul>
                                </Col>
                                <Col md={4}>
                                    <h3><Link to="/favourites/conference">Conférences</Link></h3>
                                    {/*this.props.favourites.conferences*/}

                                    <h3><Link to="/favourites/community">Communautés</Link></h3>
                                    {/*this.props.favourites.communities*/}

                                    <h3><Link to="/favourites/calendar">Événements</Link></h3>
                                    {/*this.props.favourites.upcomingEvents*/}
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