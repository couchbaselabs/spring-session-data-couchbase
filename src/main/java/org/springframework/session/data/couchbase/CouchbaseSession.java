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

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import org.springframework.lang.Nullable;
import org.springframework.session.Session;
import org.springframework.session.data.couchbase.config.annotation.web.http.CouchbaseSessionDefaults;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Session object providing additional information about the datetime of expiration.
 *
 * @author Denis Rosa
 */
@EqualsAndHashCode(of = { "id" })
public class CouchbaseSession implements Session {

	@Getter
	@Setter
	private String id;
	private long createdMillis = System.currentTimeMillis();
	private long accessedMillis;
	@Getter
	@Setter
	private long intervalSeconds;
	@Getter
	@Setter
	private Date expireAt;
	private Map<String, Object> attrs = new HashMap<>();

	public CouchbaseSession() {
		this(CouchbaseSessionDefaults.DEFAULT_INACTIVE_INTERVAL);
	}

	public CouchbaseSession(long maxInactiveIntervalInSeconds) {
		this(UUID.randomUUID().toString(), maxInactiveIntervalInSeconds);
	}

	public CouchbaseSession(String id, long maxInactiveIntervalInSeconds) {

		this.id = id;
		this.intervalSeconds = maxInactiveIntervalInSeconds;
		setLastAccessedTime(Instant.ofEpochMilli(this.createdMillis));
	}

	public String changeSessionId() {

		String changedId = UUID.randomUUID().toString();
		this.id = changedId;
		return changedId;
	}

	@Override
	@Nullable
	public <T> T getAttribute(String attributeName) {
		return (T) this.attrs.get(attributeName);
	}

	public Set<String> getAttributeNames() {

		return this.attrs.keySet();
	}

	public void setAttribute(String attributeName, Object attributeValue) {
		if (attributeValue == null) {
			removeAttribute(attributeName);
		}
		else {
			this.attrs.put(attributeName, attributeValue);
		}
	}

	public void removeAttribute(String attributeName) {
		this.attrs.remove(attributeName);
	}

	public Instant getCreationTime() {
		return Instant.ofEpochMilli(this.createdMillis);
	}

	public void setCreationTime(long created) {
		this.createdMillis = created;
	}

	public Instant getLastAccessedTime() {
		return Instant.ofEpochMilli(this.accessedMillis);
	}

	public void setLastAccessedTime(Instant lastAccessedTime) {

		this.accessedMillis = lastAccessedTime.toEpochMilli();
		this.expireAt = Date
				.from(lastAccessedTime.plus(Duration.ofSeconds(this.intervalSeconds)));
	}

	public Duration getMaxInactiveInterval() {
		return Duration.ofSeconds(this.intervalSeconds);
	}

	public void setMaxInactiveInterval(Duration interval) {
		this.intervalSeconds = interval.getSeconds();
	}

	public boolean isExpired() {
		return this.intervalSeconds >= 0 && new Date().after(this.expireAt);
	}
}
