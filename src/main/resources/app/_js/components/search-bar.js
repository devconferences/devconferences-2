var React = require('react');
var ReactDOM = require('react-dom');
var Router = require('react-router');

var DevConferencesClient = require('../client/client');

var SearchBar = React.createClass({
    // All kind of research
    EVENTS: 0x01,
    CALENDAR: 0x02,
    ALL: 0xFF,

    queryChanged: function(e) {
        var value = this.refs.searchInput.value;

        var searchValue = (e ? e.target.value : ReactDOM.findDOMNode(this.refs.searchInput).value);
                var query = this.props.query || "";
                var page = 0;

                if(query != "" && searchValue == query) {
                    page = this.props.page || 0;
                }

        this.research(searchValue, page, null);
    },

    research: function(query, page, searchType) {
        // Prepare data
        if(!page) {
            page = 0;
        }
        if(!searchType) {
            searchType = this.props.searchType || this.ALL;
        }

        setTimeout(function() {
            if(ReactDOM.findDOMNode(this.refs.searchInput).value == query) {
                var data = {};
                data.query = query;
                data.page = page;
                data.events = null;
                data.calendar = null;

                if(searchType & this.EVENTS) {
                    DevConferencesClient.searchEvents(query, page).then(result => {
                        data.events = result.data;
                        this.props.onUpdate(data);
                    });
                }
                if(searchType & this.CALENDAR) {
                   DevConferencesClient.searchCalendar(query, page).then(result => {
                       data.calendar = result.data;
                       this.props.onUpdate(data);
                   });
                }
            }
        }.bind(this), 300);

    },

    render: function() {
        return (
            <div className="text-center">
                <input type="text" className="input-text" ref="searchInput" onChange={this.queryChanged} placeholder="Entrez votre recherche ici..."/>
            </div>
        );
    }
});

module.exports = SearchBar;