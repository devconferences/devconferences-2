var React = require('react');
var Router = require('react-router');
var ReactBootstrap = require('react-bootstrap');
var moment = require('moment');

var DevConferencesClient = require('../client/client');

var Link = Router.Link;
var Grid = ReactBootstrap.Grid;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;

var NotificationList = React.createClass({
    removeMessage: function(e) {
        DevConferencesClient.deleteMessage(e.target.dataset.id).then(result => {
            DevConferencesClient.auth.user(true);
        });
    },

    render: function () {
        var message = function(message) {
            var dateNotif = new Date(parseInt(message.date));
            var formattedDate = moment(message.date).format("DD/MM/YYYY HH:mm");
            return (
                <li className="list-group-item" key={message.date + "/" + message.text}>
                    <span className="close">
                        <button data-id={message.id} type="button" title="Marquer comme lu" onClick={this.removeMessage}>
                            &times;
                        </button>
                    </span>
                    <Row>
                    <Col md={3}>
                        {formattedDate}
                    </Col>
                    <Col md={8}>
                        <Link to={message.link} title="Voir">{message.text}</Link>
                    </Col>
                    </Row>
                </li>
            );
        }.bind(this);
        if(this.props.messages.length > 0) {
            return (
                <ul className="notification-list list-group">
                    {this.props.messages.map(message)}
                </ul>
            );
        } else {
            return (
                <div className="text-center">
                    <i>Pas de nouvelles notifications.</i>
                </div>
            );
        }
    }
});

module.exports = NotificationList;