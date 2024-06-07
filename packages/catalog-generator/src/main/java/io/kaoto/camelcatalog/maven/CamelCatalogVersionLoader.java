package io.kaoto.camelcatalog.maven;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.tooling.maven.MavenArtifact;
import org.apache.camel.tooling.maven.MavenDownloader;
import org.apache.camel.tooling.maven.MavenDownloaderImpl;
import org.apache.camel.tooling.maven.MavenResolutionException;

import io.kaoto.camelcatalog.model.Constants;
import io.kaoto.camelcatalog.model.MavenCoordinates;
import io.kaoto.camelcatalog.model.CatalogRuntime;

public class CamelCatalogVersionLoader {
    private static final Logger LOGGER = Logger.getLogger(CamelCatalogVersionLoader.class.getName());
    private final KaotoMavenVersionManager KAOTO_VERSION_MANAGER = new KaotoMavenVersionManager();
    private CamelCatalog camelCatalog = new DefaultCamelCatalog(false);
    private String camelYamlDSLSchema;
    private String kubernetesSchema;
    private List<String> kameletBoundaries = new ArrayList<>();
    private List<String> kamelets = new ArrayList<>();
    private List<String> camelKCRDs = new ArrayList<>();
    private Map<String, String> localSchemas = new HashMap<>();
    private CatalogRuntime runtime;

    public CamelCatalogVersionLoader(CatalogRuntime runtime) {
        this.runtime = runtime;
        camelCatalog.setVersionManager(KAOTO_VERSION_MANAGER);
    }

    public CatalogRuntime getRuntime() {
        return runtime;
    }

    public CamelCatalog getCamelCatalog() {
        return camelCatalog;
    }

    public String getCamelYamlDslSchema() {
        return camelYamlDSLSchema;
    }

    public List<String> getKameletBoundaries() {
        return kameletBoundaries;
    }

    public List<String> getKamelets() {
        return kamelets;
    }

    public String getKubernetesSchema() {
        return kubernetesSchema;
    }

    public List<String> getCamelKCRDs() {
        return camelKCRDs;
    }

    public Map<String, String> getLocalSchemas() {
        return localSchemas;
    }

    public boolean loadCamelCatalog(String version) {
        if (version.contains("redhat")) {
            KAOTO_VERSION_MANAGER.addMavenRepository("central", "https://repo1.maven.org/maven2/");
            KAOTO_VERSION_MANAGER.addMavenRepository("maven.redhat.ga", "https://maven.repository.redhat.com/ga/");
        }

        MavenCoordinates mavenCoordinates = getCatalogMavenCoordinates(runtime, version);

        return loadDependencyInClasspath(mavenCoordinates);
        // return camelCatalog.loadVersion(mavenCoordinates.getVersion());
    }

    public boolean loadCamelYamlDsl(String version) {
        if (version.contains("redhat")) {
            KAOTO_VERSION_MANAGER.addMavenRepository("central", "https://repo1.maven.org/maven2/");
            KAOTO_VERSION_MANAGER.addMavenRepository("maven.redhat.ga", "https://maven.repository.redhat.com/ga/");
        }

        MavenCoordinates mavenCoordinates = getYamlDslMavenCoordinates(runtime, version);
        boolean isCamelYamlDslLoaded = loadDependencyInClasspath(mavenCoordinates);

        ClassLoader classLoader = KAOTO_VERSION_MANAGER.getClassLoader();
        URL resourceURL = classLoader.getResource(Constants.CAMEL_YAML_DSL_ARTIFACT);
        if (resourceURL == null) {
            return false;
        }

        try (InputStream inputStream = resourceURL.openStream()) {
            try (Scanner scanner = new Scanner(inputStream)) {
                scanner.useDelimiter("\\A");
                camelYamlDSLSchema = scanner.hasNext() ? scanner.next() : "";
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            return false;
        }

        return isCamelYamlDslLoaded;
    }

    public boolean loadKameletBoundaries() {
        ClassLoader classLoader = KAOTO_VERSION_MANAGER.getClassLoader();

        URL resourceUrl = classLoader.getResource("kamelet-boundaries");

        try {
            Files.walk(Paths.get(resourceUrl.toURI()))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".kamelet.yaml"))
                    .forEach(path -> {
                        LOGGER.log(Level.INFO, "Parsing: " + path.toString());

                        try {
                            kameletBoundaries.add(new String(Files.readAllBytes(path)));
                        } catch (IOException e) {
                            LOGGER.log(Level.SEVERE, e.toString(), e);
                        }
                    });
        } catch (IOException | URISyntaxException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }

        return !kameletBoundaries.isEmpty();
    }

    public boolean loadKamelets(String version) {
        MavenCoordinates mavenCoordinates = new MavenCoordinates(Constants.APACHE_CAMEL_KAMELETS_ORG,
                Constants.KAMELETS_PACKAGE,
                version);
        boolean areKameletsLoaded = loadDependencyInClasspath(mavenCoordinates);

        ClassLoader classLoader = KAOTO_VERSION_MANAGER.getClassLoader();
        try {
            Iterator<URL> it = classLoader.getResources("kamelets").asIterator();

            while (it.hasNext()) {
                URL resourceUrl = it.next();

                if ("jar".equals(resourceUrl.getProtocol())) {
                    JarURLConnection connection = (JarURLConnection) resourceUrl.openConnection();
                    JarFile jarFile = connection.getJarFile();
                    Enumeration<JarEntry> entries = jarFile.entries();

                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().startsWith(connection.getEntryName()) && !entry.isDirectory()
                                && entry.getName().endsWith(".kamelet.yaml")) {

                            LOGGER.log(Level.INFO, "Parsing: " + entry.getName());
                            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                                try (Scanner scanner = new Scanner(inputStream)) {
                                    scanner.useDelimiter("\\A");
                                    kamelets.add(scanner.hasNext() ? scanner.next() : "");
                                }
                            } catch (IOException e) {
                                LOGGER.log(Level.SEVERE, e.toString(), e);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }

        return areKameletsLoaded;
    }

    public boolean loadKubernetesSchema() {
        String url = "https://raw.githubusercontent.com/kubernetes/kubernetes/master/api/openapi-spec/v3/api__v1_openapi.json";

        try (InputStream in = new URI(url).toURL().openStream();
                Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("\\A");
            kubernetesSchema = scanner.hasNext() ? scanner.next() : "";
        } catch (IOException | URISyntaxException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            return false;
        }

        return true;
    }

    public boolean loadCamelKCRDs(String version) {
        MavenCoordinates mavenCoordinates = new MavenCoordinates(Constants.APACHE_CAMEL_K_ORG,
                Constants.CAMEL_K_CRDS_PACKAGE,
                version);
        boolean areCamelKCRDsLoaded = loadDependencyInClasspath(mavenCoordinates);

        ClassLoader classLoader = KAOTO_VERSION_MANAGER.getClassLoader();

        for (String crd : Constants.CAMEL_K_CRDS_ARTIFACTS) {
            URL resourceURL = classLoader.getResource(crd);
            if (resourceURL == null) {
                return false;
            }

            try (InputStream inputStream = resourceURL.openStream()) {
                try (Scanner scanner = new Scanner(inputStream)) {
                    scanner.useDelimiter("\\A");
                    camelKCRDs.add(scanner.hasNext() ? scanner.next() : "");
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return false;
            }
        }

        return areCamelKCRDsLoaded;
    }

    public void loadLocalSchemas() {
        ClassLoader classLoader = KAOTO_VERSION_MANAGER.getClassLoader();
        URL schemasFolderUrl = classLoader.getResource("schemas");

        try {
            Files.walk(Paths.get(schemasFolderUrl.toURI()))
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        LOGGER.log(Level.INFO, "Parsing: " + path.toString());

                        try {
                            String filenameWithoutExtension = path.toFile().getName().substring(0,
                                    path.toFile().getName().lastIndexOf('.'));
                            localSchemas.put(filenameWithoutExtension, new String(Files.readAllBytes(path)));
                        } catch (IOException e) {
                            LOGGER.log(Level.SEVERE, e.toString(), e);
                        }
                    });
        } catch (IOException | URISyntaxException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
    }

    private MavenCoordinates getCatalogMavenCoordinates(CatalogRuntime runtime, String version) {
        switch (runtime) {
            case Quarkus:
                return new MavenCoordinates(Constants.APACHE_CAMEL_ORG + ".quarkus", "camel-quarkus-catalog", version);
            case SpringBoot:
                return new MavenCoordinates(Constants.APACHE_CAMEL_ORG + ".springboot", "catalog",
                        version);
            default:
                return new MavenCoordinates(Constants.APACHE_CAMEL_ORG, "camel-catalog", version);
        }
    }

    private MavenCoordinates getYamlDslMavenCoordinates(CatalogRuntime runtime, String version) {
        switch (runtime) {
            case Quarkus:
                return new MavenCoordinates(Constants.APACHE_CAMEL_ORG + ".quarkus", "camel-quarkus-yaml-dsl", version);
            case SpringBoot:
                return new MavenCoordinates(Constants.APACHE_CAMEL_ORG + ".springboot", "catalog",
                        version);
            default:
                return new MavenCoordinates(Constants.APACHE_CAMEL_ORG,
                        Constants.CAMEL_YAML_DSL_PACKAGE,
                        version);
        }
    }

    /*
     * This method is used to load a dependency in the classpath. This is a
     * workaround
     * to load dependencies that are not in the classpath, while the Kamel Catalog
     * exposes a method to load dependencies in the classpath.
     */
    private boolean loadDependencyInClasspath(MavenCoordinates mavenCoordinates) {
        return camelCatalog.loadRuntimeProviderVersion(mavenCoordinates.getGroupId(), mavenCoordinates.getArtifactId(),
                mavenCoordinates.getVersion());
    }
}
