package org.wso2.mi.tool.connector.tools.generator.grpc.model;

public class CodeGeneratorMetaData {

    private final String protofilePath;
    private final String javaoutPath;
    private final String grpcoutPath;
    private final String miVersion;

    private CodeGeneratorMetaData(Builder builder) {
        this.protofilePath = builder.protofielPath;
        this.javaoutPath = builder.javaoutPath;
        this.grpcoutPath = builder.grpcoutPath;
        this.miVersion = builder.miVersion;
    }

    // Getters (optional, depending on your needs)
    public String getProtofilePath() {
        return protofilePath;
    }

    public String getJavaoutPath() {
        return javaoutPath;
    }

    public String getGrpcoutPath() {
        return grpcoutPath;
    }

    public String getMiVersion() {
        return miVersion;
    }

    // Builder class
    public static class Builder {
        private String protofielPath;
        private String javaoutPath;
        private String grpcoutPath;
        private String miVersion;

        public Builder withProtofielPath(String protofielPath) {
            this.protofielPath = protofielPath;
            return this;
        }

        public Builder withJavaoutPath(String javaoutPath) {
            this.javaoutPath = javaoutPath;
            return this;
        }

        public Builder withGrpcoutPath(String grpcoutPath) {
            this.grpcoutPath = grpcoutPath;
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
