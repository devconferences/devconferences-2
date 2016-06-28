var React = require('react');
var moment = require('moment');
var ReactBootstrap = require('react-bootstrap');

var Cfp = require('./properties/cfp');
var Location = require('./properties/location');
var FavouriteButton = require('./favourite-button');

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

        var renderCfp = function(cfpData) {
            if(cfpData) {
                return (
                    <p>
                        <Cfp cfp={cfpData} />
                    </p>
                );
            } else {
                return null;
            }
        };

        var renderLocation = function(event) {
            if(event.location) {
                return (
                    <p>
                        <Location location={event.location} />
                    </p>
                );
            } else {
                return null;
            }
        };

        var isFavouriteUser = function() {
            return (this.props.favourites &&
                    this.props.favourites.upcomingEvents.indexOf(event.id) > -1);
        }.bind(this);

        var rotate180 = function(e) {
            var elem = e.target.parentNode;
            elem.classList.toggle("expanded");
            if(elem.classList.contains("expanded")) {
                elem.title = "Cliquez pour affichez moins d'informations";
            } else {
                elem.title = "Cliquez pour afficher plus d'informations";
            }
        };

        return (
            <div className="timeline-event panel panel-default" >
                <div>
                    <h3>
                        <FavouriteButton isAuthenticated={this.props.favourites != null} favouriteUser={isFavouriteUser()} type="CALENDAR" value={event.id}/> {nameTitle(event)}
                    </h3>
                    <p>
                        {prettyDates(date, event.duration)}{organizer(event.organizer)}
                    </p>
                    {renderLocation(event)}
                </div>
                <div id={"collapse_" + event.id}>
                    <div>
                        <div className="text-center">
                            <span data-toggle="collapse" data-target={"#collapse_" + event.id + "_show"}
                                    onClick={rotate180}>
                                <span className="expand-glyph"><Glyphicon glyph='chevron-down'></Glyphicon></span>
                            </span>
                        </div>
                        <div id={"collapse_" + event.id + "_show"} className="collapse">
                            {renderCfp(event.cfp)}
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