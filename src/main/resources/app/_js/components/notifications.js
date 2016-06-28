var React = require('react');

var NotificationList = require('./notification-list.js');

var Notifications = React.createClass({
    render: function() {
        return (
            <div className="modal fade" id="notificationsModal" role="dialog">
                <div className="modal-dialog" role="document">
                    <div className="modal-content">
                        <div className="modal-header text-center">
                            <button type="button" className="close" data-dismiss="modal" aria-label="Close">
                                <span aria-hidden="true">&times;</span>
                            </button>
                            <h2>Mes notifications</h2>
                        </div>
                        <div className="modal-body">
                            <NotificationList messages={this.props.messages} />
                        </div>
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = Notifications;
