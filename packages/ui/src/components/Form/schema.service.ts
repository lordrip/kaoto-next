import { filterDOMProps, FilterDOMPropsKeys } from 'uniforms';
import { SchemaBridge } from './schema-bridge';

export class SchemaService {
  static readonly DROPDOWN_PLACEHOLDER = 'Select an option...';
  static readonly OMIT_FORM_FIELDS = [
    'from',
    'expression',
    'dataFormatType',
    'outputs',
    'steps',
    'onWhen',
    'when',
    'otherwise',
    'doCatch',
    'doFinally',
    'uri',
  ];
  private static readonly noopValidator = (_model: unknown) => null;
  private static readonly FILTER_DOM_PROPS = ['$comment', 'additionalProperties'];

  getSchemaBridge(schema?: unknown): SchemaBridge | undefined {
    if (!schema) return undefined;

    // uniforms passes it down to the React elements as an attribute, causes a warning
    SchemaService.FILTER_DOM_PROPS.forEach((prop) => filterDOMProps.register(prop as FilterDOMPropsKeys));

    return new SchemaBridge({ schema, validator: SchemaService.noopValidator });
  }
}
