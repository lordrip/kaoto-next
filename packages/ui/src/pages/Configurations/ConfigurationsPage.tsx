import {
  Card,
  Divider,
  Flex,
  FlexItem,
  JumpLinks,
  JumpLinksItem,
  Split,
  SplitItem,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from '@patternfly/react-core';
import { FunctionComponent } from 'react';
import { NewConfiguration } from '../../components/Configurations';

const verticalOrientation = {
  default: 'vertical',
} as const;

export const ConfigurationsPage: FunctionComponent = () => {
  const items = (
    <>
      <ToolbarItem variant="search-filter">
        <NewConfiguration />
      </ToolbarItem>
    </>
  );

  return (
    <Card>
      <Toolbar id="toolbar-items-example">
        <ToolbarContent>{items}</ToolbarContent>
      </Toolbar>

      <Split>
        <SplitItem>hola</SplitItem>

        <SplitItem>bbb</SplitItem>
      </Split>
    </Card>
  );
};
