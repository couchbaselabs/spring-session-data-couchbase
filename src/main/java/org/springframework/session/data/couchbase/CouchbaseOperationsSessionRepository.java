/*
 * Copyright 2014-2017 the original author or authors.
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

import static org.springframework.session.data.couchbase.CBSessionUtils.*;
import static org.springframework.session.data.couchbase.config.annotation.web.http.CouchbaseSessionDefaults.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.lang.Nullable;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionExpiredEvent;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.couchbase.client.java.query.Select;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.path.OffsetPath;

import lombok.Setter;

/**
 * Session repository implementation which stores sessions in Couchbase. Uses
 * {@link AbstractCouchbaseSessionConverter} to transform session objects from/to native
 * Couchbase representation ({@code JsonDocument}). Repository is also responsible for
 * removing expired sessions from database. Cleanup is done every minute.
 *
 * @author Denis Rosa
 */

public class CouchbaseOperationsSessionRepository
		implements FindByIndexNameSessionRepository<CouchbaseSession>,
		ApplicationEventPublisherAware, InitializingBean {

	private static final Logger logger = LoggerFactory
			.getLogger(CouchbaseOperationsSessionRepository.class);
	private final Bucket bucket;

	@Setter
	private Integer maxInactiveIntervalInSeconds = DEFAULT_INACTIVE_INTERVAL;
	@Setter
	private String nameType = DEFAULT_NAME_TYPE;
	@Setter
	private String valueType = DEFAULT_VALUE_TYPE;

	@Setter
	private AbstractCouchbaseSessionConverter couchbaseSessionConverter = new JdkCouchbaseSessionConverter(
			DEFAULT_NAME_TYPE, DEFAULT_VALUE_TYPE, DEFAULT_INACTIVE_INTERVAL,
			DEFAULT_KEEP_STRING_AS_LITERAL);

	private ApplicationEventPublisher eventPublisher;

	public CouchbaseOperationsSessionRepository(Bucket bucket) {
		this.bucket = bucket;
	}

	@Override
	public CouchbaseSession createSession() {
		CouchbaseSession session = new CouchbaseSession();
		if (this.maxInactiveIntervalInSeconds != null) {
			session.setMaxInactiveInterval(
					Duration.ofSeconds(this.maxInactiveIntervalInSeconds));
		}
		publishEvent(new SessionCreatedEvent(this, session));
		return session;
	}

	@Override
	public void save(CouchbaseSession session) {
		this.bucket.upsert(Assert.requireNonNull(
				convertToJsonDoc(this.couchbaseSessionConverter, session),
				"convertToJsonDoc must not be null!"));
	}

	@Override
	@Nullable
	public CouchbaseSession findById(String id) {

		JsonDocument sessionWrapper = findSession(id);

		if (sessionWrapper == null) {
			return null;
		}

		CouchbaseSession session = convertToSession(this.couchbaseSessionConverter,
				sessionWrapper);
		if (session != null && session.isExpired()) {
			publishEvent(new SessionExpiredEvent(this, session));
			deleteById(id);

			return null;
		}

		return session;
	}

	/**
	 * Currently this repository allows only querying against
	 * {@code PRINCIPAL_NAME_INDEX_NAME}.
	 *
	 * @param indexName the name if the index (i.e.
	 *     {@link FindByIndexNameSessionRepository#PRINCIPAL_NAME_INDEX_NAME})
	 * @param indexValue the value of the index to search for.
	 * @return sessions map
	 */
	@Override
	public Map<String, CouchbaseSession> findByIndexNameAndIndexValue(String indexName,
			String indexValue) {

		return getQuery(this.bucket, indexValue).stream()
				.map(row -> convertObjectToSession(this.couchbaseSessionConverter, row))
				.collect(Collectors.toMap(CouchbaseSession::getId,
						mapSession -> mapSession));
	}

	@Override
	public void deleteById(String id) {

		Optional.ofNullable(findSession(id)).ifPresent(document -> {

			CouchbaseSession session = convertToSession(this.couchbaseSessionConverter,
					document);
			if (session != null) {
				publishEvent(new SessionDeletedEvent(this, session));
			}

			this.bucket.remove(document);
		});
	}

	@Override
	public void afterPropertiesSet() {
		boolean created = bucket.bucketManager().createN1qlIndex("spring_sessions_index",
				Arrays.asList(this.nameType,
						AbstractCouchbaseSessionConverter.PRINCIPAL_FIELD_NAME),
				Expression.x(nameType).eq(valueType), true, false);

		if (created) {
			logger.warn(
					"The index 'spring_sessions_index' does not exist, it will be created automatically");
		}
		else {
			logger.info(
					"The index 'spring_sessions_index' already exist. Whenever you change the document type attribute "
							+ " or the the document type value, this index must be recreated.");
		}
	}

	@Nullable
	private JsonDocument findSession(String id) {
		return this.bucket.get(id);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	private void publishEvent(ApplicationEvent event) {
		try {
			this.eventPublisher.publishEvent(event);
		}
		catch (Throwable ex) {
			logger.error("Error publishing " + event + ".", ex);
		}
	}

	@Nullable
	protected List<JsonObject> getQuery(Bucket bucket, String indexValue) {

		Expression expForType = Expression
				.x(AbstractCouchbaseSessionConverter.PRINCIPAL_FIELD_NAME)
				.eq(Expression.x(indexValue));
		OffsetPath statement = Select.select("  meta().id as id, *")
				.from(Expression.i(bucket.name())).where(expForType);

		N1qlQuery q = N1qlQuery.simple(statement);
		List<N1qlQueryRow> list = bucket.query(q).allRows();

		return list.stream().map(e -> e.value()).collect(Collectors.toList());

	}
}
