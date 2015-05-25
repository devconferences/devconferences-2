var React = require('react');
var ReactBootstrap = require('react-bootstrap');

var Grid = ReactBootstrap.Grid;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;
var Glyphicon = ReactBootstrap.Glyphicon;

var Conference = React.createClass({

    render: function () {
        return (
            <div>
                <a name={this.props.conference.id}></a>
                <h2>
                    <Glyphicon glyph='chevron-right'> {this.props.conference.name}</Glyphicon>
                </h2>

                <Grid>
                    <Row>
                        <Col md={2}>
                            <img src={this.props.conference.avatar} className="img-responsive"/>
                        </Col>
                        <Col md={10} className="text-justify">
                            <p>
                                {this.props.conference.description}
                            </p>
                            <p>
                                <Glyphicon glyph='home'>
                                    <a href={this.props.conference.website}> {this.props.conference.website}</a>
                                </Glyphicon>
                            </p>
                        </Col>
                    </Row>
                </Grid>
            </div>
        );
    }

});

module.exports = Conference;