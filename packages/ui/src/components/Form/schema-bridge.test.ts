import { errorHandlerSchema } from '../../stubs/error-handler';
import { SchemaBridge } from './schema-bridge';

describe('SchemaBridge', () => {
  let schemaBridge: SchemaBridge;

  beforeEach(() => {
    schemaBridge = new SchemaBridge({ validator: () => null, schema: errorHandlerSchema });
    schemaBridge.getSubfields();
    Object.keys(errorHandlerSchema.properties!).forEach((key) => {
      schemaBridge.getSubfields(key);
    });
  });
});
