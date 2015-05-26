var React = require('react');

var TwitterLink = React.createClass({

    render: function () {
        var twitterId = this.props.twitterId;
        return (
            <i className="fa fa-twitter">
                <a href={'https://twitter.com/' + twitterId}> {'@' + twitterId}</a>
            </i>
        )
    }
});

module.exports = TwitterLink;