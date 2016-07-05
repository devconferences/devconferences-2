var React = require('react');
var DocumentTitle = require('react-document-title');
var Router = require('react-router');
var ReactBootstrap = require('react-bootstrap');

var NotFound = require('./not-found');

var TimelineEvent = require('../components/timeline-event');
var DevConferencesClient = require('../client/client');

var Glyphicon = ReactBootstrap.Glyphicon;

var CalendarPage = React.createClass({
    mixins: [Router.Navigation],

    getInitialState: function() {
        return {
            event: null,
            error: null,
            user: null
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
        if(this.state.event) {
            return (
                <DocumentTitle title={"Dev Conferences - " + this.state.event.name}>
                    <div className="container">
                        <TimelineEvent event={this.state.event} favourites={(this.state.user ? this.state.user.favourites : null)}/>
                    </div>
                </DocumentTitle>
            );
        } else if (this.state.error) {
            return (<NotFound />);
        } else {
            return (
                <p className="container text-center">
                    <Glyphicon glyph="refresh" className="refresh-animate"></Glyphicon>
                </p>
            );
        }
    }

});

module.exports = CalendarPage;