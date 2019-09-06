Pustike JSON   [![][Maven Central img]][Maven Central] [![][Javadocs img]][Javadocs] [![][license img]][license]
============
Contains `ObjectMapper` class that can create Java objects from JSON or create JSON value from Java objects, by using [Jakarta JSON-P](https://github.com/eclipse-ee4j/jsonp/) APIs internally. And it also integrates with [Java JWT](https://github.com/jwtk/jjwt/) library by providing implementations for `Serializer`, `Deserializer` services.

The key feature of this object mapper is it's ability to generate JSON values with only specified list of fields/properties of an object depending on the given context. For ex, when a list of customer objects are being shown in a select field, very few properties like code and name are required. But when editing the the same customer object, many additional properties are needed which can be defined in another context. This helps in reducing the transferred data size when communicating with JSON objects in the API.

Following are some of its features:

* Read JSON text and convert it to the given type.
* Convert an object to `JsonValue`, optionally in the given context to include listed fields only.
* `@JsonIncludes` allows multiple contexts to be defined on a class with each of them listing fields to be included in JSON format.
* A `TypeConverter` utility class to convert objects between given source and target types. 
* JWT `Serializer` implementation using the Object Mapper with JSON-P.
* JWT `Deserializer` implementation using the Object Mapper with JSON-P.

**Dependencies** 

This library requires Java 11 and following modules:

| Group Id        | Artifact Id      | Version |
|-----------------|------------------|---------|
| jakarta.json    | jakarta.json-api | 1.1.5   |
| org.glassfish   | jakarta.json     | 1.1.5   |
| io.jsonwebtoken | jjwt-api         | 0.10.8* |
| io.jsonwebtoken | jjwt-impl        | 0.10.8* |

**Documentation:** Latest javadocs is available [here][Javadocs].

Download
--------
To add a dependency using Maven, use the following:
```xml
<dependency>
    <groupId>io.github.pustike</groupId>
    <artifactId>pustike-json</artifactId>
    <version>0.1.0</version>
</dependency>
```
To add a dependency using Gradle:
```
dependencies {
    compile 'io.github.pustike:pustike-json:0.1.0'
}
```
Or, download the latest JAR(~25kB) from [Maven Central][latest-jar].

License
-------
This library is published under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)

[Maven Central]:https://maven-badges.herokuapp.com/maven-central/io.github.pustike/pustike-json
[Maven Central img]:https://maven-badges.herokuapp.com/maven-central/io.github.pustike/pustike-json/badge.svg
[latest-jar]:https://search.maven.org/remote_content?g=io.github.pustike&a=pustike-json&v=LATEST

[Javadocs]:https://javadoc.io/doc/io.github.pustike/pustike-json
[Javadocs img]:https://javadoc.io/badge/io.github.pustike/pustike-json.svg

[license]:LICENSE
[license img]:https://img.shields.io/badge/license-Apache%202-blue.svg
