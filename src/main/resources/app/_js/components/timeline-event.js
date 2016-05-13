var React = require('react');
var moment = require('moment');


var TimelineEvent = React.createClass({

    render: function() {
        var event =this.props.event;
        var date = new Date(parseInt(event.date));
        var formattedDate = moment(date).format("DD/MM/YYYY HH:mm");
        return (
            <div>
                <h3><a href={event.url}>{event.name}</a></h3>
                <p>
                    {formattedDate}, par <a href={event.organizerUrl}>{event.organizerName}</a>
                </p>
                <p>
                    {event.description}
                </p>
            </div>
        );
    }
});

module.exports = TimelineEvent;