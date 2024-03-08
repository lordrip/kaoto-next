import { Button, Card, CardTitle } from '@patternfly/react-core';
import { PlusIcon } from '@patternfly/react-icons';
import { ConnectedFieldProps, connectField, useForm } from 'uniforms';
import { getValue } from '../../../utils';

interface CustomStepsFieldProps {
  'data-testid': string;
  [key: string]: string;
}

export const EmptyNestField = connectField((props: ConnectedFieldProps<CustomStepsFieldProps>) => {
  const form = useForm();
  const model = getValue(form.model, props.name);
  console.log(model);

  const onClick = () => {
    props.onChange?.({});
  };

  return (
    <Card>
      <CardTitle>
        {props.label}{' '}
        <Button variant="plain">
          <PlusIcon onClick={onClick} />
        </Button>
      </CardTitle>
    </Card>
  );
});
