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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Instant;

/**
 * Shared test POJOs used across multiple test classes for {@link ObjectMapper} and {@link TypeConverter}.
 */
public final class TestPojo {

    private TestPojo() {}

    // -- Basic POJOs --

    public static class SimplePojo {
        private String name;
        private int age;
        private boolean active;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    // -- Context-aware POJOs --

    @JsonInclude(type = "select", fields = {"id", "name"})
    @JsonInclude(type = "", fields = {"id", "name", "description"})
    public static class ContextPojo {
        private long id;
        private String name;
        private String description;
        private LocalDateTime createdAt;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    // -- @JsonIgnore POJOs --

    public static class IgnoredPojo {
        private String visible;
        @JsonIgnore
        private String hidden;

        public String getVisible() { return visible; }
        public void setVisible(String visible) { this.visible = visible; }
        public String getHidden() { return hidden; }
        public void setHidden(String hidden) { this.hidden = hidden; }
    }

    @JsonIgnore
    public static class ClassLevelIgnoredPojo {
        private String field1;
        private String field2;

        public String getField1() { return field1; }
        public void setField1(String field1) { this.field1 = field1; }
        public String getField2() { return field2; }
        public void setField2(String field2) { this.field2 = field2; }
    }

    // -- Method-level @JsonIgnore POJO --

    public static class MethodIgnoredPojo {
        private String visible;
        private String hidden;

        public String getVisible() { return visible; }
        public void setVisible(String visible) { this.visible = visible; }

        @JsonIgnore
        public String getHidden() { return hidden; }
        @JsonIgnore
        public void setHidden(String hidden) { this.hidden = hidden; }
    }

    // -- Method-level @JsonProperty POJO --

    public static class MethodMappedPojo {
        private String firstName;
        private String lastName;

        @JsonProperty("first_name")
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        @JsonProperty("last_name")
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
    }

    // -- @JsonProperty POJOs --

    public static class MappedPojo {
        @JsonProperty("first_name")
        private String firstName;
        @JsonProperty("last_name")
        private String lastName;

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
    }

    // -- Nested object POJOs --

    public static class NestedPojo {
        private String name;
        private Inner inner;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Inner getInner() { return inner; }
        public void setInner(Inner inner) { this.inner = inner; }

        public static class Inner {
            private String innerName;

            public String getInnerName() { return innerName; }
            public void setInnerName(String innerName) { this.innerName = innerName; }
        }
    }

    // -- Date/Time POJOs --

    public static class DatePojo {
        private LocalDate date;

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
    }

    public static class TimePojo {
        private LocalTime time;

        public LocalTime getTime() { return time; }
        public void setTime(LocalTime time) { this.time = time; }
    }

    public static class InstantPojo {
        private Instant instant;

        public Instant getInstant() { return instant; }
        public void setInstant(Instant instant) { this.instant = instant; }
    }

    // -- Nesting depth POJOs --

    public static class SelfRefPojo {
        private String name;
        private SelfRefPojo child;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public SelfRefPojo getChild() { return child; }
        public void setChild(SelfRefPojo child) { this.child = child; }
    }

    public static class LevelPojo {
        private String name;
        private LevelPojo child;

        LevelPojo(String name, LevelPojo child) {
            this.name = name;
            this.child = child;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public LevelPojo getChild() { return child; }
        public void setChild(LevelPojo child) { this.child = child; }
    }

    // -- Nested context propagation POJOs --

    @JsonInclude(type = "summary", fields = {"title"})
    @JsonInclude(type = "", fields = {"title", "details"})
    public static class InnerContextPojo {
        private String title;
        private String details;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
    }

    @JsonInclude(type = "display", fields = {"name", "inner@summary"})
    @JsonInclude(type = "full", fields = {"name", "inner"})
    @JsonInclude(type = "compact", fields = {"name", "inner@"})
    public static class OuterPojo {
        private String name;
        private InnerContextPojo inner;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public InnerContextPojo getInner() { return inner; }
        public void setInner(InnerContextPojo inner) { this.inner = inner; }
    }

    // -- Primitive type POJOs --

    public static class PrimitivePojo {
        private boolean boolVal;
        private byte byteVal;
        private short shortVal;
        private int intVal;
        private long longVal;
        private float floatVal;
        private double doubleVal;
        private char charVal;

        public boolean isBoolVal() { return boolVal; }
        public void setBoolVal(boolean boolVal) { this.boolVal = boolVal; }
        public byte getByteVal() { return byteVal; }
        public void setByteVal(byte byteVal) { this.byteVal = byteVal; }
        public short getShortVal() { return shortVal; }
        public void setShortVal(short shortVal) { this.shortVal = shortVal; }
        public int getIntVal() { return intVal; }
        public void setIntVal(int intVal) { this.intVal = intVal; }
        public long getLongVal() { return longVal; }
        public void setLongVal(long longVal) { this.longVal = longVal; }
        public float getFloatVal() { return floatVal; }
        public void setFloatVal(float floatVal) { this.floatVal = floatVal; }
        public double getDoubleVal() { return doubleVal; }
        public void setDoubleVal(double doubleVal) { this.doubleVal = doubleVal; }
        public char getCharVal() { return charVal; }
        public void setCharVal(char charVal) { this.charVal = charVal; }
    }

    public static class WrapperPojo {
        private String name;
        private Integer count;
        private Boolean active;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    // -- Read-only / Write-only --

    public static class ReadOnlyPojo {
        private final String computedValue;

        ReadOnlyPojo(String computedValue) {
            this.computedValue = computedValue;
        }

        public String getComputedValue() { return computedValue; }
    }

    public static class WriteOnlyPojo {
        private String inputValue;

        public void setInputValue(String inputValue) { this.inputValue = inputValue; }
    }

    // -- Inheritance --

    public static class ParentPojo {
        private String parentField;

        public String getParentField() { return parentField; }
        public void setParentField(String parentField) { this.parentField = parentField; }
    }

    public static class ChildPojo extends ParentPojo {
        private String childField;

        public String getChildField() { return childField; }
        public void setChildField(String childField) { this.childField = childField; }
    }

    // -- Enum --

    public enum Status { ACTIVE, INACTIVE }

    public static class EnumPojo {
        private Status status;

        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }
    }

    // -- Empty POJO --

    public static class EmptyPojo {
    }

    // -- Records --

    public record PersonRecord(String name, int age) {}

    public record SensitiveRecord(String name, @JsonIgnore String password) {}
}
