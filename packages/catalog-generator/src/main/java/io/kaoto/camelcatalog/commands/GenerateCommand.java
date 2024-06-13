package io.kaoto.camelcatalog.commands;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.kaoto.camelcatalog.beans.ConfigBean;
import io.kaoto.camelcatalog.generator.CatalogGeneratorBuilder;
import io.kaoto.camelcatalog.model.CatalogDefinition;
import io.kaoto.camelcatalog.model.CatalogLibrary;

public class GenerateCommand implements Runnable {
    private ConfigBean configBean;

    public GenerateCommand(ConfigBean configBean) {
        this.configBean = configBean;
    }

    File createSubFolder(File parentFolder, String folderName) {
        File newSubFolder = parentFolder.toPath().resolve(folderName).toFile();
        newSubFolder.mkdirs();
        return newSubFolder;
    }

    @Override
    public void run() {
        System.out.println("Output folder: " + configBean.getOutputFolder());
        System.out.println("Catalog Repository name: " + configBean.getCatalogsName());
        System.out.println("Catalog versions: " + configBean.getCatalogVersionSet());
        System.out.println("Kamelets version: " + configBean.getKameletsVersion());

        CatalogLibrary library = new CatalogLibrary();
        library.setName(configBean.getCatalogsName());

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

        configBean.getCatalogVersionSet()
                .forEach(catalogCliArg -> {
                    String runtimeFolderName = "camel-" + catalogCliArg.getRuntime().name().toLowerCase();
                    File runtimeFolder = createSubFolder(outputFolder, runtimeFolderName);
                    File catalogDefinitionFolder = createSubFolder(runtimeFolder, catalogCliArg.getCatalogVersion());

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
