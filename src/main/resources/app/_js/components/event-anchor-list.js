var React = require('react');

var EventAnchor = require('./event-anchor');

var EventAnchorList = React.createClass({

    render: function () {
        var renderEventAnchor = function (event) {
            return (
                <EventAnchor key={event.id} event={event} />
            );
        };
        return (
            <div className="conflinks">
                <h3>{ this.props.title }</h3>
                { this.props.events.map(renderEventAnchor) }
            </div>
        );
    }

});

module.exports = EventAnchorList;