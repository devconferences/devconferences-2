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
    componentDidMount: function() {
        this.reloadCE(null);
    },
    reloadCE: function(e) {
        DevConferencesClient.calendar(this.state.page + 10).then(calendar => this.setState({
           eventsList: calendar.data,
            page: calendar.data.length
        }));
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
                    {this.state.eventsList.map(calendarEvent)}
                    <div className="text-center blockLink">
                        <a onClick={this.reloadCE} className="moreLink">Plus d'événements...</a>
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = TimelineEventList;