var React = require('react');
var ReactDOM = require('react-dom');
var Router = require('react-router');

var DevConferencesClient = require('../client/client');

var SearchBar = React.createClass({
    // All kind of research
    EVENTS: 0x01,
    CALENDAR: 0x02,
    ALL: 0x03,

    getInitialState: function() {
        return {
            page: null,
            query: null,
            searchType: null
        };
    },

    componentDidMount: function() {
        if(this.props.query != null) {
            this.queryChanged(null);
        }
    },

    componentWillReceiveProps: function(newProps) {
        this.research(newProps.query, newProps.page, newProps.searchType);
    },

    changeSearchType: function(searchType) {
        this.research(null, (this.props.all ? 0 : 1), searchType);
    },

    queryChanged: function(e) {
        var value = this.refs.searchInput.value;

        var searchValue = (e ? e.target.value : ReactDOM.findDOMNode(this.refs.searchInput).value);
        var query = this.props.query || "";
        var page = (this.props.all ? 0 : 1);

        if(query != "" && searchValue == query) {
            page = this.props.page || (this.props.all ? 0 : 1);
        }

        this.research(searchValue, page, null);
    },

    research: function(query, page, searchType) {
        // Prepare data
        if(query == null) {
            query = this.state.query;
        }
        if(!page) {
            page = (this.props.all ? 0 : 1);
        } else if(page == -1) {
            page = this.state.page;
        }
        if(!searchType) {
            searchType = this.props.searchType || this.state.searchType || this.ALL;
        }

        if(query == this.state.query && page == this.state.page && searchType == this.state.searchType) {
            return;
        } else {
            this.setState({
                query: query,
                page: page,
                searchType: searchType
            });
        }

        setTimeout(function() {
            var inputVal = ReactDOM.findDOMNode(this.refs.searchInput).value;
            if(inputVal == query || (
                inputVal == "" && query == null // Case of "" (e.target.value == null , #searchInput.value == "") ...
            )) {
                var data = {};
                var searchDone = 0x00; // Avoid multiple calls of onUpdate when searchType == ALL
                data.query = query;
                data.page = page;
                data.events = null;
                data.calendar = null;

                if(searchType & this.EVENTS) {
                    DevConferencesClient.searchEvents(query, page).then(result => {
                        data.events = result.data;
                        searchDone += this.EVENTS;
                        if(searchDone == searchType) {
                            this.props.onUpdate(data);
                        }
                    });
                }
                if(searchType & this.CALENDAR) {
                   DevConferencesClient.searchCalendar(query, page).then(result => {
                       data.calendar = result.data;
                       searchDone += this.CALENDAR;
                       if(searchDone == searchType) {
                           this.props.onUpdate(data);
                       }
                   });
                }
            }
        }.bind(this), 300);
    },

    render: function() {
        return (
            <div className="text-center">
                <input type="text" className="input-text" ref="searchInput" onChange={this.queryChanged} placeholder="Entrez votre recherche ici..." defaultValue={this.props.query}/>
            </div>
        );
    }
});

module.exports = SearchBar;