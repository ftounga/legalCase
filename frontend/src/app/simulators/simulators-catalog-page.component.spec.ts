import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { Router, provideRouter } from '@angular/router';

import { SimulatorsCatalogPageComponent } from './simulators-catalog-page.component';
import { SimulatorsCatalogService } from '../core/services/simulators-catalog.service';
import { SimulatorsCatalogResponse } from '../core/models/simulators-catalog.model';
import { SimulatorInfoDialogComponent } from './simulator-info-dialog.component';
import { DecisionToolsPanelComponent } from '../case-files/decisional-tools-panel/decisional-tools-panel.component';
import { STANDALONE_READY_TOOL_IDS } from './standalone-ready-tools';

// Récupère un échantillon de toolIds réels dans TOOL_REGISTRY pour les tests.
// Ces 3 IDs existent à la date du 2026-05-11 (cf. seeds F-IA-04 / F-DT-* / F-IM-*).
const REAL_TOOL_IDS = Array.from(
  DecisionToolsPanelComponent.TOOL_REGISTRY.keys(),
);
const ID_OQTF = REAL_TOOL_IDS.find((id) => id === 'F-IM-08-oqtf-avec-delai-fr') ?? REAL_TOOL_IDS[0];
const ID_LICENCIEMENT = REAL_TOOL_IDS.find((id) => id === 'F-DT-08-licenciement-validity') ?? REAL_TOOL_IDS[1];
const ID_RUPTURE = REAL_TOOL_IDS.find((id) => id === 'F-DT-10-rupture-conv-validity') ?? REAL_TOOL_IDS[2];
const ORPHAN_ID = 'F-XXX-tool-id-totalement-inconnu-orphelin';

function buildResponse(toolIds: string[]): SimulatorsCatalogResponse {
  return {
    legalDomain: 'DROIT_IMMIGRATION',
    country: 'FRANCE',
    toolIds,
  };
}

interface ServiceStub {
  getCatalog: jest.Mock;
}

interface DialogStub {
  open: jest.Mock;
}

function setupComponent(
  catalogReturn: any,
  options: { dialogOpen?: jest.Mock; snackOpen?: jest.Mock } = {},
): {
  fixture: ComponentFixture<SimulatorsCatalogPageComponent>;
  component: SimulatorsCatalogPageComponent;
  service: ServiceStub;
  dialog: DialogStub;
  snack: { open: jest.Mock };
} {
  const service: ServiceStub = {
    getCatalog: jest.fn().mockReturnValue(catalogReturn),
  };
  const dialog: DialogStub = {
    open: options.dialogOpen ?? jest.fn().mockReturnValue({
      afterClosed: () => of(false),
    } as unknown as MatDialogRef<unknown>),
  };
  const snack = { open: options.snackOpen ?? jest.fn() };

  TestBed.configureTestingModule({
    imports: [SimulatorsCatalogPageComponent, NoopAnimationsModule],
    providers: [
      provideRouter([]),
      { provide: SimulatorsCatalogService, useValue: service },
      { provide: MatDialog, useValue: dialog },
      { provide: MatSnackBar, useValue: snack },
    ],
  });

  const fixture = TestBed.createComponent(SimulatorsCatalogPageComponent);
  const component = fixture.componentInstance;
  return { fixture, component, service, dialog, snack };
}

describe('SimulatorsCatalogPageComponent — affichage initial', () => {
  it('charge le catalogue et affiche la liste complète des outils', fakeAsync(() => {
    const { fixture, component } = setupComponent(
      of(buildResponse([ID_OQTF, ID_LICENCIEMENT, ID_RUPTURE])),
    );
    fixture.detectChanges();
    tick();

    expect(component.loading()).toBe(false);
    expect(component.error()).toBe(false);
    expect(component.availableTools().length).toBe(3);
    expect(component.filteredTools().length).toBe(3);
    expect(component.resultsCount()).toEqual({ matching: 3, total: 3 });
  }));

  it('expose le sous-titre dynamique avec domaine + pays', fakeAsync(() => {
    const { fixture, component } = setupComponent(
      of(buildResponse([ID_OQTF])),
    );
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.legalDomainLabel()).toBe('Droit de l’immigration');
    expect(component.countryLabel()).toBe('France');
    const text: string = fixture.nativeElement.textContent;
    expect(text).toContain('Droit de l’immigration');
    expect(text).toContain('France');
  }));
});

describe('SimulatorsCatalogPageComponent — recherche', () => {
  it('filtre la liste en case-insensitive sur le displayLabel', fakeAsync(() => {
    const { fixture, component } = setupComponent(
      of(buildResponse([ID_OQTF, ID_LICENCIEMENT, ID_RUPTURE])),
    );
    fixture.detectChanges();
    tick();

    component.onSearchChange('OQTF');
    fixture.detectChanges();
    expect(component.filteredTools().length).toBeGreaterThan(0);
    for (const t of component.filteredTools()) {
      expect(t.displayLabel.toLowerCase()).toContain('oqtf');
    }

    component.onSearchChange('oqtf');
    fixture.detectChanges();
    expect(component.filteredTools().length).toBeGreaterThan(0);
  }));

  it('filtre la liste insensible aux accents (validite → Validité)', fakeAsync(() => {
    const { fixture, component } = setupComponent(
      of(buildResponse([ID_OQTF, ID_LICENCIEMENT, ID_RUPTURE])),
    );
    fixture.detectChanges();
    tick();

    component.onSearchChange('validite');
    fixture.detectChanges();
    // 'F-DT-08-licenciement-validity' (Licenciement — validité) doit matcher.
    const labels = component.filteredTools().map((t) => t.displayLabel.toLowerCase());
    expect(labels.some((l) => l.includes('validité'))).toBe(true);
  }));

  it('met à jour le compteur N résultats sur M à chaque frappe', fakeAsync(() => {
    const { fixture, component } = setupComponent(
      of(buildResponse([ID_OQTF, ID_LICENCIEMENT, ID_RUPTURE])),
    );
    fixture.detectChanges();
    tick();

    expect(component.resultsCount()).toEqual({ matching: 3, total: 3 });

    component.onSearchChange('OQTF');
    expect(component.resultsCount().total).toBe(3);
    expect(component.resultsCount().matching).toBeGreaterThan(0);
    expect(component.resultsCount().matching).toBeLessThanOrEqual(3);

    component.onSearchChange('');
    expect(component.resultsCount()).toEqual({ matching: 3, total: 3 });
  }));

  it('affiche un état vide « Aucun simulateur ne correspond » quand 0 résultat', fakeAsync(() => {
    const { fixture, component } = setupComponent(
      of(buildResponse([ID_OQTF, ID_LICENCIEMENT])),
    );
    fixture.detectChanges();
    tick();

    component.onSearchChange('zzzz-pas-de-match-possible');
    fixture.detectChanges();
    expect(component.isSearchEmpty()).toBe(true);
    const text: string = fixture.nativeElement.textContent;
    expect(text).toContain('Aucun simulateur ne correspond à votre recherche');
  }));

  it('onResetSearch() vide la recherche', fakeAsync(() => {
    const { fixture, component } = setupComponent(
      of(buildResponse([ID_OQTF])),
    );
    fixture.detectChanges();
    tick();

    component.onSearchChange('abc');
    expect(component.searchQuery()).toBe('abc');
    component.onResetSearch();
    expect(component.searchQuery()).toBe('');
  }));
});

describe('SimulatorsCatalogPageComponent — orphelins', () => {
  it('skip un tool_id orphelin avec console.warn (forward-compat F-164)', fakeAsync(() => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    const { fixture, component } = setupComponent(
      of(buildResponse([ID_OQTF, ORPHAN_ID, ID_LICENCIEMENT])),
    );
    fixture.detectChanges();
    tick();

    expect(component.availableTools().length).toBe(2);
    expect(component.availableTools().map((t) => t.toolId)).not.toContain(ORPHAN_ID);
    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining(ORPHAN_ID),
    );
    warnSpy.mockRestore();
  }));
});

describe('SimulatorsCatalogPageComponent — états vides / erreur', () => {
  it('état vide quand workspace sans legalDomain (catalog.legalDomain = null)', fakeAsync(() => {
    const { fixture, component } = setupComponent(
      of({ legalDomain: null, country: null, toolIds: [] } satisfies SimulatorsCatalogResponse),
    );
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.isWorkspaceUnconfigured()).toBe(true);
    const text: string = fixture.nativeElement.textContent;
    expect(text).toContain('Précisez le domaine de votre workspace');
  }));

  it('état d’erreur affiche bannière + propose un Réessayer + snackbar', fakeAsync(() => {
    const snackOpen = jest.fn();
    const { fixture, component } = setupComponent(
      throwError(() => ({ status: 500 })),
      { snackOpen },
    );
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.error()).toBe(true);
    expect(snackOpen).toHaveBeenCalledWith(
      expect.stringContaining('Impossible de charger'),
      expect.any(String),
      expect.objectContaining({ panelClass: ['snack-error'] }),
    );
    const text: string = fixture.nativeElement.textContent;
    expect(text).toContain('Réessayer');
  }));
});

describe('SimulatorsCatalogPageComponent — interaction dialog', () => {
  it('onCardClick() ouvre MatDialog SimulatorInfoDialogComponent avec le label', fakeAsync(() => {
    const dialogOpen = jest.fn().mockReturnValue({
      afterClosed: () => of(false),
    } as unknown as MatDialogRef<unknown>);
    const { fixture, component } = setupComponent(
      of(buildResponse([ID_OQTF])),
      { dialogOpen },
    );
    fixture.detectChanges();
    tick();

    const tool = component.availableTools()[0];
    component.onCardClick(tool);

    expect(dialogOpen).toHaveBeenCalledTimes(1);
    const call = dialogOpen.mock.calls[0];
    expect(call[0]).toBe(SimulatorInfoDialogComponent);
    expect(call[1]).toEqual(
      expect.objectContaining({
        data: { displayLabel: tool.displayLabel },
      }),
    );
  }));
});

describe('SimulatorsCatalogPageComponent — navigation runner (F-163 SF-163-02a)', () => {
  it('onCardClick() navigue vers /simulators/:toolId si toolId whitelisted (CA-06 + CA-12 d)', fakeAsync(() => {
    // F-DT-08 doit être dans la whitelist standalone.
    expect(STANDALONE_READY_TOOL_IDS.has(ID_LICENCIEMENT)).toBe(true);

    const dialogOpen = jest.fn();
    const { fixture, component } = setupComponent(
      of(buildResponse([ID_LICENCIEMENT])),
      { dialogOpen },
    );
    fixture.detectChanges();
    tick();

    const router = TestBed.inject(Router);
    const navigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);

    const tool = component.availableTools().find((t) => t.toolId === ID_LICENCIEMENT)!;
    expect(tool).toBeDefined();

    component.onCardClick(tool);

    // Navigation déclenchée, pas de dialog.
    expect(navigateSpy).toHaveBeenCalledWith(['/simulators', ID_LICENCIEMENT]);
    expect(dialogOpen).not.toHaveBeenCalled();
  }));

  it('onCardClick() ouvre le dialog si toolId NON whitelisted (rétrocompat)', fakeAsync(() => {
    // ID_OQTF n'est PAS encore dans la whitelist standalone.
    expect(STANDALONE_READY_TOOL_IDS.has(ID_OQTF)).toBe(false);

    const dialogOpen = jest.fn().mockReturnValue({
      afterClosed: () => of(false),
    } as unknown as MatDialogRef<unknown>);
    const { fixture, component } = setupComponent(
      of(buildResponse([ID_OQTF])),
      { dialogOpen },
    );
    fixture.detectChanges();
    tick();

    const router = TestBed.inject(Router);
    const navigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);

    const tool = component.availableTools()[0];
    component.onCardClick(tool);

    expect(navigateSpy).not.toHaveBeenCalled();
    expect(dialogOpen).toHaveBeenCalledTimes(1);
  }));
});

describe('SimulatorsCatalogPageComponent — normalisation', () => {
  it('normalizeForTest enlève les diacritiques et lowercase', () => {
    expect(SimulatorsCatalogPageComponent.normalizeForTest('Validité')).toBe('validite');
    expect(SimulatorsCatalogPageComponent.normalizeForTest('Désunion')).toBe('desunion');
    expect(SimulatorsCatalogPageComponent.normalizeForTest('OQTF')).toBe('oqtf');
  });
});
