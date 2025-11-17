/*
 *  Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.mi.tool.connector.tools.generator.grpc;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.ProtocolStringList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.wso2.mi.tool.connector.tools.generator.grpc.exception.ConnectorGenException;
import org.wso2.mi.tool.connector.tools.generator.grpc.model.CodeGeneratorMetaData;
import org.wso2.mi.tool.connector.tools.generator.grpc.model.RPCService;
import org.wso2.mi.tool.connector.tools.generator.common.utils.ConnectorBuilderUtils;
import org.wso2.mi.tool.connector.tools.generator.grpc.utils.CodeGenerationUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.ARTIFACTS;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.CONNECTOR_NAME;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.CONNECTOR_VERSION;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.GROUP_ID;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.IS_JAVA_GRPC_STUB_FILE;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.IS_JAVA_MULTIPLE_FILES;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.JAVA_GRPC_STUB_FILE;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.JAVA_PACKAGE;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.PACKAGE;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.PROTO_FILE_NAME;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.SERVICE_NAME;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.TEMP_JAVA_DIRECTORY;
import static org.wso2.mi.tool.connector.tools.generator.grpc.utils.CodeGenerationUtils.buildTypeIndex;
import static org.wso2.mi.tool.connector.tools.generator.grpc.utils.CodeGenerationUtils.loadDescriptorSet;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.SERVICE;
import static org.wso2.mi.tool.connector.tools.generator.grpc.utils.CodeGenerationUtils.resolveJavaFqn;
import static org.wso2.mi.tool.connector.tools.generator.grpc.utils.CodeGenerationUtils.validateOrThrow;
import static org.wso2.mi.tool.connector.tools.generator.grpc.utils.ProjectGeneratorUtils.generateConnectorProject;

/**
 * Generates the connector using the Protobuf spec.
 */
public class GRPCConnectorGenerator {
    private static final Log LOG = LogFactory.getLog(GRPCConnectorGenerator.class);

    private static final VelocityEngine velocityEngine = new VelocityEngine();
    private static Map<String, CodeGenerationUtils.FileAndMsg> typeIndex = null;

    /**
     * Generates the connector using the OpenAPI spec.
     *
     * @param protoFile The proto spec.
     * @param connectorPath The output directory.
     * @param miVersion The MI version (default is 4.4.0 if not provided).
     * @param integrationProjectPath The integration project path.
     * @throws ConnectorGenException If an error occurs during connector generation.
     */
    public static String generateConnector(String protoFile, String connectorPath, String miVersion, String integrationProjectPath) throws ConnectorGenException {

        // 1. Download + Extract protoc
        try {
            Path protocPath = ProtocExecutor.downloadAndExtractProtoc();
            // 2. Download + Mark executable the gRPC plugin
            Path grpcPluginPath = ProtocExecutor.downloadGrpcPlugin();
            Path protoFPath = Paths.get(protoFile);
            String protoSourceDir = protoFPath.getParent().toString();
            String protoFileName = protoFPath.getFileName().toString();

            String tempOutputDir = Files.createTempDirectory(TEMP_JAVA_DIRECTORY).toString();

            // 3. Run protoc to generate Java & gRPC stubs
            boolean success = ProtocExecutor.runProtoc(
                    protocPath.toFile(),
                    grpcPluginPath.toFile(),
                    protoSourceDir,
                    protoFileName,
                    tempOutputDir,
                    null
            );
            LOG.info("Protoc execution " + (success ? "succeeded" : "failed"));
            if (!success) {
                return null;
            }
            FileDescriptorSet fileDescriptorSet = loadDescriptorSet(tempOutputDir + "/Descriptor.desc");
            //Handle proto dependencies
            for (DescriptorProtos.FileDescriptorProto fileDescriptorProto:fileDescriptorSet.getFileList()) {
                ProtocolStringList dependencyList = fileDescriptorProto.getDependencyList();
                if (fileDescriptorProto.getName().contains("google/protobuf/")) {
                    // Skip standard protobuf dependencies
                    continue;
                }
                boolean success1 = ProtocExecutor.runProtoc(
                        protocPath.toFile(),
                        grpcPluginPath.toFile(),
                        protoSourceDir,
                        fileDescriptorProto.getName(),
                        tempOutputDir,
                        dependencyList
                );
                if (!success1) {
                    LOG.error(ErrorMessages.GRPC_CONNECTOR_104.getDescription());
                    return null;
                }
            }
            typeIndex = buildTypeIndex(fileDescriptorSet);
            VelocityContext velocityForProtoFile = createVelocityForProtoFile(fileDescriptorSet, protoFileName);
            CodeGeneratorMetaData metaData = new CodeGeneratorMetaData.Builder()
                    .withMIVersion(miVersion)
                    .withProtoFilePath(protoFile)
                    .withConnectorPath(connectorPath)
                    .withProtoFileName(protoFileName)
                    .build();
            String connectorProjectDir = generateConnectorProject(metaData, velocityEngine, velocityForProtoFile, tempOutputDir, integrationProjectPath);
            if (connectorProjectDir == null) {
                throw new ConnectorGenException(ErrorMessages.GRPC_CONNECTOR_101.getDescription());
            }
            connectorPath = ConnectorBuilderUtils.buildConnector(connectorProjectDir);
        } catch (IOException | InterruptedException e) {
            throw new ConnectorGenException(e.getMessage());
        }
        return connectorPath;
    }

    private static VelocityContext createVelocityForProtoFile(FileDescriptorSet descriptorSet, String protoFileName)
            throws ConnectorGenException {
        VelocityContext context = new VelocityContext();
        initVelocityEngine();
        // Iterate through each proto file
        for (com.google.protobuf.DescriptorProtos.FileDescriptorProto fileProto : descriptorSet.getFileList()) {
            String name = fileProto.getName();
            if (!name.equals(protoFileName)) {
                continue;
            }
            context.put(PROTO_FILE_NAME, name);
            String javaOuterClassname = fileProto.getOptions().getJavaOuterClassname();
            boolean javaMultipleFiles = fileProto.getOptions().getJavaMultipleFiles();

            if (fileProto.getOptions().hasJavaPackage()) {
                String javaPackage = fileProto.getOptions().getJavaPackage();
                validateOrThrow(javaPackage);
                context.put(JAVA_PACKAGE, javaPackage);
            }
            context.put(IS_JAVA_MULTIPLE_FILES, javaMultipleFiles);
            if (javaOuterClassname != null && !javaOuterClassname.isEmpty()) {
                context.put(JAVA_GRPC_STUB_FILE, javaOuterClassname);
                context.put(IS_JAVA_GRPC_STUB_FILE, true);
            } else {
                context.put(IS_JAVA_GRPC_STUB_FILE, false);
            }
            String aPackage = fileProto.getPackage();
            context.put(PACKAGE, aPackage);
            Map<String, DescriptorProtos.DescriptorProto> messageTypeMap =
                    fileProto.getMessageTypeList().stream()
                            .collect(Collectors.toMap(
                                    DescriptorProtos.DescriptorProto::getName,
                                    descriptorProto -> descriptorProto));

            List<DescriptorProtos.ServiceDescriptorProto> serviceList = fileProto.getServiceList();
            if (serviceList.isEmpty()) {
                throw new ConnectorGenException("Given proto has no services to generate a connector.");
            }
            for (DescriptorProtos.ServiceDescriptorProto service : serviceList) {
                String serviceName = service.getName();
                String resolvedConnectorName = serviceName.toLowerCase().replace(" ", "");
                updateConnectorMetaInfo(context, serviceName, resolvedConnectorName);

                List<DescriptorProtos.MethodDescriptorProto> methodList = service.getMethodList();
                Map<String, RPCService.RPCCall> rcpMap = new HashMap<>();
                for (DescriptorProtos.MethodDescriptorProto method : methodList) {
                    if (method.getServerStreaming() || method.getClientStreaming()) {
                        LOG.warn(ErrorMessages.GRPC_CONNECTOR_102.format(method.getName()));
                    } else {
                        populateRPCcall(rcpMap, method);
                    }
                }
                RPCService rpcService = new RPCService.Builder()
                        .serviceName(serviceName)
                        .addRpcCall(rcpMap)
                        .build();
                context.put(SERVICE, rpcService);
            }
            break;
        }
        return context;
    }

    private static void populateRPCcall(Map<String, RPCService.RPCCall> rcpMap,
                                        DescriptorProtos.MethodDescriptorProto method) {
        String methodName = method.getName();
        String inputType = method.getInputType();
        String outputType = method.getOutputType();
        String inJava = resolveJavaFqn(typeIndex, inputType);
        String outJava = resolveJavaFqn(typeIndex, outputType);
        RPCService.RPCCall.RPCCallBuilder callBuilder = new RPCService.RPCCall.RPCCallBuilder()
                .rpcCallName(methodName)
                .inputName(inJava)
                .outputName(outJava);
        RPCService.RPCCall rcpCall = callBuilder.build();
        rcpMap.put(methodName, rcpCall);
    }

    private static void updateConnectorMetaInfo(VelocityContext context, String serviceName, String resolvedConnectorName) {
        String artifactId = "org.wso2.mi.connector." + resolvedConnectorName;
        context.put(ARTIFACTS, artifactId);
        context.put(CONNECTOR_VERSION, "1.0.0");
        context.put(GROUP_ID, "org.wso2.mi.connector");
        context.put(CONNECTOR_NAME, resolvedConnectorName);
        context.put(SERVICE_NAME, serviceName);
    }

    private static void initVelocityEngine() {
        Properties properties = new Properties();
        properties.setProperty("resource.loader", "class");
        properties.setProperty("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine.init(properties);
    }
}
