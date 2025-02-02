/*
 * Copyright (c) 2016, 2024 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

package org.eclipse.yasson.internal.components;

import java.lang.reflect.Type;

import jakarta.json.bind.serializer.JsonbDeserializer;

/**
 * Component containing deserializer.
 *
 * @param <T> type of contained deserializer
 */
public class DeserializerBinding<T> extends AbstractComponentBinding<JsonbDeserializer<T>> {

    /**
     * Creates a new instance.
     *
     * @param bindingType       Binding type.
     * @param jsonbDeserializer Deserializer.
     */
    public DeserializerBinding(Type bindingType, JsonbDeserializer<T> jsonbDeserializer) {
        super(bindingType, jsonbDeserializer);
    }
}
