package io.kaoto.camelcatalog;

import java.io.File;
import java.util.List;

import io.kaoto.camelcatalog.model.CatalogRuntime;

public class Main {

    public static void main(String[] args) {
        List.of("4.4.0", "4.6.0").forEach(version -> {
            File outputDirectory = new File(System.getProperty("user.dir") + "/dist/" + version);
            outputDirectory.mkdirs();

            CatalogGeneratorBuilder builder = new CatalogGeneratorBuilder();
            var catalogGenerator = builder.withRuntime(CatalogRuntime.fromString("main"))
                    .withCamelCatalogVersion(version)
                    .withKameletsVersion("4.6.0")
                    .withCamelKCRDsVersion("2.3.1")
                    .withOutputDirectory(outputDirectory)
                    .build();

            String indexFilename = catalogGenerator.generate();
            System.out.println("Generated catalog to " + indexFilename);
        });
    }
}
