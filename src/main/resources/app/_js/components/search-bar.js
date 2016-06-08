var React = require('react');
var ReactDOM = require('react-dom');
var Router = require('react-router');

var DevConferencesClient = require('../client/client');

var SearchBar = React.createClass({
    // All kind of research
    EVENTS: 0x01,
    CALENDAR: 0x02,
    CITIES: 0x04,
    ALL: 0x07,

    getInitialState: function() {
        return {
            page: null,
            query: null,
            searchType: null,
            suggests: {
                hits: []
            },
            showSuggests: false
        };
    },

    componentDidMount: function() {
        if(this.props.query != null) {
            this.prepareSearch(null);
        }
    },

    componentWillReceiveProps: function(newProps) {
        this.research(newProps.query, newProps.page, newProps.searchType);
    },

    changeSearchType: function(searchType) {
        this.research(null, 1, searchType);
    },

    queryChanged: function(e) {
        if(e) {
            DevConferencesClient.suggest(e.target.value).then(result => {
                if(this.refs.searchInput.value == result.data.query) {
                    this.setState({
                        suggests: result.data
                    });
                }
            })
        } else {
            this.setState({
                suggests: {}
            })
        }
    },

    showSuggests: function(e) {
        this.setState({
            showSuggests: true
        });
    },

    hideSuggests: function(e) {
        setTimeout(function() {
            this.setState({
                showSuggests: false
            });
        }.bind(this), 300);
    },

    research: function(query, page, searchType) {
        // Prepare data
        if(query == null) {
            query = this.state.query;
        }
        if(!page) {
            page = 1;
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
                data.cities = null;

                if(searchType & this.EVENTS) {
                    var allWhenEmpty = false;
                    if(!query) {
                        allWhenEmpty = this.props.allDataWhenEmpty || false;
                    }
                    DevConferencesClient.searchEvents(query, page, null, null, null, allWhenEmpty).then(result => {
                        data.events = result.data;
                        searchDone += this.EVENTS;
                        if(searchDone == searchType) {
                            this.props.onUpdate(data);
                        }
                    });
                }
                if(searchType & this.CALENDAR) {
                    var allWhenEmpty = false;
                    if(!query) {
                        allWhenEmpty = this.props.allDataWhenEmpty || false;
                    }
                    DevConferencesClient.searchCalendar(query, page, null, null, null, allWhenEmpty).then(result => {
                        data.calendar = result.data;
                        searchDone += this.CALENDAR;
                        if(searchDone == searchType) {
                            this.props.onUpdate(data);
                       }
                   });
                }
                if(searchType & this.CITIES) {
                    var allWhenEmpty = false;
                    if(!query) {
                        allWhenEmpty = this.props.allDataWhenEmpty || false;
                    }
                    DevConferencesClient.cities(query, allWhenEmpty).then(result => {
                        data.cities = result.data;
                        searchDone += this.CITIES;
                        if(searchDone == searchType) {
                            this.props.onUpdate(data);
                       }
                   });
                }
            }
        }.bind(this), 300);
    },


    setSearchQuery: function(e) {
        // Change searchInput value, to pass condition with the timeout
        ReactDOM.findDOMNode(this.refs.searchInput).value = e.target.firstChild.nodeValue;
        this.research(e.target.firstChild.nodeValue, null, null);
    },

    prepareSearch: function(e) {
        var value = this.refs.searchInput.value;

        var searchValue = (e ? e.target.value : ReactDOM.findDOMNode(this.refs.searchInput).value);
        var query = this.props.query || "";
        var page = 1;

        if(query != "" && searchValue == query) {
            page = this.props.page || 1;
        }

        this.research(searchValue, page, null);
    },

    onEnterPress: function(e) {
        if(e.key == 'Enter') {
            this.prepareSearch(e);
        }
    },

    render: function() {
        var suggestList = function(suggests, showSuggests) {
            var hoverSuggest = function(e) {
                e.target.className = "hovered";
            };
            var noHoverSuggest = function(e) {
                e.target.className = "";
            };
            var suggestItem = function(suggest, onClickFunc) {
                return (
                    <li onClick={this.setSearchQuery} onMouseEnter={hoverSuggest} onMouseLeave={noHoverSuggest}>{suggest.text}</li>
                );
            }.bind(this);

            if(suggests.hits.length <= 0 || !showSuggests) {
                return null;
            } else {
                return (
                    <div className="search-suggests panel panel-default">
                        <ul>
                            {suggests.hits.map(suggestItem)}
                        </ul>
                    </div>
                );
            }
        }.bind(this);
        return (
            <div className="search-bar-container text-center">
                <input type="text" className="search-bar" ref="searchInput" onKeyPress={this.onEnterPress} onChange={this.queryChanged} onBlur={this.hideSuggests} onFocus={this.showSuggests} placeholder="Entrez votre recherche ici..." defaultValue={this.props.query}/>
                {suggestList(this.state.suggests, this.state.showSuggests)}
            </div>
        );
    }
});

module.exports = SearchBar;