var React = require('react');

var Cfp = React.createClass({
    render: function() {
        var cfp = function(cfpData) {
            if(cfpData) {
                var statusCFP = function() {
                    if(new Date() < new Date(cfpData.dateSubmission)) {
                    var prettyDate = moment(new Date(cfpData.dateSubmission)).format("DD/MM/YYYY à HH:mm");
                        return (
                            <span><span className="label label-success">Ouvert</span> (Fermeture le {prettyDate})</span>
                        );
                    } else {
                        return (
                            <span className="label label-danger">Fermé</span>
                        );
                    }
                }.bind(this);
                return (
                    <span>CFP : {statusCFP()} <a href={cfpData.url}>{cfpData.url}</a></span>
                );
            } else {
                return null;
            }
        };
        return (
            <span>{cfp(this.props.cfp)}</span>
        );
    }
});

module.exports = Cfp;