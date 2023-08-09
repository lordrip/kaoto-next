export interface CamelCatalogIndex {
  catalogs: Catalogs;
  schemas: CatalogEntry[];
  kamelets: CatalogEntry[];
}

export interface Catalogs {
  models: CatalogEntry;
  components: CatalogEntry;
  languages: CatalogEntry;
  dataformats: CatalogEntry;
}

export interface CatalogEntry {
  name: string;
  version: string;
  files: string[];
}
