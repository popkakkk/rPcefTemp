package phoebe.eqx.pcef.utils;

import com.google.gson.*;

import java.lang.reflect.Type;

public class FieldNamingPolicyDeserializer implements JsonDeserializer<FieldNamingStrategy> {

	@Override
	public FieldNamingStrategy deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		FieldNamingStrategy[] enumFieldNaming = FieldNamingPolicy.values();
		if(null != json){
			for(FieldNamingStrategy obj : enumFieldNaming){
				if(String.valueOf(obj).equals(json.getAsString())){
					return obj;
				}
			}
		}
		return null;
	}

}
