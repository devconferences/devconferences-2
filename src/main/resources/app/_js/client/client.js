import Axios from "axios";

const CLEVER_URL = "http://devconferences.cleverapps.io";
const DEV_URL = "http://localhost:8080";
const apiRoot = "api/v2";

function createClient(actualUrl) {

    function cities() {
        return Axios.get(`${actualUrl}/${apiRoot}/cities`).catch(response => console.error(response));
    }

    return {
        useDevUrl() {
            return createClient(DEV_URL);
        },
        useCleverUrl() {
            return createClient(CLEVER_URL);
        },
        cities
    };
}

export default createClient("");

