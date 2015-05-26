var React = require('react');

var BreizhcampTeaser = React.createClass({

    render: function () {
        return (
            <div id="breizhcamp" className="text-center">
                <a href="/breizhcamp.html">
                    Venez coder la v2 au BreizhCamp le 10 juin de 14h à 17h30
                </a>
                &nbsp;avec
                <a href="//github.com/cwoodrow">
                &nbsp;
                    <img src="//www.gravatar.com/avatar/71ca7cf0450a8a352d98955ee460dacb?s=28" title="Chris" alt="Chris" className="img-circle"/>
                &nbsp;
                </a>
                <a href="//twitter.com/TrevorReznik">
                &nbsp;
                    <img src="//www.gravatar.com/avatar/79dc5d13bab6d382ae346ecbb0b9876a?s=28" title="Mathieu" alt="Mathieu" className="img-circle"/>
                &nbsp;
                </a>
                <a href="//twitter.com/sebprunier">
                &nbsp;
                    <img src="//www.gravatar.com/avatar/9ec96799dd90029b4f1caf6d1475c1bb?s=28" title="Sébastien" alt="Sébastien" className="img-circle"/>
                &nbsp;
                </a>
                <a href="//www.serli.com">
                &nbsp;
                    <img src="//pbs.twimg.com/profile_images/445868085153497089/cgPOpuas.jpeg" title="Serli" alt="Serli"
                        className="img-circle"
                        height="28px" width="28px"/>
                &nbsp;
                </a>
                Les technos sont sympas !
                <a href="//github.com/CodeStory/fluent-http">
                &nbsp;
                    <img src="//pbs.twimg.com/profile_images/1705996674/image.jpg" title="Fluent HTTP" alt="Fluent HTTP"
                        className="img-circle"
                        height="28px" width="28px"/>
                &nbsp;
                </a>
                <a href="//facebook.github.io/react/">
                &nbsp;
                    <img src="//github.com/facebook/react/wiki/react-logo-1000-transparent.png" title="ReactJS" alt="ReactJS"
                        className="img-circle"
                        height="28px" width="28px"/>
                &nbsp;
                </a>
                <a href="//www.elastic.co/">
                &nbsp;
                    <img src="//pbs.twimg.com/profile_images/575347766857064448/USjimvnS.png" title="Elastic" alt="Elastic"
                        className="img-circle"
                        height="28px" width="28px"/>
                &nbsp;
                </a>
                <a href="//www.clever-cloud.com">
                &nbsp;
                    <img src="//pbs.twimg.com/profile_images/595661754392301569/-pgTbcC5.png" title="Clever Cloud" alt="Clever Cloud"
                        className="img-circle"
                        height="28px" width="28px"/>
                &nbsp;
                </a>
            </div>
        )
    }
});

module.exports = BreizhcampTeaser;

