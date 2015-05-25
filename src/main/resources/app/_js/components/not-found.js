var React = require('react');

var NotFound = React.createClass({
    render: function () {
        return (
            <div className="container text-center">
                <img src="/img/404.png"/>
            </div>
        )
    }
});

module.exports = NotFound;