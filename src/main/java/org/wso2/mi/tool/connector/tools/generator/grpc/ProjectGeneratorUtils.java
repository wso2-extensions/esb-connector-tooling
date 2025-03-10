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
package org.wso2.mi.tool.connector.tools.generator.grpc;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.wso2.mi.tool.connector.tools.generator.grpc.model.RPCService;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.wso2.mi.tool.connector.tools.generator.grpc.CodeGenerationUtils.capitalizeFirstLetter;
import static org.wso2.mi.tool.connector.tools.generator.grpc.CodeGenerationUtils.deleteDirectory;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.ARTIFACTS;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.CONNECTOR_NAME;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.HAS_RESPONSE_MODEL;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.OUTPUT_FIELD_METHODS;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.SERVICE;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.TEMP_COMPILE_DIRECTORY;

/**
 * The VelocityConfigGenerator handles the generation of the openapi connector
 * project files.
 */
public class ProjectGeneratorUtils {

    private static final Log log = LogFactory.getLog(ProjectGeneratorUtils.class);
    private static List<RPCService.RPCCall> operationList = new ArrayList<>();
    private static URLClassLoader classLoader;

    public static void generateConnectorProject(String protoPath, String projectPath, String miVersion,
                                                  String integrationProjectPath, VelocityEngine engine,
                                                  VelocityContext context, String tempJavaPath) {

        String pathToConnectorDir;
        try {
            String artifactId = (String) context.get(ARTIFACTS);
            String connectorName = context.get(CONNECTOR_NAME).toString();
            pathToConnectorDir = projectPath + "generated/" + artifactId;
            String pathToMainDir = pathToConnectorDir +
                    "/src/main/java/org/wso2/carbon/" + connectorName + "connector";

            String pathToResourcesDir = pathToConnectorDir + "/src/main/resources";
            context.put(HAS_RESPONSE_MODEL, false);
            if (miVersion != null && miVersion.compareTo("4.4.0") >= 0) {
                context.put(HAS_RESPONSE_MODEL, true);
            }
            createConnectorDirectory(pathToConnectorDir, pathToMainDir, pathToResourcesDir, tempJavaPath, context);
            copyConnectorStaticFiles(pathToConnectorDir, pathToResourcesDir, engine, context);
            generateInitJsonFiles(pathToResourcesDir, engine, context);
            generateConnectorConfig(pathToConnectorDir, pathToResourcesDir, pathToMainDir, engine, context);
            String pathToGrpcProtocDir = pathToConnectorDir +
                    "/src/main/java/org/wso2/carbon/" + connectorName + "connector/" + context.get("package").toString().replace(".", "/");
            // Create a Temp Directory for Compilation
            Path tempDir = Files.createTempDirectory(TEMP_COMPILE_DIRECTORY);

            // Collect file paths and names as pairs
            List<AbstractMap.SimpleEntry<String, String>> javaFiles = Files.walk(Paths.get(pathToGrpcProtocDir))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(path -> new AbstractMap.SimpleEntry<>(path.toString(), path.getFileName().toString()))
                    .collect(Collectors.toList());

            // Iterate and print
            List<String> javaFilePaths = new ArrayList<>();
            for (AbstractMap.SimpleEntry<String, String> file : javaFiles) {
                javaFilePaths.add(file.getKey());
                String fileName = file.getValue();
                if (fileName.endsWith("Grpc.java")) {
                    context.put("javaGrpcServerFile", fileName.replace(".java",""));
                    context.put("javaGrpcServerFilePath", file.getKey());
                } else {
                    if(!(boolean)context.get("isJavaGrpcStubFile")) {
                        context.put("javaGrpcStubFile", fileName.replace(".java",""));
                    }

                }
            }

            // Convert list to array for JavaCompiler
            String[] compileArgs = new String[javaFilePaths.size() + 2];
            compileArgs[0] = "-d";
            compileArgs[1] = tempDir.toString();
            for (int i = 0; i < javaFilePaths.size(); i++) {
                compileArgs[i + 2] = javaFilePaths.get(i);
            }

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                return;
            }
            int result = compiler.run(null, null, null, compileArgs);
            if (result != 0) {
                //todo error
                return;
            }

            classLoader = URLClassLoader.newInstance(new URL[]{tempDir.toFile().toURI().toURL()});
            generateRPCfunctions(pathToMainDir, pathToResourcesDir, engine, context);
            deleteDirectory(tempDir);

            copyGenResources(protoPath, pathToConnectorDir, engine, context);
            if (integrationProjectPath != null) {
                copyMavenArtifacts(pathToConnectorDir, integrationProjectPath);
            }
        } catch (IOException e) {
        }
    }

    private static void generateJavaMediators(VelocityContext context) {
        RPCService.RPCCall rpcCall = (RPCService.RPCCall) context.get("rpcCall");
        //output and input message
        String inputName = rpcCall.getOutputName();
        String inputFileName = context.get("package") + "." + context.get("javaGrpcStubFile") + "$" +
                capitalizeFirstLetter(inputName) ;
        Class<?> outputClass = null;
        try {
            outputClass = Class.forName(inputFileName, true, classLoader);
        } catch (ClassNotFoundException e) {
            //todo: error message
        }
        Set<String> outputs = rpcCall.getOutput().keySet();
        Map<String, String> outputMethods = new HashMap<>();
        Method[] methods = outputClass.getMethods();
        // Create a lookup map for faster method matching
        Map<String, Method> methodMap = Arrays.stream(methods)
                .collect(Collectors.toMap(Method::getName, Function.identity(), (m1, m2) -> m1));

        // Map each field to its getter method
        for (String fieldName : outputs) {
            String expectedGetterName = "get" + capitalizeFirstLetter(fieldName);

            if (methodMap.containsKey(expectedGetterName)) {
                outputMethods.put(fieldName, expectedGetterName);
            }
        }
        context.put(OUTPUT_FIELD_METHODS, outputMethods);
    }


    private static void generateRPCfunctions(String pathToMainDir, String pathToResourcesDir, VelocityEngine engine, VelocityContext context) throws IOException {
        RPCService service = (RPCService) context.get(SERVICE);
        Map<String, RPCService.RPCCall> rpcCallsMap = service.getRpcCallsMap();
        for (Map.Entry<String, RPCService.RPCCall> rpc: rpcCallsMap.entrySet()) {
            addOperation(pathToMainDir, pathToResourcesDir, rpc.getKey(), rpc.getValue(), engine, context);
        }

    }

    private static void createConnectorDirectory(String pathToConnectorDir, String pathToMainDir,
                                                 String pathToResourcesDir, String tempJavaPath,
                                                 VelocityContext context)
            throws IOException {

        Files.createDirectories(Paths.get(pathToConnectorDir));
        Files.createDirectories(Paths.get(pathToMainDir));
        moveGeneratedFiles(tempJavaPath, pathToMainDir);
        Files.createDirectories(Paths.get(pathToConnectorDir + "/docs"));
        Files.createDirectories(Paths.get(pathToConnectorDir + "/gen_resources"));
        Files.createDirectories(Paths.get(pathToResourcesDir + "/config"));
        Files.createDirectories(Paths.get(pathToResourcesDir + "/functions"));
        Files.createDirectories(Paths.get(pathToResourcesDir + "/icon"));
        Files.createDirectories(Paths.get(pathToResourcesDir + "/uischema"));
        if ("true".equals(String.valueOf(context.get("hasResponseModel")))) {
            Files.createDirectories(Paths.get(pathToResourcesDir + "/outputschema"));
        }
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

    private static void copyConnectorStaticFiles(String pathToConnectorDir, String pathToResourcesDir,
                                                 VelocityEngine engine,
                                                 VelocityContext context) throws IOException {

        ClassLoader classLoader = ProjectGeneratorUtils.class.getClassLoader();

        // create assembly directory
        createAssemblyDirectory(pathToConnectorDir + "/src/main/assembly");

        // Copy connector files
        try (InputStream staticFilesStream = classLoader.getResourceAsStream("connector-files/src/main/assembly/assemble-connector.xml")) {
            if (staticFilesStream != null) {
                Files.copy(staticFilesStream, Paths.get(pathToConnectorDir + "/src/main/assembly/assemble-connector.xml"), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        // Determine the template based on the auth type
        String outputFile;
        String template = "templates/grpc/synapse/init_config.vm";

        outputFile = pathToResourcesDir + "/config/init.xml";
        mergeVelocityTemplate(outputFile, template, engine, context);

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

    private static void mergeVelocityTemplate(String outputFile, String template, VelocityEngine velocityEngine,
                                              VelocityContext context) throws IOException {

        File file = new File(outputFile);
        Writer writer = new FileWriter(file);
        velocityEngine.mergeTemplate(template, "UTF-8", context, writer);
        writer.flush();
        writer.close();
    }

    private static void generateConnectorConfig(String pathToConnectorDir, String pathToResourcesDir,
                                                String pathToMainDir, VelocityEngine engine, VelocityContext context)
            throws IOException {

        String outputFile = pathToConnectorDir + "/pom.xml";
        String template = "templates/grpc/synapse/pom_template.vm";
        mergeVelocityTemplate(outputFile, template, engine, context);

        outputFile = pathToResourcesDir + "/connector.xml";
        template = "templates/grpc/synapse/connector_template.vm";
        mergeVelocityTemplate(outputFile, template, engine, context);

        outputFile = pathToResourcesDir + "/functions/component.xml";
        template = "templates/grpc/synapse/component_functions_template.vm";
        mergeVelocityTemplate(outputFile, template, engine, context);

        outputFile = pathToResourcesDir + "/config/component.xml";
        template = "templates/grpc/synapse/component_init_template.vm";
        mergeVelocityTemplate(outputFile, template, engine, context);

        generateJavaMainFiles(pathToMainDir, engine, context);
        generateDocs(pathToConnectorDir, engine, context);
    }

    private static void copyGenResources(String protoFilePath, String pathToConnectorDir,
                                         VelocityEngine engine, VelocityContext context) throws IOException {

        String genResourcesPath = pathToConnectorDir + "/gen_resources";
        String outputFile = genResourcesPath + "/README.md";

        String template = "templates/md/gen_resources_template.vm";
        mergeVelocityTemplate(outputFile, template, engine, context);

        FileUtils.copyFileToDirectory(new File(protoFilePath), new File(genResourcesPath));
    }

    private static void generateInitJsonFiles(String pathToResourcesDir, VelocityEngine engine,
                                              VelocityContext context) throws IOException {
        String outputFile = pathToResourcesDir + "/uischema/init.json";
        String template = "templates/grpc/uischema/init_json.vm";
        mergeVelocityTemplate(outputFile, template, engine, context);
    }


    private static void generateJavaMainFiles(String pathToMainDir, VelocityEngine engine, VelocityContext context)
            throws IOException {

        String outputFile = pathToMainDir + "/GRPCChannelBuilder.java";
        String template = "templates/grpc/java/grpc_channel_builder.vm";
        mergeVelocityTemplate(outputFile, template, engine, context);
        String converter = pathToMainDir + "/TypeConverter.java";
        String converterTemplate = "templates/grpc/java/type_converter.vm";
        mergeVelocityTemplate(converter, converterTemplate, engine, context);
    }

    private static void generateDocs(String pathToConnectorDir, VelocityEngine engine, VelocityContext context)
            throws IOException {

        String outputFile = pathToConnectorDir + "/docs/config.md";
        String template = "templates/md/config_template.vm";
        mergeVelocityTemplate(outputFile, template, engine, context);

        outputFile = pathToConnectorDir + "/docs/operations.md";
        template = "templates/md/operations_template.vm";
        mergeVelocityTemplate(outputFile, template, engine, context);

        outputFile = pathToConnectorDir + "/README.md";
        template = "templates/md/readme_template.vm";
        mergeVelocityTemplate(outputFile, template, engine, context);
    }

    private static void addOperation(String pathToMain, String pathToResourcesDir, String operationName, RPCService.RPCCall rpcCall,
                                     VelocityEngine engine, VelocityContext context) throws IOException {
        operationList.add(rpcCall);
        context.put("rpcCall", rpcCall);
        String synapseFileName = pathToResourcesDir + "/functions/" + operationName + ".xml";
        String uischemaFileName = pathToResourcesDir + "/uischema/" + operationName + ".json";
        generateJavaMediators(context);
        String javaMediator = pathToMain + "/" + capitalizeFirstLetter(rpcCall.getRpcCallName()) + "Mediator.java";
        mergeVelocityTemplate(javaMediator, "templates/grpc/java/rpc_java_mediator.vm", engine, context);
        mergeVelocityTemplate(synapseFileName, "templates/grpc/synapse/rpc_function.vm", engine, context);
        mergeVelocityTemplate(uischemaFileName, "templates/grpc/uischema/operation_template.vm", engine, context);
        if (context.get("hasResponseModel").equals("true")) {
            String outputSchemaFileName = pathToResourcesDir + "/outputschema/" + operationName + ".json";
            mergeVelocityTemplate(outputSchemaFileName,
                    "templates/outputschema/operation_response_schema_template.vm", engine, context);
        }
    }

    private static String makePackageNameCompatible(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
    }

    private static void copyMavenArtifacts(String pathToConnectorDir, String integrationProjectPath) throws IOException {
        String[] wrapperFiles = {"mvnw", "mvnw.cmd", ".mvn"};

        for (String fileName : wrapperFiles) {
            File source = new File(integrationProjectPath, fileName);
            File destination = new File(pathToConnectorDir, fileName);

            if (source.exists()) {
                if (source.isDirectory()) {
                    copyDirectory(source.toPath(), destination.toPath());
                } else {
                    Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                log.info("Copied Maven wrapper artifact: " + fileName);
            }
        }
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(srcPath -> {
            Path destPath = target.resolve(source.relativize(srcPath));
            try {
                if (Files.isDirectory(srcPath)) {
                    Files.createDirectories(destPath);
                } else {
                    Files.copy(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                log.error("Error occurred while copying directory: " + e.getMessage());
            }
        });
    }

    private static void moveGeneratedFiles(String sourceDir, String destinationDir) {
        File src = new File(sourceDir);
        File dest = new File(destinationDir);

        if (!src.exists()) {
            System.out.println("Source directory does not exist: " + sourceDir);
            return;
        }

        if (!dest.exists()) {
            dest.mkdirs();
        }

        try {
            Files.walk(src.toPath())
                    .filter(Files::isRegularFile)
                    .forEach(source -> {
                        try {
                            Path relativePath = src.toPath().relativize(source);
                            Path target = dest.toPath().resolve(relativePath);

                            Files.createDirectories(target.getParent());

                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("Copied: " + source + " to " + target);

                            Files.delete(source);
                        } catch (IOException e) {
                            System.err.println("Error processing file: " + source + " - " + e.getMessage());
                        }
                    });

            Files.walk(src.toPath())
                    .sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(src.toPath()))
                    .forEach(path -> {
                        try {
                            if (Files.isDirectory(path)) {
                                if (Files.list(path).count() == 0) {
                                    Files.delete(path);
                                    System.out.println("Deleted directory: " + path);
                                } else {
                                    System.out.println("Directory not empty, skipping: " + path);
                                }
                            }
                        } catch (IOException e) {
                            System.err.println("Error deleting directory: " + path + " - " + e.getMessage());
                        }
                    });

            try {
                if (Files.list(src.toPath()).count() == 0) {
                    Files.delete(src.toPath());
                } else {
                    System.out.println("Source directory not empty after processing, cannot delete: " + src.toPath());
                }
            } catch (IOException e) {
                System.err.println("Error deleting source directory: " + src.toPath() + " - " + e.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
