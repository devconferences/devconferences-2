package org.devconferences.elastic;

import com.google.gson.*;
import org.elasticsearch.common.geo.GeoPoint;

import java.lang.reflect.Type;

/** Gson adapter to deserialize an ES GeoPoint field.
 *
 * Use this to deserialize a ES GeoPoint object (if you don't, Gson may throw an JsonParseException) :
 * <pre>
 *  new GsonBuilder().registerTypeAdapter(GeoPoint.class, new GeoPointAdapter()).create()
 * </pre>
 *
 * instead of :
 * <pre>
 *  new Gson()
 * </pre>
 *
 * It supports GeoPoint fields as Object, String, Geohash or Array.
 *
 */

public class GeoPointAdapter implements JsonDeserializer<GeoPoint> {
    @Override
    public GeoPoint deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if(json instanceof JsonPrimitive) { // String : "lat,lon" or "hashcode"
            return new GeoPoint(json.getAsJsonPrimitive().getAsString());
        } else if (json instanceof JsonObject) { // Object : {"lat": ???, "lon": ???}
            JsonObject jsonObject = json.getAsJsonObject();
            return new GeoPoint(jsonObject.get("lat").getAsDouble(), jsonObject.get("lat").getAsDouble());
        } else if (json instanceof JsonArray) { // Array : [lon,lat]
            JsonArray jsonArray = json.getAsJsonArray();
            return new GeoPoint(jsonArray.get(1).getAsDouble(), jsonArray.get(0).getAsDouble());
        } else {
            throw new JsonParseException("Unable to deserialize GeoPoint field");
        }
    }
}