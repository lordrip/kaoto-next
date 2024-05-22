package io.kaoto.camelcatalog;

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

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.VersionManager;
import org.apache.camel.catalog.maven.MavenVersionManager;

public class CamelCatalogVersionLoader {
    private final VersionManager VERSION_MANAGER = new MavenVersionManager();
    private CamelCatalog camelCatalog = new DefaultCamelCatalog(false);
    private String camelYamlDSLSchema;
    private String kubernetesSchema;
    private List<String> kameletBoundaries = new ArrayList<>();
    private List<String> kamelets = new ArrayList<>();
    private List<String> camelKCRDs = new ArrayList<>();
    private Map<String, String> localSchemas = new HashMap<>();

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
        camelCatalog.setVersionManager(VERSION_MANAGER);

        return camelCatalog.loadVersion(version);
    }

    public boolean loadCamelYamlDsl(String version) {
        boolean isCamelYamlDslLoaded = loadDepedencyInClasspath(Constants.APACHE_CAMEL_ORG,
                Constants.CAMEL_YAML_DSL_PACKAGE,
                version);

        ClassLoader classLoader = VERSION_MANAGER.getClassLoader();
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
            e.printStackTrace();
            return false;
        }

        return isCamelYamlDslLoaded;
    }

    public boolean loadKameletBoundaries() {
        ClassLoader classLoader = VERSION_MANAGER.getClassLoader();

        URL resourceUrl = classLoader.getResource("kamelet-boundaries");

        try {
            Files.walk(Paths.get(resourceUrl.toURI()))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".kamelet.yaml"))
                    .forEach(path -> {
                        System.out.println(path);
                        try {
                            kameletBoundaries.add(new String(Files.readAllBytes(path)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        return kameletBoundaries.size() > 0;
    }

    public boolean loadKamelets(String version) {
        boolean areKameletsLoaded = loadDepedencyInClasspath(Constants.APACHE_CAMEL_KAMELETS_ORG,
                Constants.KAMELETS_PACKAGE,
                version);

        ClassLoader classLoader = VERSION_MANAGER.getClassLoader();
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
                            System.out.println(entry.getName());
                            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                                try (Scanner scanner = new Scanner(inputStream)) {
                                    scanner.useDelimiter("\\A");
                                    kamelets.add(scanner.hasNext() ? scanner.next() : "");
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean loadCamelKCRDs(String version) {
        boolean areCamelKCRDsLoaded = loadDepedencyInClasspath(Constants.APACHE_CAMEL_K_ORG,
                Constants.CAMEL_K_CRDS_PACKAGE,
                version);

        ClassLoader classLoader = VERSION_MANAGER.getClassLoader();

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
                e.printStackTrace();
                return false;
            }
        }

        return areCamelKCRDsLoaded;
    }

    public void loadLocalSchemas() {
        ClassLoader classLoader = VERSION_MANAGER.getClassLoader();
        URL schemasFolderUrl = classLoader.getResource("schemas");

        try {
            Files.walk(Paths.get(schemasFolderUrl.toURI()))
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        System.out.println(path);
                        try {
                            String filenameWithoutExtension = path.toFile().getName().substring(0,
                                    path.toFile().getName().lastIndexOf('.'));
                            localSchemas.put(filenameWithoutExtension, new String(Files.readAllBytes(path)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /*
     * This method is used to load a dependency in the classpath. This is a
     * workaround
     * to load dependencies that are not in the classpath, while the Kamel Catalog
     * exposes a method to load dependencies in the classpath.
     */
    private boolean loadDepedencyInClasspath(String groupId, String artifactId, String version) {
        return camelCatalog.loadRuntimeProviderVersion(groupId,
                artifactId,
                version);
    }
}
