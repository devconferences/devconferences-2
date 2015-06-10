var React = require('react');
var Router = require('react-router');
var $ = require('jquery');

var EventAnchorList = require('./event-anchor-list');
var EventList = require('./event-list');

var City = React.createClass({

    mixins: [Router.Navigation],

    getInitialState: function () {
        return {
            city: null
        };
    },

    componentDidMount: function () {
      DevConferencesClient.city(this.props.params.id).then(city => this.setState({ city: city.data }));
    },

    render: function () {
        var renderAnchorList = function (items, title) {
            if (items.length > 0) {
                return (
                    <EventAnchorList events={items} title={title} />
                )
            }
        };
        if (this.state.city) {
            return (
                <div className="container">
                    <div className="text-center">
                        <h1>Dev Conferences @ {this.state.city.name}</h1>
                    </div>

                    {renderAnchorList(this.state.city.conferences, 'Conférences')}
                    {renderAnchorList(this.state.city.communities, 'Communautés')}

                    <hr />

                    <EventList events={this.state.city.conferences} />
                    <EventList events={this.state.city.communities} />
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
