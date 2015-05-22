var React = require('react');
var ReactBootstrap = require('react-bootstrap');
var $ = require('jquery');

var City = require('./city');

var Grid = ReactBootstrap.Grid;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;

var CityList = React.createClass({
    getInitialState: function () {
        return {
            cities: []
        };
    },

    componentDidMount: function () {
        var url = '/api/v1/city';
        $.ajax({
            url: url,
            dataType: 'json',
            cache: false,
            success: function (data) {
                this.setState({cities: data});
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(url, status, err.toString());
            }.bind(this)
        });
    },

    render: function () {
        var renderCity = function (city) {
            return (
                <Col md={2}>
                    <City name={city} />
                </Col>
            );
        };
        return (
            <Grid>
                <Row className='show-grid'>
                    { this.state.cities.map(renderCity) }
                </Row>
            </Grid>
        );
    }
});

module.exports = CityList;