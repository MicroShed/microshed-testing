/*
 * Copyright (c) 2019 IBM Corporation and others
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.microshed.testing.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Scanner;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import org.microshed.testing.internal.InternalLogger;

@Provider
@Produces({ "*/*" })
@Consumes({ "*/*" })
public class JsonBProvider implements MessageBodyWriter<Object>, MessageBodyReader<Object> {

    private static final Jsonb jsonb = JsonbBuilder.create();
    private static final InternalLogger LOG = InternalLogger.get(JsonBProvider.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public Object readFrom(Class<Object> clazz, Type genericType, Annotation[] annotations,
                           MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        String stringResult = convertStreamToString(entityStream);
        LOG.info("Response from server: " + stringResult);
        return jsonb.fromJson(stringResult, genericType);
    }

    @SuppressWarnings("resource")
    private static String convertStreamToString(java.io.InputStream is) {
        try (Scanner s = new Scanner(is).useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(Object obj, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        String strData = jsonb.toJson(obj);
        LOG.info("Sending data to server: " + strData);
        jsonb.toJson(obj, entityStream);
    }
}