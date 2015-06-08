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

    return {
        useDevUrl() {
            return createClient(DEV_URL);
        },
        useCleverUrl() {
            return createClient(CLEVER_URL);
        },
        cities,
        city,
        searchEvents
    };
}

export default createClient("");

