import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { SimulatorRunnerPageComponent } from './simulator-runner-page.component';
import { SimulatorsCatalogService } from '../core/services/simulators-catalog.service';
import { DecisionToolsPanelComponent } from '../case-files/decisional-tools-panel/decisional-tools-panel.component';
import { STANDALONE_READY_TOOL_IDS } from './standalone-ready-tools';

/**
 * F-163 SF-163-02a — Tests du runner `/simulators/:toolId`.
 *
 * Couvre les 4 cas CA-12 :
 *   (a) toolId whitelisted → componentInputs() est non-null, entry resolved.
 *   (b) toolId valide hors whitelist → bannière « Disponible bientôt ».
 *   (c) toolId inconnu de TOOL_REGISTRY → bannière 404.
 *   (d) Navigation depuis catalogue → couverte côté `simulators-catalog-page`.
 */

interface CatalogServiceStub {
  getCatalog: jest.Mock;
}

interface SnackStub {
  open: jest.Mock;
}

function setup(
  toolIdParam: string | null,
  options: {
    catalogReturn?: ReturnType<typeof of> | ReturnType<typeof throwError>;
  } = {},
): {
  fixture: ComponentFixture<SimulatorRunnerPageComponent>;
  component: SimulatorRunnerPageComponent;
  service: CatalogServiceStub;
  snack: SnackStub;
} {
  const service: CatalogServiceStub = {
    getCatalog: jest.fn().mockReturnValue(
      options.catalogReturn ??
        of({ legalDomain: 'DROIT_DU_TRAVAIL', country: 'FRANCE', toolIds: [] }),
    ),
  };
  const snack: SnackStub = { open: jest.fn() };

  TestBed.configureTestingModule({
    imports: [SimulatorRunnerPageComponent, NoopAnimationsModule],
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideHttpClientTesting(),
      {
        provide: ActivatedRoute,
        useValue: {
          paramMap: of({
            get: (key: string) => (key === 'toolId' ? toolIdParam : null),
          }),
        },
      },
      { provide: SimulatorsCatalogService, useValue: service },
      { provide: MatSnackBar, useValue: snack },
    ],
  });

  const fixture = TestBed.createComponent(SimulatorRunnerPageComponent);
  const component = fixture.componentInstance;
  return { fixture, component, service, snack };
}

describe('SimulatorRunnerPageComponent — toolId whitelisted', () => {
  it('résout entry et expose componentInputs avec standaloneMode=true (CA-12 a)', fakeAsync(() => {
    // Sanity : F-DT-08 doit être dans la whitelist + dans TOOL_REGISTRY.
    expect(STANDALONE_READY_TOOL_IDS.has('F-DT-08-licenciement-validity')).toBe(true);
    expect(
      DecisionToolsPanelComponent.TOOL_REGISTRY.has('F-DT-08-licenciement-validity'),
    ).toBe(true);

    const { fixture, component } = setup('F-DT-08-licenciement-validity');
    fixture.detectChanges();
    tick();

    expect(component.toolId()).toBe('F-DT-08-licenciement-validity');
    expect(component.entry()).not.toBeNull();
    expect(component.isStandaloneReady()).toBe(true);
    expect(component.isNotFound()).toBe(false);

    const inputs = component.componentInputs();
    expect(inputs).not.toBeNull();
    // standaloneMode propagé via TOOL_REGISTRY.inputs(ctx) (entrée F-DT-08).
    expect(inputs!['standaloneMode']).toBe(true);
    // workspaceCountry résolu depuis le service catalogue.
    expect(inputs!['workspaceCountry']).toBe('FRANCE');
  }));

  it('résout workspaceCountry=BELGIQUE quand le workspace est BE', fakeAsync(() => {
    const { fixture, component } = setup('F-DT-08-licenciement-validity', {
      catalogReturn: of({ legalDomain: 'DROIT_DU_TRAVAIL', country: 'BELGIQUE', toolIds: [] }),
    });
    fixture.detectChanges();
    tick();

    expect(component.workspaceCountry()).toBe('BELGIQUE');
    expect(component.componentInputs()!['workspaceCountry']).toBe('BELGIQUE');
  }));

  it('fallback workspaceCountry=FRANCE quand le service catalogue échoue', fakeAsync(() => {
    const { fixture, component, snack } = setup('F-DT-08-licenciement-validity', {
      catalogReturn: throwError(() => new Error('500')),
    });
    fixture.detectChanges();
    tick();

    expect(component.workspaceCountry()).toBe('FRANCE');
    expect(snack.open).toHaveBeenCalled();
  }));
});

describe('SimulatorRunnerPageComponent — toolId hors whitelist', () => {
  it('affiche "Disponible bientôt" pour un toolId valide non whitelisted (CA-12 b)', fakeAsync(() => {
    // Trouver un toolId présent dans TOOL_REGISTRY mais ABSENT de la whitelist.
    const allIds = Array.from(DecisionToolsPanelComponent.TOOL_REGISTRY.keys());
    const nonWhitelisted = allIds.find((id) => !STANDALONE_READY_TOOL_IDS.has(id));
    expect(nonWhitelisted).toBeDefined();

    const { fixture, component } = setup(nonWhitelisted!);
    fixture.detectChanges();
    tick();

    expect(component.entry()).not.toBeNull();
    expect(component.isStandaloneReady()).toBe(false);
    expect(component.isNotFound()).toBe(false);
    expect(component.componentInputs()).toBeNull();

    // Bannière "Disponible bientôt" visible.
    const banner = fixture.nativeElement.querySelector(
      '[data-testid="banner-not-ready"]',
    );
    expect(banner).not.toBeNull();
  }));
});

describe('SimulatorRunnerPageComponent — toolId inconnu', () => {
  it('affiche bannière 404 pour un toolId absent de TOOL_REGISTRY (CA-12 c)', fakeAsync(() => {
    const { fixture, component } = setup('F-XXX-totally-unknown');
    fixture.detectChanges();
    tick();

    expect(component.entry()).toBeNull();
    expect(component.isNotFound()).toBe(true);
    expect(component.isStandaloneReady()).toBe(false);

    const banner = fixture.nativeElement.querySelector(
      '[data-testid="banner-not-found"]',
    );
    expect(banner).not.toBeNull();
  }));
});

describe('SimulatorRunnerPageComponent — toolId null', () => {
  it('toolId null → entry null, pas de crash', fakeAsync(() => {
    const { fixture, component } = setup(null);
    fixture.detectChanges();
    tick();

    expect(component.toolId()).toBeNull();
    expect(component.entry()).toBeNull();
    expect(component.isNotFound()).toBe(false);
    expect(component.isStandaloneReady()).toBe(false);
  }));
});
