import Axios from "axios";

const CLEVER_URL = "http://devconferences.cleverapps.io";
const DEV_URL = "http://localhost:8080";
const apiRoot = "api/v2";

const errorCallback = response => console.error(`${response.status} - '${response.statusText}'`,response);

function createClient(u) {

    let actualUrl = u;

    let check = Axios.get(`${actualUrl}/ping`).catch(function(e) {
      console.log('Using Clever Cloud as datasource');
      actualUrl = CLEVER_URL;
    });

    var auth = authPriv();

    function authPriv() {
        var UNKNOWN = 0x01;
        var PENDING = 0x02;
        var SETTLED = 0x04;
        var userInfo = null;
        var status = UNKNOWN;
        var listeners = [];

        function user(forceUpdate) {
            return new Promise(function(resolve, reject) {
                if(status == UNKNOWN || forceUpdate == true) {
                    status = PENDING;
                    userInfo = check.then(nothing => Axios.get(`${actualUrl}/auth/connected-user`).catch(errorCallback));
                    updateListeners();
                    status = SETTLED;
                    return resolve(userInfo);
                } else if(status == SETTLED) {
                    return resolve(userInfo);
                } else if(status == PENDING) {
                    // Wait the answer
                    setTimeout(function(){}, 100);
                    return user;
                }
            });
        }
        function clientId() {
            return check.then(nothing => Axios.get(`${actualUrl}/auth/client-id`).catch(errorCallback));
        }
        function addListener(method) {
            if(listeners.indexOf(method) == -1) {
                listeners.push(method);
            }
        }
        function removeListener(method) {
            if(listeners.indexOf(method) > -1) {
                listeners.splice(listeners.indexOf(method),1);
            }
        }
        function updateListeners() {
            userInfo.then(result => {
                if(result != undefined) {
                    listeners.forEach(function(method) {
                        method(result.data);
                    });
                }
            });
        }

        return {
            userInfo,
            user,
            clientId,
            addListener,
            removeListener
        };
    };

    function cities(query) {
        return check.then(nothing => Axios.get(`${actualUrl}/${apiRoot}/cities?q=${query}`).catch(errorCallback));
    }

    function city(id,query) {
        return check.then(nothing => Axios.get(`${actualUrl}/${apiRoot}/cities/${id}?q=${query}`).catch(errorCallback));
    }

    function searchEvents(q,p,limit) {
        return check.then(nothing => Axios.get(`${actualUrl}/${apiRoot}/search/events?q=${q}&page=${p}&limit=${limit}`).catch(errorCallback));
    }

    function searchCalendar(q,p,limit) {
        return check.then(nothing => Axios.get(`${actualUrl}/${apiRoot}/search/calendar?q=${q}&page=${p}&limit=${limit}`).catch(errorCallback));
    }

    function getEvent(id) {
        return check.then(nothing => Axios.get(`${actualUrl}/${apiRoot}/events/${id}`));
    }

    function getUpcomingEvent(id) {
        return check.then(nothing => Axios.get(`${actualUrl}/${apiRoot}/calendar/${id}`));
    }

    function suggest(query) {
        return check.then(nothing => Axios.get(`${actualUrl}/${apiRoot}/suggest?q=${query}`).catch(errorCallback));
    }

    function creationEvent(event) {
        if (!event.id) {
            throw new Error('Event does not have an id');
        }
        return check.then(nothing => Axios.post(`${actualUrl}/${apiRoot}/events/`, event).catch(errorCallback));
    }

    function updateEvent(event) {
        if (!event.id) {
            throw new Error('Event does not have an id');
        }
        return check.then(nothing => Axios.put(`${actualUrl}/${apiRoot}/events/${event.id}`, event).catch(errorCallback));
    }

    function deleteEvent(event) {
        return check.then(nothing => Axios.delete(`${actualUrl}/${apiRoot}/events/${event.id}`).catch(errorCallback));
    }

    function connectedUser() {
        return check.then(nothing => Axios.get(`${actualUrl}/auth/connected-user`).catch(errorCallback));
    }

    function meetupInfo(meetupName) {
        return check.then(nothing => Axios.get(`${actualUrl}/${apiRoot}/meetup/${meetupName}`).catch(errorCallback));
    }

    function calendar(page) {
        return check.then(nothing => Axios.get(`${actualUrl}/${apiRoot}/calendar?p=${page}`).catch(errorCallback));
    }

    function addFavourite(typeS,valueS) {
        return check.then(nothing => Axios.post(`${actualUrl}/auth/favourites`, {type: typeS, value: valueS}).catch(errorCallback));
    }

    function getFavourites(type) {
        return check.then(nothing => Axios.get(`${actualUrl}/auth/favourites/${type.toUpperCase()}`).catch(errorCallback));
    }

    function removeFavourite(type,value,filter) {
        if(filter) {
                return check.then(nothing => Axios.delete(`${actualUrl}/auth/favourites/${type}/${value}?filter=${filter}`).catch(errorCallback));
        } else {
                return check.then(nothing => Axios.delete(`${actualUrl}/auth/favourites/${type}/${value}`).catch(errorCallback));
        }

    }

    function deleteMessage(id) {
        return check.then(nothing => Axios.delete(`${actualUrl}/auth/messages/${id}`).catch(errorCallback));
    }

    return {
        useDevUrl() {
            return createClient(DEV_URL);
        },
        useCleverUrl() {
            return createClient(CLEVER_URL);
        },
        cities,
        city,
        getEvent,
        getUpcomingEvent,
        suggest,
        searchEvents,
        searchCalendar,
        creationEvent,
        updateEvent,
        deleteEvent,
        connectedUser,
        meetupInfo,
        calendar,
        auth,
        addFavourite,
        getFavourites,
        removeFavourite,
        deleteMessage
    };
}

export default createClient("");
