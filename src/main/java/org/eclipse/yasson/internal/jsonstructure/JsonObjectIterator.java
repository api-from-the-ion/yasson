/*
 * Copyright (c) 2019, 2024 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

package org.eclipse.yasson.internal.jsonstructure;

import java.util.Iterator;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.JsonbException;
import jakarta.json.stream.JsonParser;

import org.eclipse.yasson.internal.properties.MessageKeys;
import org.eclipse.yasson.internal.properties.Messages;

/**
 * Iterates over {@link JsonObject} managing internal state.
 */
public class JsonObjectIterator extends JsonStructureIterator {

    /**
     * Location pointer.
     */
    public enum State {
        /**
         * Start of the object.
         */
        START,
        /**
         * Property key name.
         */
        KEY,
        /**
         * Property value.
         */
        VALUE,
        /**
         * End of the object.
         */
        END
    }

    private final JsonObject jsonObject;

    private final Iterator<String> keyIterator;

    private String currentKey;

    private State state = State.START;

    JsonObjectIterator(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
        this.keyIterator = jsonObject.keySet().iterator();
    }

    private void nextKey() {
        if (!keyIterator.hasNext()) {
            throw new JsonbException(Messages.getMessage(MessageKeys.INTERNAL_ERROR, "Object is empty"));
        }
        currentKey = keyIterator.next();
    }

    @Override
    public JsonParser.Event next() {
        switch (state) {
        case START:
            if (keyIterator.hasNext()) {
                nextKey();
                setState(State.KEY);
                return JsonParser.Event.KEY_NAME;
            } else {
                setState(State.END);
                return JsonParser.Event.END_OBJECT;
            }
        case KEY:
            setState(State.VALUE);
            JsonValue value = getValue();
            return getValueEvent(value);
        case VALUE:
            if (keyIterator.hasNext()) {
                nextKey();
                setState(State.KEY);
                return JsonParser.Event.KEY_NAME;
            }
            setState(State.END);
            return JsonParser.Event.END_OBJECT;
        default:
            throw new JsonbException("Illegal state");
        }

    }

    @Override
    public boolean hasNext() {
        //From the perspective of JsonParser not finished until END_OBJECT is being read.
        return state != State.END;
    }

    /**
     * {@link JsonValue} for current key.
     *
     * @return Current JsonValue.
     */
    public JsonValue getValue() {
        if (state == State.START && currentKey == null) {
            return jsonObject;
        }
        return jsonObject.get(currentKey);
    }

    @Override
    String getString() {
        if (state == State.KEY) {
            return currentKey;
        }
        return super.getString();
    }

    @Override
    JsonbException createIncompatibleValueError() {
        return new JsonbException(Messages.getMessage(MessageKeys.NUMBER_INCOMPATIBLE_VALUE_TYPE_OBJECT,
                                                      getValue().getValueType(),
                                                      currentKey));
    }

    private void setState(State state) {
        this.state = state;
    }

    /**
     * Current key this iterator is pointing at.
     *
     * @return Current key.
     */
    public String getKey() {
        return currentKey;
    }
}
