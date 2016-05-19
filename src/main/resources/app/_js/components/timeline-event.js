var React = require('react');
var moment = require('moment');
var ReactBootstrap = require('react-bootstrap');

var Glyphicon = ReactBootstrap.Glyphicon;


var TimelineEvent = React.createClass({

    render: function() {
        var event =this.props.event;
        var date = new Date(parseInt(event.date));

        var prettyDates = function(dateBegin, duration) {
            var formattedDateBegin = moment(dateBegin).format("DD/MM/YYYY HH:mm");
            if(duration <= 0) {
                return (
                    <span>{formattedDateBegin}</span>
                );
            } else {
                var dateEnd = new Date(dateBegin.getTime() + duration);
                var formattedDateEnd;
                if(dateBegin.toDateString() == dateEnd.toDateString()) {
                    formattedDateEnd = moment(dateEnd).format("HH:mm");
                } else {
                    formattedDateEnd = moment(dateEnd).format("DD/MM/YYYY HH:mm");
                }
                return (
                    <span>{formattedDateBegin} <i className="fa fa-long-arrow-right"></i> {formattedDateEnd}</span>
                );
            }
        };
        var organizer = function(name, url) {
            if(name) {
                if(url) {
                    return (
                        <span>, par <a href={url}>{name}</a></span>
                    );
                } else {
                    return (
                        <span>, par {name}</span>
                    );
                }
            } else {
                return (
                    <span></span>
                );
            }
        }
        var nameTitle = function(name, url) {
            if(url) {
                return (
                    <span><a href={url}>{name}</a></span>
                );
            } else {
                return (
                    <span>{name}</span>
                );
            }
        }

        return (
            <div className="timeline-event">
                <h3>
                    <Glyphicon glyph="chevron-right"> {nameTitle(event.name, event.url)} </Glyphicon>
                </h3>
                <p>
                    {prettyDates(date, event.duration)}{organizer(event.organizerName, event.organizerUrl)}
                </p>
                <p>
                    <div className="pre-style">{event.description}</div>
                </p>
            </div>
        );
    }
});

module.exports = TimelineEvent;