import { Component, Type } from '@angular/core';

import { getToolMetadata } from './decision-tool.contract';

@Component({ selector: 'app-with-meta', standalone: true, template: '' })
class WithMetaComponent {
  static readonly TOOL_LABEL = 'TEST LABEL';
  static readonly TOOL_ICON = 'badge';
}

@Component({ selector: 'app-without-meta', standalone: true, template: '' })
class WithoutMetaComponent {}

@Component({ selector: 'app-partial-meta', standalone: true, template: '' })
class PartialMetaComponent {
  static readonly TOOL_LABEL = 'X';
}

describe('getToolMetadata', () => {
  it('retourne label + icon si les 2 statics sont définis', () => {
    expect(getToolMetadata(WithMetaComponent as Type<unknown>)).toEqual({
      label: 'TEST LABEL',
      icon: 'badge',
    });
  });

  it('retourne null si aucun static défini', () => {
    expect(getToolMetadata(WithoutMetaComponent as Type<unknown>)).toBeNull();
  });

  it('retourne null si seulement TOOL_LABEL défini', () => {
    expect(getToolMetadata(PartialMetaComponent as Type<unknown>)).toBeNull();
  });
});
