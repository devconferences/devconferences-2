var React = require('react');

var FacebookLink = React.createClass({

    render: function () {
        var facebookId = this.props.id;
        return (
            <i className="fa fa-facebook">
                <a href={'https://www.facebook.com/' + facebookId}> {'facebook.com/' + facebookId}</a>
            </i>
        )
    }
});

module.exports = FacebookLink;