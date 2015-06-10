var React = require('react');
var ReactBootstrap = require('react-bootstrap');

var WebsiteLink = require('./social/website-link');
var TwitterLink = require('./social/twitter-link');
var FacebookLink = require('./social/facebook-link');
var MeetupLink = require('./social/meetup-link');

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
                <p>
                    <MeetupLink id={event.meetup}/>
                </p>
              );
          }
        };
        var event = this.props.event;
        return (
            <div>
                <a name={event.id}></a>
                <h2>
                    <Glyphicon glyph='chevron-right'> {event.name}</Glyphicon>
                </h2>

                <Grid>
                    <Row>
                        <Col md={2}>
                            <img src={event.avatar} className="img-responsive"/>
                        </Col>
                        <Col md={10} className="text-justify">
                            <p>
                                {event.description}
                            </p>
                            { renderWebsite(event) }
                            { renderTwitter(event) }
                            { renderFacebook(event) }
                            { renderMeetup(event) }
                        </Col>
                    </Row>
                </Grid>
            </div>
        );
    }

});

module.exports = Event;