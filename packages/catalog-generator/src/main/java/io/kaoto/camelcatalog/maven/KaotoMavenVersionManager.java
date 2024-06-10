package io.kaoto.camelcatalog.maven;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.camel.catalog.maven.MavenVersionManager;
import org.apache.camel.tooling.maven.MavenArtifact;
import org.apache.camel.tooling.maven.MavenDownloader;
import org.apache.camel.tooling.maven.MavenDownloaderImpl;
import org.apache.camel.tooling.maven.MavenResolutionException;

/**
 * This class is a copy of the MavenVersionManager class from the Apache Camel
 * Catalog project.
 *
 * This is needed because the `resolve` method doesn't resolve transitive dependencies
 * and we need to load the underlying Camel YAML DSL from Quarkus and Spring Boot
 * runtime providers.
 */
public class KaotoMavenVersionManager extends MavenVersionManager {
    private static final Logger LOGGER = Logger.getLogger(KaotoMavenVersionManager.class.getName());

    protected final MavenDownloader downloader;
    protected final Map<String, String> repositories = new LinkedHashMap<>();
    private String version;
    private String runtimeProviderVersion;
    private boolean log;

    private KaotoMavenVersionManager(MavenDownloaderImpl downloader) {
        this.downloader = downloader;
        downloader.build();
    }

    public KaotoMavenVersionManager() {
        this(new MavenDownloaderImpl());
        this.setClassLoader(new KaotoOpenURLClassLoader());
    }

    /**
     * To add a 3rd party Maven repository.
     *
     * @param name the repository name
     * @param url  the repository url
     */
    public void addMavenRepository(String name, String url) {
        super.addMavenRepository(name, url);
        repositories.put(name, url);
    }

    @Override
    public String getLoadedVersion() {
        return version;
    }

    @Override
    public String getRuntimeProviderLoadedVersion() {
        return runtimeProviderVersion;
    }

    @Override
    public boolean loadRuntimeProviderVersion(String groupId, String artifactId, String version) {
        try {
            MavenDownloader mavenDownloader = downloader;
            // if (customized) {
            // mavenDownloader = mavenDownloader.customize(localRepository, connectTimeout,
            // requestTimeout);
            // }

            String gav = String.format("%s:%s:%s", groupId, artifactId, version);
            resolve(mavenDownloader, gav, version.contains("SNAPSHOT"));

            if (artifactId.contains("catalog")) {
                this.version = version;
            } else {
                this.runtimeProviderVersion = version;
            }

            return true;
        } catch (Exception e) {
            if (log) {
                LOGGER.log(Level.WARNING,
                        String.format("Cannot load runtime provider version {} due {}", version, e.getMessage()), e);
            }
            return false;
        }
    }

    /**
     * Resolves Maven artifact using passed coordinates and use downloaded artifact
     * as one of the URLs in the
     * helperClassLoader, so further Catalog access may load resources from it.
     */
    public void resolve(MavenDownloader mavenDownloader, String gav, boolean useSnapshots)
            throws MavenResolutionException, MalformedURLException {
        try {
            Set<String> extraRepositories = new LinkedHashSet<>(repositories.values());

            // non-transitive resolve, because we load static data from the catalog
            // artifacts
            List<MavenArtifact> artifacts = mavenDownloader.resolveArtifacts(Collections.singletonList(gav),
                    extraRepositories, true, useSnapshots);

            System.out.println("Artifacts: " + artifacts);

            for (MavenArtifact ma : artifacts) {
                ((KaotoOpenURLClassLoader) getClassLoader()).addURL(ma.getFile().toURI().toURL());
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream is = null;

        if (runtimeProviderVersion != null) {
            is = doGetResourceAsStream(name, runtimeProviderVersion);
        }
        if (is == null && version != null) {
            is = doGetResourceAsStream(name, version);
        }
        if (getClassLoader() != null && is == null) {
            is = getClassLoader().getResourceAsStream(name);
        }

        return is;
    }

    private InputStream doGetResourceAsStream(String name, String version) {
        if (version == null) {
            return null;
        }

        try {
            URL found = null;
            Enumeration<URL> urls = getClassLoader().getResources(name);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url.getPath().contains(version)) {
                    found = url;
                    break;
                }
            }
            if (found != null) {
                return found.openStream();
            }
        } catch (IOException e) {
            if (log) {
                LOGGER.log(Level.WARNING, String.format("Cannot open resource {} and version {} due {}", name, version,
                        e.getMessage(), e));

            }
        }

        return null;
    }
}
