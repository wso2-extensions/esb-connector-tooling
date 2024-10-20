package org.wso2.mi.tool.connector.tools.generator.openapi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.DefaultInvoker;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class ConnectorBuilder {

    private static final Log log = LogFactory.getLog(ConnectorBuilder.class);
    private final Invoker invoker;

    public ConnectorBuilder() {
        invoker = new DefaultInvoker();
    }

    public String build(String openAPISpecPath, String projectPath) {
        String connectorPath = null;
        try {
            ConnectorProjectGenerator connectorProjectGenerator = new ConnectorProjectGenerator();
            String connectorProjectPath = connectorProjectGenerator.generateConnectorProject(openAPISpecPath, projectPath);
            connectorPath = buildConnector(connectorProjectPath);
        } catch (ConnectorGenException e) {
            log.error("Error occurred while building the connector.", e);
        }
        return connectorPath;
    }

    public static void main(String[] args) {
        ConnectorBuilder connectorBuilder = new ConnectorBuilder();
        connectorBuilder.build("/Users/arunan/wso2/connectors/esb-connector-facebookads/gen_resources/facebook_ads_open_api.yaml", "/Users/arunan/wso2/connectors/esb-connector-facebookads/gen_resources/target");
    }


    /**
     * Builds the connector using Maven.
     *
     * @param projectPath The path to the project directory.
     * @return True if the connector is built successfully, false otherwise.
     */
    public String buildConnector(String projectPath) {

        Path pomPath = Paths.get(projectPath, "pom.xml");
        InvocationRequest request = createBaseRequest(pomPath);
        request.setGoals(Collections.singletonList(Constants.MAVEN_GOALS));
        try {
            String mavenHome = getMavenHome();
            invoker.setMavenHome(new File(mavenHome));
            invoker.execute(request);
        } catch (Exception e) {
            log.error("Error occurred while building the connector.", e);
            return null;
        }
        // check target directory contains zip
        File targetDir = new File(projectPath + File.separator + "target");
        File[] files = targetDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".zip")) {
                    log.info("Connector built successfully.");
                    return file.getAbsolutePath();
                }
            }
        }
        log.error("Connector not found in the target directory.");
        return null;
    }

    /**
     * Creates a base Maven invocation request.
     *
     * @return The configured invocation request.
     */
    private InvocationRequest createBaseRequest(Path pomPath) {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pomPath.toFile());
        request.setInputStream(new ByteArrayInputStream(new byte[0])); // Avoid interactive mode
        return request;
    }

    /**
     * Retrieves the Maven home directory.
     *
     * @return The Maven home directory path, or null if it cannot be determined.
     * @throws ConnectorGenException if an error occurs while determining the Maven home directory.
     */
    private String getMavenHome() throws ConnectorGenException {

        // First try to find Maven home using system property
        String mavenHome = System.getProperty("maven.home");
        if (mavenHome != null) {
            return mavenHome;
        }

        // Fallback: Try to find Maven home using environment variable or default paths
        mavenHome = System.getenv("MAVEN_HOME");
        if (mavenHome != null) {
            return mavenHome;
        }

        // Fallback: Try to find Maven home using command line
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (System.getProperty("os.name").toLowerCase().contains(Constants.OS_WINDOWS)) {
            processBuilder.command("cmd.exe", "/c", "mvn -v");
        } else {
            processBuilder.command("sh", "-c", "mvn -v");
        }
        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Maven home: ")) {
                    return line.split("Maven home: ")[1].trim();
                }
            }
        } catch (IOException e) {
            throw new ConnectorGenException("Could not determine Maven home.", e);
        }

        throw new ConnectorGenException("Could not determine Maven home.");
    }
}
