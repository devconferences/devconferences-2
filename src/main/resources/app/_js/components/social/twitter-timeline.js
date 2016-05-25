var React = require('react');
var ReactDOM = require('react-dom');

var TwitterTimeline = React.createClass({

    componentDidMount: function () {
        var twitterWidget = ReactDOM.findDOMNode(this.refs.twitterWidget);
        var script = document.createElement('script');
        script.id = 'twitter-wjs';
        script.src = '//platform.twitter.com/widgets.js'
        twitterWidget.appendChild(script);
    },

    render: function () {
        var twitterId = this.props.twitterId;
        var widgetId = this.props.widgetId;
        return (
            <div ref="twitterWidget">
                <a
                    className="twitter-timeline"
                    href={'https://twitter.com/' + twitterId}
                    data-widget-id={widgetId}>
                    @{twitterId}
                </a>
            </div>
        )
    }
});

module.exports = TwitterTimeline;
