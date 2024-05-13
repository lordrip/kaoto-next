import {
  Editor,
  EditorFactory,
  EditorInitArgs,
  KogitoEditorEnvelopeContextType,
} from '@kie-tools-core/editor/dist/api';
import { KaotoEditorApp } from './KaotoEditorApp';
import { KaotoEditorChannelApi } from './api/KaotoEditorChannelApi';

export class KaotoEditorFactory implements EditorFactory<Editor, KaotoEditorChannelApi> {
  public createEditor(
    envelopeContext: KogitoEditorEnvelopeContextType<KaotoEditorChannelApi>,
    initArgs: EditorInitArgs,
  ): Promise<Editor> {
    return Promise.resolve(new KaotoEditorApp(envelopeContext, initArgs));
  }
}
