## OpenAPI Connector Code Generation Tool

The OpenAPI connector codegen tool is able to generate multiple at a given time.

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

3. Extract the zip file. Let's refer it as `<TOOL_HOME>`. Following contents will be available in the `<TOOL_HOME>`.
   ```bash
   .
   ├── README.txt
   ├── bin
   │ ├── run.sh
   │ └── version.txt
   ├── config.json
   ├── connector-resources
   │ ├── connector-files
   │ └── icon
   └── libs
    ```
4. Construct a file similar to the `config.json` file available in the `<TOOL_HOME>`. Constructing a config
   file is described in [Writing a descriptor for FHIR client connector generator tool](src/main/resources/profiles/README.MD).

5. Execute the script `run.sh` available in the `<TOOL_HOME>` by passing the file created in the above step.
   ```
   sh run.sh /path/to/config.json
   ```

6. Now there will be new folder called `/generated-connectors` created in the `<TOOL_HOME>`.
   This folder will have the generated connectors.

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
