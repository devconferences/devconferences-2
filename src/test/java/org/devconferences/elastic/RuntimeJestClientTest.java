package org.devconferences.elastic;

import org.assertj.core.api.Assertions;
import org.devconferences.jobs.ImportEventsJob;
import org.junit.*;

import java.net.SocketTimeoutException;

@Ignore
public class RuntimeJestClientTest {
    private static RuntimeJestClient jestClient;

    @BeforeClass
    public static void classSetUp() {
        DeveloppementESNode.createDevNode("9250");
        Assertions.assertThat(DeveloppementESNode.portNode).matches("9250");
        // This should not throw an exception
        try {
            DeveloppementESNode.createDevNode("9250");
        } catch (RuntimeException e) {
            Assertions.fail("This should not throw an exception !", e);
        }
        jestClient = ElasticUtils.createClient();
        Assertions.assertThat(jestClient).isNotNull();
        // Index should not exists yet
        try { // This might throw randomly a SocketTimeoutException which we can't manage
            Assertions.assertThat(ElasticUtils.indiceExists(
                    jestClient,ElasticUtils.DEV_CONFERENCES_INDEX
            ).isSucceeded()).isFalse();
            ElasticUtils.createIndex(); // Index + type creation
        } catch(RuntimeException e) {
            if(e.getCause() instanceof SocketTimeoutException) {
                System.out.println();
                System.out.println("SocketTimeoutException !");
                System.out.println();
            } else {
                throw e;
            }
        }
    }

    @AfterClass
    public static void tearDownOne() {
        ElasticUtils.deleteIndex();
        try { // Need waiting few seconds, or loaded data won't be founded...
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertThat(ElasticUtils.indiceExists(
                jestClient,ElasticUtils.DEV_CONFERENCES_INDEX
        ).isSucceeded()).isFalse();
        DeveloppementESNode.deleteDevNode();
        Assertions.assertThat(DeveloppementESNode.esNode).isNull();
        Assertions.assertThat(DeveloppementESNode.portNode).isNull();
        // This should not throw an exception
        try {
            DeveloppementESNode.deleteDevNode();
        } catch(RuntimeException e) {
            Assertions.fail("This should not throw an exception !", e);
        }
    }

    @Before
    public void setUp() {
        (new ImportEventsJob()).reloadData(true);

        try { // Need waiting few seconds, or loaded data won't be founded...
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
