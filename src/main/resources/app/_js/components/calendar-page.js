var React = require('react');
var Router = require('react-router');
var ReactBootstrap = require('react-bootstrap');

var TimelineEvent = require('./timeline-event');
var DevConferencesClient = require('../client/client');
var NotFound = require('./not-found');

var CalendarPage = React.createClass({
    mixins: [Router.Navigation],

    getInitialState: function() {
        return {
            event: null,
            error: null
        }
    },

    componentDidMount: function() {
        DevConferencesClient.getUpcomingEvent(this.props.params.id).then((result => {
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
                    <TimelineEvent event={this.state.event} />
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

module.exports = CalendarPage;