package io.kaoto.camelcatalog.model;

import java.util.ArrayList;
import java.util.List;

public class CatalogLibrary {
    private String name;
    private List<CatalogLibraryEntry> definitions = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CatalogLibraryEntry> getDefinitions() {
        return definitions;
    }

    public void addDefinition(CatalogDefinition catalogDefinition) {
        CatalogLibraryEntry entry = new CatalogLibraryEntry();
        entry.setName(catalogDefinition.getName());
        entry.setVersion(catalogDefinition.getVersion());
        entry.setRuntime(catalogDefinition.getRuntime());
        entry.setFileName(catalogDefinition.getFileName());

        definitions.add(entry);
    }
}
