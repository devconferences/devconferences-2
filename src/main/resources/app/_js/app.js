var React = require('react');
var Router = require('react-router');
var ReactBootstrap = require('react-bootstrap');
var injectTapEventPlugin = require("react-tap-event-plugin");

var Home = require('./components/home');
var City = require('./components/city');
var Search = require('./components/search');
var NotFound = require('./components/not-found');
var BreizhcampTeaser = require('./components/breizhcamp-teaser');
var Authentication = require('./components/authentication');

var Route = Router.Route;
var RouteHandler = Router.RouteHandler;
var DefaultRoute = Router.DefaultRoute;
var NotFoundRoute = Router.NotFoundRoute;

var Grid = ReactBootstrap.Grid;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;
var Glyphicon = ReactBootstrap.Glyphicon;

var DevConferencesClient = require('./client/client');

// Needed for onTouchTap
// Can go away when react 1.0 release
// Check this repo: https://github.com/zilverline/react-tap-event-plugin
injectTapEventPlugin();

var App = React.createClass({
    render: function () {
        return (
            <div>
                <header>
                    <Authentication />

                    <div className="container title">
                        <a href="/">
                            Dev Conferences
                        </a>

                    </div>

                </header>

                <RouteHandler/>

                <hr />

                <footer>
                    <Grid>
                        <Row className="text-center">
                            <Col md={4}>
                                <Glyphicon glyph="cloud-upload">
                                &nbsp;Hébergé chez&nbsp;
                                    <a href="//www.clever-cloud.com">Clever Cloud</a>
                                </Glyphicon>
                            </Col>
                            <Col md={4}>
                                <i className="fa fa-twitter">
                                &nbsp;Suivez-nous sur&nbsp;
                                    <a href="//twitter.com/DevConferences">Twitter</a>
                                </i>
                            </Col>
                            <Col md={4}>
                                <i className="fa fa-bug">
                                &nbsp;Remontez les bugs via&nbsp;
                                    <a href="//github.com/devconferences/devconferences-2/issues">&nbsp;Github</a>
                                </i>

                            </Col>
                        </Row>
                    </Grid>
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
        <Route name="search" path="search" handler={Search}/>
        <Route name="searchWithParam" path="search/:query" handler={Search}/>
    </Route>
);

Router.run(routes, Router.HistoryLocation, function (Root) {
    React.render(<Root/>, document.body);
});
