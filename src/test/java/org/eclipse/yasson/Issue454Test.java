/*
 * Copyright (c) 2022, 2024 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

package org.eclipse.yasson;

import static org.eclipse.yasson.Jsonbs.testWithJsonbBuilderCreate;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.annotation.JsonbTransient;

class Issue454Test {

    private Issue454Test() {
    }

    @Test
    public void test() {
        final String EXPECTED = "{\"field2\":\"bbb\"}";
        testWithJsonbBuilderCreate(new JsonbConfig(), jsonb -> {
            assertEquals(EXPECTED, jsonb.toJson(new TheInterface() {

                @Override
                public String getField1() {
                    return "aaa";
                }

                @Override
                public String getField2() {
                    return "bbb";
                }
            }));
            assertEquals(EXPECTED, jsonb.toJson(new TheClass() {
                @Override
                public String getField1() {
                    return "aaa";
                }

                @Override
                public String getField2() {
                    return "bbb";
                }
            }));
            assertEquals(EXPECTED, jsonb.toJson(new TheClass2()));
            assertEquals(EXPECTED, jsonb.toJson(new TheClass2() {
            }));
        });
    }
    
    public static abstract class TheClass {

        private TheClass() {
        }

        @JsonbTransient
        public abstract String getField1();

        public abstract String getField2();
    }

    public static class TheClass2 extends TheClass {

        private TheClass2() {
        }

        @Override
        public String getField1() {
            return "aaa";
        }
        @Override
        public String getField2() {
            return "bbb";
        }
    }
    
    public interface TheInterface {

        @JsonbTransient
        String getField1();

        String getField2();
    }

}
