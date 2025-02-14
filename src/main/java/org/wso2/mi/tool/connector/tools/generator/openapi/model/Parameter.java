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

/**
 * Class to model the parameter of the OpenAPI spec
 */
public class Parameter {

    private String name;
    private boolean required;
    private String description;
    private String defaultValue;
    private String parameterName;
    private String displayName;
    private String xmlDescription;
    private String jsonDescription;
    private String innerSchema;

    public Parameter(String name, boolean required, String description, String defaultValue) {

        this.name = name;
        this.required = required;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public boolean isRequired() {

        return required;
    }

    public void setRequired(boolean required) {

        this.required = required;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public String getDefaultValue() {

        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {

        this.defaultValue = defaultValue;
    }

    public String getParameterName() {

        return parameterName;
    }

    public void setParameterName(String parameterName) {

        this.parameterName = parameterName;
    }

    public String getDisplayName() {

        return displayName;
    }

    public void setDisplayName(String displayName) {

        this.displayName = displayName;
    }

    public String getXmlDescription() {

        return xmlDescription;
    }

    public void setXmlDescription(String xmlDescription) {

        this.xmlDescription = xmlDescription;
    }

    public String getJsonDescription() {

        return jsonDescription;
    }

    public void setJsonDescription(String jsonDescription) {

        this.jsonDescription = jsonDescription;
    }

    public String getInnerSchema() {

        return innerSchema;
    }

    public void setInnerSchema(String innerSchema) {

        this.innerSchema = innerSchema;
    }


    @Override
    public String toString() {

        return "Parameter{" + "name='" + name + '\'' + ", required=" + required + ", description='" +
                description + '\'' + ", defaultValue='" + defaultValue + '\'' + ", parameterName='" +
                parameterName + '\'' + ", displayName='" + displayName + '\'' + ", xmlDescription='" +
                xmlDescription + '\'' + ", jsonDescription='" + jsonDescription + '\'' + '}';
    }
}
