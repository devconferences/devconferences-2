var React = require('react');

var DevConferencesClient = require('../client/client');

var Favourite = React.createClass({
    getInitialState: function() {
        return {
            favouriteUser: this.props.favouriteUser
        };
    },

    componentWillReceiveProps: function(newProps) {
        this.setState({
            favouriteUser: newProps.favouriteUser
        })
    },

    onClick: function(e) {
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
    },
    onMouseEnter: function(e) {
    },
    onMouseLeave: function(e) {
    },

    render: function () {
        var className = "favourite";
        if(this.state.favouriteUser == true) {
            className += " favourite-user"
        }
        return (
            <span className={className} onClick={this.onClick} onMouseEnter={this.onMouseEnter}
                    onMouseLeave={this.onMouseLeave}>
            &nbsp;
            </span>
        );
    }

});

module.exports = Favourite;