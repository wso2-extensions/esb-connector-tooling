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

package org.wso2.mi.tool.connector.tools.generator.common;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.mi.tool.connector.tools.generator.common.exception.ConnectorGenException;
import org.wso2.mi.tool.connector.tools.generator.grpc.GRPCConnectorGenerator;

import java.io.IOException;

import static org.wso2.mi.tool.connector.tools.generator.openapi.ConnectorGenerator.generateConnector;

/**
 * This class is the main starting API for connector generate.
 */
public class ConnectorFactory {
    private static final Log LOG = LogFactory.getLog(ConnectorFactory.class);
    public static void main(String[] args) throws ConnectorGenException {
        if (args.length < 3 || args.length > 4) {
            throw new IllegalArgumentException("Usage: <idl_file> <outputpath> [miVersion]");
        }
        String idlFile = args[0];
        String connectorPath = args[1];
        String miVersion = args.length == 3 ? args[2] : "4.4.0";

        try {
            if (idlFile.endsWith(".proto")) {
                GRPCConnectorGenerator.generateConnector(idlFile, connectorPath, miVersion);
            } else if (idlFile.endsWith(".yaml") || idlFile.endsWith(".yml")|| idlFile.endsWith(".json")) {
                generateConnector(idlFile, connectorPath, miVersion, null);
            } else {
                LOG.error("Please provide a valid Protocol Buffer (.proto) or OpenAPI file (.yaml/.json).");
            }
        } catch (org.wso2.mi.tool.connector.tools.generator.grpc.exception.ConnectorGenException e) {
            throw new ConnectorGenException(e.getMessage());
        }
    }
}
