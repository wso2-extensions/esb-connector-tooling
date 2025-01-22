# Specification : WSO2 MI Connector Generator

## Introduction

This is the specification for the tool that is used to generate a Micro Integrator connector for a given OpenAPI specification. 

A connector is a collection or a set of operations that can be used in WSO2 Micro Interator to access a specific service or a functionality. This can be a third party HTTP API, remote SOAP service, a legacy system with a proprietary protocol or even a local library function.

This tool specifically generates a REST based connector for a given OpenAPI specification. Since Integrations in Micro Integrator is written in Synapse language, the generated connector will have Synapse language templates for each operation in the OpenAPI specification.

## Contents


## 1. Overview

The tool generates a Micro Integrator connector for a given OpenAPI specification. The generated connector can be used in WSO2 Micro Integrator to access a specific service or a functionality.

## 2. OpenAPI to Synapse Mapping

### 2.1 OpenAPI operation to Synapse template mapping

## 2. OpenAPI to Synapse Mapping

### 2.1 OpenAPI operation to Synapse template mapping

#### 2.1.1 Authentication

The MI Connector Generator tool supports one of the following authentication mechanisms: Basic Authentication, API Key Authentication, OAuth2 Client Credentials, and OAuth2 Authorization Code. If more than one authentication mechanism is defined, the first one will be taken and others will be ignored.

- **Basic Authentication**

In the OpenAPI specification, basic authentication is defined as follows:

```yaml
components:
  securitySchemes:
    basicAuth:
      type: http
      scheme: basic
```

Generated files:
- `init.xml` with the following Synapse logic:

```xml
<property name="Authorization" expression="fn:concat('Basic ', base64Encode('username:password'))" scope="transport"/>
```

Properties to be entered by the user:

| Property  | Description                  | Required |
|-----------|------------------------------|----------|
| username  | Username for authentication  | Yes      |
| password  | Password for authentication  | Yes      |

- **API Key Authentication**

In the OpenAPI specification, API key authentication is defined as follows:

```yaml
components:
  securitySchemes:
    apiKey:
      type: apiKey
      in: header
      name: X-API-Key
```

Generated files:
- `init.xml` with the following Synapse logic:

```xml
<property name="X-API-Key" expression="fn:concat('Bearer ', get-property('uri.var.api_key'))" scope="transport"/>
```

***Note: The `name` attribute in the OpenAPI specification is used as the header name in the Synapse template.***

Properties to be entered by the user:

| Property | Description                        | Required |
|----------|------------------------------------|----------|
| api_key  | API key for authentication         | Yes      |

- **OAuth2 Client Credentials**

In the OpenAPI specification, OAuth2 Client Credentials authentication is defined as follows:

```yaml
components:
  securitySchemes:
    oauth2ClientCredentials:
      type: oauth2
      flows:
        clientCredentials:
          tokenUrl: https://example.com/oauth/token
          scopes:
            read: Grants read access
            write: Grants write access
```

Generated files:
- `InMemoryTokenStore.java`
- `TokenStore.java`
- `TokenManager.java`
- `Token.java`
- `ClientCredentialsTokenHandler.java`
- `Constants.java`
- `init.xml` with the following Synapse logic:

```xml
<property name="Authorization" expression="fn:concat('Bearer ', get-property('uri.var.access_token'))" scope="transport"/>
```

Properties to be entered by the user:

| Property       | Description                                | Required |
|----------------|--------------------------------------------|----------|
| clientId       | Client ID of the registered application    | Yes      |
| clientSecret   | Client secret of the registered application| Yes      |
| tokenEndpoint  | Token endpoint URL                         | Yes      |
| scope          | Scope of the resources                     | No       |

- **OAuth2 Authorization Code**

In the OpenAPI specification, OAuth2 Authorization Code authentication is defined as follows:

```yaml
components:
  securitySchemes:
    oauth2AuthorizationCode:
      type: oauth2
      flows:
        authorizationCode:
          authorizationUrl: https://example.com/oauth/authorize
          tokenUrl: https://example.com/oauth/token
          scopes:
            read: Grants read access
            write: Grants write access
```

Generated files:
- `InMemoryTokenStore.java`
- `TokenStore.java`
- `TokenManager.java`
- `Token.java`
- `AuthorizationCodeTokenHandler.java`
- `Constants.java`
- `init.xml` with the following Synapse logic:

```xml
<property name="Authorization" expression="fn:concat('Bearer ', get-property('uri.var.access_token'))" scope="transport"/>
```

Properties to be entered by the user:
¬
| Property          | Description                                | Required |
|-------------------|--------------------------------------------|----------|
| clientId          | Client ID of the registered application    | Yes      |
| clientSecret      | Client secret of the registered application| Yes      |
| tokenEndpoint     | Token endpoint URL                         | Yes      |
| authorizationCode | Authorization code                         | Yes      |
| redirectUri       | Redirect URI                               | Yes      |
| scope             | Scope of the resources                     | No       |

If none of these authentication mechanisms are specified, there will be no authentication, and direct HTTP requests will be sent for operations.

#### 2.1.1 Parameters

In OpenAPI 3.0, parameters are defined in the parameters section of an operation or path. Here is an example:

```yaml
paths:
  /users/{userId}:
    get:
      summary: Get a user by ID
      parameters:
        - in: path
          name: userId
          schema:
            type: integer
          required: true
          description: Numeric ID of the user to get
```

The name of the parameter property generated in the Synapse template is derived from the `name` attribute in the parameters section of the OpenAPI file. 

The `in` attribute in the parameters section specifies whether the parameter should be included as a path parameter, query parameter, header parameter, or cookie parameter.

The type of the parameter is determined from the `schema` attribute, and the value specified for the `required` attribute determines whether a parameter is mandatory or optional.

##### Path parameters

Path parameters in the OpenAPI specification are passed to a Class Mediator where the path parameters are used construct the URL.

- Synapse template
```xml
        <class name="org.wso2.carbon.${connectorName}connector.RestURLBuilder">
            <property name="operationPath" value="$operation.getPath()"/>
            #if ($pathParameterList != "")
                <property name="pathParameters" value="$pathParameterList"/>
            #end
            #if ($queryParameterList != "")
            <property name="queryParameters" value="$queryParameterList"/>
            #end
        </class>
```

- Class Mediator extract
```java
            String urlPath = getOperationPath();
            if (StringUtils.isNotEmpty(this.pathParameters)) {
                String[] pathParameterList = getPathParameters().split(",");
                for (String pathParameter : pathParameterList) {
                    String paramValue = (String) getParameter(messageContext, pathParameter);
                    if (StringUtils.isNotEmpty(paramValue)) {
                        String encodedParamValue = URLEncoder.encode(paramValue, encoding);
                        urlPath = urlPath.replace("{" + pathParameter + "}", encodedParamValue);
                    } else {
                        String errorMessage = Constants.GENERAL_ERROR_MSG + "Mapping parameter '" + pathParameter + "' is not set.";
                        Utils.setErrorPropertiesToMessage(messageContext, Constants.ErrorCodes.INVALID_CONFIG, errorMessage);
                        handleException(errorMessage, messageContext);
                    }
                }
            }
```

Following are few specific path parameter related scenarios.

**Scenario 1**

_Sample OpenAPI snippet_

```yaml
paths:
  /v1/{id}:
    get:
      operationId: operationId03
      parameters:
        - name: id
          description: "id value"
          in: path
          required: true
          schema:
            $ref: "#/components/schemas/Id"
      responses:
        "200":
          description: Ok
          content:
            text/plain:
              schema:
                type: string
components:
  schemas:
    Id:
      type: integer
```

_Generated synapse template_

```xml
public type Id int;
...
  
  remote isolated function operationId03(Id id) returns string|error {
      string resourcePath = string `/v1/${getEncodedUri(id)}`;
      string response = check self.clientEp-> get(resourcePath);
      return response;
  }
...
```

_Generated Class mediator_

```java

```

**Scenario 2**

It is important to note that having a path parameter with `nullable: true` is not valid in Ballerina and will result in an error.

_Sample OpenAPI snippet_

```yaml
  parameters:
    - description: "Department name"
      in: path
      name: department
      schema:
        type: string
        nullable: true
      required: true
```

**Scenario 3**

If a default value is given for a parameter, it will be ignored when generating the client since in Ballerina path parameter should always be a required parameter.

_Sample OpenAPI snippet_

```yaml
  parameters:
    - description: "Department name"
      in: path
      name: department
      schema:
        type: string
        default: HR
      required: true
```

##### Query parameters

Query parameters in the OpenAPI specification are passed to a Class Mediator where the query parameters are used to construct the URL.

- Synapse template
```
        <class name="org.wso2.carbon.${connectorName}connector.RestURLBuilder">
            <property name="operationPath" value="$operation.getPath()"/>
            #if ($pathParameterList != "")
                <property name="pathParameters" value="$pathParameterList"/>
            #end
            #if ($queryParameterList != "")
            <property name="queryParameters" value="$queryParameterList"/>
            #end
        </class>
```

- Class Mediator extract
```
            StringBuilder urlQueryBuilder = new StringBuilder();
            if (StringUtils.isNotEmpty(this.queryParameters)) {
                String[] queryParameterList = getQueryParameters().split(",");
                for (String queryParameter : queryParameterList) {
                    String paramValue = (String) getParameter(messageContext, queryParameter);
                    if (StringUtils.isNotEmpty(paramValue)) {
                        String encodedParamValue = URLEncoder.encode(paramValue, encoding);
                        urlQueryBuilder.append(queryParameter).append('=').append(encodedParamValue).append('&');
                    }
                }
            }
```

#### 2.1.2 Request body

Request body in the OpenAPI specification is mapped to Synapse template as follows.

```
<property name="messageType" value="application/json" scope="axis2" type="STRING"/>
<property name="messageType" value="application/xml" scope="axis2" type="STRING"/>
<property name="messageType" value="application/x-www-form-urlencoded" scope="axis2" type="STRING"/>
```




