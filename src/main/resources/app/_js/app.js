var React = require('react');
import { Router, Route, IndexRoute, NorFoundRoute, browserHistory} from 'react-router';
var ReactDOM = require('react-dom');
var ReactBootstrap = require('react-bootstrap');
var injectTapEventPlugin = require("react-tap-event-plugin");

var Home = require('./components/home');
var City = require('./components/city');
var Search = require('./components/search');
var NotFound = require('./components/not-found');
var BreizhcampTeaser = require('./components/breizhcamp-teaser');
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
                <nav className="navbar navbar-default">
                    <div className="container">
                        <Authentication />

                        <div className="title">
                            <a href="/">
                                Dev Conferences
                            </a>
                        </div>
                    </div>
                </nav>

                {this.props.children}

                <hr />

                <footer>
                    <Grid>
                        <Row className="text-center">
                            <Col md={4}>
                                <Glyphicon glyph="cloud-upload"></Glyphicon>
                                &nbsp;Hébergé chez&nbsp;
                                    <a href="//www.clever-cloud.com">Clever Cloud</a>

                            </Col>
                            <Col md={4}>
                                <i className="fa fa-twitter"></i>
                                &nbsp;Suivez-nous sur&nbsp;
                                    <a href="//twitter.com/DevConferences">Twitter</a>

                            </Col>
                            <Col md={4}>
                                <i className="fa fa-bug"></i>
                                &nbsp;Remontez les bugs via&nbsp;
                                    <a href="//github.com/devconferences/devconferences-2/issues">&nbsp;Github</a>


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
        <Route path="/city/:id" component={City}/>
        <Route path="/city/:id/:query" component={City}/>
        <Route path="/search" component={Search}/>
        <Route path="/search/:query" component={Search}/>
        <Route path="/search/:query/:page" component={Search}/>

        <Route path="*" component={NotFound}/>
    </Route>
);

ReactDOM.render(<Router history={browserHistory}>
    {routes}
</Router>, document.getElementById('app'));
