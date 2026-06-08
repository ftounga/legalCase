import type { Config } from 'jest';

const config: Config = {
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  restoreMocks: true,
  testMatch: ['<rootDir>/src/**/*.spec.ts'],
  transform: {
    '^.+\\.(ts|js|mjs|html|svg)$': [
      'jest-preset-angular',
      {
        tsconfig: '<rootDir>/tsconfig.spec.json',
        stringifyContentPathRegex: '\\.html$',
      },
    ],
  },
  moduleFileExtensions: ['ts', 'html', 'js', 'json', 'mjs'],
  // `marked` est publié en ESM pur via un `.js` (package.json "type": "module").
  // Jest tourne en CommonJS : il faut autoriser sa transformation. On reprend le
  // pattern par défaut de jest-preset-angular (qui débloque déjà les `.mjs` et
  // les locales Angular) en y ajoutant `marked`, sinon ce pattern écraserait
  // celui du preset et casserait le chargement des modules Angular ESM.
  transformIgnorePatterns: [
    'node_modules/(?!(.*\\.mjs$|@angular/common/locales/.*\\.js$|marked/))',
  ],
};

export default config;
