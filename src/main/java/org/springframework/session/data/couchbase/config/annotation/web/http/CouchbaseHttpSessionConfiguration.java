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

import static org.springframework.session.data.couchbase.config.annotation.web.http.CouchbaseSessionDefaults.*;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.springframework.session.data.couchbase.AbstractCouchbaseSessionConverter;
import org.springframework.session.data.couchbase.CouchbaseOperationsSessionRepository;
import org.springframework.session.data.couchbase.JdkCouchbaseSessionConverter;
import org.springframework.util.StringValueResolver;

import com.couchbase.client.java.Bucket;

/**
 * Configuration class registering {@code CouchbaseSessionRepository} bean. To import this
 * configuration use {@link EnableCouchbaseHttpSession} annotation.
 *
 * @author Denis Rosa
 */
@Configuration
public class CouchbaseHttpSessionConfiguration extends SpringHttpSessionConfiguration
		implements BeanClassLoaderAware, EmbeddedValueResolverAware, ImportAware {

	private AbstractCouchbaseSessionConverter couchbaseSessionConverter;
	private Integer maxInactiveIntervalInSeconds;
	private String typeName;
	private String typeValue;
	private boolean keepStringAsLiterals;
	private StringValueResolver embeddedValueResolver;
	private ClassLoader classLoader;

	@Bean
	public CouchbaseOperationsSessionRepository couchbaseSessionRepository(
			Bucket bucket) {

		CouchbaseOperationsSessionRepository repository = new CouchbaseOperationsSessionRepository(
				bucket);
		repository.setMaxInactiveIntervalInSeconds(this.maxInactiveIntervalInSeconds);
		repository.setNameType(this.typeName);
		repository.setValueType(this.typeValue);

		if (this.couchbaseSessionConverter != null) {
			repository.setCouchbaseSessionConverter(this.couchbaseSessionConverter);
		}
		else {
			JdkCouchbaseSessionConverter couchbaseSessionConverter = new JdkCouchbaseSessionConverter(
					this.typeName, this.typeValue, this.maxInactiveIntervalInSeconds,
					this.keepStringAsLiterals);
			repository.setCouchbaseSessionConverter(couchbaseSessionConverter);
		}

		return repository;
	}

	public void setMaxInactiveIntervalInSeconds(Integer maxInactiveIntervalInSeconds) {
		this.maxInactiveIntervalInSeconds = maxInactiveIntervalInSeconds;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public void setTypeValue(String typeValue) {
		this.typeValue = typeValue;
	}

	public void setImportMetadata(AnnotationMetadata importMetadata) {

		AnnotationAttributes attributes = AnnotationAttributes.fromMap(importMetadata
				.getAnnotationAttributes(EnableCouchbaseHttpSession.class.getName()));

		if (attributes != null) {
			this.maxInactiveIntervalInSeconds = attributes
					.getNumber("maxInactiveIntervalInSeconds");
			this.typeName = attributes.getString("typeName");
			this.typeValue = attributes.getString("typeValue");
			this.keepStringAsLiterals = attributes.getBoolean("keepStringAsLiteral");
		}

		if (this.maxInactiveIntervalInSeconds == null) {
			this.maxInactiveIntervalInSeconds = DEFAULT_INACTIVE_INTERVAL;
		}
		if (this.typeName == null) {
			this.typeName = DEFAULT_NAME_TYPE;
		}
		if (this.typeValue == null) {
			this.typeValue = DEFAULT_VALUE_TYPE;
		}

	}

	@Autowired(required = false)
	public void setCouchbaseSessionConverter(
			AbstractCouchbaseSessionConverter cbSessionConverter) {
		this.couchbaseSessionConverter = cbSessionConverter;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

}
