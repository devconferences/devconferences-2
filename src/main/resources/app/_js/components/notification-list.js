var React = require('react');
var moment = require('moment');

var NotificationList = React.createClass({
    render: function () {
        var message = function(message) {
            var dateNotif = new Date(parseInt(message.date));
            var formattedDate = moment(message.date).format("DD/MM/YYYY HH:mm");
            return (
                <p key={message.date + "/" + message.text}>
                    {formattedDate} : <br/> {message.text}
                </p>
            );
        }
        return (
            <div>
                {this.props.messages.map(message)}
            </div>
        );
    }
});

module.exports = NotificationList;