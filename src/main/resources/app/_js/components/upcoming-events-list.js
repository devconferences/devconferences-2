var React = require('react');

var TimelineEvent = require('./timeline-event');

var UpcomingEventsList = React.createClass({

    render: function () {
        var renderTimelineEvent = function (event) {
            return (
                <TimelineEvent key={event.id} event={event} />
            );
        };
        if(this.props.events.length > 0) {
            return (
                <div>
                    <hr />

                    <a name="upcomingevents"></a>
                    <div className="text-center">
                        <h3>Prochains événements @ {this.props.cityName}</h3>
                    </div>
                    <div>
                        { this.props.events.map(renderTimelineEvent) }
                    </div>
                </div>
            );
        } else {
            return null;
        }
    }

});

module.exports = UpcomingEventsList;