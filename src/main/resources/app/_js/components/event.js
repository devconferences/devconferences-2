var React = require('react');
var ReactBootstrap = require('react-bootstrap');

var WebsiteLink = require('./social/website-link');
var TwitterLink = require('./social/twitter-link');
var FacebookLink = require('./social/facebook-link');
var MeetupLink = require('./social/meetup-link');
var YoutubeLink = require('./social/youtube-link');
var ParleysLink = require('./social/parleys-link');
var Tags = require('./tags');

var Grid = ReactBootstrap.Grid;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;
var Glyphicon = ReactBootstrap.Glyphicon;

var Event = React.createClass({

    render: function () {
        var renderWebsite = function (event) {
            if (event.website) {
                return (
                    <p>
                        <WebsiteLink url={event.website} />
                    </p>
                )
            }
        };
        var renderTwitter = function (event) {
            if (event.twitter) {
                return (
                    <p>
                        <TwitterLink id={event.twitter} />
                    </p>
                )
            }
        };
        var renderFacebook = function (event) {
            if (event.facebook) {
                return (
                    <p>
                        <FacebookLink id={event.facebook} />
                    </p>
                )
            }
        };
        var renderMeetup = function (event) {
          if (event.meetup) {
              return (
                <div>
                    <MeetupLink id={event.meetup}/>
                </div>
              );
          }
        };
        var renderTags = function (event) {
            if(event.tags.length > 0) {
                return (
                    <p>
                        <Tags tags={event.tags} />
                    </p>
                );
            }
        };
        var renderYoutube = function (event) {
            if(event.youtube) {
                return (
                    <p>
                        <YoutubeLink youtube={event.youtube} />
                    </p>
                );
            }
        };
        var renderParleys = function (event) {
            if(event.parleys) {
                return (
                    <p>
                        <ParleysLink channel={event.parleys} />
                    </p>
                );
            }
        };
        var event = this.props.event;
        return (
            <div className="event">
                <a name={event.id}></a>
                <h3>
                    <Glyphicon glyph='chevron-right'></Glyphicon> {event.name}
                </h3>

                <Grid>
                    <Row>
                        <Col md={2}>
                            <img src={event.avatar} className="avatar img-responsive"/>
                        </Col>
                        <Col md={10} className="text-justify">
                            <p>
                                {event.description}
                            </p>
                            { renderWebsite(event) }
                            { renderTwitter(event) }
                            { renderFacebook(event) }
                            { renderMeetup(event) }
                            { renderTags(event) }
                            { renderYoutube(event) }
                            { renderParleys(event) }
                        </Col>
                    </Row>
                </Grid>
            </div>
        );
    }

});

module.exports = Event;