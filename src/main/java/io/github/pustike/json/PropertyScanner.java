/*
 * Copyright (C) 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.pustike.json;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class PropertyScanner {
    private static final MethodType GETTER_TYPE = MethodType.methodType(Object.class, Object.class);
    private static final MethodType SETTER_TYPE = MethodType.methodType(void.class, Object.class, Object.class);

    // --- Core Entry Point ---
    static Map<String, PropertyAccessor> discoverProperties(Class<?> targetClass,
            MethodHandles.Lookup baseLookup) throws IllegalAccessException {
        if (targetClass.isRecord()) {
            return discoverRecordProperties(targetClass, baseLookup);
        }
        Map<String, PropertyBuilder> builders = new LinkedHashMap<>();
        // Walk hierarchy from most-specific subclass up to Object
        for (Class<?> c = targetClass; c != null && c != Object.class; c = c.getSuperclass()) {
            if (c.isAnnotationPresent(JsonIgnore.class)) {
                continue;
            }
            LookupContext ctx = resolveLookupContext(c, baseLookup);
            // 1. Discover methods first (captures overrides, validation logic, and computed getters)
            scanMethods(c, ctx.lookup(), builders);

            // 2. Discover fields second (fills missing getters/setters and exposes un-accessorized state)
            if (ctx.hasPrivateAccess()) {
                scanFields(c, ctx.lookup(), builders);
            }
        }
        return createNamedPropertyAccessorMap(builders);
    }

    private static Map<String, PropertyAccessor> createNamedPropertyAccessorMap(Map<String, PropertyBuilder> builders) {
        Map<String, PropertyAccessor> result = new LinkedHashMap<>(builders.size());
        for (PropertyBuilder b : builders.values()) {
            if (b.ignored || (b.getter == null && b.setter == null)) {
                continue;
            }
            String externalName = b.resolveExternalName();
            Class<?> typeArgument = b.resolveTypeArgument();
            result.put(externalName, new PropertyAccessor(externalName, b.javaName, b.resolveType(), typeArgument,
                    b.getter, b.setter, b.backingField, b.getterMethod, b.setterMethod));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Class<?> findTypeArgument(Type genericType) {
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length > 0) {
                Type typeArgument = typeArguments[0];
                if (typeArgument instanceof Class) {
                    return (Class<?>) typeArgument;
                } else if (typeArgument instanceof ParameterizedType) {
                    return (Class<?>) ((ParameterizedType) typeArgument).getRawType();
                }
            }
            return (Class<?>) parameterizedType.getRawType();
        }
        return (Class<?>) genericType;
    }

    // --- Phase 1: Method Discovery ---
    private static void scanMethods(Class<?> clazz, MethodHandles.Lookup lookup,
            Map<String, PropertyBuilder> builders) {
        for (Method method : clazz.getDeclaredMethods()) {
            int mods = method.getModifiers();
            if (Modifier.isStatic(mods) || method.isBridge() || method.isSynthetic()) {
                continue;
            }
            if (isGetter(method)) {
                String javaName = decapitalize(stripGetterPrefix(method.getName()));
                PropertyBuilder builder = builders.computeIfAbsent(javaName, PropertyBuilder::new);
                if (method.isAnnotationPresent(JsonIgnore.class)) {
                    builder.ignored = true;
                }
                applyPropertyAnnotation(method, builder, true);
                if (builder.getter == null) {
                    try {
                        MethodHandle raw = lookup.unreflect(method);
                        builder.getter = raw.asType(GETTER_TYPE);
                        builder.getterType = method.getReturnType();
                        builder.getterMethod = method;
                    } catch (IllegalAccessException ignored) {
                        // Method is non-public in an un-opened superclass; skip safely
                    }
                }
            } else if (isSetter(method)) {
                String javaName = decapitalize(method.getName().substring(3));
                PropertyBuilder builder = builders.computeIfAbsent(javaName, PropertyBuilder::new);
                if (method.isAnnotationPresent(JsonIgnore.class)) {
                    builder.ignored = true;
                }
                applyPropertyAnnotation(method, builder, false);
                if (builder.setter == null) {
                    try {
                        MethodHandle raw = lookup.unreflect(method);
                        builder.setter = raw.asType(SETTER_TYPE);
                        builder.setterType = method.getParameterTypes()[0];
                        builder.setterMethod = method;
                    } catch (IllegalAccessException ignored) {
                        // Method is non-public in an un-opened superclass; skip safely
                    }
                }
            }
        }
    }

    // --- Phase 2: Field Discovery (Fallback & Enrichment) ---
    private static void scanFields(Class<?> clazz, MethodHandles.Lookup lookup,
            Map<String, PropertyBuilder> builders) throws IllegalAccessException {
        for (Field field : clazz.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (Modifier.isStatic(mods) || field.isSynthetic()) {
                continue;
            }
            String javaName = resolveFieldPropertyName(field, builders);
            PropertyBuilder builder = builders.computeIfAbsent(javaName, PropertyBuilder::new);

            if (field.isAnnotationPresent(JsonIgnore.class)) { // Modifier.isTransient(mods) ||
                builder.ignored = true;
            }
            // Field-level @Property acts as fallback if getter/setter didn't define one
            if (builder.customName == null) {
                applyPropertyAnnotation(field, builder, false);
            }

            // Subclass field shadowing: keep the most specific field
            if (builder.backingField != null) {
                continue;
            }
            builder.backingField = field;
            builder.fieldType = field.getType();

            if (!builder.ignored) {
                if (builder.getter == null) {
                    MethodHandle rawGetter = lookup.unreflectGetter(field);
                    builder.getter = rawGetter.asType(GETTER_TYPE);
                }
                if (builder.setter == null && !Modifier.isFinal(mods)) {
                    MethodHandle rawSetter = lookup.unreflectSetter(field);
                    builder.setter = rawSetter.asType(SETTER_TYPE);
                }
            }
        }
    }

    // --- Record Discovery ---
    private static Map<String, PropertyAccessor> discoverRecordProperties(Class<?> recordClass,
            MethodHandles.Lookup baseLookup) throws IllegalAccessException {
        LookupContext ctx = resolveLookupContext(recordClass, baseLookup);
        RecordComponent[] components = recordClass.getRecordComponents();
        Map<String, PropertyAccessor> result = new LinkedHashMap<>(components.length);
        for (RecordComponent rc : components) {
            Method accessor = rc.getAccessor();
            if (rc.isAnnotationPresent(JsonIgnore.class) || accessor.isAnnotationPresent(JsonIgnore.class)) {
                continue;
            }
            String customName = extractCustomName(accessor);
            if (customName == null) {
                customName = extractCustomName(rc);
            }
            String externalName = (customName != null) ? customName : rc.getName();
            Class<?> typeArgument = findTypeArgument(rc.getGenericType());
            MethodHandle rawGetter = ctx.lookup().unreflect(accessor);
            result.put(externalName, new PropertyAccessor(externalName, rc.getName(), rc.getType(), typeArgument,
                    rawGetter.asType(GETTER_TYPE), null,null, accessor, null));
        }
        return Collections.unmodifiableMap(result);
    }

    // --- Module & Lookup Resolution ---
    private record LookupContext(MethodHandles.Lookup lookup, boolean hasPrivateAccess) {}

    static MethodHandles.Lookup resolveLookup(Class<?> clazz, MethodHandles.Lookup baseLookup) {
        return resolveLookupContext(clazz, baseLookup).lookup();
    }

    private static LookupContext resolveLookupContext(Class<?> clazz, MethodHandles.Lookup baseLookup) {
        try {
            Module callerMod = baseLookup.lookupClass().getModule();
            Module targetMod = clazz.getModule();
            // Bridge module read edge if caller is a named module
            if (callerMod.isNamed() && !callerMod.canRead(targetMod)) {
                callerMod.addReads(targetMod);
            }
            if (baseLookup.lookupClass() == clazz) {
                boolean hasPrivateAccess = (baseLookup.lookupModes() & MethodHandles.Lookup.PRIVATE) != 0;
                return new LookupContext(baseLookup, hasPrivateAccess);
            }
            if ((baseLookup.lookupModes() & MethodHandles.Lookup.MODULE) != 0) {
                MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(clazz, baseLookup);
                return new LookupContext(privateLookup, true);
            }
            // Fallback when baseLookup already had MODULE stripped (e.g., passed from privateLookupIn)
            boolean samePackage = baseLookup.lookupClass().getPackageName().equals(clazz.getPackageName())
                    && callerMod == targetMod;
            boolean hasPrivateAccess = samePackage && (baseLookup.lookupModes() & MethodHandles.Lookup.PRIVATE) != 0;
            return new LookupContext(baseLookup, hasPrivateAccess);
        } catch (IllegalAccessException e) {
            // Superclass resides in an un-opened module; restrict to public API methods
            return new LookupContext(MethodHandles.publicLookup(), false);
        }
    }

    // --- Naming & Method Predicates ---
    private static String resolveFieldPropertyName(Field field, Map<String, PropertyBuilder> builders) {
        String rawName = field.getName();
        Class<?> type = field.getType();
        if ((type == boolean.class || type == Boolean.class)
                && rawName.startsWith("is")
                && rawName.length() > 2
                && Character.isUpperCase(rawName.charAt(2))) {
            String stripped = decapitalize(rawName.substring(2));
            if (builders.containsKey(stripped)) {
                return stripped;
            }
        }
        return rawName;
    }

    private static String extractCustomName(AnnotatedElement element) {
        JsonProperty prop = element.getAnnotation(JsonProperty.class);
        return (prop != null && !prop.value().isEmpty()) ? prop.value() : null;
    }

    private static void applyPropertyAnnotation(AnnotatedElement element, PropertyBuilder builder, boolean override) {
        String name = extractCustomName(element);
        if (name != null && (override || builder.customName == null)) {
            builder.customName = name;
        }
    }

    private static boolean isGetter(Method m) {
        if (m.getParameterCount() != 0 || m.getReturnType() == void.class) {
            return false;
        }
        String name = m.getName();
        if (name.equals("getClass")) {
            return false;
        }
        if (name.startsWith("get") && name.length() > 3) {
            return true;
        }
        if (name.startsWith("is") && name.length() > 2) {
            Class<?> ret = m.getReturnType();
            return ret == boolean.class || ret == Boolean.class;
        }
        return false;
    }

    private static boolean isSetter(Method m) {
        return m.getParameterCount() == 1 && m.getName().startsWith("set") && m.getName().length() > 3;
    }

    private static String stripGetterPrefix(String name) {
        return name.startsWith("is") ? name.substring(2) : name.substring(3);
    }

    private static String decapitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) && Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        char[] chars = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    // --- Internal Builder ---
    private static final class PropertyBuilder {
        final String javaName;
        String customName;
        boolean ignored;
        MethodHandle getter;
        MethodHandle setter;
        Field backingField;
        Method getterMethod;
        Method setterMethod;
        Class<?> getterType;
        Class<?> setterType;
        Class<?> fieldType;

        PropertyBuilder(String javaName) {
            this.javaName = javaName;
        }

        String resolveExternalName() {
            return (customName != null && !customName.isEmpty()) ? customName : javaName;
        }

        Class<?> resolveType() {
            if (getterType != null) return getterType;
            if (fieldType != null) return fieldType;
            return setterType;
        }

        public Class<?> resolveTypeArgument() {
            if (backingField != null) {
                return findTypeArgument(backingField.getGenericType());
            } else if (getterMethod != null) {
                return findTypeArgument(getterMethod.getGenericReturnType());
            } else if (setterMethod != null) {
                return findTypeArgument(setterMethod.getGenericParameterTypes()[0]);
            }
            return resolveType();
        }
    }
}