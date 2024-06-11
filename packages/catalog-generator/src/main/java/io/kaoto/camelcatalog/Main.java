package io.kaoto.camelcatalog;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.kaoto.camelcatalog.model.CatalogCliArgument;
import io.kaoto.camelcatalog.model.CatalogDefinition;
import io.kaoto.camelcatalog.model.CatalogLibrary;
import io.kaoto.camelcatalog.model.CatalogRuntime;

public class Main {
    private static final ObjectMapper jsonMapper = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    public static void main(String[] args) {
        CatalogLibrary library = new CatalogLibrary();

        File outputDirectory = new File(System.getProperty("user.dir") + "/dist");
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        } else {
            File[] files = outputDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }

        List.of(
                new CatalogCliArgument(CatalogRuntime.Main, "4.4.0"),
                new CatalogCliArgument(CatalogRuntime.Main, "4.4.0.redhat-00019"),
                new CatalogCliArgument(CatalogRuntime.Main, "4.6.0"),
                new CatalogCliArgument(CatalogRuntime.Quarkus, "3.8.0"),
//                new CatalogCliArgument(CatalogRuntime.Quarkus, "3.8.0.redhat-00004") // Cannot load the Camel YAML DSL from here because of a missing dependency
                new CatalogCliArgument(CatalogRuntime.SpringBoot, "4.4.0"),
                new CatalogCliArgument(CatalogRuntime.SpringBoot, "4.4.0.redhat-00014")
        //
        ).forEach(cliCatalog -> {
            String catalogFolderName = cliCatalog.runtime() + "-" + cliCatalog.version();
            File catalogDefinitionFolder = outputDirectory.toPath().resolve(catalogFolderName).toFile();
            catalogDefinitionFolder.mkdirs();

            CatalogGeneratorBuilder builder = new CatalogGeneratorBuilder();
            var catalogGenerator = builder.withRuntime(cliCatalog.runtime())
                    .withCamelCatalogVersion(cliCatalog.version())
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
