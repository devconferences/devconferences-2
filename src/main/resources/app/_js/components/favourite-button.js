var React = require('react');

var DevConferencesClient = require('../client/client');

var FavouriteButton = React.createClass({
    getInitialState: function() {
        return {
            isAuthenticated: null,
            favouriteUser: null
        };
    },

    componentDidMount: function() {
        this.setState({
            isAuthenticated: this.props.isAuthenticated,
            favouriteUser: this.props.favouriteUser
        });
    },

    componentWillReceiveProps: function(newProps) {
        this.setState({
            isAuthenticated: newProps.isAuthenticated,
            favouriteUser: newProps.favouriteUser
        })
    },

    onClick: function(e) {
        if(this.props.value != "") {
            if(this.state.favouriteUser == false) {
                var favourite  = this.props.value + (this.props.filter ? "/" + this.props.filter: "");
                DevConferencesClient.addFavourite(this.props.type, favourite).then(result => {
                    this.setState({
                        favouriteUser: true
                    });
                    DevConferencesClient.auth.user(true);
                })
            } else if(this.state.favouriteUser == true) {
                DevConferencesClient.removeFavourite(this.props.type, this.props.value, this.props.filter).then(result => {
                    this.setState({
                        favouriteUser: false
                    });
                    DevConferencesClient.auth.user(true);
                });
            }
        }
    },
    onMouseEnter: function(e) {
    },
    onMouseLeave: function(e) {
    },

    render: function () {
        if(this.state.isAuthenticated == true) {
            var className = "favourite";
            var title = "Ajouter aux favoris";
            if(this.state.favouriteUser == true && this.props.value != "") {
                className += " favourite-user";
                title = "Retirer des favoris";
            }
            return (
                <span title={title} className={className} onClick={this.onClick} onMouseEnter={this.onMouseEnter}
                        onMouseLeave={this.onMouseLeave}>
                &nbsp;
                </span>
            );
        } else {
            return null;
        }
    }

});

module.exports = FavouriteButton;