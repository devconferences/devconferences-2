var React = require('react');
var DocumentTitle = require('react-document-title');
var Router = require('react-router');
var ReactBootstrap = require('react-bootstrap');

var NotFound = require('./not-found');

var Event = require('../components/event');
var DevConferencesClient = require('../client/client');

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
                <DocumentTitle title={"Dev Conferences - " + this.state.event.name}>
                    <div className="container">
                        <Event event={this.state.event} />
                    </div>
                </DocumentTitle>
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