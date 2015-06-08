package org.devconferences.elastic;

import org.devconferences.jobs.ImportCitiesJob;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

/**
 * Created by chris on 08/06/15.
 */
public class DeveloppementESNode {
    public static void createDevNode() {
        System.out.println("Creating dev ES node ...");
        Settings settings = ImmutableSettings.builder()
                .put("http.port", "9200")
                .put("network.host", "localhost")
                .put("path.data", "tmp/es-local-data")
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
