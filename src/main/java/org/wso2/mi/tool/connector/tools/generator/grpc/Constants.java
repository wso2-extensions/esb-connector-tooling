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

public class Constants {
    public static final String OS_NAME_SYSTEM_PROPERTY = "os.name";
    public static final String OS_ARCH_SYSTEM_PROPERTY = "os.arch";
    public static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    public static final String OS_ARCH = System.getProperty("os.arch").toLowerCase();

    //context keywords
    public static final String ARTIFACTS = "artifactId";
    public static final String CONNECTOR_NAME = "connectorName";
    public static final String OUTPUT_FIELD_METHODS = "outputFMethods";
    public static final String INPUT_FIELD_METHODS = "inputFMethods";

    public static final String SERVICE = "service";
    public static final String HAS_RESPONSE_MODEL = "hasResponseModel";
    public static final String TEMP_COMPILE_DIRECTORY = "_grpc_compile";
    public static final String METHODS_WITH_ARRAY = "methodsWithArray";

}
