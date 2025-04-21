# The gRPC Proto Tool Specification

## Table of Contents

1. [Authentication](#authentication)  
   1.1 [Authentication Mechanisms](#authentication-mechanisms)  
   1.2 [Authentication Modes Supported](#authentication-modes-supported)
2. [Protobuf Data Types](#protobuf-data-types)  
   2.1 [Primitive & Wrapper Types](#primitive--wrapper-types)  
   2.2 [Complex Types](#complex-types)
3. [Java Code Generation Options](#java-code-generation-options)
4. [Notes & Limitations](#notes--limitations)

---
## 1. Authentication
**gRPC Authentication** refers to the mechanisms used to securely validate and authorize client-server communications over the gRPC protocol. In this tool, we support **TLS** for encrypted transmission and allow the use of custom headers (e.g., `Authorization`) to carry credentials or tokens. Other authentication mechanisms are not currently supported.
## 2. Protobuf Data Types

### 2.1 Primitive & Wrapper Types

The table below outlines the supported Protobuf scalar, wrapper, and well-known types in the tool, including their array (repeated) support.

| Protobuf Type                 | Maps To | Supported | Array (repeated) |
|-------------------------------|---------|-----------|------------------|
| `google.protobuf.DoubleValue` | `double`| ✅ Yes    | ✅ Yes           |
| `google.protobuf.FloatValue`  | `float` | ✅ Yes    | ✅ Yes           |
| `google.protobuf.Int64Value`  | `int64` | ✅ Yes    | ✅ Yes           |
| `google.protobuf.Int32Value`  | `int32` | ✅ Yes    | ✅ Yes           |
| `google.protobuf.BoolValue`   | `bool`  | ✅ Yes    | ✅ Yes           |
| `google.protobuf.StringValue` | `string`| ✅ Yes    | ✅ Yes           |
| `google.protobuf.BytesValue`  | `bytes` | ✅ Yes    | ✅ Yes           |

**Key Notes**
- All supported primitive types also support repeated (array) usage.
- Wrapper types (like `google.protobuf.StringValue`) are also supported as arrays.
- Unsigned types (`UInt32`, `UInt64`) and dynamic types (`Struct`, `Any`, `Empty`) are **not** supported.
- For timestamps or durations, manual handling or custom message definitions are required.

### 2.2 Complex Types

Supported Complex type can be found here,
#### Top-Level Message Definitions

```proto
message Outer {
  string id = 1;
  int32 name = 2;
  // ...
}
```
### Nested Message Type 
1.
```proto
message Person {
  Address address = 1;
  // ...
}
```
2.
```proto
message SearchResponse {

  message Result {
    string url = 1;
    string title = 2;
    repeated string snippets = 3;
  }

  repeated Result results = 1;
}

```
### Message Type with Array Fields
```proto
message Outer {
  repeated int32 items = 1;
  repeated Address addresses = 2;
}
```
Unsupported data types
1. OneOf scenarios
2. Enum type
3. Map type

## 3. Java Code Generation Options
Below java code option can be supported,

| Option                       | Supported | Description                                                                 |
|-----------------------------|----------|-----------------------------------------------------------------------------|
| **Multiple Java Classes**   | ✅       | Each message generates a separate `.java` file (uses `java_multiple_files`). |
| **Outer Class Name Override** | ✅       | `option java_outer_classname`                                              |
| **Java Package Name Override** | ✅       | `option java_package`                                                      |

**Note:**
- Not supported for multiple package imports
- Not supported for the deadline option  
- Stream types are not supported
