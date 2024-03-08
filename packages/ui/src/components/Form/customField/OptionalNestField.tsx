import { NestField } from '@kaoto-next/uniforms-patternfly';
import { ConnectedFieldProps, useForm } from 'uniforms';
import { getValue, isDefined } from '../../../utils';
import { EmptyNestField } from './EmptyNestField';

interface CustomStepsFieldProps {
  'data-testid': string;
  [key: string]: string;
}

export const OptionalNestField = (props: ConnectedFieldProps<CustomStepsFieldProps>) => {
  const form = useForm();
  const model = getValue(form.model, props.name);
  console.log(model);

  if (isDefined(model)) {
    return <NestField {...props} />;
  }

  return <EmptyNestField {...props} />;
};
