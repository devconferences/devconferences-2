var React = require('react');
var injectTapEventPlugin = require("react-tap-event-plugin");

var CityList = require('./components/city-list');

// Needed for onTouchTap
// Can go away when react 1.0 release
// Check this repo: https://github.com/zilverline/react-tap-event-plugin
injectTapEventPlugin();

React.render(<CityList />, document.getElementById('cities'));
