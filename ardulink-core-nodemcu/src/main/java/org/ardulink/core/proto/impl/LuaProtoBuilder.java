/**
Copyright 2013 project Ardulink http://www.ardulink.org/
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
    http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.ardulink.core.proto.impl;

import static org.ardulink.util.LoadStream.asString;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;

public class LuaProtoBuilder {

	public enum LuaProtocolKey {

		 POWER_PIN_SWITCH("gpio.mode(${PIN},gpio.OUTPUT) gpio.write(${PIN},gpio.${STATE})", new PowerPinSwitchMapper())
		,POWER_PIN_INTENSITY("pwm.setup(${PIN},1000,1023) pwm.start(${PIN}) pwm.setduty(${PIN},${INTENSITY})", new PowerPinIntensityMapper())
		,CUSTOM_MESSAGE("${VALUES}", new CustomMessageMapper())
		,START_LISTENING_DIGITAL("StartListeningDigitalTemplate.snippet", new StartListeningDigitalMapper())
		,STOP_LISTENING_DIGITAL("gpio.mode(${PIN},gpio.OUTPUT)", new StopListeningDigitalMapper())
		 ;

		private String messageTemplate;
		private Mapper mapper;

		private LuaProtocolKey(String snippetName, Mapper mapper) {
			this.messageTemplate = getMessageSnippetIfExists(snippetName);
			this.mapper = mapper;
		}

		private String getMessageSnippetIfExists(String snippet) {
			InputStream is = this.getClass().getResourceAsStream(snippet);
			String retvalue = snippet;
			if(is != null) {
				retvalue = asString(is);
				// Scripts on more than on line cause random error on NodeMCU because its echo
				// We should investigate on ESPlorer code to understand how improve this code.
				// Actually we remove CR and LF sending the script on a single line.
				retvalue = retvalue.replaceAll("\\r", " ");
				retvalue = retvalue.replaceAll("\\n", " ");
				try {
					is.close();
				} catch (IOException e) {}
			}
			return retvalue;
		}

		public String getMessageTemplate() {
			return messageTemplate;
		}

		public Mapper getMapper() {
			return mapper;
		}
	}
	
	public static LuaProtoBuilder getBuilder(LuaProtocolKey key) {
		return new LuaProtoBuilder(key);
	}

	private LuaProtocolKey key;
	private Integer pin;
	private Object[] values;
	
	public LuaProtoBuilder(LuaProtocolKey key) {
		this.key = key;
	}

	public LuaProtoBuilder forPin(int pinNum) {
		this.pin = pinNum;
		return this;
	}

	public LuaProtoBuilder withValue(Object value) {
		return withValues(value);
	}

	public LuaProtoBuilder withValues(Object... values) {
		this.values = values;
		return this;
	}
	
	public String build() {
		Map<String, String> mappedValues = key.getMapper()
				.buildMap(pin, values);
		String retvalue = key.getMessageTemplate();
		for (Entry<String, String> entry : mappedValues.entrySet()) {
			String variableName = "\\$\\{" + entry.getKey() + "\\}";
			retvalue = retvalue.replaceAll(variableName, entry.getValue());
		}
		return retvalue;
	}

}