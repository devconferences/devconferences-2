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
            searchType: null,
            suggests: [],
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

    mergeSuggests: function(data) {
        // Concat both arrays
        data.suggests = [];
        if(data.events) {
            data.suggests = data.suggests.concat(data.events.suggests);
        }
        if(data.calendar) {
            data.suggests = data.suggests.concat(data.calendar.suggests);
        }

        // Merge items with the same text (then add the score)
        var uniqueItems = [];
        for(var i = 0; i < data.suggests.length; i++) {
            var item = {
                text: data.suggests[i].text,
                score: data.suggests[i].score
            };
            for(var j = i + 1; j < data.suggests.length; j++) {
                if(item.text == data.suggests[j].text) {
                    // Add score in lower index, then remove higher index
                    item.score += data.suggests[j].score;
                    data.suggests.splice(j,1);
                }
            }
            uniqueItems.push(item);
        }
        data.suggests = uniqueItems;

        // Sort suggests
        data.suggests.sort(function(o,t1){
            if(o.score != t1.score) {
                return t1.score - o.score;
            } else {
                return o.text.localeCompare(t1.text);
            }
        });
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
                            this.mergeSuggests(data);
                            this.props.onUpdate(data);
                            this.setState({
                                suggests: data.suggests
                            });
                        }
                    });
                }
                if(searchType & this.CALENDAR) {
                    DevConferencesClient.searchCalendar(query, page).then(result => {
                        data.calendar = result.data;
                        searchDone += this.CALENDAR;
                        if(searchDone == searchType) {
                        this.mergeSuggests(data);
                            this.props.onUpdate(data);
                            this.setState({
                                suggests: data.suggests
                            });
                       }
                   });
                }
            }
        }.bind(this), 300);
    },

    render: function() {
        var suggestItem = function(suggest) {
            return (
                <li>{suggest.text}</li>
            );
        }
        var classNameSuggests = function(suggests) {
            if(suggests.length > 0) {
                return "search-suggests panel panel-default";
            } else {
                return "hidden search-results panel panel-default";
            }
        }
        return (
            <div className="search-bar-container text-center">
                <input type="text" className="search-bar" ref="searchInput" onChange={this.queryChanged} placeholder="Entrez votre recherche ici..." defaultValue={this.props.query}/>
                <div className={classNameSuggests(this.state.suggests)}>
                    <ul>
                        {this.state.suggests.map(suggestItem)}
                    </ul>
                </div>
            </div>
        );
    }
});

module.exports = SearchBar;