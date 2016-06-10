var React = require('react');
var moment = require('moment');

var TimelineEvent = require('./timeline-event');
var DevConferencesClient = require('../client/client');

var TimelineEventList = React.createClass({
    getInitialState: function() {
        return {
            eventsList: [],
            page: 0
        };
    },
    render: function() {
        var calendarEvent = function(event) {
            return (
                <TimelineEvent key={event.id} event={event} />
            );
        };
        return (
            <div id="timeline-event-list" className="text-left separe scrollable">
                <div>
                    {this.props.calendar.map(calendarEvent)}
                </div>
            </div>
        );
    }
});

module.exports = TimelineEventList;