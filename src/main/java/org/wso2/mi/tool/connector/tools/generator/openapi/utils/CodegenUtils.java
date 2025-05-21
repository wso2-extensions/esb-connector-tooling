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
package org.wso2.mi.tool.connector.tools.generator.openapi.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.velocity.VelocityContext;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wso2.mi.tool.connector.tools.generator.openapi.ConnectorGenException;
import org.wso2.mi.tool.connector.tools.generator.openapi.model.Operation;
import org.wso2.mi.tool.connector.tools.generator.openapi.model.Parameter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;

/**
 * The GeneratorUtils class contains all common methods used in connector generation.
 */
public class CodegenUtils {

    private static final Log log = LogFactory.getLog(CodegenUtils.class);

    /**
     * Throw a ConnectorGenException with given message and exception
     * @param message                   message
     * @param throwable                 exception
     * @throws ConnectorGenException    ConnectorGenException
     */
    public static void handleException(String message, Throwable throwable) throws ConnectorGenException {

        log.error(message);
        throw new ConnectorGenException(message, throwable);
    }

    /**
     * Throw a ConnectorGenException with given message
     * @param message                   message
     * @throws ConnectorGenException    ConnectorGenException
     */
    public static void handleException(String message) throws ConnectorGenException {

        log.error(message);
        throw new ConnectorGenException(message);
    }

    /**
     * Generate current time in a particular format
     * @return current time
     */
    public static String getGeneratedTimestamp() {

        SimpleDateFormat sdf = new SimpleDateFormat("dd-EEE, MM, yyyy HH:mm:ssZ");
        return sdf.format(new Date());
    }

    /**
     * Tokenize given string and make first character of each token capital
     * @param name     string to be tokenized  
     * @return tokenized string
     */
    public static String toStartCase(String name) {

        String str = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(name), ' ');
        str = str.replaceAll("[,.\\-_]", " ");
        str = str.replaceAll("[&']", "");
        str = str.replaceAll("\\s+", " ");
        // capitalize using apache commons
        str = StringUtils.capitalize(str);

        if (str.startsWith(" ")) {
            str = str.substring(1);
        }
        return str;
    }

    /**
     * Convert the given string to lower camel case
     * @param name string to be converted
     * @return converted string
     */
    public static String toLowerCamelCase(String name) {

        String str = toStartCase(name);
        str = str.replaceAll("\\s+", "");
        str = UPPER_CAMEL.to(LOWER_CAMEL, str);
        return str;
    }

    /**
     * Replace all '/' characters with '_' character.
     * @param path  path to be normalized
     * @return      normalized path
     */
    public static String normPath(String path) {
        return path.replaceAll("/", "_");
    }

    /**
     *  Create a Parameter object with given params
     * @param name          parameter name     
     * @param required      is required      
     * @param description   description
     * @param defaultValue  default value
     * @return              Parameter object
     */
    public static Parameter createParameter(String name, boolean required, String description, String defaultValue) {

        Parameter parameter = new Parameter(name, required, description, defaultValue);
        parameter.setParameterName(CodegenUtils.toLowerCamelCase(name));
        parameter.setDisplayName(CodegenUtils.toStartCase(name));
        parameter.setXmlDescription(StringEscapeUtils.escapeXml10(description));
        parameter.setJsonDescription(StringEscapeUtils.escapeJson(description));
        return parameter;
    }

    /**
     * Create an Operation object with given params
     * @param name                  operation name
     * @param path                  operation path
     * @param description           operation description
     * @param pathParameterList     path parameters
     * @param queryParameterList    query parameters
     * @param headerParameterList   header parameters
     * @param cookieParameterList   cookie parameters
     * @param responseSchema        response schema
     * @return                      Operation object
     */
    public static Operation createOperation(String name, String path, String description, List<Parameter> pathParameterList,
                                            List<Parameter> queryParameterList, List<Parameter> headerParameterList,
                                            List<Parameter> cookieParameterList, String responseSchema) {

        Operation operation = new Operation(name, path, description);
        operation.setPathParameters(pathParameterList);
        operation.setQueryParameters(queryParameterList);
        operation.setHeaderParameters(headerParameterList);
        operation.setCookieParameters(cookieParameterList);
        String displayName = toStartCase(name);
        operation.setDisplayName(displayName);
        String documentationLink = displayName.replaceAll(" ", "-").toLowerCase();
        operation.setDocumentationLink(documentationLink);
        operation.setXmlDescription(StringEscapeUtils.escapeXml10(description));
        operation.setResponseSchema(responseSchema);
        return operation;
    }

    /**
     * Read properties given via a JSON object and populate velocity context
     * @param properties                properties object
     * @param parentPath                parent path
     * @return                          velocity context
     * @throws ConnectorGenException    ConnectorGenException
     */
    public static VelocityContext createVelocityContext(Properties properties, String parentPath)
            throws ConnectorGenException {

        VelocityContext context = new VelocityContext();

        String connectorName = (String) properties.get("connectorName");
        if (StringUtils.isEmpty(connectorName)) {
            handleException("The \"connectorName\" value is missing. Please check the properties file.");
        }

        String auth = (String) properties.get("auth");
        if (auth == null) {
            handleException("The \"auth\" value is missing. Please check the properties file.");
        } else {
            if (!(auth.equalsIgnoreCase("oauth2") || auth.equalsIgnoreCase("none") || auth.equalsIgnoreCase("basic"))) {
                handleException("Invalid value " + auth + "for \"auth\"");
            }
        }

        JSONArray openAPISpecificationArray = (JSONArray) properties.get("openapiSpecificationPath");
        if (openAPISpecificationArray == null || openAPISpecificationArray.isEmpty()) {
            handleException("The \"OpenAPI Specification\" value is missing. Please check the properties file.");
        }

        JSONObject project = (JSONObject) properties.get("project");
        if (project == null || project.isEmpty()) {
            handleException("The \"project\" object is missing. Please check the properties file.");

        } else {
            String artifactId = (String) project.get("artifactId");
            String version = (String) project.get("version");

            if (StringUtils.isEmpty(artifactId) || StringUtils.isEmpty(version)) {
                handleException("The \"artifactId\" or \"version\" of the project is missing. Please check the properties file.");
            }
            context.put("artifactId", artifactId);
            context.put("version", version);

            JSONObject parent = (JSONObject) project.get("parent");
            boolean parentFlag = false;
            if (parent != null) {
                String parentGroupId = (String) parent.get("groupId");
                String parentArtifactId = (String) parent.get("artifactId");
                String parentVersion = (String) parent.get("version");

                if (StringUtils.isEmpty(parentGroupId) || StringUtils.isEmpty(parentArtifactId) || StringUtils.isEmpty(parentVersion)) {
                    handleException(
                            "The \"groupId\", \"artifactId\" or \"version\" of the parent element is missing. Please " +
                                    "check the properties file.");
                } else {
                    context.put("parentGroupId", parentGroupId);
                    context.put("parentArtifactId", parentArtifactId);
                    context.put("parentVersion", parentVersion);
                    parentFlag = true;
                }
            }

            if (!parentFlag) {
                log.info("The POM does not have a parent element.");

                String groupId = (String) project.get("groupId");
                if (StringUtils.isEmpty(groupId)) {
                    handleException("The \"groupId\" of the project is missing. Please check the properties file.");
                }
                context.put("groupId", groupId);
            }
            context.put("parentFlag", parentFlag);
        }

        String icon = (String) properties.get("iconFolderPath");
        if (StringUtils.isEmpty(icon)) {
            log.info("The \"iconFolderPath\" value is missing from the properties file. Therefore default icons are being used.");
            icon = parentPath + "/connector-resources/icon";
        }

        JSONObject mappersObject = (JSONObject) properties.get("mappers");
        JSONArray operationNameArray = new JSONArray();
        if (mappersObject == null || mappersObject.isEmpty()) {
            log.info("The \"mappers\" object is empty.");
        } else {
            operationNameArray = (JSONArray) mappersObject.get("operationName");
        }

        connectorName = toLowerCamelCase(connectorName);
        context.put("connectorName", connectorName);
        context.put("auth", auth);
        context.put("openAPISpecificationArray", openAPISpecificationArray);
        context.put("icon", icon);
        context.put("operationNameArray", operationNameArray);
        context.put("exceptionName", LOWER_CAMEL.to(UPPER_CAMEL, connectorName));
        return context;
    }

    /**
     * Remove an attribute from the velocity context
     * @param key       key to be removed
     * @param context   velocity context
     */
    public static void removeFromContext(String key, VelocityContext context) {
        if (context.containsKey(key)) {
            context.remove(key);
        }
    }
}
