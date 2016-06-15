var React = require('react');
var ReactBootstrap = require('react-bootstrap');

var Grid = ReactBootstrap.Grid;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;

var FavouriteList = React.createClass({
    render: function () {
        var tagsLink = function(tag) {
            return (
                <li key={tag}>{tag}</li>
            );
        }
        if(this.props.favourites) {
            return (
                <div className="favourite-list" data-toggle="collapse" data-target="#favouriteCollapse">
                    <div>
                        <h2>Mes favoris</h2>
                    </div>
                    <div id="favouriteCollapse" className="collapse">
                        <Grid className="panel panel-default favourite-grid">
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
                                        {this.props.favourites.cities.map(tagsLink)}
                                    </ul>
                                </Col>
                                <Col md={4}>
                                    <h3>Conférences</h3>
                                    {/*this.props.favourites.conferences*/}

                                    <h3>Communautés</h3>
                                    {/*this.props.favourites.communities*/}

                                    <h3>Événements</h3>
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