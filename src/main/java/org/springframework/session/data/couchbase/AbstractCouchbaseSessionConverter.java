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

import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.lang.Nullable;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * Base class for serializing and deserializing session objects. To create custom
 * serializer you have to implement this interface and simply register your class as a
 * bean.
 *
 * @author Denis Rosa
 */
public abstract class AbstractCouchbaseSessionConverter implements GenericConverter {

	public static final String ID = "_id";
	public static final String CREATION_TIME = "_created";
	public static final String LAST_ACCESSED_TIME = "_accessed";
	public static final String MAX_INTERVAL = "_interval";
	public static final String ATTRIBUTES = "_attr";
	public static final String PRINCIPAL_FIELD_NAME = "_principal";
	public static final String EXPIRE_AT_FIELD_NAME = "_expireAt";
	private static final String SPRING_SECURITY_CONTEXT = "SPRING_SECURITY_CONTEXT";

	protected String extractPrincipal(Session expiringSession) {

		String resolvedPrincipal = AuthenticationParser
				.extractName(expiringSession.getAttribute(SPRING_SECURITY_CONTEXT));

		if (resolvedPrincipal != null) {
			return resolvedPrincipal;
		}
		else {
			return expiringSession.getAttribute(
					FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME);
		}
	}

	public Set<ConvertiblePair> getConvertibleTypes() {

		return Collections.singleton(
				new ConvertiblePair(JsonDocument.class, CouchbaseSession.class));
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public Object convert(Object source, TypeDescriptor sourceType,
			TypeDescriptor targetType) {

		if (source == null) {
			return null;
		}

		if (JsonDocument.class.isAssignableFrom(sourceType.getType())) {
			return convert((JsonDocument) source);
		}
		else {
			return convert((CouchbaseSession) source);
		}
	}

	protected abstract JsonDocument convert(CouchbaseSession session);

	protected abstract CouchbaseSession convert(JsonDocument sessionWrapper);

	protected abstract CouchbaseSession convertObject(JsonObject sessionWrapper);
}
