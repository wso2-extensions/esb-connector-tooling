package org.wso2.mi.tool.connector.tools.generator.grpc.model;

import com.google.protobuf.DescriptorProtos;

import java.util.HashMap;
import java.util.Map;
public class RPCService {
    private final String serviceName;
    private final Map<String, RPCCall> rpcCallsMap;

    private RPCService(Builder builder) {
        this.serviceName = builder.serviceName;
        this.rpcCallsMap = builder.rpcCallsMap;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Map<String, RPCCall> getRpcCallsMap() {
        return rpcCallsMap;
    }

    public static class RPCCall {
        private final String rpcCallName;
        private final String inputName;
        private final String outputName;
        private final Map<String, DescriptorProtos.FieldDescriptorProto> input;
        private final Map<String, DescriptorProtos.FieldDescriptorProto> output;

        private RPCCall(RPCCallBuilder builder) {
            this.rpcCallName = builder.rpcCallName;
            this.inputName = builder.inputName;
            this.outputName = builder.outputName;
            this.input = builder.input;
            this.output = builder.output;
        }

        public String getRpcCallName() {
            return rpcCallName;
        }

        public String getInputName() {
            return inputName;
        }

        public String getOutputName() {
            return outputName;
        }

        public Map<String, DescriptorProtos.FieldDescriptorProto> getInput() {
            return input;
        }

        public Map<String, DescriptorProtos.FieldDescriptorProto> getOutput() {
            return output;
        }

        public static class RPCCallBuilder {
            private String rpcCallName;
            private String inputName;
            private String outputName;
            private Map<String, DescriptorProtos.FieldDescriptorProto> input = new HashMap<>();
            private Map<String, DescriptorProtos.FieldDescriptorProto> output = new HashMap<>();

            public RPCCallBuilder rpcCallName(String rpcCallName) {
                this.rpcCallName = rpcCallName;
                return this;
            }
            public RPCCallBuilder inputName(String inputName) {
                this.inputName = inputName;
                return this;
            }

            public RPCCallBuilder outputName(String outputName) {
                this.outputName = outputName;
                return this;
            }

            public RPCCallBuilder input(Map<String, DescriptorProtos.FieldDescriptorProto> input) {
                this.input = input;
                return this;
            }

            public RPCCallBuilder addInputParam(String key, DescriptorProtos.FieldDescriptorProto value) {
                this.input.put(key, value);
                return this;
            }

            public RPCCallBuilder addInputParam(Map<String, DescriptorProtos.FieldDescriptorProto> value) {
                this.input.putAll(value);
                return this;
            }

            public RPCCallBuilder output(Map<String, DescriptorProtos.FieldDescriptorProto> output) {
                this.output = output;
                return this;
            }

            public RPCCallBuilder addOutputParam(String key, DescriptorProtos.FieldDescriptorProto value) {
                this.output.put(key, value);
                return this;
            }


            public RPCCallBuilder addOutputParam(Map<String, DescriptorProtos.FieldDescriptorProto> value) {
                this.output.putAll(value);
                return this;
            }

            public RPCCall build() {
                return new RPCCall(this);
            }
        }
    }

    public static class Builder {
        private String serviceName;
        private Map<String, RPCCall> rpcCallsMap = new HashMap<>();

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder rpcCallsMap(Map<String, RPCCall> rpcCallsMap) {
            this.rpcCallsMap = rpcCallsMap;
            return this;
        }

        public Builder addRpcCall(String name, RPCCall rpcCall) {
            this.rpcCallsMap.put(name, rpcCall);
            return this;
        }

        public Builder addRpcCall(Map<String, RPCCall> rpcCall) {
            this.rpcCallsMap.putAll(rpcCall);
            return this;
        }

        public RPCService build() {
            return new RPCService(this);
        }
    }
}
