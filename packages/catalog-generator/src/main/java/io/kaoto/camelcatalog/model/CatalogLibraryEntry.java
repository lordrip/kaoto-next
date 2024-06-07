package io.kaoto.camelcatalog.model;

public class CatalogLibraryEntry {
    private String name;
    private String version;
    private CatalogRuntime runtime;
    private String fileName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public CatalogRuntime getRuntime() {
        return runtime;
    }

    public void setRuntime(CatalogRuntime runtime) {
        this.runtime = runtime;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
