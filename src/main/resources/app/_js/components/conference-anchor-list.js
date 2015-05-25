var React = require('react');

var ConferenceAnchor = require('./conference-anchor');

var ConferenceAnchorList = React.createClass({

    render: function () {
        var renderConferenceAnchor = function (conference) {
            return (
                <ConferenceAnchor conference={conference} />
            );
        };
        return (
            <div className="conflinks">
                    { this.props.conferences.map(renderConferenceAnchor) }
            </div>
        );
    }

});

module.exports = ConferenceAnchorList;