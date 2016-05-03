var React = require('react');
var $ = require('jquery');
var Event = require('./event');
var DevConferencesClient = require('../client/client');

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

        DevConferencesClient.searchEvents(searchValue).then(items => this.setState({items: items.data}));
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