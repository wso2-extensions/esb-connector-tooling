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
package org.wso2.mi.tool.connector.tools.generator.grpc;

import com.google.protobuf.DescriptorProtos;
import org.apache.velocity.VelocityContext;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import org.apache.velocity.app.VelocityEngine;
import org.wso2.mi.tool.connector.tools.generator.grpc.model.RPCService;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.wso2.mi.tool.connector.tools.generator.grpc.CodeGenerationUtils.getTypeName;
import static org.wso2.mi.tool.connector.tools.generator.grpc.CodeGenerationUtils.loadDescriptorSet;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.SERVICE;
import static org.wso2.mi.tool.connector.tools.generator.grpc.ProjectGeneratorUtils.generateConnectorProject;

/**
 * Generates the connector using the Protobuf spec.
 */
public class ConnectorGenerator {
//    private static final Log LOG = LogFactory.getLog(ConnectorGenerator.class);

    private static final VelocityEngine velocityEngine = new VelocityEngine();

    /**
     * Main method to generate the connector.
     *
     * @param args The arguments.
     */
    public static void main(String[] args)  {
//        if (args.length < 3 || args.length > 4) {
//            throw new IllegalArgumentException("Usage: <protobuffile> <output_dir_JavaFiles> <grpc_java> [miVersion]");
//        }
//        String protoFile = args[0];
//        // hardcode
//        String outputJavaDir = args[1];
//        String grpcJavaDir = args[2];
//        String miVersion = args.length == 4 ? args[3] : "4.4.0";
//
//        String connectorPath = "./";
        try {
//        if (!protoFile.endsWith("**.proto")) {
//            throw new ConnectorGenException(ErrorMessages.GRPC_CONNECTOR_100.getDescription());
//        }
            generateConnector("/Users/hansaninissanka/Desktop/GRPC/Protofiles/bookcpy.proto", "/Users/hansaninissanka/Desktop/GRPC/",
                    "4.4.0", "/Users/hansaninissanka/Desktop/GRPC/");
        } catch (IOException | InterruptedException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates the connector using the OpenAPI spec.
     *
     * @param protoFile The OpenAPI spec.
     * @param connectorPath The output directory.
     * @param miVersion The MI version (default is 4.4.0 if not provided).
     * @param projectHome The Integration project home.
     * @return The path to the generated connector.
     */
    public static void generateConnector(String protoFile, String connectorPath, String miVersion, String projectHome)
            throws IOException, InterruptedException {

        // 1. Download + Extract protoc
        Path protocPath = ProtocExecutor.downloadAndExtractProtoc();
        // 2. Download + Mark executable the gRPC plugin
        Path grpcPluginPath = ProtocExecutor.downloadGrpcPlugin();
        Path protoFPath = Paths.get(protoFile);
        String protoSourceDir = protoFPath.getParent().toString();
        String protoFileName = protoFPath.getFileName().toString();

        String tempOutputDir = protoSourceDir + "/generated";

        // 3. Run protoc to generate Java & gRPC stubs
        boolean success = ProtocExecutor.runProtoc(
                protocPath.toFile(),
                grpcPluginPath.toFile(),
                protoSourceDir,
                protoFileName,
                tempOutputDir
        );
        System.out.println("Protoc execution " + (success ? "succeeded" : "failed"));
        FileDescriptorSet fileDescriptorSet = loadDescriptorSet(tempOutputDir + "/Descriptor.desc");
        VelocityContext velocityForProtoFile = createVelocityForProtoFile(fileDescriptorSet, protoFileName);
        generateConnectorProject(protoFileName, projectHome, miVersion, connectorPath, velocityEngine,
                velocityForProtoFile, tempOutputDir);

    }

    private static VelocityContext createVelocityForProtoFile(FileDescriptorSet descriptorSet, String protoFileName) {
        VelocityContext context = new VelocityContext();
        initVelocityEngine();
        // Iterate through each proto file
        for (com.google.protobuf.DescriptorProtos.FileDescriptorProto fileProto : descriptorSet.getFileList()) {
            String name = fileProto.getName();
            String javaOuterClassname = fileProto.getOptions().getJavaOuterClassname();
            boolean javaMultipleFiles = fileProto.getOptions().getJavaMultipleFiles();
            context.put("isJavaMultipleFiles", javaMultipleFiles);
            if (javaOuterClassname != null && !javaOuterClassname.isEmpty()) {
                context.put("javaGrpcStubFile", javaOuterClassname);
                context.put("isJavaGrpcStubFile", true);
            } else {
                context.put("isJavaGrpcStubFile", false);
            }

            if (!name.equals(protoFileName)) {
                continue;
            }
            context.put("protoFileName", name);
            String aPackage = fileProto.getPackage();
            context.put("package", aPackage);
            Map<String, DescriptorProtos.DescriptorProto> messageTypeMap =
                    fileProto.getMessageTypeList().stream()
                            .collect(Collectors.toMap(
                                    DescriptorProtos.DescriptorProto::getName,
                                    descriptorProto -> descriptorProto));

            List<DescriptorProtos.ServiceDescriptorProto> serviceList = fileProto.getServiceList();
            for (DescriptorProtos.ServiceDescriptorProto service: serviceList) {
                String serviceName = service.getName();
                String resolvedConnectorName = serviceName.toLowerCase().replace(" ", "");
                updateConnectorMetaInfo(context, serviceName, resolvedConnectorName);

                List<DescriptorProtos.MethodDescriptorProto> methodList = service.getMethodList();
                Map<String, RPCService.RPCCall> rcpMap = new HashMap<>();
                for (DescriptorProtos.MethodDescriptorProto method: methodList) {
                    populateRPCcall(fileProto, messageTypeMap, rcpMap, method);
                }
                RPCService rpcService = new RPCService.Builder()
                        .serviceName(serviceName)
                        .addRpcCall(rcpMap)
                        .build();
                context.put(SERVICE, rpcService);
            }
        }
        return context;
    }

    private static void populateRPCcall(DescriptorProtos.FileDescriptorProto fileProto,
                                        Map<String, DescriptorProtos.DescriptorProto> messageTypeMap,
                                        Map<String, RPCService.RPCCall> rcpMap,
                                        DescriptorProtos.MethodDescriptorProto method) {
        String methodName = method.getName();
        String inputType = method.getInputType();
        String packageName = fileProto.getPackage();
        String inputTypeName = getTypeName(inputType, packageName);
        Map<String, DescriptorProtos.FieldDescriptorProto> inputFields = fieldMap(messageTypeMap, inputTypeName);
        String outputType = method.getOutputType();
        String outputName = getTypeName(outputType, packageName);
        Map<String, DescriptorProtos.FieldDescriptorProto> outputFields = fieldMap(messageTypeMap,
                getTypeName(outputType, packageName));
        RPCService.RPCCall.RPCCallBuilder callBuilder = new RPCService.RPCCall.RPCCallBuilder()
                .rpcCallName(methodName)
                .inputName(inputTypeName)
                .outputName(outputName)
                .addInputParam(inputFields)
                .addOutputParam(outputFields);
        RPCService.RPCCall rcpCall = callBuilder.build();
        rcpMap.put(methodName, rcpCall);
    }

    private static void updateConnectorMetaInfo(VelocityContext context, String serviceName, String resolvedConnectorName) {
        String artifactId = "org.wso2.mi.connector." + resolvedConnectorName;
        context.put("artifactId", artifactId);
        context.put("version", "1.0.0");
        context.put("groupId", "org.wso2.mi.connector");
        context.put("connectorName", resolvedConnectorName);
        context.put("serviceName", serviceName);
    }

    private static Map<String, DescriptorProtos.FieldDescriptorProto> fieldMap(Map<String, DescriptorProtos.DescriptorProto> messageTypeMap, String result) {
        Map<String, DescriptorProtos.FieldDescriptorProto> inputFields = new HashMap<>();
        DescriptorProtos.DescriptorProto descriptorProto = messageTypeMap.get(result);
        List<DescriptorProtos.FieldDescriptorProto> fieldList = descriptorProto.getFieldList();
        if (!fieldList.isEmpty()) {
            for (DescriptorProtos.FieldDescriptorProto field: fieldList) {
                inputFields.put(field.getName(), field);
            }
        }
        return inputFields;
    }

    private static void initVelocityEngine() {
        Properties properties = new Properties();
        properties.setProperty("resource.loader", "class");
        properties.setProperty("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine.init(properties);
    }
}
