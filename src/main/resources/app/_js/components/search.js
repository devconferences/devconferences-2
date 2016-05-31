var React = require('react');
var Router = require('react-router');
var ReactDOM = require('react-dom');
var $ = require('jquery');

var Event = require('./event');
var TimelineEvent = require('./timeline-event');
var SearchBar = require('./search-bar');
var DevConferencesClient = require('../client/client');

var Link = Router.Link;

var Search = React.createClass({

    mixins: [Router.Navigation],

    getInitialState: function(){
        return {
            lastSearch: null,
            searchType: "events",
            page: null
        }
    },

    componentDidMount: function() {
        // TODO Add forceUpdate function in SearchBar
        //this.changeInput(null);
    },

    componentWillReceiveProps: function(newProps) {
        // TODO idem
        this.setState({
            page: newProps.page
        })
    },

    changeSearchType: function(e) {
        var newSearchType = e.target.value;
        this.refs.searchBar.changeSearchType(this.convertSearchType(newSearchType));
    },

    searchBarUpdated: function(data) {
        this.setState({
            lastSearch: data,
            searchType: (data.events ? "events" : "calendar"),
            page: data.page
        });
    },

    convertSearchType: function(searchType) {
        if(!searchType) {
            searchType = this.state.searchType
        }

        if(searchType == "events") {
            return new SearchBar().EVENTS;
        } else if(searchType == "calendar") {
            return new SearchBar().CALENDAR;
        }
    },

    render: function () {
        var items = function(lastSearch, searchType) {
            var list = [];
            if(lastSearch) {
                if(searchType == "events") {
                    list = lastSearch.events.hits;
                } else if(searchType == "calendar") {
                     list = lastSearch.calendar.hits;
                 }
            }
            return (
                list.map(function (event) {
                    if(searchType == "events") {
                        return (
                            <div>
                                <Event event={event} />
                            </div>
                        );
                    } else if(searchType == "calendar") {
                        return (
                            <div>
                                <TimelineEvent event={event} />
                            </div>
                        );
                    }
                }.bind(this))
            )
        };
        var dataSearch = function(searchType, changeSearchType) {
            var searchTypeUI = function() {
                return (
                    <ul className="list-inline text-center">
                        <li><label><input type="radio" name="searchType" value="events" checked={searchType == "events"}  onChange={changeSearchType} />Conférence / Communauté</label></li>
                        <li><label><input type="radio" name="searchType" value="calendar" checked={searchType == "calendar"} onChange={changeSearchType}/>Événements</label></li>
                    </ul>
                );
            }.bind(this);
            return (
                <div>
                    {searchTypeUI()}
                </div>
            );
        };
        var resultsHead = function(lastSearch, searchType, changeSearchType) {
            var nbrResults = 0;
            var totalPage = 0;
            var currPage = 1;
            var query = "";
            if(lastSearch) {
                var data = null;
                if(searchType == "events") {
                    data = lastSearch.events;
                } else if(searchType == "calendar") {
                   data = lastSearch.calendar;
                }
                nbrResults = data.totalHits;
                totalPage = data.totalPage;
                currPage = data.currPage;
                query = data.query;
            }
            var pageLinks = function() {
                var linkList = [];
                for(var i = 1; i <= totalPage; i++) {
                    var linkURL = "/search/" + query + "/" + i;
                    if(i == currPage) {
                        linkList.push(<li className="active"><Link to={linkURL}>{i}</Link></li>);
                    } else {
                        linkList.push(<li><Link to={linkURL}>{i}</Link></li>);
                    }
                }
                return (
                    <div>
                        <ul className="pagination">
                            {linkList}
                        </ul>
                    </div>
                );
            }.bind(this);
            return (
                <div>
                    <div className="container text-center">
                        {nbrResults} résultat(s)
                        {pageLinks()}
                    </div>
                </div>
            );
        }.bind(this);
        var query = this.state.lastSearch ? this.state.lastSearch.query : (this.props.params.query || "");

        var searchType = this.convertSearchType();
        // condition fix searchType update and pagination
        var page = this.state.page ? this.state.page : (this.props.params.page || 1);

        return (
            <div className="search">
                <div className="text-center">
                    <SearchBar ref="searchBar" onUpdate={this.searchBarUpdated} searchType={searchType} query={query} page={page}/>
                    {dataSearch(this.state.searchType, this.changeSearchType)}
                </div>
                <div className="search-result">
                    {resultsHead(this.state.lastSearch, this.state.searchType, this.changeSearchType)}
                    <div className="results">
                        {items(this.state.lastSearch, this.state.searchType)}
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = Search;