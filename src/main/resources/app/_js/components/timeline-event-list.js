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
        var moreUpcomingEvents = function(totalPage, moreUpcomingEventsCall) {
            if(totalPage > 1) {
                return (
                    <div className="text-center blockLink">
                        <a onClick={moreUpcomingEventsCall} className="moreLink">Plus d'événements...</a>
                    </div>
                );
            } else {
                return null;
            }
        };
        var moreUpcomingEventsCall = function(e) {
            console.log(this.props.calendar);
            this.props.moreUpcomingEvents(parseInt(this.props.calendar.hitsAPage) + 10);
        }.bind(this);
        return (
            <div id="timeline-event-list" className="text-left separe scrollable">
                <div>
                    {this.props.calendar.hits.map(calendarEvent)}
                </div>
                {moreUpcomingEvents(this.props.calendar.totalPage, moreUpcomingEventsCall)}
            </div>
        );
    }
});

module.exports = TimelineEventList;