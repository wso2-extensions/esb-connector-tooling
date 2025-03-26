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

package org.wso2.mi.tool.connector.tools.generator.grpc.utils;

import com.google.protobuf.DescriptorProtos;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

public class CodeGenerationUtils {

    public static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String lowercaseFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toLowerCase(Locale.ENGLISH) + str.substring(1);
    }

    public static DescriptorProtos.FileDescriptorSet loadDescriptorSet(String descriptorFilePath) throws IOException {
        // Load the binary descriptor file
        try (FileInputStream fis = new FileInputStream(descriptorFilePath)) {
            return DescriptorProtos.FileDescriptorSet.parseFrom(fis);
        }
    }

    public static String getTypeName(String input, String packageName) {
        // Use Pattern.quote to escape special characters in the package name
        return input.replaceAll("^\\." + Pattern.quote(packageName) + "\\.", "").replace(".", "");
    }

    public static void deleteDirectory(Path tempDir) throws IOException {
        Files.walk(tempDir)
                .sorted((p1, p2) -> p2.compareTo(p1))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public static String getGetterNames(DescriptorProtos.FieldDescriptorProto field) {
        String fieldName = field.getName();
        String camelCaseName = toCamelCase(fieldName);

        if (field.getType() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL) {
            return "get" + camelCaseName;
        }
        if (field.getLabel() == DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED) {
            return "get" + camelCaseName + "List";
        }

        if (isMapField(field)) {
            return "get" + camelCaseName + "Map";
        }
        if (field.getType() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE) {
            return "get" + camelCaseName;
        }
        return "get" + camelCaseName;
    }

    public static String getSetterNames(DescriptorProtos.FieldDescriptorProto field) {
        String fieldName = field.getName();
        String camelCaseName = toCamelCase(fieldName);
        if (field.getType() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL) {
            return "set" + camelCaseName;
        }
        if (isMapField(field)) {
            return "putAll" + camelCaseName;
        }
        if (field.getLabel() == DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED) {
            return "addAll" + camelCaseName;
        }
        if (field.getType() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE) {
            return "set" + camelCaseName;
        }
        return "set" + camelCaseName;
    }

    private static String toCamelCase(String fieldName) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : fieldName.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    private static boolean isMapField(DescriptorProtos.FieldDescriptorProto field) {
        return field.getType() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE &&
                field.getLabel() == DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED &&
                field.getTypeName().endsWith("Entry");
    }
}
