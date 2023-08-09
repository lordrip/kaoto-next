import { FunctionComponent } from 'react';
import { ComponentsCatalog as ComponentsCatalogComponent } from '../../components/ComponentsCatalog';
import { useComponentsCatalogStore } from '../../store';

export const ComponentsCatalog: FunctionComponent = () => {
  const { componentsCatalog } = useComponentsCatalogStore((state) => state);

  return (
    <>
      <p>ComponentsCatalog Page</p>
      <ComponentsCatalogComponent components={componentsCatalog} />
    </>
  );
};
