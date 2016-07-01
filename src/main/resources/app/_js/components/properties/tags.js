var React = require('react');
var Router = require('react-router');

var Link = Router.Link;

var Tags = React.createClass({
    render: function() {
        var aTag = function(tag) {
            var linkToURL = "/search/" + tag;
            return ( <span key={tag}> <Link to={linkToURL} >{tag}</Link>; </span> );
        };
        return (
            <span onClick={(e) => {e.stopPropagation()}}>
                <i className="fa fa-key"></i>{ this.props.tags.map(aTag) }
            </span>
        );
    }
});

module.exports = Tags;