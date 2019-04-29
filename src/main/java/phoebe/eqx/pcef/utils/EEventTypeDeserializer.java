package phoebe.eqx.pcef.utils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import ec02.data.enums.EEventType;
import ec02.data.enums.EEventTypeDefault;

import java.lang.reflect.Type;

public class EEventTypeDeserializer implements JsonDeserializer<EEventType> {
	@Override
	public EEventType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		EEventType[] eventTypeList = EEventTypeDefault.values();
		//final JsonObject root = json.getAsJsonObject();
		//final JsonElement field = root.get("eventType");
		if(null != json){
			for(EEventType eventType : eventTypeList){
				if(String.valueOf(eventType).equals(json.getAsString())){
					return eventType;
				}
			}
		}
		return null;
	} 
}