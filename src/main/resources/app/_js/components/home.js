var React = require('react');
var $ = require('jquery');

var CityLinkList = require('./city-link-list');

var Home = React.createClass({

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
        return (
            <div className="container">

                <div className="text-center">
                    Annuaire des
                    <abbr title="Evénements se déroulant sur un ou plusieurs jours, généralement annuellement"> conférences </abbr>
                    et
                    <abbr title="Groupes d'utilisateurs se rencontrant généralement mensuellement"> communautés </abbr>
                    de développeurs en France.
                </div>

                <CityLinkList cities={this.state.cities}/>

            </div>
        )
    }
});

module.exports = Home;
