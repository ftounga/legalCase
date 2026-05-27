import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { NaturalisationConjointBelgeBeSectionComponent } from './naturalisation-conjoint-belge-be-section.component';
import { NaturalisationConjointBelgeBeResponse } from '../../core/models/naturalisation-conjoint-belge-be.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('NaturalisationConjointBelgeBeSectionComponent', () => {
  let component: NaturalisationConjointBelgeBeSectionComponent;
  let fixture: ComponentFixture<NaturalisationConjointBelgeBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/naturalisation-conjoint-belge-be-analysis';

  function beResponse(
    overrides: Partial<NaturalisationConjointBelgeBeResponse> = {},
  ): NaturalisationConjointBelgeBeResponse {
    return {
      caseFileId: 'case-1',
      dateMarriage: '2020-05-15',
      cohabitationLegale: false,
      dureeCohabitationMois: 72,
      niveauLangue: 'A2',
      preuveIntegration: true,
      menaceOrdrePublic: false,
      condamnationPenale: false,
      eligible: true,
      dureeManquante: 0,
      criteresNonRemplis: [],
      delaiDeclaration:
        "Déclaration devant officier d'état civil de la commune de résidence — délai d'instruction : 3-6 mois",
      baseJuridique:
        'Code de la nationalité belge art. 16 ; Loi 28/06/1984 mod. Loi 04/12/2012',
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [NaturalisationConjointBelgeBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(NaturalisationConjointBelgeBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match((r) => r.url.includes('/jurisprudence-citations')).forEach((r) => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(NaturalisationConjointBelgeBeSectionComponent.TOOL_LABEL).toContain('CONJOINT BELGE');
    expect(NaturalisationConjointBelgeBeSectionComponent.TOOL_ICON).toBe('family_restroom');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(NaturalisationConjointBelgeBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 3 when all 3 REAL IA signals present (BELGIQUE) — MAX', () => {
    expect(NaturalisationConjointBelgeBeSectionComponent.getPrefillCount({
      aiData: {
        naturalisationBeArt16DateMarriage: '2020-05-15',
        naturalisationBeArt16DureeCohabitation: 72,
        naturalisationBeArt16NiveauLangue: 'A2',
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(3);
  });

  it('static getPrefillCount returns 1 on partial pré-fill (only dureeCohabitation)', () => {
    expect(NaturalisationConjointBelgeBeSectionComponent.getPrefillCount({
      aiData: { naturalisationBeArt16DureeCohabitation: 36 },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=FRANCE', () => {
    expect(NaturalisationConjointBelgeBeSectionComponent.getPrefillCount({
      aiData: { naturalisationBeArt16DureeCohabitation: 72 },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  it('BELGIQUE -> GET called on ngOnInit', () => {
    expect(component.isBelgique()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  });

  it('FRANCE -> no HTTP on ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse());
    expect(component.result()!.eligible).toBe(true);
    expect(component.showForm()).toBe(false);
    expect(component.dateMarriage()).toBe('2020-05-15');
    expect(component.dureeCohabitationMois()).toBe(72);
    expect(component.niveauLangue()).toBe('A2');
    expect(component.preuveIntegration()).toBe(true);
    expect(component.menaceOrdrePublic()).toBe(false);
    expect(component.condamnationPenale()).toBe(false);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false if any required field is missing', () => {
    expect(component.formValid()).toBe(false);
    component.dateMarriage.set('2020-05-15');
    expect(component.formValid()).toBe(false);
    component.dureeCohabitationMois.set(72);
    expect(component.formValid()).toBe(false);
    component.niveauLangue.set('A2');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false if dureeCohabitation out of bounds', () => {
    component.dateMarriage.set('2020-05-15');
    component.niveauLangue.set('A2');
    component.dureeCohabitationMois.set(-1);
    expect(component.formValid()).toBe(false);
    component.dureeCohabitationMois.set(601);
    expect(component.formValid()).toBe(false);
    component.dureeCohabitationMois.set(60);
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack + body has 7 fields', () => {
    component.ngOnInit();
    flush404();
    component.dateMarriage.set('2020-05-15');
    component.cohabitationLegale.set(false);
    component.dureeCohabitationMois.set(72);
    component.niveauLangue.set('A2');
    component.preuveIntegration.set(true);
    component.menaceOrdrePublic.set(false);
    component.condamnationPenale.set(false);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateMarriage: '2020-05-15',
      cohabitationLegale: false,
      dureeCohabitationMois: 72,
      niveauLangue: 'A2',
      preuveIntegration: true,
      menaceOrdrePublic: false,
      condamnationPenale: false,
    });
    req.flush(beResponse());
    expect(component.result()!.eligible).toBe(true);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.dateMarriage.set('2020-05-15');
    component.dureeCohabitationMois.set(24);
    component.niveauLangue.set('INFERIEUR_A2');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  it('aiData with 3 REAL IA signals -> pre-fills + provenance IA (4 checkboxes NOT pre-filled)', () => {
    component.aiData = {
      naturalisationBeArt16DateMarriage: '2020-05-15',
      naturalisationBeArt16DureeCohabitation: 84,
      naturalisationBeArt16NiveauLangue: 'SUPERIEUR_A2',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateMarriage()).toBe('2020-05-15');
    expect(component.provenanceDateMarriage()).toBe('IA');
    expect(component.dureeCohabitationMois()).toBe(84);
    expect(component.provenanceDureeCohabitation()).toBe('IA');
    expect(component.niveauLangue()).toBe('SUPERIEUR_A2');
    expect(component.provenanceNiveauLangue()).toBe('IA');
    // PREFILL_COUNT_ALWAYS_ZERO — 4 checkboxes restent false par défaut sans provenance signal.
    expect(component.cohabitationLegale()).toBe(false);
    expect(component.preuveIntegration()).toBe(false);
    expect(component.menaceOrdrePublic()).toBe(false);
    expect(component.condamnationPenale()).toBe(false);
  });

  it('GET 200 -> no pre-fill (server values win)', () => {
    component.aiData = {
      naturalisationBeArt16DureeCohabitation: 36,
      naturalisationBeArt16DateMarriage: '2010-01-01',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({ dureeCohabitationMois: 120, dateMarriage: '2015-06-20' }));
    expect(component.dureeCohabitationMois()).toBe(120);
    expect(component.dateMarriage()).toBe('2015-06-20');
    expect(component.provenanceDateMarriage()).toBeNull();
    expect(component.provenanceDureeCohabitation()).toBeNull();
  });

  it('onDateMarriageChange clears provenance', () => {
    component.aiData = {
      naturalisationBeArt16DateMarriage: '2020-05-15',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateMarriage()).toBe('IA');
    component.onDateMarriageChange('2021-01-01');
    expect(component.provenanceDateMarriage()).toBeNull();
  });

  it('onDureeCohabitationChange clears provenance', () => {
    component.aiData = {
      naturalisationBeArt16DureeCohabitation: 60,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDureeCohabitation()).toBe('IA');
    component.onDureeCohabitationChange(84);
    expect(component.provenanceDureeCohabitation()).toBeNull();
  });

  it('onNiveauLangueChange clears provenance', () => {
    component.aiData = {
      naturalisationBeArt16NiveauLangue: 'A2',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceNiveauLangue()).toBe('IA');
    component.onNiveauLangueChange('SUPERIEUR_A2');
    expect(component.provenanceNiveauLangue()).toBeNull();
  });

  it('eligibleBannerClass covers ELIGIBLE (green) and INELIGIBLE (red)', () => {
    expect(component.eligibleBannerClass(true)).toContain('natcjbe-banner--success');
    expect(component.eligibleBannerClass(false)).toContain('natcjbe-banner--danger');
  });

  it('eligibleChipClass covers 2 verdicts', () => {
    expect(component.eligibleChipClass(true)).toContain('natcjbe-chip--success');
    expect(component.eligibleChipClass(false)).toContain('natcjbe-chip--danger');
  });

  it('eligibleIcon covers 2 verdicts', () => {
    expect(component.eligibleIcon(true)).toBe('check_circle');
    expect(component.eligibleIcon(false)).toBe('block');
  });

  it('eligibleLabel covers 2 verdicts', () => {
    expect(component.eligibleLabel(true)).toBe('ELIGIBLE');
    expect(component.eligibleLabel(false)).toBe('INELIGIBLE');
  });

  it('niveauLangueLabel resolves the 3 codes', () => {
    expect(component.niveauLangueLabel('INFERIEUR_A2')).toContain('Inférieur');
    expect(component.niveauLangueLabel('A2')).toContain('A2');
    expect(component.niveauLangueLabel('SUPERIEUR_A2')).toContain('Supérieur');
    expect(component.niveauLangueLabel(null)).toBe('');
  });

  it('toggleCollapse inverts collapsed state', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
  });

  it('editMode resets showForm to true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('ngOnChanges with new aiData in form mode -> re-prefill', () => {
    component.ngOnInit();
    flush404();
    expect(component.dureeCohabitationMois()).toBeNull();
    component.aiData = {
      naturalisationBeArt16DateMarriage: '2018-09-12',
      naturalisationBeArt16DureeCohabitation: 96,
      naturalisationBeArt16NiveauLangue: 'A2',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dureeCohabitationMois()).toBe(96);
    expect(component.provenanceDureeCohabitation()).toBe('IA');
    expect(component.dateMarriage()).toBe('2018-09-12');
    expect(component.provenanceDateMarriage()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result already loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({ dureeCohabitationMois: 120 }));
    component.aiData = {
      naturalisationBeArt16DureeCohabitation: 9999,
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dureeCohabitationMois()).toBe(120);
    expect(component.provenanceDureeCohabitation()).toBeNull();
  });

  it('FRANCE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.natcjbe-banner--info');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Belgique uniquement');
  });

  it('standaloneMode -> no GET, form visible, banner displayed', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Mode simulateur');
  });

  it('renders verdict chip in result header (INELIGIBLE = red)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({
      eligible: false, dureeManquante: 24,
    }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.natcjbe-chip');
    expect(chip).not.toBeNull();
    expect(chip.classList.contains('natcjbe-chip--danger')).toBe(true);
    expect(chip.textContent).toContain('INELIGIBLE');
  });

  it('renders verdict chip in result header (ELIGIBLE = green)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ eligible: true }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.natcjbe-chip');
    expect(chip).not.toBeNull();
    expect(chip.classList.contains('natcjbe-chip--success')).toBe(true);
    expect(chip.textContent).toContain('ELIGIBLE');
  });

  it('renders dureeManquante block when > 0', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({
      eligible: false, dureeManquante: 18,
    }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('[data-testid="duree-manquante-block"]');
    expect(block).not.toBeNull();
    expect(block.textContent).toContain('18');
    expect(block.textContent).toContain('mois à attendre');
  });

  it('does NOT render dureeManquante block when = 0', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ dureeManquante: 0 }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('[data-testid="duree-manquante-block"]');
    expect(block).toBeNull();
  });

  it('renders criteresNonRemplis list when result has missing criteria', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({
      eligible: false,
      criteresNonRemplis: ['Cohabitation insuffisante (< 5 ans)', 'Langue < A2', 'Pas de preuve intégration'],
    }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const items = fixture.nativeElement.querySelectorAll('.natcjbe-criteres-list mat-list-item');
    expect(items.length).toBe(3);
  });

  it('renders delaiDeclaration (Officier état civil) in result', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse());
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const proc = fixture.nativeElement.querySelector('[data-testid="delai-declaration-block"]');
    expect(proc).not.toBeNull();
    expect(proc.textContent).toContain("officier d'état civil");
  });

  it('renders form fields without binding errors (regression #1385/#1386 — no orphan [coherenceAlert])', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
    fixture.detectChanges();
    // Form must render — date/duree/niveauLangue inputs visible.
    expect(fixture.nativeElement.querySelector('[data-testid="date-marriage-input"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="duree-cohabitation-input"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="niveau-langue-select"]')).not.toBeNull();
    // 4 aspirational checkboxes also rendered.
    expect(fixture.nativeElement.querySelector('[data-testid="cohabitation-legale-checkbox"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="preuve-integration-checkbox"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="menace-ordre-public-checkbox"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="condamnation-penale-checkbox"]')).not.toBeNull();
  });
});
