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

    function cities(query, all) {
        return check.then(nothing => Axios.get(`${actualUrl}/${apiRoot}/cities?q=${query}&all=${all}`).catch(errorCallback));
    }

    function city(id) {
        return check.then(nothing => Axios.get(`${actualUrl}/${apiRoot}/cities/${id}`).catch(errorCallback));
    }

    function searchEvents(q,p,lat,lon,dist,all) {
        return check.then(nothing => Axios.get(`${actualUrl}/${apiRoot}/search/events?q=${q}&p=${p}&lat=${lat}&lon=${lon}&dist=${dist}&all=${all}`).catch(errorCallback));
    }

    function searchCalendar(q,p,lat,lon,dist,all) {
        return check.then(nothing => Axios.get(`${actualUrl}/${apiRoot}/search/calendar?q=${q}&p=${p}&lat=${lat}&lon=${lon}&dist=${dist}&all=${all}`).catch(errorCallback));
    }

    function event(id) {
        return check.then(nothing => Axios.get(`${actualUrl}/${apiRoot}/events/${id}`).catch(errorCallback));
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

    return {
        useDevUrl() {
            return createClient(DEV_URL);
        },
        useCleverUrl() {
            return createClient(CLEVER_URL);
        },
        cities,
        city,
        event,
        searchEvents,
        searchCalendar,
        creationEvent,
        updateEvent,
        deleteEvent,
        connectedUser,
        meetupInfo,
        calendar
    };
}

export default createClient("");
