/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.session.data.couchbase;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.lang.Nullable;
import org.springframework.session.Session;
import org.springframework.util.Assert;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * {@code AbstractCouchbaseSessionConverter} implementation using standard Java
 * serialization.
 *
 * @author Denis Rosa
 */
public class JdkCouchbaseSessionConverter extends AbstractCouchbaseSessionConverter {

	private final Converter<Object, byte[]> serializer;
	private final Converter<byte[], Object> deserializer;
	private String documentValue;
	private String documentTypeName;
	private Integer maxExpirationTime;
	private boolean keepStringAsLiteral;
	private ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

	public JdkCouchbaseSessionConverter(String documentTypeName, String documentValue,
			Integer maxExpirationTime, boolean keepStringAsLiteral) {
		this(new SerializingConverter(), new DeserializingConverter(), documentTypeName,
				documentValue, maxExpirationTime, keepStringAsLiteral);
	}

	public JdkCouchbaseSessionConverter(Converter<Object, byte[]> serializer,
			Converter<byte[], Object> deserializer, String documentTypeName,
			String documentValue, Integer maxExpirationTime,
			boolean keepStringAsLiteral) {
		Assert.notNull(serializer, "serializer cannot be null");
		Assert.notNull(deserializer, "deserializer cannot be null");
		Assert.notNull(documentTypeName, "documentTypeName cannot be null");
		Assert.notNull(documentValue, "documentValue cannot be null");
		Assert.notNull(maxExpirationTime, "maxExpirationTime cannot be null");

		this.serializer = serializer;
		this.deserializer = deserializer;
		this.documentTypeName = documentTypeName;
		this.documentValue = documentValue;
		this.maxExpirationTime = maxExpirationTime;
		this.keepStringAsLiteral = keepStringAsLiteral;
	}

	public static byte[] toPrimitive(Byte[] byteArray) {

		byte[] result = new byte[byteArray.length];
		for (int i = 0; i < byteArray.length; i++) {
			result[i] = byteArray[i].byteValue();
		}
		return result;
	}

	@Override
	protected JsonDocument convert(CouchbaseSession session) {
		JsonObject obj = null;
		try {
			obj = JsonObject.create().put(this.documentTypeName, this.documentValue)
					.put(CREATION_TIME, session.getCreationTime().toEpochMilli())
					.put(LAST_ACCESSED_TIME, session.getLastAccessedTime().toEpochMilli())
					.put(MAX_INTERVAL, session.getMaxInactiveInterval().getSeconds())
					.put(PRINCIPAL_FIELD_NAME, extractPrincipal(session))
					.put(EXPIRE_AT_FIELD_NAME, session.getExpireAt().getTime())
					.put(ATTRIBUTES,
							serializeAttributes(session, this.keepStringAsLiteral));

			if (this.keepStringAsLiteral) {
				extractStringSessionAttributes(session, obj);
			}
		}
		catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new IllegalStateException("Could not serialize the session", e);
		}

		JsonDocument doc = JsonDocument.create(session.getId(), maxExpirationTime, obj);
		return doc;
	}

	@Override
	protected CouchbaseSession convert(JsonDocument sessionWrapper) {

		CouchbaseSession session = convertObject(sessionWrapper.content());
		session.setId(sessionWrapper.id());
		return session;
	}

	@Override
	protected CouchbaseSession convertObject(JsonObject sessionWrapper) {
		Duration maxIntervalDuration = null;

		if (sessionWrapper.getLong(MAX_INTERVAL) != null) {
			maxIntervalDuration = Duration
					.ofSeconds(sessionWrapper.getLong(MAX_INTERVAL));
		}

		CouchbaseSession session = new CouchbaseSession(sessionWrapper.getString(ID),
				maxIntervalDuration.getSeconds());

		session.setCreationTime(sessionWrapper.getLong(CREATION_TIME));
		session.setLastAccessedTime(
				Instant.ofEpochMilli(sessionWrapper.getLong(LAST_ACCESSED_TIME)));
		session.setExpireAt(new Date(sessionWrapper.getLong(EXPIRE_AT_FIELD_NAME)));

		try {
			deserializeAttributes(sessionWrapper.getString(ATTRIBUTES), session);
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}

		if (this.keepStringAsLiteral) {
			populateStringSessionAttributes(sessionWrapper, session);
		}

		return session;
	}

	/**
	 * If keepStringAsLiteral is true, string attributes won't be included in the
	 * serialized atttributes map
	 * @param session
	 * @param keepStringAsLiteral
	 * @return
	 * @throws JsonProcessingException
	 */
	@Nullable
	private String serializeAttributes(Session session, boolean keepStringAsLiteral)
			throws JsonProcessingException {

		Map<String, Object> attributes = new HashMap<>();
		for (String attrName : session.getAttributeNames()) {
			if (!(keepStringAsLiteral
					&& session.getAttribute(attrName) instanceof String)) {
				attributes.put(attrName, session.getAttribute(attrName));
			}
		}
		return mapper.writeValueAsString(this.serializer.convert(attributes));

	}

	@SuppressWarnings("unchecked")
	private void deserializeAttributes(String sessionAttributes, Session session)
			throws IOException {

		Byte[] test2 = mapper.readValue(sessionAttributes, Byte[].class);
		byte[] attributesBytes = toPrimitive(test2);

		Map<String, Object> attributes = (Map<String, Object>) this.deserializer
				.convert(attributesBytes);

		if (attributes != null) {
			for (Map.Entry<String, Object> entry : attributes.entrySet()) {
				session.setAttribute(entry.getKey(), entry.getValue());
			}
		}
	}

	private JsonObject extractStringSessionAttributes(Session session,
			JsonObject jsonObject) {
		for (String attrName : session.getAttributeNames()) {
			if (!attrName.startsWith("_") && !attrName.equals(this.documentTypeName)
					&& session.getAttribute(attrName) instanceof String) {
				jsonObject.put(attrName, (String) session.getAttribute(attrName));
			}
		}

		return jsonObject;
	}

	private Session populateStringSessionAttributes(JsonObject jsonObject,
			Session session) {
		Map<String, Object> map = jsonObject.toMap();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			if (!entry.getKey().startsWith("_")
					&& !entry.getKey().equals(this.documentTypeName)) {
				session.setAttribute(entry.getKey(), entry.getValue());
			}
		}

		return session;
	}
}
