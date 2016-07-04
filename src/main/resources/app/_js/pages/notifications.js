var React = require('react');
var DocumentTitle = require('react-document-title');
var Router = require('react-router');
var ReactBootstrap = require('react-bootstrap');

var NotificationList = require('../components/notification-list');
var DevConferencesClient = require('../client/client');

var Notifications = React.createClass({
    getInitialState: function() {
        return {
            user: null
        };
    },

    componentDidMount: function() {
        DevConferencesClient.auth.user().then(result => {
            this.updateUser(result.data);
        });
        DevConferencesClient.auth.addListener(this.updateUser);
    },

    componentWillUnmount: function() {
        DevConferencesClient.auth.removeListener(this.updateUser);
    },

    updateUser: function(user) {
        this.setState({
            user: user
        });
    },

    render: function() {
        if(this.state.user) {
            return (
                <DocumentTitle title="Dev Conferences - Notifications">
                    <div className="container">
                        <div className="text-center">
                            <h1>Notifications</h1>
                            <NotificationList messages={this.state.user.messages}/>
                        </div>
                    </div>
                </DocumentTitle>
            );
        } else {
            return (
                <div className="container text-center">
                    Chargement...
                </div>
            );
        }
    }
});

module.exports = Notifications;
