var React = require('react');

var Event = require('./event');

var EventList = React.createClass({

    render: function () {
        var renderEvent = function (event) {
            return (
                <Event key={event.id} event={event} />
            );
        };
        return (
            <div>
                { this.props.events.map(renderEvent) }
            </div>
        );
    }

});

module.exports = EventList;