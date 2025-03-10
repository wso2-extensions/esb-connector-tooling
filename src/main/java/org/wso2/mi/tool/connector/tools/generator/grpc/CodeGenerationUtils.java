package org.wso2.mi.tool.connector.tools.generator.grpc;

import com.google.protobuf.DescriptorProtos;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class CodeGenerationUtils {

    public static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static DescriptorProtos.FileDescriptorSet loadDescriptorSet(String descriptorFilePath) throws IOException {
        // Load the binary descriptor file
        try (FileInputStream fis = new FileInputStream(descriptorFilePath)) {
            return DescriptorProtos.FileDescriptorSet.parseFrom(fis);
        }
    }

    public static String getTypeName(String input, String packageName) {
        // Use Pattern.quote to escape special characters in the package name
        return input.replaceAll("^\\." + Pattern.quote(packageName) + "\\.", "");
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

}
