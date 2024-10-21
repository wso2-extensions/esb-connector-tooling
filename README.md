## OpenAPI Connector Generation Tool

This tool is used to generate a Micro Integrator connector for a given OpenAPI specification.

### Steps

1. Navigate to `<PROJECT_HOME>/` and run the following command.

   To run without test -
   ```
   mvn clean install
   ```
   To run with unit tests -
   ```
   mvn clean install -Dskip-tests=false
   ```

2. The tool zip `mi-connector-generator-{version}.zip` can be found in the `<PROJECT_HOME>/target` directory.

---
**NOTE**

Media types supported -

* application/json
* application/xml
* application/x-www-form-urlencoded

Operation types supported -

* GET
* PUT
* POST
* DELETE
* PATCH

---
