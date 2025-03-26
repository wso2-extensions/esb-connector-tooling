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

package org.wso2.mi.tool.connector.tools.generator.grpc.model;

public class CodeGeneratorMetaData {

    private final String protoFilePath;
    private final String protoFileName;
    private final String connectorPath;
    private final String miVersion;

    private CodeGeneratorMetaData(Builder builder) {
        this.protoFilePath = builder.protoFilePath;
        this.protoFileName = builder.protoFileName;
        this.connectorPath = builder.connectorPath;
        this.miVersion = builder.miVersion;
    }

    public String getProtoFilePath() {
        return protoFilePath;
    }

    public String getProtoFileName() {
        return protoFileName;
    }

    public String getConnectorPath() {
        return connectorPath;
    }

    public String getMiVersion() {
        return miVersion;
    }

    // Builder class
    public static class Builder {
        private String protoFilePath;
        private String protoFileName;
        private String connectorPath;
        private String miVersion;

        public Builder withProtoFilePath(String protoFilePath) {
            this.protoFilePath = protoFilePath;
            return this;
        }

        public Builder withProtoFileName(String protoFileName) {
            this.protoFileName = protoFileName;
            return this;
        }

        public Builder withConnectorPath(String connectorPath) {
            this.connectorPath = connectorPath;
            return this;
        }

        public Builder withMIVersion(String miVersion) {
            this.miVersion = miVersion;
            return this;
        }

        public CodeGeneratorMetaData build() {
            return new CodeGeneratorMetaData(this);
        }
    }
}
