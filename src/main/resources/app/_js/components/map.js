var React = require('react');
var Router = require('react-router');
var $ = require('jquery');

var GGMaps = require('react-google-maps');
var GoogleMaps = GGMaps.GoogleMaps;
var Circle = GGMaps.Circle;
var Marker = GGMaps.Marker;
var InfoBox = GGMaps.InfoBox;

var EventAnchorList = require('./event-anchor-list');
var EventList = require('./event-list');

var DevConferencesClient = require('../client/client');

var CityMap = React.createClass({

    mixins: [Router.Navigation],

    getInitialState: function () {

        return {
            cities: [],
            citiesLoaded: false,
            browserLocation: {
                lat: 46.5,
                lng: 1
            },
            zoom: 6
        };
    },

    componentDidMount: function() {
        var geocoder = new google.maps.Geocoder();
        var that = this;
        var citiesFromCache = localStorage.getItem("devconferences-cities");
        if (citiesFromCache) {
            this.setState({
               citiesLoaded: true,
               cities: JSON.parse(citiesFromCache)
            });
        } else {

            DevConferencesClient.cities().then(function(resp) {
                var cities = resp.data;
                var finalCities = [];
                function codeAddress() {
                    if (cities.length === 0) {
                        localStorage.setItem("devconferences-cities", JSON.stringify(finalCities));
                        that.setState({
                            citiesLoaded: true,
                            cities: finalCities
                        });
                    } else {
                        var city = cities.pop();
                        if (!city.position) {
                            console.debug("Je cherche l'adresse de la ville " + city.name);
                            geocoder.geocode({ 'address': city.name}, function (results, status) {
                                if (status == google.maps.GeocoderStatus.OK) {
                                    console.debug("found ville " + city.name);
                                    city.position = {"lat":results[0].geometry.location.A, "lng":results[0].geometry.location.F};

                                    finalCities.push(city);
                                } else {
                                    console.debug('Geocode was not successful for the following reason: ' + status);
                                }
                                that.setState({
                                    cities: finalCities
                                });
                                setTimeout(codeAddress, 1000);
                            });
                        }
                    }
                }
                codeAddress();
            });
        }

    },

    position : function(position){
        this.setState({
            browserLocation: {
                lat: position.coords.latitude,
                lng: position.coords.longitude
            },
            zoom: 8
        });
    },

    render: function () {

        if(this.state.citiesLoaded && navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(this.position);
        }
        if(this.state.browserLocation) {
            return (
                <div>
                    <GoogleMaps containerProps={{
                        style: {
                            height: "500px",
                            width: "80%"
                        }
                    }}
                    googleMapsApi={
                            "undefined" !== typeof google ? google.maps : null
                        }
                    zoom={this.state.zoom}
                    center={{lat: this.state.browserLocation.lat, lng: this.state.browserLocation.lng}}
                    >
                     {this.state.cities.map(toMarker, this)}
                    </GoogleMaps>

                </div>
                );
            function toMarker (city, index) {
                if(city.count>1) {
                    return (
                        <Circle center={city.position} radius={2000 * city.count} fillColor="red" fillOpacity={0.20} strokeColor="red" strokeOpacity={1} strokeWeight={1} />
                        );
                }else{
                    return ( <Marker position={city.position} key={city.name}/>);
                }
            }

        }else{
            return(<div>en chargement</div>)
        }
    }
});

/* <InfoBox
 closeBoxURL=""
 position={city.position}
 content={city.name+' '+city.count} /> */

module.exports = CityMap;