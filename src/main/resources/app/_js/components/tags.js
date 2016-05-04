var React = require('react');
var Router = require('react-router');

var Link = Router.Link;

var Tags = React.createClass({
    render: function() {
        var aTag = function(tag) {
            var linkToURL = "/search/" + tag;
            return ( <span> <Link to={linkToURL} >{tag}</Link>; </span> );
        };
        return (
            <i className="fa fa-key">
                { this.props.tags.map(aTag) }
            </i>
        );
    }
});

module.exports = Tags;