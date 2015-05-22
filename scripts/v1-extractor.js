var cheerio = require('cheerio');
var request = require('request');
var async = require('async');
var jf = require('jsonfile');

var extractConferences = function (html) {
    var $ = cheerio.load(html);

    var conferences = [];
    $('a[name]').map(function (i, link) {
        var baseNode = $(link);
        var titleNode = baseNode.next();
        var contentNode = titleNode.next();

        var conferenceId = baseNode.attr('name');
        var conferenceName = titleNode.text().trim();
        var conferenceAvatar = $('div > img', contentNode).attr('src');
        var conferenceDescription = '';
        var website = null;
        var twitter = null;
        var facebook = null;
        $('div + div > p', contentNode).map(function (i, description) {
            var descriptionNode = $(description);
            if (descriptionNode.children().length === 0) {
                conferenceDescription += descriptionNode.text().replace(/(\r\n|\n|\r)/gm, '');
                conferenceDescription += ' ';
            } else {
                var websiteNode = $('span[class~="glyphicon-home"]', descriptionNode);
                if (websiteNode.length) {
                    website = websiteNode.next().text();
                }
                var twitterNode = $('i[class~="fa-twitter"]', descriptionNode);
                if (twitterNode.length) {
                    twitter = twitterNode.next().text();
                }
                var facebookNode = $('i[class~="fa-facebook"]', descriptionNode);
                if (facebookNode.length) {
                    facebook = facebookNode.next().text();
                }
            }
        });

        conferences.push({
            id: conferenceId,
            name: conferenceName,
            avatar: conferenceAvatar,
            description: conferenceDescription.replace(/\s+/g, " ").trim(),
            website: website,
            twitter: twitter,
            facebook: facebook
        });
    });
    return conferences;
};

var normalizeFileName = function (city) {
    switch (city) {
        case 'clermont':
            return 'Clermont-Ferrand';
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

        var conferences = extractConferences(body);
        var fileName = baseOutput + normalizeFileName(city) + '.json';
        jf.writeFileSync(fileName, conferences);
        callback();
    });

});
