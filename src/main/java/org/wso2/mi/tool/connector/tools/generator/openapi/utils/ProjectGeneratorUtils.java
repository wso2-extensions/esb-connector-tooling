/**
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
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

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.wso2.mi.tool.connector.tools.generator.openapi.ConnectorGenException;
import org.wso2.mi.tool.connector.tools.generator.openapi.Constants;
import org.wso2.mi.tool.connector.tools.generator.openapi.model.Operation;
import org.wso2.mi.tool.connector.tools.generator.openapi.model.Parameter;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * The VelocityConfigGenerator handles the generation of the openapi connector
 * project files.
 */
public class ProjectGeneratorUtils {

    private static final Log log = LogFactory.getLog(ProjectGeneratorUtils.class);
    private static List<Operation> operationList = new ArrayList<>();
    private static Map<String, String> parameterNameMap = new HashMap<>();
    private static Map<String, String> operationNameMap = new HashMap<>();
    private static final VelocityEngine velocityEngine = new VelocityEngine();
    static VelocityContext context;
    static Map<String, Schema> componentsSchema;

    public static String generateConnectorProject(String openAPISpecPath, String projectPath) throws
            ConnectorGenException {

        String pathToConnectorDir = null;
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        parseOptions.setResolveCombinators(true);
        OpenAPIV3Parser oasParser = new OpenAPIV3Parser();
        SwaggerParseResult result;
        try {
            result = oasParser.readLocation(openAPISpecPath, null, parseOptions);
            OpenAPI openAPI = result.getOpenAPI();
            if (openAPI != null) {
                context = createVelocityContext(openAPI);
                String artifactId = (String) context.get("artifactId");
                String connectorName = context.get("connectorName").toString();
                pathToConnectorDir = projectPath + "/generated/" + artifactId;
                String pathToMainDir = pathToConnectorDir +
                        "/src/main/java/org/wso2/carbon/" + connectorName + "connector";
                String pathToResourcesDir = pathToConnectorDir + "/src/main/resources";
                createConnectorDirectory(pathToConnectorDir, pathToMainDir, pathToResourcesDir, connectorName);
                copyConnectorStaticFiles(pathToConnectorDir, pathToResourcesDir, pathToMainDir);
                if (openAPI.getComponents() != null) {
                    componentsSchema = openAPI.getComponents().getSchemas();
                }
                readOpenAPISpecification(openAPI, pathToResourcesDir);
                operationList.sort(Comparator.comparing(Operation::getName));
                context.put("operations", operationList);
                context.put("parameterNameMap", parameterNameMap);
                generateConnectorConfig(pathToConnectorDir, pathToResourcesDir, pathToMainDir);
                copyGenResources(openAPISpecPath, pathToConnectorDir);
            } else {
                String errorMsg = "Couldn't parse OpenAPI specification.";
                CodegenUtils.handleException(errorMsg);
            }
        } catch (IOException | ConnectorGenException e) {
            String errorMsg = "Error occurred while reading the OpenAPI specification";
            CodegenUtils.handleException(errorMsg, e);
        }
        return pathToConnectorDir;
    }

    private static void initVelocityEngine() {
        Properties properties = new Properties();
        properties.setProperty("resource.loader", "class");
        properties.setProperty("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine.init(properties);
    }

    private static void createConnectorDirectory(String pathToConnectorDir, String pathToMainDir,
                                                 String pathToResourcesDir, String connectorName) throws IOException {

        Files.createDirectories(Paths.get(pathToConnectorDir));
        Files.createDirectories(Paths.get(pathToMainDir));
        Files.createDirectories(Paths.get(pathToConnectorDir + "/docs"));
        Files.createDirectories(Paths.get(pathToConnectorDir + "/gen_resources"));
        Files.createDirectories(Paths.get(pathToResourcesDir + "/config"));
        Files.createDirectories(Paths.get(pathToResourcesDir + "/functions"));
        Files.createDirectories(Paths.get(pathToResourcesDir + "/icon"));
        Files.createDirectories(Paths.get(pathToResourcesDir + "/uischema"));
        Files.createDirectories(Paths.get(pathToResourcesDir + "/outputschema"));
    }

    private static void createAssemblyDirectory(String assemblyDirPath) {
        Path path = Paths.get(assemblyDirPath);
        try {
            Files.createDirectories(path);
            System.out.println("Assembly directory created at: " + path.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to create assembly directory: " + e.getMessage());
        }
    }

    private static void copyConnectorStaticFiles(String pathToConnectorDir, String pathToResourcesDir,String pathToMainDir) throws IOException {

        ClassLoader classLoader = ProjectGeneratorUtils.class.getClassLoader();
        String artifactId = (String) context.get("artifactId");

        // create assembly directory
        createAssemblyDirectory(pathToConnectorDir + "/src/main/assembly");

        // Copy connector files
        try (InputStream staticFilesStream = classLoader.getResourceAsStream("connector-files/src/main/assembly/assemble-connector.xml")) {
            if (staticFilesStream != null) {
                Files.copy(staticFilesStream, Paths.get(pathToConnectorDir + "/src/main/assembly/assemble-connector.xml"), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        // Determine the template based on the auth type
        String auth = (String) context.get("auth");
        String oauth2flow = (String) context.get("oauth2flow");
        String outputFile;
        String template;
        if (auth.equalsIgnoreCase("oauth2") && oauth2flow.equalsIgnoreCase("client_credentials")) {
            String[][] oauth2Templates = {
                    {"templates/java/in_memory_token_store_template.vm", "/InMemoryTokenStore.java"},
                    {"templates/java/token_store_template.vm", "/TokenStore.java"},
                    {"templates/java/client_credentials_token_handler_template.vm", "/ClientCredentialsTokenHandler" +
                            ".java"},
                    {"templates/java/token_manager_template.vm", "/TokenManager.java"},
                    {"templates/java/token_template.vm", "/Token.java"},
                    {"templates/java/constants_template.vm", "/Constants.java"}
            };

            for (String[] entry : oauth2Templates) {
                template = entry[0];
                outputFile = pathToMainDir + entry[1];
                mergeVelocityTemplate(outputFile, template);
            }
            template = "templates/auth/init_oauth2_client_credentials_template.vm";
        } else if (auth.equalsIgnoreCase("oauth2") && oauth2flow.equalsIgnoreCase("authorization_code")) {
            String[][] oauth2AuthCodeTemplates = {
                    {"templates/java/in_memory_token_store_template.vm", "/InMemoryTokenStore.java"},
                    {"templates/java/token_store_template.vm", "/TokenStore.java"},
                    {"templates/java/token_manager_template.vm", "/TokenManager.java"},
                    {"templates/java/token_template.vm", "/Token.java"},
                    {"templates/java/constants_template.vm", "/Constants.java"},
                    {"templates/java/authorization_code_token_handler_template.vm", "/AuthorizationCodeTokenHandler.java"}
            };

            for (String[] entry : oauth2AuthCodeTemplates) {
                template = entry[0];
                outputFile = pathToMainDir + entry[1];
                mergeVelocityTemplate(outputFile, template);
            }
            template = "templates/auth/init_oauth2_authorization_code_template.vm";
        } else if (auth.equalsIgnoreCase("basic")) {
            template = "templates/auth/init_basic_auth_template.vm";
        } else if (auth.equalsIgnoreCase("apiKey")) {
            template = "templates/auth/init_api_key_template.vm";
        } else {
            template = "templates/auth/init_no_auth_template.vm";
        }
        outputFile = pathToResourcesDir + "/config/init.xml";
        mergeVelocityTemplate(outputFile, template);

        // Copy icon directory
        try (InputStream iconStream = classLoader.getResourceAsStream("icon/icon-large.gif")) {
            if (iconStream != null) {
                Files.copy(iconStream, Paths.get(pathToResourcesDir, "icon", "icon-large.gif"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
        try (InputStream iconStream = classLoader.getResourceAsStream("icon/icon-small.gif")) {
            if (iconStream != null) {
                Files.copy(iconStream, Paths.get(pathToResourcesDir, "icon", "icon-small.gif"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void mergeVelocityTemplate(String outputFile, String template) throws IOException {

        File file = new File(outputFile);
        Writer writer = new FileWriter(file);
        velocityEngine.mergeTemplate(template, "UTF-8", context, writer);
        writer.flush();
        writer.close();
    }

    private static void generateConnectorConfig(String pathToConnectorDir, String pathToResourcesDir,
                                                String pathToMainDir) throws IOException {

        String outputFile = pathToConnectorDir + "/pom.xml";
        String template = "templates/synapse/pom_template.vm";
        mergeVelocityTemplate(outputFile, template);

        outputFile = pathToResourcesDir + "/connector.xml";
        template = "templates/synapse/connector_template.vm";
        mergeVelocityTemplate(outputFile, template);

        outputFile = pathToResourcesDir + "/functions/component.xml";
        template = "templates/synapse/component_functions_template.vm";
        mergeVelocityTemplate(outputFile, template);

        outputFile = pathToResourcesDir + "/config/component.xml";
        template = "templates/synapse/component_init_template.vm";
        mergeVelocityTemplate(outputFile, template);

        String auth = (String) context.get("auth");
        String oAuth2Flow = (String) context.get("oauth2flow");
        if (auth.equalsIgnoreCase("oauth2") && oAuth2Flow.equalsIgnoreCase("client_credentials")) {
            generateOAuth2Files(pathToResourcesDir);
        } else if (auth.equalsIgnoreCase("oauth2") && oAuth2Flow.equalsIgnoreCase("authorization_code")) {
            generateOAuth2AuthorizationCodeFiles(pathToResourcesDir);
        } else if (auth.equalsIgnoreCase("basic")) {
            generateBasicAuthFiles(pathToResourcesDir);
        } else if (auth.equalsIgnoreCase("apiKey")) {
            generateApiKeyAuthFiles(pathToResourcesDir);
        } else {
            generateNoAuthFiles(pathToResourcesDir);
        }

        generateJavaMainFiles(pathToMainDir);
        generateDocs(pathToConnectorDir);
    }

    private static void copyGenResources(String openAPISpecificationPath, String pathToConnectorDir) throws IOException {

        String genResourcesPath = pathToConnectorDir + "/gen_resources";
        String outputFile = genResourcesPath + "/README.md";
        String template = "templates/md/gen_resources_template.vm";
        mergeVelocityTemplate(outputFile, template);

        FileUtils.copyFileToDirectory(new File(openAPISpecificationPath), new File(genResourcesPath));
    }

    private static void generateOAuth2Files(String pathToResourcesDir) throws IOException {
        String outputFile = pathToResourcesDir + "/uischema/init.json";
        String template = "templates/uischema/init_oauth2_client_credentials_template.vm";
        mergeVelocityTemplate(outputFile, template);
    }

    private static void generateNoAuthFiles(String pathToResourcesDir) throws IOException {
        String outputFile = pathToResourcesDir + "/uischema/init.json";
        String template = "templates/uischema/init_no_auth_template.vm";
        mergeVelocityTemplate(outputFile, template);

    }

    private static void generateBasicAuthFiles(String pathToResourcesDir) throws IOException {

        String outputFile = pathToResourcesDir + "/uischema/init.json";
        String template = "templates/uischema/init_basic_auth_template.vm";
        mergeVelocityTemplate(outputFile, template);

    }

    private static void generateJavaMainFiles(String pathToMainDir) throws IOException {

        String outputFile = pathToMainDir + "/RestURLBuilder.java";
        String template = "templates/java/rest_url_builder_template.vm";
        mergeVelocityTemplate(outputFile, template);

        String utilFile = pathToMainDir + "/Utils.java";
        String utilTemplate = "templates/java/utils_template.vm";
        mergeVelocityTemplate(utilFile, utilTemplate);
    }

    private static void generateApiKeyAuthFiles(String pathToResourcesDir) throws IOException {
        String outputFile = pathToResourcesDir + "/uischema/init.json";
        String template = "templates/uischema/init_api_key_auth_template.vm";
        mergeVelocityTemplate(outputFile, template);
    }

    private static void generateOAuth2AuthorizationCodeFiles(String pathToResourcesDir) throws IOException {
        String outputFile = pathToResourcesDir + "/uischema/init.json";
        String template = "templates/uischema/init_oauth2_authorization_code_template.vm";
        mergeVelocityTemplate(outputFile, template);
    }

    private static void generateDocs(String pathToConnectorDir) throws IOException {

        String outputFile = pathToConnectorDir + "/docs/config.md";
        String template = "templates/md/config_template.vm";
        mergeVelocityTemplate(outputFile, template);

        outputFile = pathToConnectorDir + "/docs/operations.md";
        template = "templates/md/operations_template.vm";
        mergeVelocityTemplate(outputFile, template);

        outputFile = pathToConnectorDir + "/README.md";
        template = "templates/md/readme_template.vm";
        mergeVelocityTemplate(outputFile, template);
    }

    private static Parameter readParameter(io.swagger.v3.oas.models.parameters.Parameter paramObject) {

        String paramName = paramObject.getName();
        boolean paramRequired = paramObject.getRequired();
        String paramDescription = paramObject.getDescription();
        String paramDefault;
        if (paramObject.getSchema() != null && paramObject.getSchema().getDefault() != null) {
            paramDefault = paramObject.getSchema().getDefault().toString();
        } else {
            paramDefault = "";
        }

        if (StringUtils.isEmpty(paramDefault)) {
            paramDefault = "";
        }
        if (StringUtils.isEmpty(paramDescription)) {
            paramDescription = "";
        }

        Parameter param = CodegenUtils.createParameter(paramName, paramRequired, paramDescription, paramDefault);
        parameterNameMap.put(param.getParameterName(), paramName);
        return param;
    }

    private static void readOpenAPISpecification(OpenAPI openAPI, String pathToResourcesDir) throws ConnectorGenException, IOException {

        for (Map.Entry<String, PathItem> pathItemEntry : openAPI.getPaths().entrySet()) {
            String path = pathItemEntry.getKey();
            PathItem pathItem = pathItemEntry.getValue();

            List<io.swagger.v3.oas.models.parameters.Parameter> pathItemParameters = pathItem.getParameters();
            List<Parameter> pathItemPathParamList = new ArrayList<>();
            List<Parameter> pathItemQueryParamList = new ArrayList<>();
            List<Parameter> pathItemHeaderParamList = new ArrayList<>();
            List<Parameter> pathItemCookieParamList = new ArrayList<>();
            if (pathItemParameters != null) {
                for (io.swagger.v3.oas.models.parameters.Parameter parameter : pathItemParameters) {
                    String paramIn = parameter.getIn();
                    String paramName = parameter.getName();

                    if (paramName.equals(Constants.ACCEPT) || paramName.equals(Constants.CONTENT_TYPE) ||
                            paramName.equals(Constants.AUTHORIZATION)) {
                        continue;
                    }
                    Parameter param = readParameter(parameter);

                    switch (paramIn) {
                        case Constants.PATH:
                            pathItemPathParamList.add(param);
                            break;
                        case Constants.QUERY:
                            pathItemQueryParamList.add(param);
                            break;
                        case Constants.HEADER:
                            pathItemHeaderParamList.add(param);
                            break;
                        case Constants.COOKIE:
                            pathItemCookieParamList.add(param);
                            break;
                        default:
                            String errorMsg = "Unknown parameter type: " + paramIn;
                            CodegenUtils.handleException(errorMsg);
                            break;
                    }
                }
            }

            Map<PathItem.HttpMethod, io.swagger.v3.oas.models.Operation> operations = pathItem.readOperationsMap();
            for (Map.Entry<PathItem.HttpMethod, io.swagger.v3.oas.models.Operation> op : operations.entrySet()) {
                context.put(Constants.HTTP_METHOD, op.getKey().name());
                io.swagger.v3.oas.models.Operation operation = op.getValue();
                String operationName = operation.getOperationId() != null ?
                        CodegenUtils.normPath(operation.getOperationId()) :
                        op.getKey().name() + CodegenUtils.normPath(path);
                String operationDescription = operation.getDescription();
                List<io.swagger.v3.oas.models.parameters.Parameter> operationParameters = operation.getParameters();
                if (StringUtils.isEmpty(operationDescription)) {
                    operationDescription = "This is " + operationName + " operation.";
                }
                if (!(operationNameMap == null || operationNameMap.isEmpty())) {
                    String connectorOperationName = operationNameMap.get(operationName);
                    if (StringUtils.isNotEmpty(connectorOperationName)) {
                        operationName = connectorOperationName;
                    }
                }

                ApiResponse apiResponse = operation.getResponses().get("200");
                if (apiResponse != null) {
                    Content successContent = apiResponse.getContent();
                    if (successContent != null && !successContent.isEmpty()) {
                        String acceptVal = null;
                        for (MimeTypes mime : MimeTypes.values()) {
                            if (successContent.containsKey(mime.toString())) {
                                acceptVal = mime.toString();
                                break;
                            }
                        }
                        if (acceptVal == null) {
                            acceptVal = (String) successContent.keySet().toArray()[0];
                        }
                        context.put(Constants.OP_ACCEPT, acceptVal);
                    } else {
                        CodegenUtils.removeFromContext(Constants.OP_ACCEPT, context);
                        log.warn("Success response content not available.");
                    }
                } else {
                    CodegenUtils.removeFromContext(Constants.OP_ACCEPT, context);
                    log.warn("Success response not available.");
                }

                String responseSchema = "";
                if (apiResponse != null) {
                    Content successContent = apiResponse.getContent();
                    if (successContent != null && !successContent.isEmpty()) {
                        for (MimeTypes mime : MimeTypes.values()) {
                            if (successContent.containsKey(mime.toString())) {
                                MediaType mediaType = successContent.get(mime.toString());
                                responseSchema = Json.pretty(mediaType.getSchema());
                            }
                        }
                    }
                }

                List<Parameter> pathParamList = new ArrayList<>(pathItemPathParamList);
                List<Parameter> queryParamList = new ArrayList<>(pathItemQueryParamList);
                List<Parameter> headerParamList = new ArrayList<>(pathItemHeaderParamList);
                List<Parameter> cookieParamList = new ArrayList<>(pathItemCookieParamList);
                if (!(operationParameters == null || operationParameters.isEmpty())) {
                    for (io.swagger.v3.oas.models.parameters.Parameter parameter : operationParameters) {
                        String paramIn = parameter.getIn();
                        String paramName = parameter.getName();

                        if (paramName.equals(Constants.ACCEPT) || paramName.equals(Constants.CONTENT_TYPE) ||
                                paramName.equals(Constants.AUTHORIZATION)) {
                            continue;
                        }
                        Parameter param = readParameter(parameter);

                        switch (paramIn) {
                            case Constants.PATH:
                                pathParamList.add(param);
                                break;
                            case Constants.QUERY:
                                queryParamList.add(param);
                                break;
                            case Constants.HEADER:
                                headerParamList.add(param);
                                break;
                            case Constants.COOKIE:
                                cookieParamList.add(param);
                                break;
                            default:
                                String errorMsg = "Unknown parameter type: " + paramIn;
                                CodegenUtils.handleException(errorMsg);
                                break;
                        }
                    }
                }

                RequestBody requestBody = operation.getRequestBody();
                if (requestBody != null) {
                    Content content = requestBody.getContent();
                    if (content != null && !content.isEmpty()) {
                        boolean contentTypeSet = false;
                        for (MimeTypes mime : MimeTypes.values()) {
                            if (content.containsKey(mime.toString())) {
                                Operation operationLocal = CodegenUtils
                                        .createOperation(operationName, path, operationDescription, pathParamList,
                                                queryParamList, headerParamList, cookieParamList, responseSchema);
                                context.put(Constants.OP_CONTENT_TYPE, mime.toString());
                                MediaType mediaType = content.get(mime.toString());
                                readschema(pathToResourcesDir, operationName, operationLocal, mediaType, mime.toString());
                                contentTypeSet = true;
                                break;
                            }
                        }
                        if (!contentTypeSet) {
                            CodegenUtils.removeFromContext(Constants.OP_CONTENT_TYPE, context);
                            Operation operationLocal = CodegenUtils
                                    .createOperation(operationName, path, operationDescription, pathParamList,
                                            queryParamList, headerParamList, cookieParamList, responseSchema);
                            operationLocal.setUnhandledContentType(true);
                            addOperation(pathToResourcesDir, operationName, operationLocal);
                        }
                    }
                } else {
                    CodegenUtils.removeFromContext(Constants.OP_CONTENT_TYPE, context);
                    Operation operationLocal = CodegenUtils
                            .createOperation(operationName, path, operationDescription, pathParamList, queryParamList,
                                    headerParamList, cookieParamList, responseSchema);
                    addOperation(pathToResourcesDir, operationName, operationLocal);
                }
            }
        }
    }

    private static void addOperation(String pathToResourcesDir, String operationName, Operation operationLocal)
            throws IOException {

        operationList.add(operationLocal);
        context.put(Constants.OPERATION, operationLocal);
        String synapseFileName = pathToResourcesDir + "/functions/" + operationName + ".xml";
        String uischemaFileName = pathToResourcesDir + "/uischema/" + operationName + ".json";
        String outputSchemaFileName = pathToResourcesDir + "/outputschema/" + operationName + ".json";
        mergeVelocityTemplate(synapseFileName, "templates/synapse/function_template.vm");
        mergeVelocityTemplate(uischemaFileName, "templates/uischema/operation_template.vm");
        mergeVelocityTemplate(outputSchemaFileName, "templates/outputschema/operation_response_schema_template.vm");
    }

    private static void readschema(String pathToResourcesDir, String operationName, Operation operationLocal,
                            MediaType mediaType, String contentType) throws IOException {

        Schema schema = mediaType.getSchema();
        if (schema instanceof ComposedSchema) {
            operationLocal.setRequestSchema(Json.pretty(schema));
        } else {
            List<Parameter> requestParams = operationLocal.getRequestParameters();
            Map<String, Schema> properties = schema.getProperties();
            if (properties != null) {
                for (Map.Entry<String, Schema> property : properties.entrySet()) {
                    String propName;
                    if (property.getValue() != null && property.getValue().getXml() != null
                            && property.getValue().getXml().getName() != null) {
                        propName = property.getValue().getXml().getName();
                    } else if (property.getKey() != null) {
                        propName = property.getKey();
                    } else {
                        propName = "unknown_property_" + UUID.randomUUID().toString();
                    }
                    String propDescription = property.getValue().getDescription() != null ?
                            property.getValue().getDescription() + " Type: " + property.getValue().getType() :
                            " Type: " + property.getValue().getType();
                    Parameter parameter = CodegenUtils.createParameter(propName,
                            schema.getRequired() != null && schema.getRequired().contains(property.getKey()),
                            propDescription, "");
                    if ((property.getValue() instanceof ObjectSchema) || (property.getValue() instanceof ArraySchema)) {
                        parameter.setInnerSchema(Json.pretty(property.getValue()));
                    }
                    parameterNameMap.put(parameter.getParameterName(), property.getKey());
                    requestParams.add(parameter);
                }
            }
            if (Constants.APPLICATION_XML.equals(contentType)) {
                // get the root element from the xml key
                if (schema.getXml() != null && StringUtils.isNotEmpty(schema.getXml().getName())) {
                    context.put(Constants.ROOT, schema.getXml().getName());
                } else {
                    // iterate through the components.schema and get the key of the matching schema value
                    for (Map.Entry<String, Schema> entry : componentsSchema.entrySet()) {
                        if (entry.getValue() == schema) {
                            context.put(Constants.ROOT, entry.getKey());
                        }
                    }
                }
            }
        }
        addOperation(pathToResourcesDir, operationName, operationLocal);
    }

    private static VelocityContext createVelocityContext(OpenAPI openAPI) {
        context = new VelocityContext();
        initVelocityEngine();
        String connectorName = makePackageNameCompatible(openAPI.getInfo().getTitle());
        if (StringUtils.isEmpty(connectorName)) {
            connectorName = "MIConnector";
        }
        context.put("connectorName", connectorName);
        context.put("defaultUrl", openAPI.getServers().get(0).getUrl());
        String resolvedConnectorName = connectorName.toLowerCase().replace(" ", "");
        String artifactId = "org.wso2.mi.connector." + resolvedConnectorName;
        context.put("artifactId", artifactId);

        if (openAPI.getComponents() != null && openAPI.getComponents().getSecuritySchemes() != null) {
            if (openAPI.getComponents().getSecuritySchemes().containsKey("oauth2")) {
                if (openAPI.getComponents().getSecuritySchemes().get("oauth2").getFlows().getClientCredentials() != null) {
                    context.put("auth", "oauth2");
                    context.put("oauth2flow", "client_credentials");
                } else if (openAPI.getComponents().getSecuritySchemes().get("oauth2").getFlows().getAuthorizationCode() != null) {
                    context.put("auth", "oauth2");
                    context.put("oauth2flow", "authorization_code");
                } else {
                    context.put("auth", "noauth");
                }
            } else if (openAPI.getComponents().getSecuritySchemes().containsKey("basic")) {
                context.put("auth", "basic");
            } else if (openAPI.getComponents().getSecuritySchemes().containsKey("apiKey")) {
                context.put("auth", "apiKey");
                context.put("apiKeyName", openAPI.getComponents().getSecuritySchemes().get("apiKey").getName());
                context.put("apiKeyIn", openAPI.getComponents().getSecuritySchemes().get("apiKey").getIn());
            } else {
                context.put("auth", "noauth");
            }
        } else {
            context.put("auth", "noauth");
        }

        if (openAPI.getServers() != null && !openAPI.getServers().isEmpty() && openAPI.getServers().get(0) != null) {
            context.put("defaultUrl", openAPI.getServers().get(0).getUrl());
        } else {
            context.put("defaultUrl", "");
        }

        context.put("version", "1.0.0");
        context.put("groupId", "org.wso2.mi.connector");
        context.put("connectorName", resolvedConnectorName);
        return context;
    }
    private static String makePackageNameCompatible(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
    }
    
}
