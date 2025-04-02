# WSO2 Micro Integrator Connector Generator Tools

The WSO2 MI Connector Generator is a tool designed to simplify the creation of connectors for the WSO2 Micro Integrator using OpenAPI specifications and Protocol Buffer (Proto) files.

---

## Prerequisites

Before you start, ensure that you have:

- Java Development Kit (JDK) 8 or later
- Apache Maven 3.6.x or higher

### Verify Prerequisites

Check Java installation:

```bash
java -version
```

Check Maven installation:

```bash
mvn -version
```

---

## Getting Started

### Step 1: Building the Tool

Navigate to your project directory (`<PROJECT_HOME>/`) and build the project using Maven:

- **Build without tests:**

  ```bash
  mvn clean install
  ```

- **Build with unit tests:**

  ```bash
  mvn clean install -Dskip-tests=false
  ```

### Step 2: Locate and Extract the Tool

Once built, the tool will be packaged as a zip file:

```
mi-connector-generator-{version}.zip
```

This file is located in:

```
<PROJECT_HOME>/target
```

Extract this zip file to your preferred location.

### Step 3: Running the Connector Generator

Navigate to the extracted folder and go to the `bin` directory.

- **For macOS/Linux:** Use `generate.sh`

  ```bash
  ./generator <openapi-yaml-file | proto-file> <output-directory> [miVersion]
  ```

- **For Windows:** Use `generate.bat`

  ```cmd
  generator.bat <openapi-yaml-file | proto-file> <output-directory> [miVersion]
  ```

Replace:
- `<openapi-yaml-file | proto-file>` with the path to your OpenAPI or Proto file.
- `<output-directory>` with the path where the connector should be generated.
- `[miVersion]` with your specific Micro Integrator version.

---

## Supported Features

### OpenAPI Connector Generation

**Supported Media Types:**

- `application/json`
- `application/xml`
- `application/x-www-form-urlencoded`

**Supported Operation Types:**

- `GET`
- `PUT`
- `POST`
- `DELETE`
- `PATCH`

### GRPC Connector Generation

**Supported Features:**

- Provide TLS support for the communication channel
- All primitive data types
- Java options:
   - `java_multiple_files`
   - `java_outer_classname`
   - `java_package`

---

## Further Information

For more details, refer to the tool specification documentation located under the `doc` folder within your project directory.


---
