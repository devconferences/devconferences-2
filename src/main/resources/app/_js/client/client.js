import Axios from "axios";

const CLEVER_URL = "http://devconferences.cleverapps.io";
const DEV_URL = "http://localhost:8080";
const apiRoot = "api/v2";

const errorCallback = response => console.error(`${response.status} - '${response.statusText}'`,response);

function createClient(actualUrl) {

    function cities() {
        return Axios.get(`${actualUrl}/${apiRoot}/cities`).catch(errorCallback);
    }

    function city(id) {
        return Axios.get(`${actualUrl}/${apiRoot}/cities/${id}`).catch(errorCallback);
    }

    function searchEvents(q) {
        return Axios.get(`${actualUrl}/${apiRoot}/events/search?q=${q}`).catch(errorCallback);
    }

    function event(id) {
        return Axios.get(`${actualUrl}/${apiRoot}/events/${id}`).catch(errorCallback);
    }

    function creationEvent(event) {
        if (!event.id) {
            throw new Error('Event does not have an id');
        }
        return Axios.post(`${actualUrl}/${apiRoot}/events/`, event).catch(errorCallback);
    }

    function updateEvent(event) {
        if (!event.id) {
            throw new Error('Event does not have an id');
        }
        return Axios.put(`${actualUrl}/${apiRoot}/events/${event.id}`, event).catch(errorCallback);
    }

    function deleteEvent(event) {
        return Axios.delete(`${actualUrl}/${apiRoot}/events/${event.id}`).catch(errorCallback);
    }

    function connectedUser() {
        return Axios.get(`${actualUrl}/auth/connected-user`).catch(errorCallback);
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
        creationEvent,
        updateEvent,
        deleteEvent,
        connectedUser
    };
}

export default createClient("");

