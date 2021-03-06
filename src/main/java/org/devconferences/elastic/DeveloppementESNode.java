package org.devconferences.elastic;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/**
 * Created by chris on 08/06/15.
 */
public class DeveloppementESNode {

    private static final String ES_LOCAL_DATA = "tmp/es-local-data";
    static String elasticPort = "9200";
    private static final Logger LOGGER = LoggerFactory.getLogger(DeveloppementESNode.class);
    static Node esNode = null;
    static String portNode = null;

    public static String getPortNode() {
        return DeveloppementESNode.portNode;
    }

    public static void setPortNode(String portNode) {
        if(DeveloppementESNode.esNode == null) {
            DeveloppementESNode.portNode = portNode;
        } else {
            throw new RuntimeException("You try to edit port of an launched ES node !");
        }
    }

    public static void createDevNode() {
        createDevNode(elasticPort);
    }

    public static void createDevNode(String port) {
        if(esNode == null) {
            LOGGER.info("Creating dev ES node on port " + port + "...");
            Path localDevDataDirectory = Paths.get(ES_LOCAL_DATA);
            try {
                FileUtils.deleteDirectory(localDevDataDirectory.toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            Settings.Builder settings = ImmutableSettings.builder()
                    .put("http.port", port)
                    .put("network.host", "localhost")
                    .put("path.data", ES_LOCAL_DATA);

            esNode = NodeBuilder.nodeBuilder()
                    .local(true)
                    .data(true)
                    .clusterName("elasticSearch" + UUID.randomUUID())
                    .settings(settings.build())
                    .build();
            esNode.start();
            portNode = port;
        } else {
            LOGGER.warn("ES Dev Node already launched on port " + portNode);
        }
    }

    public static void deleteDevNode() {
        if(esNode != null) {
            LOGGER.info("Deleting dev ES node on port " + portNode + "...");
            esNode.close();
            esNode = null;
            portNode = null;
        } else {
            LOGGER.warn("ES Dev Node not exist");
        }
    }
}
