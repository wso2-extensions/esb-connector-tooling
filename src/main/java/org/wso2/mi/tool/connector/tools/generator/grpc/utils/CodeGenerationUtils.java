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
import org.wso2.mi.tool.connector.tools.generator.grpc.exception.ConnectorGenException;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utils for the code generations.
 */
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

    // Java reserved keywords
    public static final Set<String> JAVA_KEYWORDS;

    static {
        JAVA_KEYWORDS = new HashSet<>();
        JAVA_KEYWORDS.add("abstract");
        JAVA_KEYWORDS.add("assert");
        JAVA_KEYWORDS.add("boolean");
        JAVA_KEYWORDS.add("break");
        JAVA_KEYWORDS.add("byte");
        JAVA_KEYWORDS.add("case");
        JAVA_KEYWORDS.add("catch");
        JAVA_KEYWORDS.add("char");
        JAVA_KEYWORDS.add("class");
        JAVA_KEYWORDS.add("const");
        JAVA_KEYWORDS.add("continue");
        JAVA_KEYWORDS.add("default");
        JAVA_KEYWORDS.add("do");
        JAVA_KEYWORDS.add("double");
        JAVA_KEYWORDS.add("else");
        JAVA_KEYWORDS.add("enum");
        JAVA_KEYWORDS.add("extends");
        JAVA_KEYWORDS.add("final");
        JAVA_KEYWORDS.add("finally");
        JAVA_KEYWORDS.add("float");
        JAVA_KEYWORDS.add("for");
        JAVA_KEYWORDS.add("goto");
        JAVA_KEYWORDS.add("if");
        JAVA_KEYWORDS.add("implements");
        JAVA_KEYWORDS.add("import");
        JAVA_KEYWORDS.add("instanceof");
        JAVA_KEYWORDS.add("int");
        JAVA_KEYWORDS.add("interface");
        JAVA_KEYWORDS.add("long");
        JAVA_KEYWORDS.add("native");
        JAVA_KEYWORDS.add("new");
        JAVA_KEYWORDS.add("package");
        JAVA_KEYWORDS.add("private");
        JAVA_KEYWORDS.add("protected");
        JAVA_KEYWORDS.add("public");
        JAVA_KEYWORDS.add("return");
        JAVA_KEYWORDS.add("short");
        JAVA_KEYWORDS.add("static");
        JAVA_KEYWORDS.add("strictfp");
        JAVA_KEYWORDS.add("super");
        JAVA_KEYWORDS.add("switch");
        JAVA_KEYWORDS.add("synchronized");
        JAVA_KEYWORDS.add("this");
        JAVA_KEYWORDS.add("throw");
        JAVA_KEYWORDS.add("throws");
        JAVA_KEYWORDS.add("transient");
        JAVA_KEYWORDS.add("try");
        JAVA_KEYWORDS.add("void");
        JAVA_KEYWORDS.add("volatile");
        JAVA_KEYWORDS.add("while");
    }

    /**
     * Validate whether the given string is a valid java_package option in proto.
     *
     * @param packageName the package string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidJavaPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        String[] parts = packageName.split("\\.");
        for (String part : parts) {
            // Must match Java identifier rules
            if (!part.matches("[a-zA-Z_$][a-zA-Z\\d_$]*")) {
                return false;
            }
            // Must not be a reserved keyword
            if (JAVA_KEYWORDS.contains(part)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validate and throw a clear error if invalid.
     *
     * @param packageName the package string to validate
     * @throws IllegalArgumentException if invalid
     */
    public static void validateOrThrow(String packageName) throws ConnectorGenException {
        if (!isValidJavaPackage(packageName)) {
            throw new ConnectorGenException(
                    "Invalid java_package value: '" + packageName +
                            "'. Must be a valid Java package identifier (no reserved keywords, " +
                            "segments must start with letter/underscore, no empty parts)."
            );
        }
    }
}
