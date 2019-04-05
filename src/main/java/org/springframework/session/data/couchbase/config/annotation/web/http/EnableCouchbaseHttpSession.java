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
package org.springframework.session.data.couchbase.config.annotation.web.http;

import java.lang.annotation.*;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Add this annotation to a {@code @Configuration} class to expose the
 * SessionRepositoryFilter as a bean named "springSessionRepositoryFilter" and backed by
 * Couchbase. Use {@code typeName} to change default name of the collection used to store
 * sessions.
 *
 * @author Denis Rosa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(CouchbaseHttpSessionConfiguration.class)
@Configuration
public @interface EnableCouchbaseHttpSession {

	/**
	 * The maximum time a session will be kept if it is inactive.
	 *
	 * @return default max inactive interval in seconds
	 */
	int maxInactiveIntervalInSeconds() default CouchbaseSessionDefaults.DEFAULT_INACTIVE_INTERVAL;

	/**
	 * The name of the type attribute to use.
	 *
	 * @return name of the type attribute
	 */
	String typeName() default CouchbaseSessionDefaults.DEFAULT_NAME_TYPE;

	/**
	 * The value of the type attribute to differentiate this document from others.
	 *
	 * @return value of the type attribute
	 */
	String typeValue() default CouchbaseSessionDefaults.DEFAULT_VALUE_TYPE;

	/**
	 * If the attribute in the session map is a String, it will be saved as a standard
	 * document attribute is in Couchbase This is useful if you want to query the session
	 * data directly from the database
	 *
	 * @return value of the keepStringAsLiteral
	 */
	boolean keepStringAsLiteral() default CouchbaseSessionDefaults.DEFAULT_KEEP_STRING_AS_LITERAL;
}
