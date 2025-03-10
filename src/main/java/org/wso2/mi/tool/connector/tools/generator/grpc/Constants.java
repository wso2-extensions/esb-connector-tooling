package org.wso2.mi.tool.connector.tools.generator.grpc;

import java.io.File;

public class Constants {
    public static final String OS_NAME_SYSTEM_PROPERTY = "os.name";
    public static final String OS_ARCH_SYSTEM_PROPERTY = "os.arch";
    public static final String FILE_SEPARATOR = File.separator;
    public static final String EMPTY_STRING = "";
    public static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    public static final String OS_ARCH = System.getProperty("os.arch").toLowerCase();

    //context keywords
    public static final String ARTIFACTS = "artifactId";
    public static final String CONNECTOR_NAME = "connectorName";
    public static final  String OUTPUT_FIELD_METHODS= "outputFMethods";
    public static final String SERVICE = "service";
    public static final String HAS_RESPONSE_MODEL = "hasResponseModel";
    public static final String TEMP_COMPILE_DIRECTORY = "_grpc_compile";


}
