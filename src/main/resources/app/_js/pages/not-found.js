var React = require('react');
var DocumentTitle = require('react-document-title');

var NotFound = React.createClass({
    render: function () {
        return (
            <DocumentTitle title="Dev Conferences - 404">
                <div className="container text-center">
                    <img src="/img/404.png"/>
                </div>
            </DocumentTitle>
        )
    }
});

module.exports = NotFound;