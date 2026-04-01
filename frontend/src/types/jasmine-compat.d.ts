/**
 * Jasmine type compatibility shim for Jest migration.
 * Maps Jasmine types to their Jest equivalents so existing spec files
 * continue to compile without modification.
 */
declare namespace jasmine {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  type SpyObj<T> = jest.Mocked<T>;
  type Spy = jest.SpyInstance;

  // Runtime values declared to satisfy TS — actual impl is in setup-jest.ts
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  function createSpyObj<T>(baseName: string, methodNames: (keyof T | string)[], properties?: Record<string, any>): jest.Mocked<T>;
  function createSpy(name?: string): jest.Mock;
  function any(expectedClass: unknown): jasmine.AsymmetricMatcher<unknown>;
  function stringContaining(expected: string): jasmine.AsymmetricMatcher<string>;
  function stringMatching(expected: string | RegExp): jasmine.AsymmetricMatcher<string>;
  function objectContaining<T>(sample: Partial<T>): jasmine.AsymmetricMatcher<T>;
  function arrayContaining<T>(sample: T[]): jasmine.AsymmetricMatcher<T[]>;

  // Minimal AsymmetricMatcher to satisfy expect() usages
  interface AsymmetricMatcher<T> {
    asymmetricMatch(other: T): boolean;
    jasmineToString?(): string;
  }
}
