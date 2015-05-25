var React = require('react');

var Conference = require('./conference');

var ConferenceList = React.createClass({

    render: function () {
        var renderConference = function (conference) {
            return (
                <Conference conference={conference} />
            );
        };
        return (
            <div>
                    { this.props.conferences.map(renderConference) }
            </div>
        );
    }

});

module.exports = ConferenceList;