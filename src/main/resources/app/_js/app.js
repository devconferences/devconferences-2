var React = require('react');
import { Router, Route, IndexRoute, NorFoundRoute, browserHistory, Link} from 'react-router';
var ReactDOM = require('react-dom');
var ReactBootstrap = require('react-bootstrap');
var injectTapEventPlugin = require("react-tap-event-plugin");

var CalendarPage = require('./pages/calendar-page');
var City = require('./pages/city');
var EventPage = require('./pages/event-page');
var FavouriteList = require('./pages/favourite-list');
var Home = require('./pages/home');
var Help = require('./pages/help');
var MyFavourites = require('./pages/my-favourites');
var Notifications = require('./pages/notifications');
var NotFound = require('./pages/not-found');
var Search = require('./pages/search');

var Authentication = require('./components/authentication');

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
                <header className="navbar navbar-default navbar-header">
                    <div className="container">
                        <Authentication />

                        <div className="title">
                            <Link to="/">
                                Dev Conferences
                            </Link>
                        </div>
                    </div>
                </header>

                {this.props.children}

                <footer className="navbar navbar-default navbar-footer">
                    <Grid>
                        <Row className="text-center">
                            <Col md={4}>
                                <p>
                                    <Glyphicon glyph="cloud-upload"></Glyphicon>
                                    &nbsp;Hébergé chez&nbsp;
                                        <a href="//www.clever-cloud.com">Clever Cloud</a>
                                </p>
                            </Col>
                            <Col md={4}>
                                <p>
                                    <i className="fa fa-twitter"></i>
                                    &nbsp;Suivez-nous sur&nbsp;
                                        <a href="//twitter.com/DevConferences">Twitter</a>
                                </p>
                            </Col>
                            <Col md={4}>
                                <p>
                                    <i className="fa fa-bug"></i>
                                    &nbsp;Remontez les bugs via&nbsp;
                                        <a href="//github.com/devconferences/devconferences-2/issues">Github</a>
                                </p>
                            </Col>
                        </Row>
                    </Grid>
                </footer>

            </div>
        )
    }
});

var routes = (
    <Route path="/" component={App}>
        <IndexRoute component={Home}/>
        <Route path="/calendar/:id" component={CalendarPage}/>
        <Route path="/city/:id" component={City}/>
        <Route path="/city/:id/:query" component={City}/>
        <Route path="/event/:id" component={EventPage}/>
        <Route path="/favourites" component={FavouriteList}/>
        <Route path="/favourites/:type" component={MyFavourites}/>
        <Route path="/help" component={Help}/>
        <Route path="/notifications" component={Notifications}/>
        <Route path="/search" component={Search}/>
        <Route path="/search/:query" component={Search}/>
        <Route path="/search/:query/:page" component={Search}/>

        <Route path="*" component={NotFound}/>
    </Route>
);

ReactDOM.render(<Router history={browserHistory}>
    {routes}
</Router>, document.getElementById('app'));
