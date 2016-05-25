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
                    <span><i className="fa fa-clock-o"></i> : {formattedDateBegin}</span>
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
                    <span><i className="fa fa-clock-o"></i> : {formattedDateBegin} <i className="fa fa-long-arrow-right"></i> {formattedDateEnd}</span>
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
        };
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
        };
        var location = function(location) {
            if(location) {
                var mapsUrl = "http://maps.google.com/?q=" + location.gps.lat + ", " + location.gps.lon;
                return (
                    <p>
                        <i className="fa fa-map-marker"></i> : <a href={mapsUrl}>{location.name}</a> ({location.address}, {location.city})
                    </p>
                );
            } else {
                return (<span></span>);
            }
        };
        var cfp = function(cfpData) {
            if(cfpData) {
                var statusCFP = function() {
                    if(new Date() < new Date(cfpData.dateSubmission)) {
                    var prettyDate = moment(new Date(cfpData.dateSubmission)).format("DD/MM/YYYY à HH:mm");
                        return (
                            <span><span className="label label-success">Ouvert</span> (Fermeture le {prettyDate})</span>
                        );
                    } else {
                        return (
                            <span className="label label-danger">Fermé</span>
                        );
                    }
                }.bind(this);
                return (
                    <p>
                        CFP : {statusCFP()} <a href={cfpData.url}>{cfpData.url}</a>
                    </p>
                );
            } else {
                return (<span></span>);
            }
        }

        return (
            <div className="timeline-event">
                <h3>
                    <Glyphicon glyph="chevron-right"></Glyphicon> {nameTitle(event.name, event.url)}
                </h3>
                <p>
                    {prettyDates(date, event.duration)}{organizer(event.organizerName, event.organizerUrl)}
                </p>
                {location(event.location)}
                {cfp(event.cfp)}
                <p>
                    <div className="text-justify pre-style">{event.description}</div>
                </p>
            </div>
        );
    }
});

module.exports = TimelineEvent;