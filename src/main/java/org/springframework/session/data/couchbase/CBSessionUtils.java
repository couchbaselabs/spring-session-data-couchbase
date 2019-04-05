/*
 * Copyright 2017 the original author or authors.
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

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * @author Denis Rosa
 */
public final class CBSessionUtils {

	@Nullable
	static JsonDocument convertToJsonDoc(
			AbstractCouchbaseSessionConverter couchbaseSessionConverter,
			CouchbaseSession session) {

		return (JsonDocument) couchbaseSessionConverter.convert(session,
				TypeDescriptor.valueOf(CouchbaseSession.class),
				TypeDescriptor.valueOf(JsonDocument.class));
	}

	@Nullable
	static CouchbaseSession convertToSession(
			AbstractCouchbaseSessionConverter couchbaseSessionConverter,
			JsonDocument session) {

		return (CouchbaseSession) couchbaseSessionConverter.convert(session,
				TypeDescriptor.valueOf(JsonDocument.class),
				TypeDescriptor.valueOf(CouchbaseSession.class));
	}

	@Nullable
	static CouchbaseSession convertObjectToSession(
			AbstractCouchbaseSessionConverter couchbaseSessionConverter,
			JsonObject session) {

		return (CouchbaseSession) couchbaseSessionConverter.convert(session,
				TypeDescriptor.valueOf(JsonObject.class),
				TypeDescriptor.valueOf(CouchbaseSession.class));
	}
}
