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
package org.wso2.mi.tool.connector.tools.generator.openapi;

import org.wso2.mi.tool.connector.tools.generator.openapi.utils.ConnectorBuilderUtils;
import org.wso2.mi.tool.connector.tools.generator.openapi.utils.ProjectGeneratorUtils;

/**
 * Generates the connector using the OpenAPI spec.
 */
public class ConnectorGenerator {

    /**
     * Main method to generate the connector.
     *
     * @param args The arguments.
     */
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException("Usage: <openApiSpec> <outputDir> [miVersion]");
        }
        String openApiSpec = args[0];
        String outputDir = args[1];
        String miVersion = null;
        if (args.length == 3) {
            miVersion = args[2];
        }

        generateConnector(openApiSpec, outputDir, miVersion);
    }

    /**
     * Generates the connector using the OpenAPI spec.
     *
     * @param openApiSpec The OpenAPI spec.
     * @param outputDir The output directory.
     * @return The path to the generated connector.
     */
    // Add miVersion default value to 4.4.0
    public static String generateConnector(String openApiSpec, String outputDir, String miVersion) {
        if (miVersion == null) {
            miVersion = "4.4.0";
        }
        String connectorPath;
        try {
            String connectorProjectDir = ProjectGeneratorUtils.generateConnectorProject(openApiSpec, outputDir,miVersion);
            connectorPath =  ConnectorBuilderUtils.buildConnector(connectorProjectDir);
        } catch (ConnectorGenException e) {
            throw new RuntimeException("Error occurred while generating the connector", e);
        }
        return connectorPath;
    }
}
