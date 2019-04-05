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

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;

/**
 * Utility class to extract principal name from {@code Authentication} object.
 *
 * @author Jakub Kubrynski
 * @author Greg Turnquist
 */
final class AuthenticationParser {

	private static final String NAME_EXPRESSION = "authentication?.name";

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private AuthenticationParser() {
	}

	/**
	 * Extracts principal name from authentication.
	 *
	 * @param authentication Authentication object
	 * @return principal name
	 */
	@Nullable
	static String extractName(@Nullable Object authentication) {

		if (authentication == null) {
			return null;
		}

		Expression expression = PARSER.parseExpression(NAME_EXPRESSION);

		return expression.getValue(authentication, String.class);
	}
}
