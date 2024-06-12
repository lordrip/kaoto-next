package io.kaoto.camelcatalog.commands;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.kaoto.camelcatalog.beans.ConfigBean;
import io.kaoto.camelcatalog.generator.CatalogGeneratorBuilder;
import io.kaoto.camelcatalog.generator.Util;
import io.kaoto.camelcatalog.model.CatalogCliArgument;
import io.kaoto.camelcatalog.model.CatalogDefinition;
import io.kaoto.camelcatalog.model.CatalogLibrary;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import jakarta.inject.Inject;
import picocli.CommandLine;

@CommandLine.Command(mixinStandardHelpOptions = true, name = "generate", description = "Generate the Camel catalog.")
public class GenerateCommand implements Runnable {
    @Inject
    ConfigBean configBean;

    @CommandLine.Option(names = { "-o",
            "--output" }, description = "Output directory. It will be cleaned before generating the catalogs", required = true)
    void setOutputFolder(String outputFolder) {
        configBean.setOutputFolder(Util.getNormalizedFolder(outputFolder));
    }

    @CommandLine.Option(names = { "-k",
            "--kamelets" }, description = "Kamelets catalog version", required = true)
    void setKameletVersion(String kameletVersion) {
        configBean.setKameletsVersion(kameletVersion);
    }

    @CommandLine.Option(names = { "-m", "--main" }, description = "Camel Main version")
    void setMainVersion(List<String> mainVersion) {
        mainVersion.forEach(
                version -> configBean.addCatalogVersion(new CatalogCliArgument(CatalogRuntime.Main, version)));
    }

    @CommandLine.Option(names = { "-ceq", "--quarkus" }, description = "Camel Quarkus version")
    void setQuarkusVersion(List<String> quarkusVersion) {
        quarkusVersion.forEach(
                version -> configBean.addCatalogVersion(new CatalogCliArgument(CatalogRuntime.Quarkus, version)));
    }

    @CommandLine.Option(names = { "-csb", "--springboot" }, description = "Camel Spring Boot version")
    void setSpringBootVersion(List<String> springBootVersion) {
        springBootVersion.forEach(
                version -> configBean.addCatalogVersion(new CatalogCliArgument(CatalogRuntime.SpringBoot, version)));
    }

    @CommandLine.Option(names = { "-v", "--verbose" }, description = "Be more verbose.")
    void setVerbose(boolean verbose) {
        configBean.setVerbose(verbose);
    }

    @Override
    public void run() {
        System.out.println("Output folder: " + configBean.getOutputFolder());
        System.out.println("Catalog versions: " + configBean.getCatalogVersionSet());
        System.out.println("Kamelets version: " + configBean.getKameletsVersion());

        CatalogLibrary library = new CatalogLibrary();

        File outputFolder = configBean.getOutputFolder();
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        } else {
            File[] files = outputFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }

        // List.of(
        // new CatalogCliArgument(CatalogRuntime.Main, "4.4.0"),
        // new CatalogCliArgument(CatalogRuntime.Main, "4.4.0.redhat-00019"),
        // new CatalogCliArgument(CatalogRuntime.Main, "4.6.0"),
        // new CatalogCliArgument(CatalogRuntime.Quarkus, "3.8.0"),
        // // new CatalogCliArgument(CatalogRuntime.Quarkus, "3.8.0.redhat-00004") //
        // // Cannot load the Camel YAML DSL from here because of a missing dependency
        // new CatalogCliArgument(CatalogRuntime.SpringBoot, "4.4.0"),
        // new CatalogCliArgument(CatalogRuntime.SpringBoot, "4.4.0.redhat-00014")
        //
        configBean.getCatalogVersionSet().stream().forEach(catalogCliArg -> {
            String catalogFolderName = catalogCliArg.getRuntime() + "-" + catalogCliArg.getCatalogVersion();
            File catalogDefinitionFolder = outputFolder.toPath().resolve(catalogFolderName).toFile();
            catalogDefinitionFolder.mkdirs();

            CatalogGeneratorBuilder builder = new CatalogGeneratorBuilder();
            var catalogGenerator = builder.withRuntime(catalogCliArg.getRuntime())
                    .withCamelCatalogVersion(catalogCliArg.getCatalogVersion())
                    .withKameletsVersion(configBean.getKameletsVersion())
                    .withCamelKCRDsVersion("2.3.1")
                    .withOutputDirectory(catalogDefinitionFolder)
                    .build();

            CatalogDefinition catalogDefinition = catalogGenerator.generate();
            File indexFile = catalogDefinitionFolder.toPath().resolve(catalogDefinition.getFileName()).toFile();
            File relateIndexFile = outputFolder.toPath().relativize(indexFile.toPath()).toFile();

            catalogDefinition.setFileName(relateIndexFile.toString());

            library.addDefinition(catalogDefinition);
        });

        ObjectMapper jsonMapper = new ObjectMapper()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

        var indexFile = outputFolder.toPath().resolve("index.json").toFile();
        try {
            jsonMapper.writerWithDefaultPrettyPrinter().writeValue(indexFile, library);
        } catch (IOException e) {
            throw new RuntimeException("Error writing index file", e);
        }

    }
}
