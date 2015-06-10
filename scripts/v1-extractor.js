var cheerio = require('cheerio');
var request = require('request');
var async = require('async');
var jf = require('jsonfile');
var _ = require('underscore');

var extractConferencesAndCommunities = function (html) {
    var $ = cheerio.load(html);

    // Get communities identifiers from: <div class="conflinks"><h3>Communautés</h3>...</div>
    var communitiesId = [];
    $('div.conflinks').map(function (i, conflinks) {
        var baseNode = $(conflinks);
        var conferenceType = $('h3', baseNode).text();
        if (conferenceType === 'Communautés') {
            $('a', baseNode).map(function (i, anchor) {
                var communityId = $(anchor).attr('href').slice(1);
                communitiesId.push(communityId);
            });
        }
    });

    // Get data for each conference/community from: <a name="id"></a>...
    var conferences = [];
    var communities = [];
    $('a[name]').map(function (i, link) {
        var baseNode = $(link);
        var titleNode = baseNode.next();
        var contentNode = titleNode.next();

        var id = baseNode.attr('name');
        var name = titleNode.text().trim();
        var avatar = $('div > img', contentNode).attr('src');
        var description = '';
        var website = null;
        var twitter = null;
        var facebook = null;
        var meetup = null;
        $('div + div > p', contentNode).map(function (i, paragraph) {
            var paragraphNode = $(paragraph);
            if (paragraphNode.children().length === 0) {
                description += paragraphNode.text().replace(/(\r\n|\n|\r)/gm, '');
                description += ' ';
            } else {
                var websiteNode = $('span[class~="glyphicon-home"]', paragraphNode);
                if (websiteNode.length) {
                    website = websiteNode.next().text();
                }
                var twitterNode = $('i[class~="fa-twitter"]', paragraphNode);
                if (twitterNode.length) {
                    twitter = twitterNode.next().text().slice(1);
                }
                var facebookNode = $('i[class~="fa-facebook"]', paragraphNode);
                if (facebookNode.length) {
                    facebook = facebookNode.next().text().replace('https://www.facebook.com/','');
                }
            }
        });

        var item = {
            id: id,
            name: name,
            avatar: avatar,
            description: description.replace(/\s+/g, " ").trim(),
            website: website,
            twitter: twitter,
            facebook: facebook,
            meetup: meetup
        };
        if (_.contains(communitiesId, id)) {
            communities.push(item);
        } else {
            conferences.push(item);
        }
    });
    return {
        conferences: conferences,
        communities: communities
    };
};

var getCityName = function (city) {
    switch (city) {
        case 'larochelle':
            return 'La Rochelle';
        case 'orleans':
            return 'Orléans';
        default:
            return city[0].toUpperCase() + city.substring(1);
    }
};

var baseUrl = 'http://www.devconferences.org/';
var baseOutput = 'src/main/resources/v1/';
var cities = [
    'amiens', 'angers', 'annecy', 'bordeaux', 'brest', 'caen', 'clermont', 'dijon', 'grenoble', 'larochelle',
    'laval', 'lille', 'lyon', 'marseille', 'montpellier', 'nancy', 'nantes', 'nice', 'niort', 'orleans', 'paris',
    'rennes', 'rouen', 'strasbourg', 'toulouse', 'tours', 'partout'
];

async.eachSeries(cities, function (city, callback) {
    console.log('Processing ' + city);

    request(baseUrl + city, function (err, response, body) {
        if (err) throw err;

        var data = extractConferencesAndCommunities(body);
        var cityConferences = {
            id: city,
            name: getCityName(city),
            conferences: data.conferences,
            communities: data.communities
        };

        var fileName = baseOutput + city + '.json';
        jf.writeFileSync(fileName, cityConferences);
        callback();
    });

});
