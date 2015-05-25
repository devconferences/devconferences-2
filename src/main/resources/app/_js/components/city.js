var React = require('react');
var Router = require('react-router');
var $ = require('jquery');

var ConferenceAnchorList = require('./conference-anchor-list');
var ConferenceList = require('./conference-list');

var City = React.createClass({

    mixins: [Router.Navigation],

    getInitialState: function () {
        return {
            city: null
        };
    },

    componentDidMount: function () {
        var url = '/api/v1/city/' + this.props.params.id;
        $.ajax({
            url: url,
            dataType: 'json',
            cache: false,
            success: function (data) {
                this.setState({city: data});
            }.bind(this),
            error: function (xhr, status, err) {
                // Redirect to homepage
                console.error(url, status, err.toString());
                this.transitionTo('/');
            }.bind(this)
        });
    },

    render: function () {
        if (this.state.city) {
            return (
                <div className="container">
                    <div className="text-center">
                        <h1>Dev Conferences @ {this.state.city.name}</h1>
                    </div>

                    <ConferenceAnchorList conferences={this.state.city.conferences} />

                    <hr />

                    <ConferenceList conferences={this.state.city.conferences} />
                </div>
            )
        }
        else {
            // TODO loading icon ?
            return (
                <div className="container">
                    Chargement ...
                </div>
            )
        }
    }
});

module.exports = City;