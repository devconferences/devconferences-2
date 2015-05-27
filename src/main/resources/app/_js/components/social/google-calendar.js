var React = require('react');

var GoogleCalendar = React.createClass({

    render: function () {
        var accountUri = encodeURIComponent(this.props.account);
        return (
            <iframe src={'https://www.google.com/calendar/embed?src=' + accountUri + '&ctz=Europe/Paris'}
                className={this.props.customClass}
                style={{border: 0}}
                width="800"
                height="600"
                frameBorder="0"
                scrolling="no">
            </iframe>
        )
    }
});

module.exports = GoogleCalendar;