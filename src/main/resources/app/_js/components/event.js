var React = require('react');
var ReactBootstrap = require('react-bootstrap');

var Grid = ReactBootstrap.Grid;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;
var Glyphicon = ReactBootstrap.Glyphicon;

var Event = React.createClass({

    render: function () {
        var event = this.props.event;
        return (
            <div>
                <a name={event.id}></a>
                <h2>
                    <Glyphicon glyph='chevron-right'> {event.name}</Glyphicon>
                </h2>

                <Grid>
                    <Row>
                        <Col md={2}>
                            <img src={event.avatar} className="img-responsive"/>
                        </Col>
                        <Col md={10} className="text-justify">
                            <p>
                                {event.description}
                            </p>
                            <p>
                                <Glyphicon glyph='home'>
                                    <a href={event.website}> {event.website}</a>
                                </Glyphicon>
                            </p>
                        </Col>
                    </Row>
                </Grid>
            </div>
        );
    }

});

module.exports = Event;