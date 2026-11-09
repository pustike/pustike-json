Pustike JSON   [![][Maven Central img]][Maven Central] [![][Javadocs img]][Javadocs] [![][license img]][license]
============

A lightweight JSON object mapper built on [Jakarta JSON-P](https://github.com/jakartaee/jsonp-api) that provides bidirectional conversion between Java objects and JSON with **context-aware field filtering** — the ability to serialize different subsets of an object's properties depending on the usage context.

The library requires **Java 21+** and a single runtime dependency: `jakarta.json-api`.

**Documentation:** Latest Javadocs is available [here][Javadocs].

## Quick Start

Add the dependency to your project:

```xml
<dependency>
    <groupId>io.github.pustike</groupId>
    <artifactId>pustike-json</artifactId>
    <version>0.6.0</version>
</dependency>
```

Or, download the latest JAR (~33 kB) from [Maven Central][latest-jar].

## Core Concepts

### ObjectMapper

The `ObjectMapper` class is the main entry point for all JSON operations. It is **thread-safe** and can be safely shared across threads.

```java
ObjectMapper mapper = new ObjectMapper();
```

### Writing Java Objects to JSON

Convert any Java object to a `JsonValue` (Jakarta JSON-P):

```java
Customer customer = new Customer("C001", "Acme Corp", "acme@example.com");
JsonValue json = mapper.toJsonValue(customer);
// {"code":"C001","name":"Acme Corp","email":"acme@example.com"}
```

Null field values are silently omitted from the output.

### Reading JSON to Java Objects

Parse a JSON string into a strongly-typed Java object:

```java
String json = """
    {"code":"C001","name":"Acme Corp","email":"acme@example.com"}
    """;
Customer customer = mapper.readValue(json, Customer.class);
```

The mapper also accepts `Reader` and `byte[]` inputs:

```java
Customer customer = mapper.readValue(reader, Customer.class);
Customer customer = mapper.readValue(bytes, Customer.class);
```

A `JsonObject` can be read as a `Map`:

```java
Map<String, Object> map = mapper.readValue(json, Map.class);
```

JSON arrays are mapped to `List`, `Set`, or arrays:

```java
List<Customer> customers = mapper.readValue(jsonArray, List.class);
```

Unknown JSON fields are silently ignored during deserialization.

## Context-Aware Field Filtering

The key feature of Pustike JSON is **context-aware serialization**. Using the `@JsonInclude` annotation, you declare which fields to include for a given context — for example, a "select" context might only need `code` and `name`, while an "edit" context needs all fields.

### Defining Contexts

```java
@JsonInclude(type = "select", fields = {"code", "name"})
@JsonInclude(type = "edit", fields = {"code", "name", "email", "address"})
class Customer {
    private String code;
    private String name;
    private String email;
    private String address;
    // ...
}
```

### Serializing with a Context

```java
Customer customer = new Customer("C001", "Acme Corp", "acme@example.com", "123 Main St");

// Only code and name
JsonValue selectJson = mapper.toJsonValue(customer, "select");
// {"code":"C001","name":"Acme Corp"}

// All four fields
JsonValue editJson = mapper.toJsonValue(customer, "edit");
// {"code":"C001","name":"Acme Corp","email":"acme@example.com","address":"123 Main St"}
```

### Default Context

A context with an empty type (`""`) acts as the default fallback when no matching context is found:

```java
@JsonInclude(fields = {"code", "name"})  // default context (type = "")
class Customer { /* ... */ }
```

### Nested Context Propagation

Use the `fieldName@context` syntax to propagate a nested context to related objects:

```java
@JsonInclude(type = "summary", fields = {"code", "name", "manager@select"})
class Department {
    private String code;
    private String name;
    private Employee manager;
    // ...
}
```

When serializing a `Department` with context `"summary"`, the `manager` field will be serialized using the `"select"` context defined on the `Employee` class.

## Annotations

### @JsonProperty

Map a Java field to a different JSON property name:

```java
class Customer {
    @JsonProperty("customerCode")
    private String code;

    @JsonProperty("customerName")
    private String name;
    // ...
}
```

Serialization output:
```json
{"customerCode":"C001","customerName":"Acme Corp"}
```

The mapping works in both directions — deserialization also recognizes the custom property names.

### @JsonIgnore

Exclude a field from serialization and deserialization:

```java
class Customer {
    private String code;
    private String name;

    @JsonIgnore
    private String internalNotes;  // excluded from JSON
}
```

Applied at the **class level**, it ignores all fields unless explicitly included via `@JsonInclude`:

```java
@JsonIgnore
@JsonInclude(type = "select", fields = {"code", "name"})
class Customer {
    private String code;
    private String name;
    private String email;  // excluded unless context "select" is used
    private String address;
}
```

## TypeConverter

The `TypeConverter` utility converts objects between source and target types. It comes with built-in converters for common types and supports custom converters.

### Built-in Conversions

| Source | Target |
|--------|--------|
| String | Character, Boolean, Number (Integer, Long, Double, etc.) |
| String | Enum, LocalDate, LocalTime, LocalDateTime, Instant |
| Number | String, other Number types |
| Enum | String |
| Boolean | String |

```java
TypeConverter converter = new TypeConverter();

Integer num = converter.convert("42", Integer.class);       // 42
String text = converter.convert(42, String.class);           // "42"
Boolean flag = converter.convert("true", Boolean.class);     // true
LocalDate date = converter.convert("2025-01-15", LocalDate.class);
```

Use `convertSafely` to return `null` instead of throwing when no converter is found:

```java
Object result = converter.convertSafely("test", UnknownType.class);  // null
```

### Custom Converters

Register a custom converter for specific types:

```java
converter.addConverter(MySource.class, MyTarget.class, source -> {
    // conversion logic
    return new MyTarget(source.getValue());
});
```

Register a generic converter that receives the target type at runtime:

```java
converter.addGenericConverter(String.class, MyEnum.class, (text, targetClass) -> {
    return MyEnum.valueOf(text);
});
```

### Using TypeConverter with ObjectMapper

Pass a `TypeConverter` to the `ObjectMapper` constructor so custom types are converted during serialization:

```java
ObjectMapper mapper = new ObjectMapper(new TypeConverter());
```

## Integration with Jakarta JSON-P

Since Pustike JSON produces `JsonValue` instances from the Jakarta JSON-P API, it integrates seamlessly with any library that accepts JSON-P types. For example, it can be used as a custom serializer/deserializer for JWT libraries like jjwt:

```java
ObjectMapper objectMapper = new ObjectMapper(new TypeConverter());

// Serialize Map to JSON-P
Serializer<Map<String, ?>> serializer = new AbstractSerializer<>() {
    @Override
    protected void doSerialize(Map<String, ?> map, OutputStream out) {
        out.write(objectMapper.toJsonValue(map).toString().getBytes(StandardCharsets.UTF_8));
    }
};

// Deserialize JSON-P to Map
Deserializer<Map<String, ?>> deserializer = new AbstractDeserializer<>() {
    @Override
    protected Map<String, ?> doDeserialize(Reader reader) {
        return objectMapper.readValue(reader, Object.class);
    }
};
```

## Configuration

### Nesting Depth Protection

The mapper enforces a maximum nesting depth of **15 levels** to prevent stack overflow from deeply nested or circular references. Exceeding this limit throws a `JsonException`.

### Cache Management

The mapper caches field metadata for performance. If classes are dynamically modified at runtime (e.g., bytecode manipulation), clear the cache:

```java
ObjectMapper.clearCache();
```

## Dependencies

This library requires **Java 21+** and the following runtime dependency:

| Group Id        | Artifact Id      | Version |
|-----------------|------------------|---------|
| jakarta.json    | jakarta.json-api | 2.1.3   |

## License

This library is published under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)

[Maven Central]:https://maven-badges.herokuapp.com/maven-central/io.github.pustike/pustike-json
[Maven Central img]:https://maven-badges.herokuapp.com/maven-central/io.github.pustike/pustike-json/badge.svg
[latest-jar]:https://search.maven.org/remote_content?g=io.github.pustike&a=pustike-json&v=LATEST

[Javadocs]:https://javadoc.io/doc/io.github.pustike/pustike-json
[Javadocs img]:https://javadoc.io/badge/io.github.pustike/pustike-json.svg

[license]:LICENSE
[license img]:https://img.shields.io/badge/license-Apache%202-blue.svg
