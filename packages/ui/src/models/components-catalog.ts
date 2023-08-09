export type ComponentsCatalog = Record<string, ComponentsCatalogItem>;

export interface ComponentsCatalogItem {
  component: ComponentsCatalogItemComponent;
  componentProperties: ComponentsCatalogItemProperties;
  headers: ComponentsCatalogItemHeaders;
  properties: ComponentsCatalogItemProperties;
}

export interface ComponentsCatalogItemComponent {
  kind: string;
  name: string;
  title: string;
  description: string;
  deprecated: boolean;
  firstVersion?: string;
  label: string;
  javaType?: string;
  supportLevel: string;
  groupId?: string;
  artifactId?: string;
  version: string;
  scheme?: string;
  extendsScheme?: string;
  syntax?: string;
  async?: boolean;
  api?: boolean;
  consumerOnly?: boolean;
  producerOnly?: boolean;
  lenientProperties?: boolean;
}

export interface ComponentsCatalogItemCommon {
  index: number;
  kind: string;
  displayName: string;
  group: string;
  label: string;
  required: boolean;
  javaType: string;
  deprecated: boolean;
  deprecationNote: string;
  autowired: boolean;
  secret: boolean;
  description: string;
}

export interface ComponentsCatalogItemProperties {
  type: string;
}

export interface ComponentsCatalogItemHeaders extends ComponentsCatalogItemCommon {
  constantName: string;
}
