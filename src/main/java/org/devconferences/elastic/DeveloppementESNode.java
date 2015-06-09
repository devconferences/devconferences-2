package org.devconferences.elastic;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.devconferences.jobs.ImportCitiesJob;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by chris on 08/06/15.
 */
public class DeveloppementESNode {

    public static final String ES_LOCAL_DATA = "tmp/es-local-data";
    private static final Logger LOGGER = LoggerFactory.getLogger(DeveloppementESNode.class);

    public static void createDevNode() {
        LOGGER.info("Creating dev ES node ...");
        Path localDevDataDirectory = Paths.get(ES_LOCAL_DATA);
        try {
            FileUtils.deleteDirectory(localDevDataDirectory.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Settings settings = ImmutableSettings.builder()
                .put("http.port", "9200")
                .put("network.host", "localhost")
                .put("path.data", ES_LOCAL_DATA)
                .build();

        Node node = NodeBuilder.nodeBuilder()
                .local(true)
                .data(true)
                .clusterName("elasticSearch")
                .settings(settings)
                .build();
        node.start();

        ImportCitiesJob.runJob();
    }

}
