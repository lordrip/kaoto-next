import {
  MenuToggle,
  MenuToggleAction,
  MenuToggleElement,
  Select,
  SelectList,
  SelectOption,
  Tooltip,
} from '@patternfly/react-core';
import { FunctionComponent, MouseEvent, PropsWithChildren, Ref, useCallback, useState } from 'react';
import { SourceSchemaType } from '../../models/camel/source-schema-type';

interface ISourceTypeSelector extends PropsWithChildren {
  isStatic?: boolean;
  onSelect?: (value: SourceSchemaType) => void;
}

export const ConfigurationTypeSelector: FunctionComponent<ISourceTypeSelector> = (props) => {
  const [isOpen, setIsOpen] = useState(false);
  const [selected, setSelected] = useState<SourceSchemaType | null>(null);

  /** Toggle the DSL dropdown */
  const onToggleClick = () => {
    setIsOpen(!isOpen);
  };

  /** Selecting a DSL checking the the existing flows */
  const onSelect = useCallback((_event: MouseEvent | undefined, flowType: string | number | undefined) => {
    setIsOpen(false);
  }, []);

  const toggle = (toggleRef: Ref<MenuToggleElement>) => (
    <MenuToggle
      data-testid="dsl-list-dropdown"
      ref={toggleRef}
      onClick={onToggleClick}
      isExpanded={isOpen}
      isFullWidth
      splitButtonOptions={{
        variant: 'action',
        items: [
          <Tooltip key="dsl-list-tooltip" position="bottom" content={<span>Tooltip</span>}>
            <MenuToggleAction
              id="dsl-list-btn"
              key="dsl-list-btn"
              data-testid="dsl-list-btn"
              aria-label="DSL list"
              isDisabled={false}
            >
              {props.children}
            </MenuToggleAction>
          </Tooltip>,
        ],
      }}
    />
  );

  return (
    <Select
      id="dsl-list-select"
      isOpen={isOpen}
      selected={selected}
      onSelect={onSelect}
      onOpenChange={(isOpen) => {
        setIsOpen(isOpen);
      }}
      toggle={toggle}
      style={{ width: '20rem' }}
    >
      <SelectList>
        {[].map((obj, index) => {
          const isOptionDisabled = false;

          return (
            <SelectOption
              key={'dsl-' + index}
              data-testid={'dsl-' + index}
              itemId={'sourceType'}
              description={'Description'}
              isDisabled={isOptionDisabled}
            >
              Name
              {isOptionDisabled && ' (single route only)'}
            </SelectOption>
          );
        })}
      </SelectList>
    </Select>
  );
};
