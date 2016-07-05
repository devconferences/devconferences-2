var React = require('react');
var ReactBootstrap = require('react-bootstrap');
var moment = require('moment');

var TimelineEvent = require('./timeline-event');
var DevConferencesClient = require('../client/client');

var Glyphicon = ReactBootstrap.Glyphicon;

var TimelineEventList = React.createClass({
    render: function() {
        var calendarEvent = function(event) {
            return (
                <TimelineEvent key={event.id} event={event} favourites={this.props.favourites}/>
            );
        }.bind(this);
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
            this.props.moreUpcomingEvents(parseInt(this.props.calendar.hitsAPage) + 10);
        }.bind(this);

        var hits = function() {
            if(this.props.calendar.hits) {
                if(this.props.calendar.hits.length > 0) {
                    return (
                        <div>
                            {this.props.calendar.hits.map(calendarEvent)}
                        </div>
                    );
                } else {
                    return (<p className="text-center">Pas d'événements à venir.</p>);
                }
            } else {
                return (
                    <p className="text-center">
                        <Glyphicon glyph="refresh" className="refresh-animate"></Glyphicon>
                    </p>
                );
            }
        }.bind(this);

        return (
            <div id="timeline-event-list" className="text-left separe scrollable">
                {hits()}
                {moreUpcomingEvents(this.props.calendar.totalPage, moreUpcomingEventsCall)}
            </div>
        );
    }
});

module.exports = TimelineEventList;