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

package org.eclipse.yasson.adapters;

import static java.util.Collections.unmodifiableMap;

import static org.eclipse.yasson.Jsonbs.defaultJsonb;
import static org.eclipse.yasson.Jsonbs.testWithJsonbBuilderNewBuilder;
import static org.eclipse.yasson.Jsonbs.testWithJsonbBuilderCreate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.yasson.TestTypeToken;
import org.eclipse.yasson.adapters.model.AdaptedPojo;
import org.eclipse.yasson.adapters.model.Author;
import org.eclipse.yasson.adapters.model.Box;
import org.eclipse.yasson.adapters.model.BoxToCrateCompatibleGenericsAdapter;
import org.eclipse.yasson.adapters.model.BoxToCratePropagatedIntegerStringAdapter;
import org.eclipse.yasson.adapters.model.Crate;
import org.eclipse.yasson.adapters.model.GenericBox;
import org.eclipse.yasson.adapters.model.IntegerListToStringAdapter;
import org.eclipse.yasson.adapters.model.JsonObjectPojo;
import org.eclipse.yasson.adapters.model.NumberAdapter;
import org.eclipse.yasson.adapters.model.ReturnNullAdapter;
import org.eclipse.yasson.adapters.model.SupertypeAdapterPojo;
import org.eclipse.yasson.adapters.model.UUIDContainer;
import org.eclipse.yasson.adapters.model.Vegetables;
import org.eclipse.yasson.defaultmapping.generics.model.ScalarValueWrapper;
import org.junit.jupiter.api.Test;

import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.adapter.JsonbAdapter;

/**
 * Tests adapters to behave correctly.
 *
 * @author Roman Grigoriadi
 */
public class AdaptersTest {

    public static class NonGenericPojo {
        public String strValues;
        public Box box;
    }

    @Test
    public void testBoxToCrateNoGenerics() {
        JsonbAdapter<?, ?>[] adapters = {
                new JsonbAdapter<Box, Crate>() {
                    @Override
                    public Crate adaptToJson(Box box) {
                        final Crate crate = new Crate();
                        crate.setCrateStrField("crateAdapted" + box.getBoxStrField());
                        crate.setCrateIntField(box.getBoxIntegerField() + 1);
                        return crate;
                    }

                    @Override
                    public Box adaptFromJson(Crate crate) {
                        Box box = new Box();
                        box.setBoxStrField("boxAdapted" + crate.getCrateStrField());
                        box.setBoxIntegerField(crate.getCrateIntField() + 1);
                        return box;
                    }
                }
        };
        testWithJsonbBuilderCreate(new JsonbConfig().setProperty(JsonbConfig.ADAPTERS, adapters), jsonb -> {
            AdaptedPojo<?> pojo = new AdaptedPojo<>();
            Box box = new Box();
            box.setBoxStrField("BoxStr");
            box.setBoxIntegerField(10);
            pojo.box = box;
            String json = jsonb.toJson(pojo);
            assertEquals("{\"box\":{\"crateIntField\":11,\"crateStrField\":\"crateAdaptedBoxStr\"}}", json);

            AdaptedPojo<?> result = jsonb.fromJson("{\"box\":{\"crateIntField\":10,\"crateStrField\":\"CrateStr\"}}", AdaptedPojo.class);
            assertEquals(Integer.valueOf(11), result.box.getBoxIntegerField());
            assertEquals("boxAdaptedCrateStr", result.box.getBoxStrField());
        });
    }

    @Test
    public void testValueFieldAdapter() {
        JsonbAdapter<?, ?>[] adapters = {
                new JsonbAdapter<Integer, String>() {
                    @Override
                    public String adaptToJson(Integer integer) {
                        return String.valueOf(integer);
                    }

                    @Override
                    public Integer adaptFromJson(String s) {
                        return Integer.valueOf(s);
                    }
                }
        };
        testWithJsonbBuilderCreate(new JsonbConfig().setProperty(JsonbConfig.ADAPTERS, adapters), jsonb -> {

            AdaptedPojo<?> pojo = new AdaptedPojo<>();
            pojo.intField = 11;
            String json = jsonb.toJson(pojo);
            assertEquals("{\"intField\":\"11\"}", json);

            AdaptedPojo<?> result = jsonb.fromJson("{\"intField\":\"10\"}", AdaptedPojo.class);
            assertEquals(Integer.valueOf(10), result.intField);
        });
    }

    @Test
    public void testGenericAdapter() {
        JsonbAdapter<?, ?>[] adapters = {new BoxToCrateCompatibleGenericsAdapter<Integer>() {
        }};
        testWithJsonbBuilderCreate(new JsonbConfig().setProperty(JsonbConfig.ADAPTERS, adapters), jsonb -> {

            AdaptedPojo<Integer> pojo = new AdaptedPojo<>();
            pojo.strField = "POJO_STRING";
            pojo.intBox = new GenericBox<>("INT_BOX_STR", 11);
            pojo.tBox = new GenericBox<>("T_BOX_STR", 110);

            String marshalledJson = jsonb.toJson(pojo, new TestTypeToken<AdaptedPojo<Integer>>() {
            }.getType());
            assertEquals("{\"intBox\":{\"adaptedT\":11,\"crateStrField\":\"INT_BOX_STR\"}," +
                    "\"strField\":\"POJO_STRING\"," +
                    "\"tBox\":{\"adaptedT\":110,\"crateStrField\":\"T_BOX_STR\"}}", marshalledJson);

            String toUnmarshall = "{\"intBox\":{\"crateStrField\":\"Box3\",\"adaptedT\":33}," +
                    "\"tBox\":{\"crateStrField\":\"tGenBoxCrateStr\",\"adaptedT\":22}," +
                    "\"strField\":\"POJO_STRING\"," +
                    "\"strBox\":{\"strField\":\"strBoxStr\",\"x\":\"44\"}}";
            AdaptedPojo<?> result = jsonb.fromJson(toUnmarshall, new TestTypeToken<AdaptedPojo<Integer>>() {
            }.getType());
            assertEquals("POJO_STRING", result.strField);
            assertEquals("Box3", result.intBox.getStrField());
            assertEquals(33, result.intBox.getX());
            assertEquals("tGenBoxCrateStr", result.tBox.getStrField());
            assertEquals(22, result.tBox.getX());
            assertEquals("strBoxStr", result.strBox.getStrField());
            assertEquals("44", result.strBox.getX());
        });
    }

    @Test
    public void testPropagatedTypeArgs() {
        JsonbAdapter<?, ?>[] adapters = {new BoxToCratePropagatedIntegerStringAdapter()};
        testWithJsonbBuilderCreate(new JsonbConfig().setProperty(JsonbConfig.ADAPTERS, adapters), jsonb -> {

            AdaptedPojo<Integer> pojo = new AdaptedPojo<>();
            pojo.intBox = new GenericBox<>("INT_BOX_STR", 110);
            pojo.tBox = new GenericBox<>("T_BOX_STR", 111);
            pojo.strBox = new GenericBox<>("STR_BOX_STR", "101");

            String marshalledJson = jsonb.toJson(pojo, new TestTypeToken<AdaptedPojo<Integer>>() {
            }.getType());
            assertEquals("{\"intBox\":{\"adaptedT\":{\"x\":[\"110\"]},\"crateStrField\":\"INT_BOX_STR\"}," +
                            "\"strBox\":{\"strField\":\"STR_BOX_STR\",\"x\":\"101\"}," +
                            "\"tBox\":{\"adaptedT\":{\"x\":[\"111\"]},\"crateStrField\":\"T_BOX_STR\"}}",
                    marshalledJson);

            String toUnmarshall = "{\"intBox\":{\"crateStrField\":\"strCrateStr\",\"adaptedT\":{\"strField\":\"crateBoxStrField\",\"x\":[\"77\"]}}," +
                    "\"tBox\":{\"crateStrField\":\"tStrCrateStr\",\"adaptedT\":{\"strField\":\"crateBoxStrField\",\"x\":[\"88\"]}}," +
                    "\"strField\":\"POJO_STRING\"," +
                    "\"strBox\":{\"strField\":\"strBoxStr\",\"x\":\"44\"}}";

            AdaptedPojo<?> result = jsonb.fromJson(toUnmarshall, new TestTypeToken<AdaptedPojo<Integer>>() {
            }.getType());
            assertEquals("POJO_STRING", result.strField);
            assertEquals("strCrateStr", result.intBox.getStrField());
            assertEquals(77, result.intBox.getX());
            assertEquals("tStrCrateStr", result.tBox.getStrField());
            assertEquals(88, result.tBox.getX());
            assertEquals("strBoxStr", result.strBox.getStrField());
            assertEquals("44", result.strBox.getX());
        });
    }

    @Test
    public void testStringToGenericCollectionAdapter() {
        JsonbAdapter<?, ?>[] adapters = {new IntegerListToStringAdapter()};
        testWithJsonbBuilderCreate(new JsonbConfig().setProperty(JsonbConfig.ADAPTERS, adapters), jsonb -> {

            AdaptedPojo<List<Integer>> pojo = new AdaptedPojo<>();
            pojo.tVar = Arrays.asList(11, 22, 33);
            pojo.integerList = Arrays.asList(110, 111, 101);
            String marshalledJson = jsonb.toJson(pojo, new TestTypeToken<AdaptedPojo<List<Integer>>>() {
            }.getType());
            assertEquals("{\"integerList\":\"110#111#101\"," +
                    "\"tVar\":\"11#22#33\"}", marshalledJson);

            String toUnmarshall = "{\"integerList\":\"11#22#33#44\",\"stringList\":[\"first\",\"second\"]," +
                    "\"tVar\":\"110#111#101\"}";

            AdaptedPojo<?> result = jsonb.fromJson(toUnmarshall, new TestTypeToken<AdaptedPojo<List<Integer>>>() {
            }.getType());
            List<Integer> expectedIntegerList = Arrays.asList(11, 22, 33, 44);
            List<String> expectedStringList = Arrays.asList("first", "second");
            List<Integer> expectedTList = Arrays.asList(110, 111, 101);

            assertEquals(expectedIntegerList, result.integerList);
            assertEquals(expectedStringList, result.stringList);
            assertEquals(expectedTList, result.tVar);
        });
    }

    @Test
    public void testAdaptObjectInCollection() {
        JsonbAdapter<?, ?>[] adapters = {new BoxToCrateCompatibleGenericsAdapter<Integer>() {
        }};
        testWithJsonbBuilderCreate(new JsonbConfig().setProperty(JsonbConfig.ADAPTERS, adapters), jsonb -> {

            AdaptedPojo<Integer> pojo = new AdaptedPojo<>();

            pojo.tGenericBoxList = new ArrayList<>();
            pojo.tGenericBoxList.add(new GenericBox<>("GEN_BOX_STR_1", 110));
            pojo.tGenericBoxList.add(new GenericBox<>("GEN_BOX_STR_2", 101));

            String marshalledJson = jsonb.toJson(pojo, new TestTypeToken<AdaptedPojo<Integer>>() {
            }.getType());
            assertEquals(
                    "{\"tGenericBoxList\":[{\"adaptedT\":110,\"crateStrField\":\"GEN_BOX_STR_1\"},{\"adaptedT\":101,\"crateStrField\":\"GEN_BOX_STR_2\"}]}",
                    marshalledJson);

            String toUnmarshall = "{\"integerList\":[11,22,33,44],\"stringList\":[\"first\",\"second\"]," +
                    "\"tGenericBoxList\":[{\"crateStrField\":\"FirstCrate\",\"adaptedT\":11},{\"crateStrField\":\"SecondCrate\",\"adaptedT\":22}]}";

            AdaptedPojo<Integer> result = jsonb.fromJson(toUnmarshall, new TestTypeToken<AdaptedPojo<Integer>>() {
            }.getType());
            assertEquals("FirstCrate", result.tGenericBoxList.get(0).getStrField());
            assertEquals("SecondCrate", result.tGenericBoxList.get(1).getStrField());
            assertEquals(Integer.valueOf(11), result.tGenericBoxList.get(0).getX());
            assertEquals(Integer.valueOf(22), result.tGenericBoxList.get(1).getX());
        });
    }

    @Test
    public void testAdaptTypeIntoCollection() {

        JsonbAdapter<?, ?>[] adapters = {new JsonbAdapter<String, List<Integer>>() {
            @Override
            public List<Integer> adaptToJson(String s) {
                List<Integer> result = new ArrayList<>();
                for (String str : s.split(",")) {
                    result.add(Integer.parseInt(str));
                }
                return result;
            }

            @Override
            public String adaptFromJson(List<Integer> ints) {
                StringBuilder sb = new StringBuilder();
                for (Integer i : ints) {
                    if (!sb.toString().isEmpty()) {
                        sb.append(",");
                    }
                    sb.append(i);
                }
                return sb.toString();
            }
        }
        };
        testWithJsonbBuilderCreate(new JsonbConfig().setProperty(JsonbConfig.ADAPTERS, adapters), jsonb -> {

            String json = "{\"strValues\":[11,22,33]}";
            final NonGenericPojo object = new NonGenericPojo();
            object.strValues = "11,22,33";
            assertEquals(json, jsonb.toJson(object));
            NonGenericPojo pojo = jsonb.fromJson(json, NonGenericPojo.class);
            assertEquals("11,22,33", pojo.strValues);
        });
    }

    @Test
    public void testMarshallGenericField() {
        JsonbAdapter<?, ?>[] adapters = {new BoxToCratePropagatedIntegerStringAdapter()};
        testWithJsonbBuilderCreate(new JsonbConfig().setProperty(JsonbConfig.ADAPTERS, adapters), jsonb -> {

            AdaptedPojo<Integer> adaptedPojo = new AdaptedPojo<>();
            adaptedPojo.tBox = new GenericBox<>("tGenBoxStrField", 22);
            adaptedPojo.intBox = new GenericBox<>("genBoxStrField", 11);
            String json = jsonb.toJson(adaptedPojo, new TestTypeToken<AdaptedPojo<Integer>>() {
            }.getType());
            assertEquals(
                    "{\"intBox\":{\"adaptedT\":{\"x\":[\"11\"]},\"crateStrField\":\"genBoxStrField\"},\"tBox\":{\"adaptedT\":{\"x\":[\"22\"]},\"crateStrField\":\"tGenBoxStrField\"}}",
                    json);

            AdaptedPojo<Integer> unmarshalledAdaptedPojo = jsonb.fromJson(json, new TestTypeToken<AdaptedPojo<Integer>>() {
            }.getType());
            assertEquals("genBoxStrField", unmarshalledAdaptedPojo.intBox.getStrField());
            assertEquals(Integer.valueOf(11), unmarshalledAdaptedPojo.intBox.getX());
        });
    }

    @Test
    public void testTypeVariable() {
        JsonbAdapter<?, ?>[] adapters = {new JsonbAdapter<List<GenericBox<Double>>, BigDecimal>() {
            @Override
            public BigDecimal adaptToJson(List<GenericBox<Double>> genericBoxes) {
                return BigDecimal.valueOf(genericBoxes.get(0).getX());
            }

            @Override
            public List<GenericBox<Double>> adaptFromJson(BigDecimal bigDecimal) {
                List<GenericBox<Double>> list = new ArrayList<>();
                list.add(new GenericBox<>("", bigDecimal.doubleValue()));
                return list;
            }
        }};
        testWithJsonbBuilderCreate(new JsonbConfig().setProperty(JsonbConfig.ADAPTERS, adapters), jsonb -> {

            AdaptedPojo<List<GenericBox<Double>>> intBoxPojo = new AdaptedPojo<>();
            List<GenericBox<Double>> intBoxList = new ArrayList<>();
            intBoxList.add(new GenericBox<>("", 11d));
            intBoxPojo.tVar = intBoxList;

            String json = jsonb.toJson(intBoxPojo, new TestTypeToken<AdaptedPojo<List<GenericBox<Double>>>>() {
            }.getType());
            assertEquals("{\"tVar\":11.0}", json);

            AdaptedPojo<List<GenericBox<Double>>> result = jsonb.fromJson(json, new TestTypeToken<AdaptedPojo<List<GenericBox<Double>>>>() {
            }.getType());
            assertEquals(Double.valueOf(11), result.tVar.get(0).getX());
        });
    }

    @Test
    public void testAdaptRoot() {

        JsonbAdapter<?, ?>[] adapters = {new JsonbAdapter<Box, Crate>() {
            @Override
            public Crate adaptToJson(Box box) {
                return new Crate(box.getBoxStrField(), box.getBoxIntegerField());
            }

            @Override
            public Box adaptFromJson(Crate crate) {
                return new Box(crate.getCrateStrField(), crate.getCrateIntField());
            }
        }};
        testWithJsonbBuilderCreate(new JsonbConfig().setProperty(JsonbConfig.ADAPTERS, adapters), jsonb -> {

            Box pojo = new Box("BOX_STR", 101);
            String marshalledJson = jsonb.toJson(pojo);
            assertEquals("{\"crateIntField\":101,\"crateStrField\":\"BOX_STR\"}", marshalledJson);

            Box result = jsonb.fromJson("{\"crateIntField\":110,\"crateStrField\":\"CRATE_STR\"}", Box.class);
            assertEquals("CRATE_STR", result.getBoxStrField());
            assertEquals(Integer.valueOf(110), result.getBoxIntegerField());
        });
    }

    @Test
    public void testAdaptMapString() {

        JsonbAdapter<?, ?>[] adapters = {new JsonbAdapter<Map<String, Integer>, String>() {
            @Override
            public Map<String, Integer> adaptFromJson(String obj) {
                final HashMap<String, Integer> result = new HashMap<>();
                result.put("fake", 101);
                return result;
            }

            @Override
            public String adaptToJson(Map<String, Integer> obj) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Integer> entry : obj.entrySet()) {
                    if (!sb.toString().isEmpty()) {
                        sb.append("#");
                    }
                    sb.append(entry.getKey()).append("-").append(entry.getValue());
                }
                return sb.toString();
            }
        }};
        testWithJsonbBuilderCreate(new JsonbConfig().setProperty(JsonbConfig.ADAPTERS, adapters), jsonb -> {

            AdaptedPojo<Integer> pojo = new AdaptedPojo<>();
            pojo.stringIntegerMap = new HashMap<>();
            pojo.stringIntegerMap.put("first", 11);
            pojo.stringIntegerMap.put("second", 22);
            pojo.tMap = new HashMap<>(pojo.stringIntegerMap);
            String marshalledJson = jsonb.toJson(pojo, new AdaptedPojo<Integer>() {
            }.getClass());
            assertEquals("{\"stringIntegerMap\":\"first-11#second-22\",\"tMap\":\"first-11#second-22\"}", marshalledJson);

            AdaptedPojo<Integer> result =
                    jsonb.fromJson("{\"stringIntegerMap\":\"fake-value\",\"tMap\":\"fake-value\"}", new TestTypeToken<AdaptedPojo<Integer>>() {
                    }.getType());
            assertEquals(Integer.valueOf(101), result.stringIntegerMap.get("fake"));
            assertEquals(Integer.valueOf(101), result.tMap.get("fake"));
        });
    }

    @Test
    public void testAdaptMapToObject() {
        JsonbAdapter<?, ?>[] adapters = {new JsonbAdapter<Map<String, String>, Crate>() {
            @Override
            public Map<String, String> adaptFromJson(Crate obj) {
                final HashMap<String, String> fake = new HashMap<>();
                fake.put("fake", "11");
                return fake;
            }

            @Override
            public Crate adaptToJson(Map<String, String> obj) {
                final Map.Entry<String, String> next = obj.entrySet().iterator().next();
                return new Crate(next.getKey(), Integer.parseInt(next.getValue()));
            }
        }};
        testWithJsonbBuilderCreate(new JsonbConfig().setProperty(JsonbConfig.ADAPTERS, adapters), jsonb -> {

            AdaptedPojo<String> pojo = new AdaptedPojo<>();
            pojo.tMap = new HashMap<>();
            pojo.tMap.put("first", "101");

            TestTypeToken<AdaptedPojo<String>> typeToken = new TestTypeToken<>() {
			};

            String marshalledJson = jsonb.toJson(pojo, typeToken.getType());
            assertEquals("{\"tMap\":{\"crateIntField\":101,\"crateStrField\":\"first\"}}", marshalledJson);

            AdaptedPojo<String> result = jsonb.fromJson("{\"tMap\":{\"crateIntField\":101,\"crateStrField\":\"first\"}}", typeToken.getType());
            assertEquals("11", result.tMap.get("fake"));
        });
    }

    @Test
    public void testAdaptJsonObject() {
        JsonObjectPojo pojo = new JsonObjectPojo();
        pojo.box = new Box("strFieldValue", 110);

        String json = defaultJsonb.toJson(pojo);
        assertEquals("{\"box\":{\"boxStrField\":\"strFieldValue\",\"boxIntegerField\":110}}", json);

        JsonObjectPojo result = defaultJsonb.fromJson(json, JsonObjectPojo.class);
        assertEquals("strFieldValue", result.box.getBoxStrField());
        assertEquals(Integer.valueOf(110), result.box.getBoxIntegerField());
    }

    @Test
    public void testAdaptAuthor() {
        Author author = new Author();
        author.setFirstName("John");
        author.setLastName("Connor");

        String json = defaultJsonb.toJson(author);
        assertEquals("{\"firstName\":\"J\",\"lastName\":\"Connor\"}", json);

        Author result = defaultJsonb.fromJson("{\"firstName\":\"J\",\"lastName\":\"Connor\"}", Author.class);
        assertEquals("\"J\"", result.getFirstName());
        assertEquals("Connor", result.getLastName());
    }

    @Test
    public void testAdapterReturningNull() {
        testWithJsonbBuilderCreate(new JsonbConfig().withAdapters(new ReturnNullAdapter()).withNullValues(true), jsonb -> {

            ScalarValueWrapper<Number> wrapper = new ScalarValueWrapper<>();
            wrapper.setValue(10);
            Type type = new TestTypeToken<ScalarValueWrapper<Number>>() {
            }.getType();
            String json = jsonb.toJson(wrapper, type);

            assertEquals("{\"value\":null}", json);

            ScalarValueWrapper<Number> result = jsonb.fromJson("{\"value\":null}", type);
            assertNull(result.getValue());
        });
    }

    @Test
    public void testAdaptUUID() {
        UUIDContainer pojo = new UUIDContainer();
        UUID uuid = UUID.fromString("b329da91-0d96-44b6-b466-56c2458b2877");
        pojo.setUuidClsBased(uuid);
        pojo.setUuidIfcBased(uuid);

        String result = defaultJsonb.toJson(pojo);
        assertEquals("{\"uuidClsBased\":\"b329da91-0d96-44b6-b466-56c2458b2877\",\"uuidIfcBased\":\"b329da91-0d96-44b6-b466-56c2458b2877\"}", result);

        UUIDContainer uuidContainer = defaultJsonb.fromJson(result, UUIDContainer.class);
        assertEquals(uuid, uuidContainer.getUuidClsBased());
        assertEquals(uuid, uuidContainer.getUuidIfcBased());
    }

    @Test
    public void testSupertypeAdapter() {
        NumberAdapter.getCounter().resetCount();
        SupertypeAdapterPojo pojo = new SupertypeAdapterPojo();
        pojo.setNumberInteger(10);
        pojo.setSerializableInteger(11);
        assertEquals("{\"numberInteger\":\"11\",\"serializableInteger\":12}", defaultJsonb.toJson(pojo));
        pojo = defaultJsonb.fromJson("{\"numberInteger\":\"11\",\"serializableInteger\":12}", SupertypeAdapterPojo.class);
        assertEquals(Integer.valueOf(10), pojo.getNumberInteger());
        assertEquals(Integer.valueOf(11), pojo.getSerializableInteger());
        //assert that the adapter was used just once
        assertEquals(1, NumberAdapter.getCounter().getCount());
    }
    
    @Test
    public void testSupertypeAdapter_withConfiguration() {
        NumberAdapter.getCounter().resetCount();
        testWithJsonbBuilderCreate(new JsonbConfig().withAdapters(new NumberAdapter()), jsonb -> {
            SupertypeAdapterPojo pojo = new SupertypeAdapterPojo();
            pojo.setNumberInteger(10);
            pojo.setSerializableInteger(11);
            assertEquals("{\"numberInteger\":\"11\",\"serializableInteger\":12}", jsonb.toJson(pojo));
            pojo = jsonb.fromJson("{\"numberInteger\":\"11\",\"serializableInteger\":12}", SupertypeAdapterPojo.class);
            assertEquals(Integer.valueOf(10), pojo.getNumberInteger());
            assertEquals(Integer.valueOf(11), pojo.getSerializableInteger());
            //assert that the adapter was reused
            assertEquals(1, NumberAdapter.getCounter().getCount());
        });
    }

    public static class PropertyTypeMismatch {
        private Throwable error = new RuntimeException("foo");
        
        public Optional<Throwable> getError() {
            return Optional.ofNullable(error);
        }
        
        public void setError(Instant errorTime) {
            this.error = new RuntimeException("Error at: " + errorTime.toString());
        }
    }        
    
    public static class ThrowableAdapter implements JsonbAdapter<Throwable, Map<String, Object>> {

        public int callCount = 0;

        @Override
        public Map<String, Object> adaptToJson(Throwable obj) throws Exception {
            HashMap<String, Object> output = new HashMap<>();
            output.put("message", obj.getMessage());
            output.put("type", obj.getClass().getName());
            callCount++;

            return unmodifiableMap(output);
        }

        @Override
        public Throwable adaptFromJson(Map<String, Object> obj) throws Exception {
            throw new UnsupportedOperationException("not implemented");
        }
    }
    
    /**
     * Serialize a class that has mismatching properties. The field is of type
     * Throwable but the getter method is of type Optional<Throwable>. The user-defined
     * adapter for Throwable should still be called.
     */
    @Test
    public void testOptionalAdapter() {
        ThrowableAdapter adapter = new ThrowableAdapter();
        testWithJsonbBuilderNewBuilder(new JsonbConfig().withAdapters(adapter), jsonb -> {

            PropertyTypeMismatch obj = new PropertyTypeMismatch();
            String json = jsonb.toJson(obj);
            assertEquals("{\"error\":{\"message\":\"foo\",\"type\":\"java.lang.RuntimeException\"}}", json);
            assertEquals(1, adapter.callCount, "The user-defined ThrowableAdapter should have been called");
        });
    }
    
    public static class InstantAdapter implements JsonbAdapter<Instant, String> {

        public int callCount = 0;

        @Override
        public String adaptToJson(Instant obj) throws Exception {
            return obj.toString();
        }

        @Override
        public Instant adaptFromJson(String obj) throws Exception {
            callCount++;
            if (obj.equals("CUSTOM_VALUE"))
                return Instant.MAX;
            return Instant.parse(obj);
        }
    }
    
    /**
     * Make sure that the same property can use a different adapter for
     * serialization and deserialization.
     */
    @Test
    public void testDifferentAdapters() {
        ThrowableAdapter throwableAdapter = new ThrowableAdapter();
        InstantAdapter instantAdapter = new InstantAdapter();
        testWithJsonbBuilderNewBuilder(new JsonbConfig().withAdapters(throwableAdapter, instantAdapter), jsonb -> {

            String json = "{\"error\":\"CUSTOM_VALUE\"}";
            PropertyTypeMismatch obj = jsonb.fromJson(json, PropertyTypeMismatch.class);
            assertEquals("Error at: +1000000000-12-31T23:59:59.999999999Z", obj.getError().orElseThrow().getMessage());
            assertEquals(1, instantAdapter.callCount);

            String afterJson = jsonb.toJson(obj);
            assertEquals("{\"error\":{\"message\":\"Error at: +1000000000-12-31T23:59:59.999999999Z\",\"type\":\"java.lang.RuntimeException\"}}",
                    afterJson);
            assertEquals(1, throwableAdapter.callCount);
        });
    }
    
    public static class StringAdapter implements JsonbAdapter<String, String> {
		@Override
		public String adaptToJson(String obj) throws Exception {
			return obj.toUpperCase();
		}

		@Override
		public String adaptFromJson(String obj) throws Exception {
			return obj.toLowerCase();
		}
    }
    
    /**
     * Test for: <a href="https://github.com/eclipse-ee4j/yasson/issues/346">issue 346</a>
     */
    @Test
    public void testAdaptedRootType() {
        testWithJsonbBuilderNewBuilder(new JsonbConfig().withAdapters(new StringAdapter()), jsonb -> {
            String original = "hello world!";
            assertEquals("\"HELLO WORLD!\"", jsonb.toJson(original));
            assertEquals(original, jsonb.fromJson("\"HELLO WORLD!\"", String.class));
        });
    }

    @Test
    public void testCustomAdapterInEnum() {
        Vegetables expected = Vegetables.TOMATO;

        String expectedJson = defaultJsonb.toJson(expected);

        assertEquals(expected, defaultJsonb.fromJson(expectedJson, Vegetables.class));
    }

    @Test
    void testTypeToSameTypeAdapter_shouldntBeStackOverflowError() {
        RuntimeException runtimeException
                = assertThrows(RuntimeException.class, () -> testWithJsonbBuilderNewBuilder(new JsonbConfig().withAdapters(
                    new JsonbAdapter<Box, Box>() {
                        @Override
                        public Box adaptToJson(Box obj) {
                            return obj;
                        }

                        @Override
                        public Box adaptFromJson(Box obj) {
                            return obj;
                        }
                    }),
                jsonb -> jsonb.toJson(new Box())));

        assertInstanceOf(JsonbException.class, runtimeException.getCause());
    }
}
