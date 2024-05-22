/*
 * Copyright (C) 2023 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.kaoto.camelcatalog;

import static io.kaoto.camelcatalog.Constants.CAMEL_CATALOG_AGGREGATE;
import static io.kaoto.camelcatalog.Constants.CAMEL_YAML_DSL_FILE_NAME;
import static io.kaoto.camelcatalog.Constants.CRD_SCHEMA;
import static io.kaoto.camelcatalog.Constants.K8S_V1_OPENAPI;
import static io.kaoto.camelcatalog.Constants.KAMELETS;
import static io.kaoto.camelcatalog.Constants.KAMELETS_AGGREGATE;
import static io.kaoto.camelcatalog.Constants.KAMELET_BOUNDARIES_FILENAME;
import static io.kaoto.camelcatalog.Constants.KAMELET_BOUNDARIES_KEY;
import static io.kaoto.camelcatalog.Constants.KUBERNETES_DEFINITIONS;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;

/**
 * Collects the camel metadata files such as catalog and schema and
 * tailors them to fit with Kaoto needs.
 * This class expects the following directory structure under inputDirectory:
 *
 * <ul>
 * <li>catalog/ - The root directory of extracted camel-catalog</li>
 * <li>crds/ - Holds Camel K CRD YAML files</li>
 * <li>kamelets/ - Holds Kamelet definition YAML files</li>
 * <li>schema/ - Holds Camel YAML DSL JSON schema files</li>
 * </ul>
 *
 * In addition to what is generated from above input files, this plugin
 * generates index.json file that holds the list of all the generated.
 */
public class KaotoCamelCatalogBuilder {

    private String camelCatalogVersion;
    private String kameletsVersion;
    private String camelKCRDsVersion;

    public KaotoCamelCatalogBuilder withCamelCatalogVersion(String camelCatalogVersion) {
        this.camelCatalogVersion = camelCatalogVersion;
        return this;
    }

    public KaotoCamelCatalogBuilder withKameletsVersion(String kameletsVersion) {
        this.kameletsVersion = kameletsVersion;
        return this;
    }

    public KaotoCamelCatalogBuilder withCamelKCRDsVersion(String camelKCRDsVersion) {
        this.camelKCRDsVersion = camelKCRDsVersion;
        return this;
    }

    public KaotoCamelCatalogMojo build() {
        var mojo = new KaotoCamelCatalogMojo();
        mojo.cameCatalogVersion = camelCatalogVersion;
        mojo.kameletsVersion = kameletsVersion;
        mojo.camelKCRDsVersion = camelKCRDsVersion;
        return mojo;
    }

    class KaotoCamelCatalogMojo {
        private static final Logger LOGGER = Logger.getLogger(KaotoCamelCatalogMojo.class.getName());

        private static final ObjectMapper jsonMapper = new ObjectMapper()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

        private CamelCatalogVersionLoader camelCatalogVersionLoader = new CamelCatalogVersionLoader();
        private File outputDirectory = new File(System.getProperty("user.dir") + "/dist");
        private String cameCatalogVersion;
        private String kameletsVersion;
        private String camelKCRDsVersion;

        public void generate() {
            camelCatalogVersionLoader.loadCamelCatalog(cameCatalogVersion);
            camelCatalogVersionLoader.loadCamelYamlDsl(cameCatalogVersion);
            camelCatalogVersionLoader.loadKameletBoundaries();
            camelCatalogVersionLoader.loadKamelets(kameletsVersion);
            camelCatalogVersionLoader.loadKubernetesSchema();
            camelCatalogVersionLoader.loadCamelKCRDs(camelKCRDsVersion);
            camelCatalogVersionLoader.loadLocalSchemas();

            outputDirectory.mkdirs();
            var index = new Index();
            var yamlDslSchemaProcessor = processCamelSchema(index);
            processCatalog(yamlDslSchemaProcessor, index);
            processKameletBoundaries(index);
            processKamelets(index);
            processK8sSchema(index);
            processKameletsCRDs(index);
            processAdditionalSchemas(index);

            try {
                var indexFile = outputDirectory.toPath().resolve("index.json").toFile();
                jsonMapper.writerWithDefaultPrettyPrinter().writeValue(indexFile, index);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }

        private CamelYamlDslSchemaProcessor processCamelSchema(Index index) {
            if (camelCatalogVersionLoader.getCamelYamlDslSchema() == null) {
                LOGGER.severe(String.format(
                        "Camel YAML DSL JSON Schema is not loaded"));
                return null;
            }

            try {
                var outputFileName = String.format("%s-%s.json", CAMEL_YAML_DSL_FILE_NAME,
                        Util.generateHash(camelCatalogVersionLoader.getCamelYamlDslSchema()));
                var output = outputDirectory.toPath().resolve(outputFileName);
                output.getParent().toFile().mkdirs();

                Files.writeString(output, camelCatalogVersionLoader.getCamelYamlDslSchema());

                var indexEntry = new Entry(
                        CAMEL_YAML_DSL_FILE_NAME,
                        "Camel YAML DSL JSON schema",
                        cameCatalogVersion,
                        outputFileName);
                index.getSchemas().put(CAMEL_YAML_DSL_FILE_NAME, indexEntry);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return null;
            }

            try {
                var yamlDslSchema = (ObjectNode) jsonMapper.readTree(camelCatalogVersionLoader.getCamelYamlDslSchema());

                var schemaProcessor = new CamelYamlDslSchemaProcessor(jsonMapper, yamlDslSchema);
                var schemaMap = schemaProcessor.processSubSchema();

                schemaMap.forEach((name, subSchema) -> {
                    try {
                        var subSchemaFileName = String.format(
                                "%s-%s-%s.json",
                                CAMEL_YAML_DSL_FILE_NAME,
                                name,
                                Util.generateHash(subSchema));
                        var subSchemaPath = outputDirectory.toPath().resolve(subSchemaFileName);
                        subSchemaPath.getParent().toFile().mkdirs();
                        Files.writeString(subSchemaPath, subSchema);
                        var subSchemaIndexEntry = new Entry(
                                name,
                                "Camel YAML DSL JSON schema: " + name,
                                cameCatalogVersion,
                                subSchemaFileName);
                        index.getSchemas().put(name, subSchemaIndexEntry);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, e.toString(), e);
                    }
                });

                return schemaProcessor;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return null;
            }
        }

        private void processCatalog(CamelYamlDslSchemaProcessor schemaProcessor, Index index) {
            var catalogProcessor = new CamelCatalogProcessor(jsonMapper, schemaProcessor);
            try {
                var catalogMap = catalogProcessor.processCatalog();
                catalogMap.forEach((name, catalog) -> {
                    try {
                        // Adding Kamelet & Pipe Configuration Schema to the Entities Catalog
                        if (name.equals("entities")) {
                            var catalogNode = jsonMapper.readTree(catalog);
                            String files[] = { "KameletConfiguration.json", "PipeConfiguration.json" };
                            for (String file : files) {
                                // resolve schemas from the resources folder
                                var schema = Paths
                                        .get(getClass().getClassLoader().getResource("schemas/" + file).toURI());

                                ((ObjectNode) catalogNode).putObject(file.split("\\.")[0])
                                        .putObject("propertiesSchema");
                                ((ObjectNode) catalogNode.path(file.split("\\.")[0]).path("propertiesSchema"))
                                        .setAll((ObjectNode) jsonMapper.readTree(schema.toFile()));
                            }

                            StringWriter writer = new StringWriter();
                            var jsonGenerator = new JsonFactory().createGenerator(writer).useDefaultPrettyPrinter();
                            jsonMapper.writeTree(jsonGenerator, catalogNode);
                            catalog = writer.toString();
                        }

                        var outputFileName = String.format(
                                "%s-%s-%s.json", CAMEL_CATALOG_AGGREGATE, name, Util.generateHash(catalog));
                        var output = outputDirectory.toPath().resolve(outputFileName);
                        Files.writeString(output, catalog);
                        var indexEntry = new Entry(
                                name,
                                "Aggregated Camel catalog for " + name,
                                cameCatalogVersion,
                                outputFileName);
                        index.getCatalogs().put(name, indexEntry);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, e.toString(), e);
                    }
                });
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }

        private void processKameletBoundaries(Index index) {
            if (camelCatalogVersionLoader.getKameletBoundaries().isEmpty()) {
                LOGGER.severe(String.format(
                        "Kamelet boundaries are not loaded"));
                return;
            }

            var indexEntry = getKameletsEntry(camelCatalogVersionLoader.getKameletBoundaries(), KAMELET_BOUNDARIES_KEY,
                    KAMELET_BOUNDARIES_FILENAME, "Aggregated Kamelet boundaries in JSON");
            index.getCatalogs().put(indexEntry.name(), indexEntry);
        }

        private void processKamelets(Index index) {
            if (camelCatalogVersionLoader.getKamelets().isEmpty()) {
                LOGGER.severe(String.format(
                        "Kamelets are not loaded"));
                return;
            }

            var indexEntry = getKameletsEntry(camelCatalogVersionLoader.getKamelets(), KAMELETS, KAMELETS_AGGREGATE,
                    "Aggregated Kamelets in JSON");
            index.getCatalogs().put(indexEntry.name(), indexEntry);
        }

        private Entry getKameletsEntry(List<String> kamelets, String name, String filename, String description) {
            var root = jsonMapper.createObjectNode();

            try {
                kamelets.forEach(kamelet -> {
                    processKameletFile(kamelet, root);
                });

                JsonFactory jsonFactory = new JsonFactory();
                var outputStream = new ByteArrayOutputStream();
                var writer = new OutputStreamWriter(outputStream);
                var jsonGenerator = jsonFactory.createGenerator(writer).useDefaultPrettyPrinter();

                jsonMapper.writeTree(jsonGenerator, root);
                var rootBytes = outputStream.toByteArray();
                var outputFileName = String.format("%s-%s.json", filename, Util.generateHash(rootBytes));
                var output = outputDirectory.toPath().resolve(outputFileName);

                Files.write(output, rootBytes);

                return new Entry(
                        name,
                        description,
                        kameletsVersion,
                        outputFileName);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }

            return null;
        }

        private void processKameletFile(String kamelet, ObjectNode targetObject) {
            try {
                JsonNode kameletNode = yamlMapper.readTree(kamelet);
                String lowerFileName = kameletNode.get("metadata").get("name").asText().toLowerCase();

                KameletProcessor.process((ObjectNode) kameletNode);
                targetObject.putIfAbsent(lowerFileName, kameletNode);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }

        private void processK8sSchema(Index index) {
            if (camelCatalogVersionLoader.getKubernetesSchema() == null) {
                LOGGER.severe(String.format(
                        "Kubernetes JSON Schema is not loaded"));
            }

            try {
                var openapiSpec = (ObjectNode) jsonMapper.readTree(camelCatalogVersionLoader.getKubernetesSchema());
                var processor = new K8sSchemaProcessor(jsonMapper, openapiSpec);
                var schemaMap = processor.processK8sDefinitions(KUBERNETES_DEFINITIONS);
                for (var entry : schemaMap.entrySet()) {
                    var name = entry.getKey();
                    var schema = entry.getValue();
                    var outputFileName = String.format("%s-%s-%s.json", K8S_V1_OPENAPI, name,
                            Util.generateHash(schema));
                    var output = outputDirectory.toPath().resolve(outputFileName);
                    Files.writeString(output, schema);
                    var indexEntry = new Entry(
                            name,
                            "Kubernetes OpenAPI JSON schema: " + name,
                            "v1",
                            outputFileName);
                    index.getSchemas().put(name, indexEntry);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }

        private void processKameletsCRDs(Index index) {
            if (camelCatalogVersionLoader.getCamelKCRDs().isEmpty()) {
                LOGGER.severe(String.format(
                        "CamelK CRDs are not loaded"));
                return;
            }

            camelCatalogVersionLoader.getCamelKCRDs().forEach(crdString -> {
                processKameletCRD(crdString, index);
            });
        }

        private void processKameletCRD(String crdString, Index index) {
            try {
                var crd = yamlMapper.readValue(crdString, CustomResourceDefinition.class);
                var schema = crd.getSpec().getVersions().get(0).getSchema().getOpenAPIV3Schema();
                var name = crd.getSpec().getNames().getKind();

                var bytes = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(schema);
                var outputFileName = String.format(
                        "%s-%s-%s.json", CRD_SCHEMA, name.toLowerCase(), Util.generateHash(bytes));

                var output = outputDirectory.toPath().resolve(outputFileName);
                Files.write(output, bytes);
                var description = name;

                var indexEntry = new Entry(
                        name,
                        description,
                        camelKCRDsVersion,
                        outputFileName);
                index.getSchemas().put(name, indexEntry);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }

        private void processAdditionalSchemas(Index index) {
            if (camelCatalogVersionLoader.getLocalSchemas().isEmpty()) {
                LOGGER.severe(String.format(
                        "Local schemas are not loaded"));
                return;
            }

            for (var localSchemaEntry : camelCatalogVersionLoader.getLocalSchemas().entrySet()) {
                try {
                    var schema = (ObjectNode) jsonMapper.readTree(localSchemaEntry.getValue());
                    var name = localSchemaEntry.getKey();
                    var description = schema.get("description").asText();

                    var outputFileName = String.format("%s-%s.%s", localSchemaEntry.getKey(),
                            Util.generateHash(localSchemaEntry.getValue()), "json");
                    var output = outputDirectory.toPath().resolve(outputFileName);

                    Files.writeString(output, localSchemaEntry.getValue());

                    var indexEntry = new Entry(
                            name,
                            description,
                            "1",
                            outputFileName);

                    index.getSchemas().put(name, indexEntry);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                }
            }
        }
    }
}
