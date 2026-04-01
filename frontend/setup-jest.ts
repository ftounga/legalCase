import 'jest-preset-angular/setup-jest';

// ---------------------------------------------------------------------------
// Jasmine compatibility shim — keeps existing spec files unchanged
// ---------------------------------------------------------------------------

function makeJestSpy(mockFn: jest.Mock) {
  mockFn['and'] = {
    returnValue: (val: unknown) => { mockFn.mockReturnValue(val); return mockFn; },
    returnValues: (...vals: unknown[]) => { vals.forEach(v => mockFn.mockReturnValueOnce(v)); return mockFn; },
    callFake: (fn: (...args: unknown[]) => unknown) => { mockFn.mockImplementation(fn); return mockFn; },
    throwError: (err: unknown) => { mockFn.mockImplementation(() => { throw err; }); return mockFn; },
    callThrough: () => mockFn,
    stub: () => { mockFn.mockReset(); return mockFn; },
  };
  Object.defineProperty(mockFn, 'calls', {
    get: () => ({
      count: () => mockFn.mock.calls.length,
      reset: () => mockFn.mockReset(),
      mostRecent: () => ({ args: mockFn.mock.calls[mockFn.mock.calls.length - 1] }),
      all: () => mockFn.mock.calls.map(args => ({ args })),
      any: () => mockFn.mock.calls.length > 0,
    }),
    configurable: true,
  });
  return mockFn;
}

(globalThis as any).jasmine = {
  createSpyObj: (_name: string, methods: string[], properties?: Record<string, unknown>) => {
    const obj: Record<string, unknown> = {};
    methods.forEach(m => { obj[m] = makeJestSpy(jest.fn()); });
    if (properties) { Object.assign(obj, properties); }
    return obj;
  },
  createSpy: (_name?: string) => makeJestSpy(jest.fn()),
  any: (type: unknown) => expect.any(type as new (...args: unknown[]) => unknown),
  stringContaining: (str: string) => expect.stringContaining(str),
  stringMatching: (str: string | RegExp) => expect.stringMatching(str),
  objectContaining: (obj: object) => expect.objectContaining(obj),
  arrayContaining: (arr: unknown[]) => expect.arrayContaining(arr),
};

// spyOn is a Jasmine global — Jasmine does NOT call through by default; jest.spyOn does.
// We match Jasmine semantics: spy replaces the method with a no-op unless overridden.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
(globalThis as any).spyOn = (object: any, method: string, accessType?: 'get' | 'set') => {
  const spy = jest.spyOn(object, method, accessType);
  if (!accessType) {
    spy.mockImplementation(() => undefined);
  }
  return spy;
};

// ---------------------------------------------------------------------------
// Browser API polyfills missing from jsdom
// ---------------------------------------------------------------------------

// URL.createObjectURL / revokeObjectURL are not implemented in jsdom
if (typeof URL.createObjectURL === 'undefined') {
  URL.createObjectURL = jest.fn();
}
if (typeof URL.revokeObjectURL === 'undefined') {
  URL.revokeObjectURL = jest.fn();
}

// IntersectionObserver is not implemented in jsdom
if (typeof (globalThis as any).IntersectionObserver === 'undefined') {
  (globalThis as any).IntersectionObserver = class {
    observe = jest.fn();
    unobserve = jest.fn();
    disconnect = jest.fn();
    constructor(_callback: IntersectionObserverCallback, _options?: IntersectionObserverInit) {}
  };
}
