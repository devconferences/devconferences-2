var React = require('react');
var Router = require('react-router');
var ReactBootstrap = require('react-bootstrap');

var Event = require('./event');
var DevConferencesClient = require('../client/client');
var NotFound = require('./not-found');

var EventPage = React.createClass({
    mixins: [Router.Navigation],

    getInitialState: function() {
        return {
            event: null,
            error: null
        }
    },

    componentDidMount: function() {
        DevConferencesClient.getEvent(this.props.params.id).then((result => {
            this.setState({
                event: result.data
            });
        }), (error => {
            this.setState({
                error: error
            });
        }));
    },

    render: function () {
        if(this.state.event) {
            return (
                <div className="container">
                    <Event event={this.state.event} />
                </div>
            );
        } else if (this.state.error) {
            return (<NotFound />);
        } else {
            return (
                <div className="container text-center">
                    Chargement...
                </div>
            );
        }
    }

});

module.exports = EventPage;