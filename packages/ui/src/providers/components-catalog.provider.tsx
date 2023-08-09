import catalogIndex from '@kaoto-next/camel-catalog/index.json?url';
import { FunctionComponent, PropsWithChildren, createContext, useEffect } from 'react';
import { ComponentsCatalog } from '../models';
import { useComponentsCatalogStore } from '../store';
import { CamelCatalogIndex } from '../models/camel-catalog-index';

export const ComponentsCatalogContext = createContext<ComponentsCatalog>({});

/**
 * Loader for the components catalog.
 */
export const ComponentsCatalogProvider: FunctionComponent<PropsWithChildren> = (props) => {
  const { componentsCatalog, setComponentsCatalog } = useComponentsCatalogStore((state) => state);

  useEffect(() => {
    fetch(catalogIndex)
      .then((response) => response.json())
      .then((catalogIndex: CamelCatalogIndex) => {
        const componentsCatalog = catalogIndex.catalogs.components.files.map(async (file) => {
          const response = await fetch(`camel-catalog/${file}`);
          return await response.json();
        });

        Promise.all(componentsCatalog).then((catalogs) => {
          setComponentsCatalog(catalogs.reduce((acc, catalog) => ({ ...acc, ...catalog }), {}));
        });
      });
  }, []);

  return (
    <ComponentsCatalogContext.Provider value={componentsCatalog}>{props.children}</ComponentsCatalogContext.Provider>
  );
};
