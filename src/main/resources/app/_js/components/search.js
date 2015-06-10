var React = require('react');
var $ = require('jquery');
var Event = require('./event');

var Search = React.createClass({

    getInitialState: function(){
        return {
            items:  []
        }
    },

    selectEvent: function (e) {
        alert('yo');
    },

    changeInput: function (e) {

        var searchValue = this.refs.searchInput.getDOMNode().value;
        var url = "http://devconferences.cleverapps.io/api/v2/events/search?q=" + searchValue;

        $.ajax({
            url: url,
            dataType: 'json',
            cache: false,
            success: function(data) {
                this.setState({items: data});
            }.bind(this),
            error: function(xhr, status, err) {
                console.error(url, status, err.toString());
            }.bind(this)
        });
    },

    render: function () {
        var items = this.state.items.map(function (event) {
            return (
                <li>
                    <Event event={event} />
                </li>
            );
        }.bind(this));

        return (
            <div className="search">
                <input type="text" onKeyUp={this.changeInput} ref="searchInput" className="input-text" />
                <div className="search-result">
                    <ul>
                        {items}
                    </ul>
                </div>
            </div>
        );
    }
});

module.exports = Search;