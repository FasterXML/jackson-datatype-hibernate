package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;

public class BuilderUtil {
	public static void setConfig(BeanSerializerBuilder builder,
			SerializationConfig config) {
		builder.setConfig(config);
	}
}
