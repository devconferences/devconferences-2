var React = require('react');
var Router = require('react-router');
var $ = require('jquery');

var EventAnchorList = require('./event-anchor-list');
var EventList = require('./event-list');
var UpcomingEventsList = require('./upcoming-events-list');
var Favourite = require('./favourite');
var DevConferencesClient = require('../client/client');

var City = React.createClass({

    mixins: [Router.Navigation],

    getInitialState: function () {
        return {
            city: null,
            user: null
        };
    },

    componentDidMount: function () {
      DevConferencesClient.city(this.props.params.id, this.props.params.query).then(city => this.setState({ city: city.data }));
      DevConferencesClient.auth.user().then(result => {
          this.updateUser(result.data);
      });
      DevConferencesClient.auth.addListener(this.updateUser);
    },

    updateUser: function(user) {
        this.setState({
            user: user
        });
    },

    render: function () {
        var renderAnchorList = function (items, title) {
            if (items.length > 0) {
                return (
                    <EventAnchorList events={items} title={title} />
                )
            }
        };
        var renderUpcomingEvents = function(items) {
            if(items.length > 0) {
                return (
                    <div className="text-center">
                        <h3><a href="#upcomingevents">{items.length} événement(s) à venir</a></h3>
                    </div>
                );
            }
        };
        var queryText = function(query) {
            if(query) {
                return (
                    <span>#{query} </span>
                );
            } else {
                return null;
            }
        }
        var isFavouriteUser = function() {
            if(this.props.params.id) {
                if(this.props.params.query) {
                    if(this.state.user) {
                        return (this.state.user.favourites.cities.indexOf(
                            this.props.params.id + "/" + this.props.params.query
                        ) > -1);
                    }
                } else {
                     if(this.state.user) {
                         return (this.state.user.favourites.cities.indexOf(
                             this.props.params.id
                         ) > -1);
                     }
                 }
            }

            return null;
        }.bind(this);
        if (this.state.city) {
            return (
                <div className="container">
                    <div className="text-center">
                        <h1>
                            Dev Conferences @ {this.state.city.name} {queryText(this.props.params.query)}
                            <Favourite favouriteUser={isFavouriteUser()} type="CITY" value={this.state.city.name} filter={((this.props.params.query))}/>
                        </h1>
                    </div>

                    {renderAnchorList(this.state.city.conferences, 'Conférences')}
                    {renderAnchorList(this.state.city.communities, 'Communautés')}

                    {renderUpcomingEvents(this.state.city.upcoming_events)}

                    <hr />

                    <EventList events={this.state.city.conferences} favourites={(this.state.user ? this.state.user.favourites : null)} />
                    <EventList events={this.state.city.communities} favourites={(this.state.user ? this.state.user.favourites : null)} />

                    <UpcomingEventsList events={this.state.city.upcoming_events} cityName={this.state.city.name} favourites={(this.state.user ? this.state.user.favourites : null)} />
                </div>
            )
        }
        else {
            // TODO loading icon ?
            return (
                <div className="container">
                    Chargement ...
                </div>
            )
        }
    }
});

module.exports = City;
