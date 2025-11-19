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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utils for the code generations.
 */
public class CodeGenerationUtils {

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

    public static String resolveJavaFqn(Map<String, FileAndMsg> idx, String protoFqn) throws ConnectorGenException {
        FileAndMsg fam = idx.get(protoFqn);
        if (fam == null) throw new ConnectorGenException("Unknown type: " + protoFqn);
        return messageJavaFqn(fam.file, fam.msg);
    }

    public static Map<String, FileAndMsg> buildTypeIndex(DescriptorProtos.FileDescriptorSet set) {
        Map<String, FileAndMsg> map = new HashMap<>();
        for (DescriptorProtos.FileDescriptorProto file : set.getFileList()) {
            String pkg = file.hasPackage() ? file.getPackage() : "";
            // top-level messages
            for (DescriptorProtos.DescriptorProto msg : file.getMessageTypeList()) {
                addAllNested(map, file, pkg, "", msg);
            }
        }
        return map;
    }

    // Recursively index nested messages too: ".pkg.Outer.Inner"
    private static void addAllNested(Map<String, FileAndMsg> map, DescriptorProtos.FileDescriptorProto file,
                                     String pkg, String prefix, DescriptorProtos.DescriptorProto msg) {
        String fullName = "." + (pkg.isEmpty() ? "" : (pkg + ".")) + (prefix.isEmpty() ? "" : (prefix + ".")) + msg.getName();
        map.put(fullName, new FileAndMsg(file, msg));

        for (DescriptorProtos.DescriptorProto nested : msg.getNestedTypeList()) {
            addAllNested(map, file, pkg, (prefix.isEmpty() ? msg.getName() : prefix + "." + msg.getName()), nested);
        }
    }

    // Build Java FQN for a message using file options
    static String messageJavaFqn(DescriptorProtos.FileDescriptorProto file, DescriptorProtos.DescriptorProto msg) {
        String pkg = file.getOptions().hasJavaPackage()
                ? file.getOptions().getJavaPackage()
                : (file.hasPackage() ? file.getPackage() : "");

        boolean multi = file.getOptions().getJavaMultipleFiles();
        String outer = null;
        if (!multi) {
            if (file.getOptions().hasJavaOuterClassname()) {
                outer = file.getOptions().getJavaOuterClassname();
            } else {
                String base = file.getName().substring(file.getName().lastIndexOf('/') + 1)
                        .replace(".proto", "");
                outer = toOuterClass(base);
            }
        }

        // Reconstruct nesting chain for Java (same names as proto nesting)
        List<String> nesting = new ArrayList<>();
        // bottom-up
        collectNesting(msg, nesting);
        Collections.reverse(nesting);
        String simple = String.join(".", nesting.isEmpty() ? Collections.singletonList(msg.getName()) : nesting);
        String pkgPrefix = pkg.isEmpty() ? "" : (pkg + ".");
        return multi ? (pkgPrefix + simple) : (pkgPrefix + outer + "." + simple);
    }

    // Collect full nesting name for a message (handles nested messages)
    private static void collectNesting(DescriptorProtos.DescriptorProto msg, List<String> out) {
        // DescriptorProto doesn’t carry parents; for Java FQN we only need simple name here.
        // Nested names are accounted for by addAllNested() when indexing; here simple is enough.
        out.add(msg.getName());
    }

    // "common" -> "Common", "my_service" -> "MyService"
    private static String toOuterClass(String base) {
        String cleaned = base.replaceAll("[^A-Za-z0-9]", " ");
        StringBuilder sb = new StringBuilder();
        for (String part : cleaned.split("\\s+")) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1));
        }
        return sb.toString();
    }

    public static final class FileAndMsg {
        final DescriptorProtos.FileDescriptorProto file;
        final DescriptorProtos.DescriptorProto msg;

        FileAndMsg(DescriptorProtos.FileDescriptorProto file, DescriptorProtos.DescriptorProto msg) {
            this.file = file;
            this.msg = msg;
        }
    }
}
