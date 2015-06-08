var React = require('react');
var Router = require('react-router');
var injectTapEventPlugin = require("react-tap-event-plugin");

var Home = require('./components/home');
var City = require('./components/city');
var NotFound = require('./components/not-found');
var BreizhcampTeaser = require('./components/breizhcamp-teaser');
var Authentication = require('./components/authentication');

var Route = Router.Route;
var RouteHandler = Router.RouteHandler;
var DefaultRoute = Router.DefaultRoute;
var NotFoundRoute = Router.NotFoundRoute;

var DevConferencesClient = require('./client/client');

// Needed for onTouchTap
// Can go away when react 1.0 release
// Check this repo: https://github.com/zilverline/react-tap-event-plugin
injectTapEventPlugin();

var App = React.createClass({
    render: function () {
        return (
            <div>
                <BreizhcampTeaser />
                <header>
                    <Authentication />

                    <div className="container title">
                        <a href="/">
                            Dev Conferences
                        </a>

                    </div>

                </header>

                <RouteHandler/>

                <footer>
                    <hr />
                    <div className="container text-center">
                        Il manque une ville &#63; Il manque une conférence &#63;
                        <br />
                        <a href="https://github.com/devconferences/devconferences.github.io">
                            Contribuez sur Github &#33;
                        </a>
                    </div>
                </footer>
            </div>
        )
    }
});

var routes = (
    <Route handler={App}>
        <DefaultRoute handler={Home}/>
        <NotFoundRoute handler={NotFound} />

        <Route name="city" path="city/:id" handler={City}/>
    </Route>
);

Router.run(routes, Router.HistoryLocation, function (Root) {
    React.render(<Root/>, document.body);
});

DevConferencesClient.useCleverUrl().cities().then(cities => console.log(cities.data));