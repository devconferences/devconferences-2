var React = require('react');

var Tags = React.createClass({
    render: function() {
        var aTag = function(tag) {
            return ( <span> <a>{tag}</a>; </span> );
        };
        return (
            <i className="fa fa-key">
                { this.props.tags.map(aTag) }
            </i>
        );
    }
});

module.exports = Tags;