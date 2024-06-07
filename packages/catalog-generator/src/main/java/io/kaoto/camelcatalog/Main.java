package io.kaoto.camelcatalog;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.kaoto.camelcatalog.model.CatalogDefinition;
import io.kaoto.camelcatalog.model.CatalogLibrary;
import io.kaoto.camelcatalog.model.CatalogRuntime;

public class Main {
    private static final ObjectMapper jsonMapper = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    public static void main(String[] args) {
        CatalogLibrary library = new CatalogLibrary();

        File outputDirectory = new File(System.getProperty("user.dir") + "/dist");
        outputDirectory.mkdirs();

        List.of("4.4.0", "4.6.0").forEach(version -> {
            File catalogDefinitionFolder = outputDirectory.toPath().resolve(version).toFile();
            catalogDefinitionFolder.mkdirs();

            CatalogGeneratorBuilder builder = new CatalogGeneratorBuilder();
            var catalogGenerator = builder.withRuntime(CatalogRuntime.fromString("main"))
                    .withCamelCatalogVersion(version)
                    .withKameletsVersion("4.6.0")
                    .withCamelKCRDsVersion("2.3.1")
                    .withOutputDirectory(catalogDefinitionFolder)
                    .build();

            CatalogDefinition catalogDefinition = catalogGenerator.generate();
            File indexFile = catalogDefinitionFolder.toPath().resolve(catalogDefinition.getFileName()).toFile();
            File relateIndexFile = outputDirectory.toPath().relativize(indexFile.toPath()).toFile();

            catalogDefinition.setFileName(relateIndexFile.toString());

            library.addDefinition(catalogDefinition);
        });

        var indexFile = outputDirectory.toPath().resolve("index.json").toFile();
        try {
            jsonMapper.writerWithDefaultPrettyPrinter().writeValue(indexFile, library);
        } catch (IOException e) {
            throw new RuntimeException("Error writing index file", e);
        }

    }
}
