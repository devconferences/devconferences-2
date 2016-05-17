var React = require('react');
var moment = require('moment');

var TimelineEvent = require('./timeline-event');
var DevConferencesClient = require('../client/client');

var TimelineEventList = React.createClass({
    getInitialState: function() {
        return {
            eventsList: []
        };
    },
    componentDidMount: function() {
        DevConferencesClient.calendar().then(calendar => this.setState({ eventsList: calendar.data }));
    },
    render: function() {
        var calendarEvent = function(event) {
            return (
                <TimelineEvent event={event} />
            );
        };
        return (
            <div className="text-left container scrollable">
                <div>
                    {this.state.eventsList.map(calendarEvent)}
                </div>
            </div>
        );
    }
});

module.exports = TimelineEventList;