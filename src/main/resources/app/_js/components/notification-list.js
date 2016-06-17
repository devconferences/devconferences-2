var React = require('react');
var moment = require('moment');

var DevConferencesClient = require('../client/client');

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
                <p key={message.date + "/" + message.text}>
                    <button data-id={message.id} type="button" className="close" title="Marquer comme lu" onClick={this.removeMessage}>
                      <span aria-hidden="true">&times;</span>
                    </button>
                    {formattedDate} : <br/> {message.text}
                </p>
            );
        }.bind(this);
        return (
            <div>
                {this.props.messages.map(message)}
            </div>
        );
    }
});

module.exports = NotificationList;