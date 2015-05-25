var React = require('react');
var ReactBootstrap = require('react-bootstrap');

var CityLink = require('./city-link');

var Grid = ReactBootstrap.Grid;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;

var CityLinkList = React.createClass({

    render: function () {
        var renderCity = function (city) {
            return (
                <Col key={city.id} md={2}>
                    <CityLink city={city} />
                </Col>
            );
        };
        return (
            <Grid>
                <Row>
                    { this.props.cities.map(renderCity) }
                </Row>
            </Grid>
        );
    }

});

module.exports = CityLinkList;