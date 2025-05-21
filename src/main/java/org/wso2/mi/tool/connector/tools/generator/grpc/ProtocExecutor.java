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

package org.wso2.mi.tool.connector.tools.generator.grpc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.OS_ARCH;
import static org.wso2.mi.tool.connector.tools.generator.grpc.Constants.OS_NAME;

/**
 * Protoc file executor.
 */
public class ProtocExecutor {

    private static final String PROTOC_VERSION = "21.12";
    private static final String GRPC_PLUGIN_VERSION = "1.70.0";

    private static String getProtocDownloadUrl(String os, String arch) {
        if (os.contains("win")) {
            return "https://github.com/protocolbuffers/protobuf/releases/download/v" + PROTOC_VERSION
                    + "/protoc-" + PROTOC_VERSION + "-win64.zip";
        } else if (os.contains("mac")) {
            if (arch.contains("arm")) {
                return "https://github.com/protocolbuffers/protobuf/releases/download/v" + PROTOC_VERSION
                        + "/protoc-" + PROTOC_VERSION + "-darwin-aarch_64.zip";
            } else {
                return "https://github.com/protocolbuffers/protobuf/releases/download/v21.12/protoc-21.12-osx-x86_64.zip";
            }
        } else if (os.contains("linux")) {
            if (arch.contains("arm")) {
                return "https://github.com/protocolbuffers/protobuf/releases/download/v" + PROTOC_VERSION
                        + "/protoc-" + PROTOC_VERSION + "-linux-aarch_64.zip";
            } else {
                return "https://github.com/protocolbuffers/protobuf/releases/download/v" + PROTOC_VERSION
                        + "/protoc-" + PROTOC_VERSION + "-linux-x86_64.zip";
            }
        }
        throw new UnsupportedOperationException("Unsupported OS/arch: " + os + " " + arch);
    }

    private static String getGrpcPluginDownloadUrl(String os, String arch) {
        String base = "https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/" + GRPC_PLUGIN_VERSION;
        if (os.contains("win")) {
            return base + "/protoc-gen-grpc-java-" + GRPC_PLUGIN_VERSION + "-windows-x86_64.exe";
        } else if (os.contains("mac")) {
            if (arch.contains("arm")) {
                return base + "/protoc-gen-grpc-java-" + GRPC_PLUGIN_VERSION + "-osx-aarch_64.exe";
            } else {
                return base + "/protoc-gen-grpc-java-" + GRPC_PLUGIN_VERSION + "-osx-x86_64.exe";
            }
        } else if (os.contains("linux")) {
            if (arch.contains("arm")) {
                return base + "/protoc-gen-grpc-java-" + GRPC_PLUGIN_VERSION + "-linux-aarch_64";
            } else {
                return base + "/protoc-gen-grpc-java-" + GRPC_PLUGIN_VERSION + "-linux-x86_64";
            }
        }
        throw new UnsupportedOperationException("Unsupported OS/arch for gRPC plugin: " + os + " " + arch);
    }


    /**
     * Download and extract protoc for the current OS/arch, returning the absolute path
     * to the "protoc" (or "protoc.exe") binary in a temp folder.
     * @return the absolute path to the protoc binary
     * @throws IOException if an error occurs during download or extraction
     */
    public static Path downloadAndExtractProtoc() throws IOException {
        String url = getProtocDownloadUrl(OS_NAME, OS_ARCH);
        System.out.println("Downloading protoc from: " + url);

        // Create a temp dir for protoc
        Path tempDir = Files.createTempDirectory("protoc-");
        tempDir.toFile().deleteOnExit();

        Path zipFile = tempDir.resolve("protoc.zip");
        downloadFile(url, zipFile);
        unzip(zipFile, tempDir);

        Path binDir = tempDir.resolve("bin");
        if (!Files.isDirectory(binDir)) {
            // Maybe the zip extracted everything at the root. Check for 'protoc' at root.
            binDir = tempDir;
        }
        Path protoc;
        if (OS_NAME.contains("win")) {
            protoc = binDir.resolve("protoc.exe");
        } else {
            protoc = binDir.resolve("protoc");
        }
        if (!Files.exists(protoc)) {
            throw new IOException("Could not find protoc binary after extraction: " + protoc);
        }
        protoc.toFile().setExecutable(true);
        return protoc;
    }

    /**
     * Download the gRPC plugin for the current OS/arch, returning the absolute path
     * to the "protoc-gen-grpc-java" (or .exe on Windows) binary in a temp folder.
     */
    static Path downloadGrpcPlugin() throws IOException {
        String url = getGrpcPluginDownloadUrl(OS_NAME, OS_ARCH);

        Path tempDir = Files.createTempDirectory("grpc-plugin-");
        tempDir.toFile().deleteOnExit();
        String fileName = OS_NAME.contains("win") ? "protoc-gen-grpc-java.exe" : "protoc-gen-grpc-java";
        Path pluginFile = tempDir.resolve(fileName);
        downloadFile(url, pluginFile);
        pluginFile.toFile().setExecutable(true);
        return pluginFile;
    }

    /**
     * Run the protoc command to generate Java and gRPC stubs.
     */
    static boolean runProtoc(File protoc, File grpcPlugin, String protoSourceDir, String protocFile, String javaOutDir) throws IOException, InterruptedException {
        Files.createDirectories(Paths.get(javaOutDir));
        List<String> command = new ArrayList<>();
        command.add(protoc.getAbsolutePath());

        // Provide the gRPC plugin
        command.add("--plugin=protoc-gen-grpc-java=" + grpcPlugin.getAbsolutePath());
        command.add("--grpc-java_out=" + javaOutDir);

        // Provide standard Java generation
        command.add("--java_out=" + javaOutDir);

        // Add the descriptor flags
        command.add("--descriptor_set_out=" + javaOutDir + "/Descriptor.desc");

        // Provide the proto path
        command.add("--proto_path=" + protoSourceDir);

        // Option 1: Process a specific proto file if provided
        if (protocFile != null && !protocFile.isEmpty()) {
            command.add(protoSourceDir + "/" + protocFile);
        }
        // Option 2: Process all proto files if no specific file is provided
        else {
            File protoDir = new File(protoSourceDir);
            File[] protoFiles = protoDir.listFiles((dir, name) -> name.endsWith(".proto"));
            if (protoFiles == null || protoFiles.length == 0) {
                throw new IOException("No .proto files found in directory: " + protoSourceDir);
            }
            for (File f : protoFiles) {
                command.add(f.getAbsolutePath());
            }
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (InputStream in = process.getInputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                System.out.write(buf, 0, len);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("Protoc execution failed with exit code: " + exitCode);
        }
        return (exitCode == 0);
    }

    /**
     * Utility to download a file from a URL to a local path.
     */
    private static void downloadFile(String fileUrl, Path destination) throws IOException {
        try (InputStream in = new URL(fileUrl).openStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Unzip the downloaded protoc archive. (Assumes it's a valid zip file.)
     */
    private static void unzip(Path zipFile, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outputPath = destDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    // Ensure parent directories exist
                    Files.createDirectories(outputPath.getParent());
                    try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
                        byte[] buffer = new byte[4096];
                        int read;
                        while ((read = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }
}
