var React = require('react');
var moment = require('moment');
var ReactBootstrap = require('react-bootstrap');

var Favourite = require('./favourite');

var Glyphicon = ReactBootstrap.Glyphicon;

var TimelineEvent = React.createClass({

    render: function() {
        var event = this.props.event;
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
        var organizer = function(org) {
            if(org) {
                if(org.name) {
                    if(org.url) {
                        return (
                            <span>, par <a href={org.url}>{org.name}</a></span>
                        );
                    } else {
                        return (
                            <span>, par {org.name}</span>
                        );
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        };
        var nameTitle = function(event) {
            if(event.url) {
                return (
                    <span><a href={event.url}>{event.name}</a></span>
                );
            } else {
                return (
                    <span>{event.name}</span>
                );
            }
        };
        var location = function(event) {
            if(event.location) {
                var mapsUrl = "http://maps.google.com/?q=" + event.location.gps.lat + ", " + event.location.gps.lon;
                return (
                    <p>
                        <i className="fa fa-map-marker"></i> : <a href={mapsUrl}>{event.location.name}</a> ({event.location.address}, {event.location.city})
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
        };

        var isFavouriteUser = function() {
            return (this.props.favourites &&
                    this.props.favourites.upcomingEvents.indexOf(event.id) > -1);
        }.bind(this);

        var expand = function(event) {
            return (
                <a data-parent={"#collapse_" + event.id} data-toggle="collapse"
                    href={"#collapse_" + event.id + "_show"}><Glyphicon glyph='chevron-down'></Glyphicon></a>
            );
        };

        var reduce = function(event) {
            return (
                <a data-parent={"#collapse_" + event.id} data-toggle="collapse"
                    href={"#collapse_" + event.id + "_hide"}><Glyphicon glyph='chevron-up'></Glyphicon></a>
            );
        };

        return (
            <div className="timeline-event panel panel-default" >
                <div>
                    <h3>
                        <Favourite isAuthenticated={this.props.favourites != null} favouriteUser={isFavouriteUser()} type="CALENDAR" value={event.id}/> {nameTitle(event)}
                    </h3>
                    <p>
                        {prettyDates(date, event.duration)}{organizer(event.organizer)}
                    </p>
                    {location(event)}
                </div>
                <div id={"collapse_" + event.id}>
                    <div className="panel no-shadow no-margin">
                        <div id={"collapse_" + event.id + "_hide"} className="collapse in">
                            <div className="text-center">
                              <span data-toggle="collapse" data-parent={"#collapse_" + event.id} data-target={"#collapse_" + event.id + "_show"}>
                                <Glyphicon glyph='chevron-down'></Glyphicon>
                              </span>
                            </div>
                        </div>
                        <div id={"collapse_" + event.id + "_show"} className="collapse">
                            <div className="text-center">
                              <span data-toggle="collapse" data-parent={"#collapse_" + event.id} data-target={"#collapse_" + event.id + "_hide"}>
                                  <Glyphicon glyph='chevron-up'></Glyphicon>
                              </span>
                            </div>
                            {cfp(event.cfp)}
                            <div className="pre-style text-justify">
                                {event.description}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = TimelineEvent;