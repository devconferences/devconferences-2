var React = require('react');
var ReactBootstrap = require('react-bootstrap');
var $ = require('jquery');
var moment = require('moment');

var Glyphicon = ReactBootstrap.Glyphicon;
var Label = ReactBootstrap.Label;

var MeetupLink = React.createClass({

  getInitialState: function () {
    return {
      meetup: null
    };
  },

  componentDidMount: function () {
    var url = '/api/v2/meetup/' + this.props.id;
    $.ajax({
      url: url,
      dataType: 'json',
      cache: false,
      success: function (data) {
        this.setState({meetup: data});
      }.bind(this),
      error: function (xhr, status, err) {
        // Redirect to homepage
        console.error(url, status, err.toString());
        this.transitionTo('/');
      }.bind(this)
    });
  },

  render: function () {
    if (this.state.meetup && this.state.meetup.nextEvent) {
      var nextEvent = this.state.meetup.nextEvent;
      var date = new Date(nextEvent.time);
      var formattedDate = moment(date).format("DD/MM/YYYY HH:mm");
      return (
        <div>
          <p className="fa fa-users">
            <a href={this.state.meetup.url}> {this.state.meetup.url}</a>
          </p>
          <p>
            <Label bsStyle='danger'>Next event:</Label> <a target="_blank" href={nextEvent.url}>{nextEvent.name} ({formattedDate})</a>
          </p>
        </div>
      )
    } else {
      // TODO loading icon ?
      return (
        <div className="container">
          Chargement ...
        </div>
      )
    }
  }
});

module.exports = MeetupLink;
