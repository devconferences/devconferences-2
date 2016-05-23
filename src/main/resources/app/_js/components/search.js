var React = require('react');
var Router = require('react-router');
var $ = require('jquery');

var Event = require('./event');
var TimelineEvent = require('./timeline-event');
var DevConferencesClient = require('../client/client');

var Link = Router.Link;

var Search = React.createClass({

    mixins: [Router.Navigation],

    getInitialState: function(){
        return {
            lastSearch: null,
            searchType: "events"
        }
    },

    componentDidMount: function() {
        this.changeInput(null);
    },

    componentWillReceiveProps: function(newProps) {
        this.research(newProps.params.query, newProps.params.page, null);
    },

    selectEvent: function (e) {
        alert('yo');
    },

    changeInput: function (e) {
        var searchValue = (e ? e.target.value : this.refs.searchInput.getDOMNode().value);
        var query = this.props.params.query || "";
        var page = 1;

        if(query != "" && searchValue == query) {
            page = this.props.params.page || 1;
        }

        this.research(searchValue, page, null);
    },

    changeSearchType: function(e) {
        this.research(null, null, e.target.value);
    },

    research: function(query, page, searchType) {
        // Prepare data
        console.log("Before : " + query + "-" + page + "-" + searchType);
        if(!query && this.state.lastSearch) {
            query = this.state.lastSearch.query;
        }
        if(!page) {
            page = 1;
        }
        if(!searchType) {
            searchType = this.state.searchType;
        }

        console.log("After : " + query + "-" + page + "-" + searchType);

        if(searchType == "events") {
            DevConferencesClient.searchEvents(query, page).then(result => {
                this.setState({
                    lastSearch: result.data,
                    searchType: searchType
                })
            });
        } else if(searchType == "calendar") {
           DevConferencesClient.searchCalendar(query, page).then(result => {
               this.setState({
                   lastSearch: result.data,
                   searchType: searchType
               })
           });
       }
    },

    render: function () {
        var items = function(lastSearch, searchType) {
            var list = [];
            if(lastSearch) {
                list = lastSearch.hits;
            }
            return (
                list.map(function (event) { // TODO Generify this
                    if(searchType == "events") {
                        return (
                            <li>
                                <Event event={event} />
                            </li>
                        );
                    } else if(searchType == "calendar") {
                        return (
                            <li>
                                <TimelineEvent event={event} />
                            </li>
                        );
                    }
                }.bind(this))
            )
        };
        var resultsHead = function(lastSearch, searchType, changeSearchType) {
            var nbrResults = 0;
            var totalPage = 0;
            var currPage = 1;
            var query = "";
            if(lastSearch) {
                nbrResults = lastSearch.totalHits;
                totalPage = lastSearch.totalPage;
                currPage = lastSearch.currPage;
                query = lastSearch.query;
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
            var searchTypeUI = function() {
                return (
                    <ul className="list-inline text-center">
                        <li><input type="radio" name="searchType" value="events" defaultChecked={searchType == "events"}  onChange={changeSearchType} />Conférence / Communauté</li>
                        <li><input type="radio" name="searchType" value="calendar" defaultChecked={searchType == "calendar"} onChange={changeSearchType}/>Événements</li>
                    </ul>
                );
            }.bind(this);

            return (
                <div>
                    <div>
                        {searchTypeUI()}
                    </div>
                    <div className="container text-center">
                        {nbrResults} résultat(s)
                        {pageLinks()}
                    </div>
                </div>
            );
        }.bind(this);
        var query = this.props.params.query || "";
        var test = function(e) {
            console.log(e.target);
        }
        return (
            <div className="search">
                <input type="text" className="input-text" ref="searchInput" onChange={this.changeInput} defaultValue={query} />
                {resultsHead(this.state.lastSearch, this.state.searchType, this.changeSearchType)}
                <div className="search-result">
                    <ul>
                        {items(this.state.lastSearch, this.state.searchType)}
                    </ul>
                </div>
            </div>
        );
    }
});

module.exports = Search;