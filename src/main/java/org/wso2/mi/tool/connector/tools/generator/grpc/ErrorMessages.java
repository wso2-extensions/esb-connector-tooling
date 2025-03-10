package org.wso2.mi.tool.connector.tools.generator.grpc;

public enum ErrorMessages {
    GRPC_CONNECTOR_100("GRPC_CONNECTOR_100", "Provide proto file");
    private final String code;
    private final String description;

    ErrorMessages(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
