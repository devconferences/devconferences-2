package org.devconferences.jobs;

import com.google.gson.Gson;
import io.searchbox.client.JestClient;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.mapping.PutMapping;
import org.devconferences.elastic.Repository;
import org.devconferences.v1.City;
import org.elasticsearch.common.settings.ImmutableSettings;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by chris on 02/06/15.
 */
public class ImportCitiesJob {

    public static void main(String[] args) throws IOException {
        Repository repository = new Repository();

        repository.createIndexIfNotFound();

        URL v1Path = ImportCitiesJob.class.getResource("/v1");
        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(FileSystems.getDefault().getPath(v1Path.getPath()));
        directoryStream.forEach(path -> {
            try {
                City city = new Gson().fromJson(new FileReader(path.toString()), City.class);
                repository.indexCity(city);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }


}
