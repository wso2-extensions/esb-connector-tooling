/**
 * Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.mi.tool.connector.tools.generator.openapi.model;

import io.swagger.v3.oas.models.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to model an operation of the OpenAPI spec
 */
public class Operation {

    private String name;
    private String displayName;
    private String path;
    private String description;
    private String requestSchema;
    private boolean isUnhandledContentType;
    private String xmlDescription;
    private String documentationLink;
    private List<Parameter> pathParameters;
    private List<Parameter> queryParameters;
    private List<Parameter> headerParameters;
    private List<Parameter> cookieParameters;
    private List<Parameter> requestParameters = new ArrayList<>();
    private String responseTargetType;
    private String authType;
    private String responseSchema;

    public Operation(String name, String path, String descriptions) {

        this.name = name;
        this.path = path;
        this.description = descriptions;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getDisplayName() {

        return displayName;
    }

    public void setDisplayName(String displayName) {

        this.displayName = displayName;
    }

    public String getPath() {

        return path;
    }

    public void setPath(String path) {

        this.path = path;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public String getRequestSchema() {

        return requestSchema;
    }

    public void setRequestSchema(String requestSchema) {

        this.requestSchema = requestSchema;
    }

    public boolean isUnhandledContentType() {

        return isUnhandledContentType;
    }

    public void setUnhandledContentType(boolean unhandledContentType) {

        isUnhandledContentType = unhandledContentType;
    }

    public String getXmlDescription() {

        return xmlDescription;
    }

    public void setXmlDescription(String xmlDescription) {

        this.xmlDescription = xmlDescription;
    }

    public String getDocumentationLink() {

        return documentationLink;
    }

    public void setDocumentationLink(String documentationLink) {

        this.documentationLink = documentationLink;
    }

    public List<Parameter> getPathParameters() {

        return pathParameters;
    }

    public void setPathParameters(List<Parameter> pathParameters) {

        this.pathParameters = pathParameters;
    }

    public List<Parameter> getQueryParameters() {

        return queryParameters;
    }

    public void setQueryParameters(List<Parameter> queryParameters) {

        this.queryParameters = queryParameters;
    }

    public List<Parameter> getHeaderParameters() {

        return headerParameters;
    }

    public void setHeaderParameters(List<Parameter> headerParameters) {

        this.headerParameters = headerParameters;
    }

    public List<Parameter> getCookieParameters() {

        return cookieParameters;
    }

    public void setCookieParameters(List<Parameter> cookieParameters) {

        this.cookieParameters = cookieParameters;
    }

    public List<Parameter> getRequestParameters() {

        return requestParameters;
    }

    public void setRequestParameters(List<Parameter> requestParameters) {

        this.requestParameters = requestParameters;
    }

    public String getResponseTargetType() {

        return responseTargetType;
    }

    public void setResponseTargetType(String responseTargetType) {

        this.responseTargetType = responseTargetType;
    }

    public String getAuthType() {

        return authType;
    }

    public void setAuthType(String authType) {

        this.authType = authType;
    }

    public String getResponseSchema() {

        return responseSchema;
    }

    public void setResponseSchema(String responseSchema) {

        this.responseSchema = responseSchema;
    }

    @Override
    public String toString() {

        return "Operation{" + "name='" + name + '\'' + ", displayName='" + displayName + '\'' + ", path='" + path +
                '\'' + ", description='" + description + '\'' + ", xmlDescription='" +
                xmlDescription + '\'' + ", documentationLink='" + documentationLink + '\'' + ", pathParameters=" +
                pathParameters + ", queryParameters=" + queryParameters + ", headerParameters=" + headerParameters +
                ", cookieParameters=" + cookieParameters + ", requestParameters=" + requestParameters +
                ", responseTargetType='" + responseTargetType + '\'' + ", authType='" + authType + '\'' + '}';
    }
}
