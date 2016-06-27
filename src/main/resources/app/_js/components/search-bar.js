var React = require('react');
var ReactDOM = require('react-dom');
var Router = require('react-router');

var DevConferencesClient = require('../client/client');
var auth = require('./authentication');
var Favourite = require('./favourite');

var SearchBar = React.createClass({
    // All kind of research
    EVENTS: 0x01,
    CALENDAR: 0x02,
    CITIES: 0x04,
    ALL: 0x07,
    MAX_RESULTS: 1000,

    getInitialState: function() {
        return {
            page: null,
            query: null,
            querySearch: null,
            searchType: null,
            suggests: {
                query: null,
                hits: []
            },
            showSuggests: false
        };
    },

    componentDidMount: function() {
        this.research(this.props.query || "", null, null);
        var e = {};
        e.target = {};
        e.target.value = "";
        this.queryChanged(e);
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
                // First condition prevents failed request to update suggests (no 'data' property)
                if(result.data) {
                    if(this.refs.searchInput.value == result.data.query) {
                        this.setState({
                            query: result.data.query,
                            suggests: result.data
                        });
                    }
                } else {
                    console.warn("Previous query failed");
                }
            })
        } else {
            this.setState({
                suggests: {
                    query: null,
                    hits: []
                }
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
            query = this.state.querySearch;
        }
        if(!page) {
            page = this.props.page || 1;
        } else if(page == -1) {
            page = this.state.page;
        }
        if(!searchType) {
            searchType = this.props.searchType || this.state.searchType || this.ALL;
        }

        if(query == this.state.querySearch && page == this.state.page && searchType == this.state.searchType) {
            return;
        } else {
            this.setState({
                querySearch: query,
                query: query,
                page: page,
                searchType: searchType
            });
        }

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
                var limit = null;
                if(!query) {
                    limit = this.props.limit || (this.props.allDataWhenEmpty ? this.MAX_RESULTS : null);
                }
                DevConferencesClient.searchEvents(query, page, limit).then(result => {
                    if(query != "" || this.props.all) {
                        data.events = result.data;
                    } else {
                        data.events = {};
                    }
                    // If an API call fail (ie there is no 'data' property), then onUpdate() won't be called
                    if(result.data) {
                        searchDone += this.EVENTS;
                    } else {
                        console.warn("Previous query failed");
                    }
                    if(searchDone == searchType) {
                        this.props.onUpdate(data);
                    }
                });
            }
            if(searchType & this.CALENDAR) {
                var limit = null;
                if(!query) {
                    limit = this.props.limit || (this.props.allDataWhenEmpty ? this.MAX_RESULTS : null);
                }
                DevConferencesClient.searchCalendar(query, page, limit).then(result => {
                    if(query != "" || this.props.all) {
                        data.calendar = result.data;
                    } else {
                        data.calendar = {};
                    }
                    // If an API call fail (ie there is no 'data' property), then onUpdate() won't be called
                    if(result.data) {
                        searchDone += this.CALENDAR;
                    } else {
                        console.warn("Previous query failed");
                    }
                    if(searchDone == searchType) {
                        this.props.onUpdate(data);
                   }
               });
            }
            if(searchType & this.CITIES) {
                DevConferencesClient.cities(query).then(result => {
                    if(query != "" || this.props.all) {
                        data.cities = result.data;
                    } else {
                        data.cities = {};
                    }
                    // If an API call fail (ie there is no 'data' property), then onUpdate() won't be called
                    if(result.data) {
                        searchDone += this.CITIES;
                    } else {
                        console.warn("Previous query failed");
                    }
                    if(searchDone == searchType) {
                        this.props.onUpdate(data);
                   }
               });
            }
        }
    },


    setSearchQuery: function(e) {
        if(e.target.nodeName == "LI") {
            // Change searchInput value, to short condition with the timeout
            ReactDOM.findDOMNode(this.refs.searchInput).value = e.target.dataset.value;
            this.research(e.target.dataset.value, null, null);
        }
    },

    prepareSearch: function(target) {
        var value = this.refs.searchInput.value;

        var searchValue = (target ? target.value : ReactDOM.findDOMNode(this.refs.searchInput).value);
        var query = this.props.query || "";
        var page = 1;

        if(query != "" && searchValue == query) {
            page = this.props.page || 1;
        }

        this.research(searchValue, page, null);
    },

    onEnterPress: function(e) {
        if(e.key == 'Enter') {
            this.prepareSearch(e.target);
        }
    },

    render: function() {
        var suggestList = function(suggests, showSuggests) {
            var hoverSuggest = function(e) {
                if(e.target.nodeName == "LI") {
                    e.target.className = "hovered";
                }
            };
            var noHoverSuggest = function(e) {
                if(e.target.nodeName == "LI") {
                    e.target.className = "";
                }
            };
            var suggestItem = function(suggest, onClickFunc) {
                var isFavouriteUser = function() {
                    return ((this.props.favourites) &&
                            (this.props.favourites.tags.indexOf(suggest.text) > -1));
                }.bind(this);
                return (
                    <li key={suggest.text} data-value={suggest.text}
                            onClick={this.setSearchQuery} onMouseOver={hoverSuggest} onMouseOut={noHoverSuggest}>
                        {suggest.text}
                        <Favourite isAuthenticated={this.props.favourites != null} favouriteUser={isFavouriteUser()} type="TAG" value={suggest.text}/>
                    </li>
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
        var isFavouriteUser = function() {
            return (this.props.favourites &&
                    this.props.favourites.tags.indexOf(this.state.query) > -1);
        }.bind(this);
        return (
            <div className="search-bar-container text-center">
                <input type="text" className="search-bar" ref="searchInput" onKeyPress={this.onEnterPress} onChange={this.queryChanged} onBlur={this.hideSuggests} onFocus={this.showSuggests} placeholder="Entrez votre recherche ici..." defaultValue={this.props.query}/>
                <Favourite isAuthenticated={this.props.favourites != null} favouriteUser={isFavouriteUser()} type="TAG" value={this.state.query}/>
                {suggestList(this.state.suggests, this.state.showSuggests)}
            </div>
        );
    }
});

module.exports = SearchBar;