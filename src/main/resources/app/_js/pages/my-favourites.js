var React = require('react');
var Router = require('react-router');
var ReactBootstrap = require('react-bootstrap');

var EventList = require('../components/event-list');
var TimelineEvent = require('../components/timeline-event');
var DevConferencesClient = require('../client/client');

var MyFavourites = React.createClass({
    getInitialState: function() {
        return {
            user: null,
            items: null
        };
    },

    componentWillReceiveProps: function(newProps) {
        this.setState({
            items: null
        });
        DevConferencesClient.auth.user().then(result => {
            this.updateUser(result.data);
        });
    },

    componentDidMount: function() {
        DevConferencesClient.auth.user().then(result => {
            this.updateUser(result.data);
        });
        DevConferencesClient.auth.addListener(this.updateUser);
    },

    updateUser: function(user) {
        DevConferencesClient.getFavourites(this.props.params.type).then(result => {
            if(result.data != this.state.items) {
                this.setState({
                    items: result.data,
                    user: user
                });
            }
        });
    },

    render: function() {
        var items = function() {
            if(this.props.params.type == "conference" ||
                    this.props.params.type == "community") {
                return (
                    <EventList events={this.state.items ? this.state.items.hits : []} favourites={(this.state.user ? this.state.user.favourites : null)} />
                );
            } else if(this.props.params.type == "calendar") {
                var aEvent = function(event) {
                    return (
                        <TimelineEvent key={event.id} event={event} favourites={(this.state.user ? this.state.user.favourites : null)}/>
                    );
                }.bind(this);
                return (
                    <div>
                        {this.state.items ? this.state.items.hits.map(aEvent) : null}
                    </div>
                );
            }
        }.bind(this);
        var typeFavouriteText = function() {
            if(this.props.params.type == "conference") {
                return "conférences";
            } else if(this.props.params.type == "community") {
                return "communautés";
            } else if(this.props.params.type == "calendar") {
                return "événements";
            } else {
                return "";
            }
        }.bind(this);
        return (
            <div className="container">
                <div className="text-center">
                    <h1>Mes favoris : {typeFavouriteText()}</h1>
                </div>
                {items()}
            </div>
        )
    }
});

module.exports = MyFavourites;
