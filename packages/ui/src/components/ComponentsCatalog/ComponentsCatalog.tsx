import { Badge, Card, CardBody, CardFooter, CardHeader, CardTitle, Gallery } from '@patternfly/react-core';
import { FunctionComponent, PropsWithChildren } from 'react';
import camelLogo from '../../assets/camel-logo.svg';
import { ComponentsCatalog as ComponentsCatalogModel } from '../../models';
import './ComponentsCatalog.scss';

interface ComponponentsCatalogProps {
  components?: ComponentsCatalogModel;
  onComponentSelect?: (componentName: string) => void;
}

export const ComponentsCatalog: FunctionComponent<PropsWithChildren<ComponponentsCatalogProps>> = (props) => {
  return (
    <Gallery className="components-catalog" hasGutter aria-label="Selectable components catalog">
      {props.components &&
        Object.entries(props.components).map(([componentName, { component }]) => (
          <Card
            isClickable
            isCompact
            key={componentName}
            id={componentName}
            onClick={() => {
              console.log(`${componentName} clicked`);
            }}
          >
            <CardHeader
              selectableActions={{
                variant: 'single',
                selectableActionId: componentName,
                selectableActionAriaLabelledby: `Selectable ${componentName}`,
                name: componentName,
              }}
            >
              <div className="components-catalog__header">
                <img className="components-catalog__icon" alt="camel logo" src={camelLogo} />
                <Badge key={`${componentName}-support-level`} isRead>
                  {component.supportLevel}
                </Badge>
              </div>

              <CardTitle className="components-catalog__title">
                <span>{component.title}</span>
              </CardTitle>
            </CardHeader>

            <CardBody>{component.description}</CardBody>

            <CardFooter className="components-catalog__header">
              <Badge key={`${componentName}-label`} isRead>
                {component.label}
              </Badge>
              <Badge key={`${componentName}-version`} isRead>
                {component.version}
              </Badge>
            </CardFooter>
          </Card>
        ))}
    </Gallery>
  );
};
