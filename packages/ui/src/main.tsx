import '@patternfly/patternfly/patternfly.scss'; // This import needs to be first
import '@patternfly/patternfly/patternfly-addons.scss';
import ReactDOM from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import { FilterDOMPropsKeys, filterDOMProps } from 'uniforms';
import { router } from './router';

filterDOMProps.register('inputRef' as FilterDOMPropsKeys, 'placeholder' as FilterDOMPropsKeys);

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  /*
   * uniforms is not compatible with StrictMode yet
   * <React.StrictMode>
   * </React.StrictMode>,
   */

  <RouterProvider router={router} />,
);
