var React = require('react');

var Event = require('./event');

var EventList = React.createClass({

    render: function () {
        var renderEvent = function (event) {
            return (
                <Event key={event.id} event={event} favourites={this.props.favourites}/>
            );
        }.bind(this);
        return (
            <div>
                { this.props.events.map(renderEvent) }
            </div>
        );
    }

});

module.exports = EventList;