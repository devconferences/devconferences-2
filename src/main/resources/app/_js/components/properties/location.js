var React = require('react');

var Location = React.createClass({
    render: function() {
        var location = function(location) {
            if(location) {
                var mapsUrl = "http://maps.google.com/?q=" + location.gps.lat + ", " + location.gps.lon;
                return (
                    <span><i className="fa fa-map-marker"></i> : <span onClick={(e) => {e.stopPropagation()}}><a href={mapsUrl} title="Afficher sur Google Maps">{location.name}</a> </span>
                        ({location.address}, {location.city})</span>
                );
            } else {
                return null;
            }
        };
        return (
            <span>{location(this.props.location)}</span>
        );
    }
});

module.exports = Location;