import { createWithEqualityFn } from 'zustand/traditional';
import { ComponentsCatalog } from '../models';
import { shallow } from 'zustand/shallow';

interface ComponentsCatalogState {
  componentsCatalog: ComponentsCatalog;
  setComponentsCatalog: (catalog: ComponentsCatalog) => void;
}

export const useComponentsCatalogStore = createWithEqualityFn<ComponentsCatalogState>(
  (set) => ({
    componentsCatalog: {},
    setComponentsCatalog: (componentsCatalog: ComponentsCatalog) => {
      set({ componentsCatalog });
    },
  }),
  shallow,
);
