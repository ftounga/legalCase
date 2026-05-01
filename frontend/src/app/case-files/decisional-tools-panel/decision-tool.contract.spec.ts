import { Component, Type } from '@angular/core';

import {
  PrefillCountInput,
  _resetPrefillWarningsForTests,
  getToolMetadata,
  getToolPrefillCount,
} from './decision-tool.contract';

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

describe('getToolPrefillCount', () => {
  beforeEach(() => _resetPrefillWarningsForTests());

  it('returns null when component does not expose getPrefillCount', () => {
    class NoStaticComponent {}
    expect(getToolPrefillCount(NoStaticComponent as Type<unknown>, {})).toBeNull();
  });

  it('returns the count from static getPrefillCount', () => {
    class WithStatic {
      static getPrefillCount(input: PrefillCountInput): number {
        return input.aiData ? 2 : 0;
      }
    }
    expect(getToolPrefillCount(WithStatic as Type<unknown>, {})).toBe(0);
    expect(getToolPrefillCount(WithStatic as Type<unknown>, { aiData: {} })).toBe(2);
  });

  it('captures errors and returns null (warn 1x per component)', () => {
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    class Throwy {
      static getPrefillCount(): number {
        throw new Error('boom');
      }
    }
    expect(getToolPrefillCount(Throwy as Type<unknown>, {})).toBeNull();
    expect(getToolPrefillCount(Throwy as Type<unknown>, {})).toBeNull();
    expect(warn).toHaveBeenCalledTimes(1);
    warn.mockRestore();
  });
});
