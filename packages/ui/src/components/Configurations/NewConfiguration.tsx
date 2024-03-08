import { PlusIcon } from '@patternfly/react-icons';
import { FunctionComponent } from 'react';
import { ConfigurationTypeSelector } from './ConfigurationTypeSelector';

export const NewConfiguration: FunctionComponent = () => {
  return (
    <>
      <ConfigurationTypeSelector
        isStatic
        onSelect={() => {
          console.log('click');
        }}
      >
        <PlusIcon />
        <span className="pf-v5-u-m-sm">New configuration</span>
      </ConfigurationTypeSelector>
    </>
  );
};
