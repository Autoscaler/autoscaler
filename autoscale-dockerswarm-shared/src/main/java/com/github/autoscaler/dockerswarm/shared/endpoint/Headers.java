/*
 * Copyright 2015-2022 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.autoscaler.dockerswarm.shared.endpoint;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Expands headers supplied in the {@code value}. Variables are permitted as values. <br>
 * <pre>
 * &#64;Headers("Content-Type: application/xml")
 * interface SoapApi {
 * ...
 * &#64;RequestLine(action="GET" request="/")
 * &#64;Headers("Cache-Control: max-age=640000")
 * ...
 *
 * &#64;RequestLine(action="POST" request="/")
 * &#64;Headers({
 *   "X-Foo: Bar",
 *   "X-Ping: {token}"
 * }) void post(&#64;Param("token") String token);
 * ...
 * </pre>
 * <br> <strong>Note:</strong> Headers do not overwrite each other. All headers with the same name will be included in the request.
 * <br><br><b>Relationship to JAXRS</b><br> <br> The following two forms are identical. <br> Feign:
 * <pre>
 * &#64;RequestLine(action="GET" request="/")
 * &#64;Headers({
 *   "X-Ping: {token}"
 * }) void post(&#64;Named("token") String token);
 * ...
 * </pre>
 * <br> JAX-RS:
 * <pre>
 * &#64;POST &#64;Path("/")
 * void post(&#64;HeaderParam("X-Ping") String token);
 * ...
 * </pre>
 */
@Target({METHOD, TYPE})
@Retention(RUNTIME)
public @interface Headers
{

    String[] value();
}
