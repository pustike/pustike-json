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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Type Converter.
 */
public final class TypeConverter {
    private static final Map<Class<?>, Class<?>> primitiveToWrapperMap = Map.of(boolean.class, Boolean.class,
        byte.class, Byte.class, char.class, Character.class, double.class, Double.class, float.class, Float.class,
        int.class, Integer.class, long.class, Long.class, short.class, Short.class, void.class, Void.class);
    private final Map<SimpleImmutableEntry<?, ?>, Function<?, ?>> converterCache;
    // The internal cache to store eventType hierarchy, when no external loader is provided.
    private final Map<Class<?>, Set<Class<?>>> typeHierarchyCache;

    /**
     * Constructs Type Converter instance with default converters registered to it.
     */
    public TypeConverter() {
        this.converterCache = new LinkedHashMap<>(64);
        this.typeHierarchyCache = new ConcurrentHashMap<>();
        // register default converters
        addDefaultConverters();
    }

    /**
     * Add an explicitly source/target type specified converter to this registry.
     * @param sourceType the source type to convert from (may be {@code null} if source is {@code null})
     * @param targetType the target type to convert to (required)
     * @param converter  the converter function to convert source type to target type
     * @param <S>        the source type
     * @param <T>        the target type
     */
    @SuppressWarnings("unchecked")
    public <S, T> void addConverter(Class<S> sourceType, Class<T> targetType,
            Function<? super S, ? extends T> converter) {
        Objects.requireNonNull(sourceType);
        Objects.requireNonNull(targetType);
        Objects.requireNonNull(converter);
        sourceType = sourceType.isPrimitive() ? (Class<S>) primitiveToWrapperMap.get(sourceType) : sourceType;
        targetType = targetType.isPrimitive() ? (Class<T>) primitiveToWrapperMap.get(targetType) : targetType;

        SimpleImmutableEntry<?, ?> cacheKey = new SimpleImmutableEntry<>(sourceType, targetType);
        Function<?, ?> previousValue = converterCache.putIfAbsent(cacheKey, converter);
        if (previousValue != null) {
            throw new IllegalStateException("Converter is already registered for: " + cacheKey);
        }
    }

    /**
     * Add a generic source/target type converter to this registry.
     * @param sourceType       the source type to convert from (may be {@code null} if source is {@code null})
     * @param targetType       the target type to convert to (required)
     * @param genericConverter the generic converter function that accepts the target type along with the source
     * @param <S>              the source type
     * @param <T>              the target type
     */
    public <S, T> void addGenericConverter(Class<S> sourceType, Class<T> targetType,
            BiFunction<S, Class<? extends T>, T> genericConverter) {
        addConverter(sourceType, targetType, new BiFunctionWrapper<>(genericConverter));
    }

    /**
     * Return {@code true} if objects of {@code sourceType} can be converted to the {@code targetType}.
     * <p>If this method returns {@code true}, it means {@link #convert(Object, Class)} is capable
     * of converting an instance of {@code sourceType} to {@code targetType}.
     * @param sourceType the source type to convert from (may be {@code null} if source is {@code null})
     * @param targetType the target type to convert to (required)
     * @return {@code true} if conversion can be performed, {@code false} if not
     * @throws NullPointerException if {@code targetType} is {@code null}
     */
    public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
        Objects.requireNonNull(targetType);
        if (sourceType == null) {
            return true;
        }
        sourceType = sourceType.isPrimitive() ? primitiveToWrapperMap.get(sourceType) : sourceType;
        targetType = targetType.isPrimitive() ? primitiveToWrapperMap.get(targetType) : targetType;
        return sourceType.equals(targetType) || findConverter(sourceType, targetType) != null;
    }

    /**
     * Convert the given {@code source} to the specified {@code targetType}.
     * @param source     the source object to convert (may be {@code null})
     * @param targetType the target type to convert to (required)
     * @return the converted object, an instance of targetType
     * @throws RuntimeException if the matching converter is not found
     * @throws NullPointerException if {@code targetType} is {@code null}
     * @param <S>              the source type
     * @param <T>              the target type
     */
    public <S, T> T convert(S source, Class<T> targetType) {
        return doConvert(source, targetType, false);
    }

    /**
     * Convert the given {@code source} to the specified {@code targetType} without exceptions.
     * If the matching converter is not found, it return {@code null}.
     * @param source     the source object to convert (may be {@code null})
     * @param targetType the target type to convert to (required)
     * @return the converted object, an instance of targetType
     * @throws NullPointerException if {@code targetType} is {@code null}
     * @param <S>              the source type
     * @param <T>              the target type
     */
    public <S, T> T convertSafely(S source, Class<T> targetType) {
        return doConvert(source, targetType, true);
    }

    @SuppressWarnings("unchecked")
    private <S, T> T doConvert(S source, Class<T> targetType, boolean ignoreConverterNotFoundError) {
        if (source == null) {
            return null;
        }
        Objects.requireNonNull(targetType);
        targetType = targetType.isPrimitive() ? (Class<T>) primitiveToWrapperMap.get(targetType) : targetType;
        Class<?> sourceType = source.getClass();
        if (sourceType.equals(targetType) || targetType.isAssignableFrom(sourceType)) {
            return (T) source;
        }
        Function<S, T> converter = (Function<S, T>) findConverter(sourceType, targetType);
        if (converter instanceof BiFunctionWrapper) {
            BiFunctionWrapper<S, T> wrapper = (BiFunctionWrapper<S, T>) converter;
            return wrapper.converterFactory.apply(source, targetType);
        } else if (converter != null) {
            return converter.apply(source);
        } else if(!ignoreConverterNotFoundError) {
            SimpleImmutableEntry<?, ?> cacheKey = new SimpleImmutableEntry<>(source.getClass(), targetType);
            throw new RuntimeException("No matching converter is registered for: " + cacheKey);
        }
        return null;
    }

    private Function<?, ?> findConverter(Class<?> sourceType, Class<?> targetType) {// Search the full type hierarchy
        Set<Class<?>> sourceCandidates = typeHierarchyCache.computeIfAbsent(sourceType, this::flattenHierarchy);
        Set<Class<?>> targetCandidates = typeHierarchyCache.computeIfAbsent(targetType, this::flattenHierarchy);
        for (Class<?> sourceCandidate : sourceCandidates) {
            for (Class<?> targetCandidate : targetCandidates) {
                SimpleImmutableEntry<?, ?> cacheKey = new SimpleImmutableEntry<>(sourceCandidate, targetCandidate);
                Function<?, ?> converter = converterCache.get(cacheKey);
                if (converter != null) {
                    return converter;
                }
            }
        }
        return null;
    }

    /**
     * Find all super classes and interfaces for the given concrete class.
     * @param concreteClass the event class
     * @return a list of subscriber methods
     */
    private Set<Class<?>> flattenHierarchy(Class<?> concreteClass) {
        Set<Class<?>> allSuperTypes = new LinkedHashSet<>();
        while (concreteClass != null) {
            allSuperTypes.add(concreteClass);
            for (Class<?> interfaceType : concreteClass.getInterfaces()) {
                allSuperTypes.addAll(flattenHierarchy(interfaceType));
            }
            concreteClass = concreteClass.getSuperclass();
        }
        return Collections.unmodifiableSet(allSuperTypes);
    }

    /**
     * Invalidate all internally cached data.
     */
    public void invalidate() {
        typeHierarchyCache.clear();
        converterCache.clear();
    }

    private static final class BiFunctionWrapper<S, T> implements Function<S, T> {
        private final BiFunction<S, Class<? extends T>, T> converterFactory;

        BiFunctionWrapper(BiFunction<S, Class<? extends T>, T> converterFactory) {
            this.converterFactory = converterFactory;
        }

        @Override
        public T apply(S source) {
            throw new RuntimeException("apply method should be called on BiFunctionWrapper!");
        }
    }

    private void addDefaultConverters() {
        // String <-> Character
        addConverter(String.class, Character.class, this::convertStringToCharacter);
        addConverter(Character.class, String.class, Object::toString);
        // String <-> Boolean
        addConverter(String.class, Boolean.class, Boolean::parseBoolean);
        addConverter(Boolean.class, String.class, Object::toString);
        // String <-> Number
        addConverter(Number.class, String.class, String::valueOf);
        addGenericConverter(String.class, Number.class, NumberUtils::parseNumber);
        addGenericConverter(Number.class, Number.class, NumberUtils::convertNumberToNumber);
        // String <-> Enum
        addGenericConverter(Enum.class, String.class, (anEnum, targetClass) -> anEnum.name());
        addGenericConverter(String.class, Enum.class, this::convertStringToEnum);
        // String <-> LocalDate
        addConverter(LocalDate.class, String.class, LocalDate::toString);
        addConverter(String.class, LocalDate.class, str -> str == null || str.isEmpty() ? null : LocalDate.parse(str));
        // String <-> LocalDateTime
        addConverter(LocalDateTime.class, String.class, LocalDateTime::toString);
        addConverter(String.class, LocalDateTime.class,
                str -> str == null || str.isEmpty() ? null : LocalDateTime.parse(str));
        // String <-> Instant
        addConverter(Instant.class, String.class, Instant::toString);
        addConverter(String.class, Instant.class, str -> str == null || str.isEmpty() ? null : Instant.parse(str));
    }

    private Character convertStringToCharacter(String source) {
        if (source.isEmpty()) {
            return null;
        } else if (source.length() > 1) {
            throw new IllegalArgumentException("Can only convert a [String] with length of 1 to a [Character];" +
                    " string value '" + source + "'  has length of " + source.length());
        }
        return source.charAt(0);
    }

    @SuppressWarnings("unchecked")
    private <T extends Enum> T convertStringToEnum(String text, Class<T> targetType) {
        text = text.trim();
        if (text.length() == 0) {
            return null;
        }
        Class<?> enumType = targetType;
        while (enumType != null && !enumType.isEnum()) {
            enumType = enumType.getSuperclass();
        }
        Objects.requireNonNull(enumType, "The target type " + targetType.getName()
                + " does not refer to an enum");
        return (T) Enum.valueOf((Class<? extends Enum>) enumType, text);
    }
}
