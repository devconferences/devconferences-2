var React = require('react');
var DocumentTitle = require('react-document-title');
var Router = require('react-router');
var ReactBootstrap = require('react-bootstrap');

var EventList = require('../components/event-list');
var TimelineEvent = require('../components/timeline-event');
var DevConferencesClient = require('../client/client');

var Link = Router.Link;

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
        var items = function(noItemText) {
            if(this.state.items) {
                if(this.state.items.hits.length > 0) {
                    if(this.props.params.type == "conference" ||
                            this.props.params.type == "community") {
                        return (
                            <EventList events={this.state.items.hits} favourites={(this.state.user ? this.state.user.favourites : null)} />
                        );
                    } else if(this.props.params.type == "calendar") {
                        var aEvent = function(event) {
                            return (
                                <TimelineEvent key={event.id} event={event} favourites={(this.state.user ? this.state.user.favourites : null)}/>
                            );
                        }.bind(this);
                        return (
                            <div>
                                {this.state.items.hits.map(aEvent)}
                            </div>
                        );
                    }
                } else {
                    return (
                        <p className="text-center">{noItemText}</p>
                    );
                }
            } else {
                return (
                    <p className="text-center">Chargement...</p>
                );
            }
        }.bind(this);
        var typeFavouriteText = function() {
            var result = {
                title: null,
                noItem: null
            };
            if(this.props.params.type == "conference") {
                result.title = "conférences";
                result.noItem = "Pas de conférences favorites.";
            } else if(this.props.params.type == "community") {
                result.title = "communautés";
                result.noItem = "Pas de communautés favorites.";
            } else if(this.props.params.type == "calendar") {
                result.title = "événements";
                result.noItem = "Pas d'événements favoris.";
            }

            return result;
        }.bind(this);
        return (
            <DocumentTitle title={"Dev Conferences - Mes favoris : " + typeFavouriteText().title}>
                <div className="container">
                    <div className="text-center">
                        <h1>Mes favoris : {typeFavouriteText().title}</h1>
                    </div>
                    {items(typeFavouriteText().noItem)}
                    <p className="text-center">
                        <Link to="/favourites">Retour à mes favouris</Link>
                    </p>
                </div>
            </DocumentTitle>
        )
    }
});

module.exports = MyFavourites;
