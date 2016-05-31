var React = require('react');
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

        this.research(searchValue, page, this.ALL);
    },

    research: function(query, page, searchType) {
        // Prepare data
        if(!query && this.state.lastSearch) {
            query = this.state.lastSearch.query;
        }
        if(!page) {
            page = 0;
        }
        if(!searchType) {
            searchType = this.ALL;
        }

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