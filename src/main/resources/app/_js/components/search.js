var React = require('react');
var Router = require('react-router');
var $ = require('jquery');

var Event = require('./event');
var DevConferencesClient = require('../client/client');

var Search = React.createClass({

    mixins: [Router.Navigation],

    getInitialState: function(){
        return {
            items:  [],
            nbrResults: 0
        }
    },

    componentDidMount: function() {
        this.changeInput(null);
    },

    selectEvent: function (e) {
        alert('yo');
    },

    changeInput: function (e) {
        var searchValue = this.refs.searchInput.getDOMNode().value;

        DevConferencesClient.searchEvents(searchValue).then(result => {
            this.setState({
                items: result.data.hits,
                nbrResults: result.data.totalHits
            })
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
        var resultsHead = function(nbrResults) {
            return <div className="container text-center">{nbrResults} résultat(s)</div>;
        }
        var query = this.props.params.query || "";
        return (
            <div className="search">
                <input type="text" onKeyUp={this.changeInput} ref="searchInput" className="input-text" defaultValue={query}/>
                {resultsHead(this.state.nbrResults)}
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