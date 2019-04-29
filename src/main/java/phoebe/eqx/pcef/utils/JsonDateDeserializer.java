package phoebe.eqx.pcef.utils;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Date;

/**
 * To support Json Date Formatting
 */
public class JsonDateDeserializer implements JsonSerializer<Date>, JsonDeserializer<Date> {
    @Override
    public Date deserialize(JsonElement json, Type typeOfSource, JsonDeserializationContext context) throws JsonParseException {
        if(null != json){
            Date d = new Date(json.getAsLong());
            return d;
        }
        return null;
    }

    @Override
    public JsonElement serialize(Date source, Type typeOfSource, JsonSerializationContext context) {
        if(null != source){
            return new JsonPrimitive(source.getTime());
        }
        return null;
    }
}