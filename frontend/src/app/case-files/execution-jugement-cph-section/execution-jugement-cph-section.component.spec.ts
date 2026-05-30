import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { ExecutionJugementCphSectionComponent } from './execution-jugement-cph-section.component';
import { ExecutionJugementCphResponse } from '../../core/models/execution-jugement-cph.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('ExecutionJugementCphSectionComponent', () => {
  let component: ExecutionJugementCphSectionComponent;
  let fixture: ComponentFixture<ExecutionJugementCphSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/execution-jugement-cph-analysis';

  function frResponse(overrides: Partial<ExecutionJugementCphResponse> = {}): ExecutionJugementCphResponse {
    return {
      caseFileId: 'case-1',
      dateJugement: '2026-01-15',
      montantCondamnation: 12000,
      executionProvisoireOrdonnee: true,
      situationEmployeur: 'IN_BONIS',
      dateOuvertureProcedureCollective: null,
      creancesSuperPrivilegiees: null,
      verdict: 'EXECUTION_DIRECTE',
      agsEligible: false,
      agsInfo: null,
      checklist: [
        { libelle: 'Signification du jugement', obligatoire: true, bloquant: false, baseJuridique: 'art. 503 CPC' },
        { libelle: 'Commandement de payer', obligatoire: true, bloquant: false, baseJuridique: 'art. L. 111-2 CPCE' },
      ],
      country: 'FRANCE',
      baseJuridique: 'art. 514 CPC ; R. 1454-28 CPC ; L. 3253-6 et s.',
      ...overrides,
    };
  }

  function agsResponse(overrides: Partial<ExecutionJugementCphResponse> = {}): ExecutionJugementCphResponse {
    return frResponse({
      situationEmployeur: 'LIQUIDATION',
      dateOuvertureProcedureCollective: '2026-01-05',
      verdict: 'RELAIS_AGS',
      agsEligible: true,
      agsInfo: {
        plafondMensuelSs: 3925,
        plafondGarantie: 94200,
        coefficientPlafond: 6,
        relaisAgsRecommande: true,
        demarches: [
          { libelle: 'Déclaration de créance au mandataire judiciaire', obligatoire: true, bloquant: false, baseJuridique: 'L. 622-24 C. com.' },
          { libelle: 'Saisine du CGEA', obligatoire: true, bloquant: false, baseJuridique: 'L. 3253-14 C. trav.' },
        ],
      },
      ...overrides,
    });
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.dateJugement.set('2026-01-15');
    component.montantCondamnation.set(12000);
    component.executionProvisoireOrdonnee.set(true);
    component.situationEmployeur.set('IN_BONIS');
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [ExecutionJugementCphSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ExecutionJugementCphSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // --- statics / contract ---

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(ExecutionJugementCphSectionComponent.TOOL_LABEL).toContain('EXÉCUTION JUGEMENT CPH');
    expect(ExecutionJugementCphSectionComponent.TOOL_ICON).toBe('gavel');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(ExecutionJugementCphSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 (partiel) with only montant', () => {
    expect(ExecutionJugementCphSectionComponent.getPrefillCount({
      aiData: { montantCondamnationCph: 8000 },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 2 (nominal) with montant + situation', () => {
    expect(ExecutionJugementCphSectionComponent.getPrefillCount({
      aiData: { montantCondamnationCph: 8000, situationEmployeurDetectee: 'REDRESSEMENT' },
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(ExecutionJugementCphSectionComponent.getPrefillCount({
      aiData: { montantCondamnationCph: 8000, situationEmployeurDetectee: 'LIQUIDATION' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  // --- gate pays / lifecycle ---

  it('FRANCE -> GET called on ngOnInit', () => {
    expect(component.isFrance()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  });

  it('BELGIQUE -> no HTTP on ngOnInit', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="country-gate-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('française uniquement');
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(agsResponse());
    expect(component.result()!.verdict).toBe('RELAIS_AGS');
    expect(component.showForm()).toBe(false);
    expect(component.dateJugement()).toBe('2026-01-15');
    expect(component.situationEmployeur()).toBe('LIQUIDATION');
    expect(component.dateOuvertureProcedureCollective()).toBe('2026-01-05');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // --- form validation ---

  it('formValid requires date + positive montant', () => {
    component.dateJugement.set(null);
    component.montantCondamnation.set(12000);
    expect(component.formValid()).toBe(false);
    component.dateJugement.set('2026-01-15');
    component.montantCondamnation.set(0);
    expect(component.formValid()).toBe(false);
    component.montantCondamnation.set(12000);
    expect(component.formValid()).toBe(true);
  });

  it('formValid requires dateOuverture when procedure collective', () => {
    component.dateJugement.set('2026-01-15');
    component.montantCondamnation.set(12000);
    component.situationEmployeur.set('REDRESSEMENT');
    expect(component.procedureCollective()).toBe(true);
    expect(component.formValid()).toBe(false);
    component.dateOuvertureProcedureCollective.set('2026-01-01');
    expect(component.formValid()).toBe(true);
  });

  it('switching back to IN_BONIS clears dateOuverture', () => {
    component.situationEmployeur.set('LIQUIDATION');
    component.dateOuvertureProcedureCollective.set('2026-01-01');
    component.onSituationEmployeurChange('IN_BONIS');
    expect(component.dateOuvertureProcedureCollective()).toBeNull();
  });

  // --- analyze ---

  it('analyze() POST nominal -> result + snack + exact body shape (IN_BONIS, no date ouverture)', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateJugement: '2026-01-15',
      montantCondamnation: 12000,
      executionProvisoireOrdonnee: true,
      situationEmployeur: 'IN_BONIS',
      dateOuvertureProcedureCollective: null,
      creancesSuperPrivilegiees: null,
    });
    req.flush(frResponse());
    expect(component.result()!.verdict).toBe('EXECUTION_DIRECTE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() sends dateOuverture when procedure collective', () => {
    component.ngOnInit();
    flush404();
    component.dateJugement.set('2026-01-15');
    component.montantCondamnation.set(12000);
    component.situationEmployeur.set('LIQUIDATION');
    component.dateOuvertureProcedureCollective.set('2026-01-05');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.situationEmployeur).toBe('LIQUIDATION');
    expect(req.request.body.dateOuvertureProcedureCollective).toBe('2026-01-05');
    req.flush(agsResponse());
  });

  it('analyze() does nothing when form invalid', () => {
    component.ngOnInit();
    flush404();
    component.dateJugement.set(null);
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  // --- chip verdicts ---

  it('verdict EXECUTION_DIRECTE -> success chip, no AGS block', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ verdict: 'EXECUTION_DIRECTE' }));
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.ejc-chip--success');
    expect(chip).not.toBeNull();
    const ags = fixture.nativeElement.querySelector('[data-testid="ags-block"]');
    expect(ags).toBeNull();
  });

  it('verdict RELAIS_AGS -> info chip + AGS block with plafonds + bareme note', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    component.dateJugement.set('2026-01-15');
    component.montantCondamnation.set(12000);
    component.situationEmployeur.set('LIQUIDATION');
    component.dateOuvertureProcedureCollective.set('2026-01-05');
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(agsResponse());
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.ejc-chip--info');
    expect(chip).not.toBeNull();
    const ags = fixture.nativeElement.querySelector('[data-testid="ags-block"]');
    expect(ags).not.toBeNull();
    const plafond = fixture.nativeElement.querySelector('[data-testid="ags-plafond-garantie"]');
    expect(plafond.textContent).toContain('94200');
    const note = fixture.nativeElement.querySelector('[data-testid="ags-bareme-note"]');
    expect(note).not.toBeNull();
    expect(note.textContent).toContain('actualiser annuellement');
    const demarches = fixture.nativeElement.querySelectorAll('[data-testid="ags-demarches"] .ejc-checklist-item');
    expect(demarches.length).toBe(2);
  });

  it('verdict BLOQUE_INFO_MANQUANTE -> warning chip', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    component.dateJugement.set('2026-01-15');
    component.montantCondamnation.set(12000);
    component.situationEmployeur.set('REDRESSEMENT');
    component.dateOuvertureProcedureCollective.set('2026-01-01');
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(
      agsResponse({ verdict: 'BLOQUE_INFO_MANQUANTE', agsEligible: false, agsInfo: null }),
    );
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.ejc-chip--warning');
    expect(chip).not.toBeNull();
  });

  // --- checklist ---

  it('renders the execution checklist items', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse());
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('[data-testid="checklist-block"]');
    expect(block).not.toBeNull();
    const items = block.querySelectorAll('.ejc-checklist-item');
    expect(items.length).toBe(2);
    expect(items[0].textContent).toContain('Signification du jugement');
    expect(items[0].textContent).toContain('art. 503 CPC');
  });

  it('renders a bloquant checklist item with the bloquant class', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({
      verdict: 'BLOQUE_INFO_MANQUANTE',
      checklist: [
        { libelle: "Date d'ouverture de la procédure collective manquante", obligatoire: true, bloquant: true, baseJuridique: 'L. 3253-8 C. trav.' },
      ],
    }));
    fixture.detectChanges();
    const bloquant = fixture.nativeElement.querySelector('.ejc-checklist-item--bloquant');
    expect(bloquant).not.toBeNull();
    expect(bloquant.textContent).toContain("Date d'ouverture");
  });

  // --- prefill IA ---

  it('aiData -> pre-fills montant + situation with provenance IA', () => {
    component.aiData = { montantCondamnationCph: 9500, situationEmployeurDetectee: 'LIQUIDATION' } as TravailExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.montantCondamnation()).toBe(9500);
    expect(component.provenanceMontant()).toBe('IA');
    expect(component.situationEmployeur()).toBe('LIQUIDATION');
    expect(component.provenanceSituationEmployeur()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { montantCondamnationCph: 99999, situationEmployeurDetectee: 'REDRESSEMENT' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ montantCondamnation: 12000, situationEmployeur: 'IN_BONIS' }));
    expect(component.montantCondamnation()).toBe(12000);
    expect(component.provenanceMontant()).toBeNull();
    expect(component.situationEmployeur()).toBe('IN_BONIS');
  });

  it('onMontantChange clears montant provenance', () => {
    component.aiData = { montantCondamnationCph: 9500 } as TravailExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceMontant()).toBe('IA');
    component.onMontantChange(8000);
    expect(component.provenanceMontant()).toBeNull();
  });

  it('ngOnChanges with new aiData in form mode -> re-prefill', () => {
    component.ngOnInit();
    flush404();
    expect(component.montantCondamnation()).toBeNull();
    component.aiData = { montantCondamnationCph: 7000, situationEmployeurDetectee: 'REDRESSEMENT' } as TravailExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.montantCondamnation()).toBe(7000);
    expect(component.situationEmployeur()).toBe('REDRESSEMENT');
  });

  // --- F-IA-03 coherence ---

  it('coherenceAlerts flags procedure collective without date ouverture', () => {
    component.situationEmployeur.set('LIQUIDATION');
    component.dateOuvertureProcedureCollective.set(null);
    expect(component.coherenceAlerts().some((a) => a.includes("date d'ouverture"))).toBe(true);
  });

  it('coherenceAlerts flags divergence between AI situation and saisie', () => {
    component.aiData = { situationEmployeurDetectee: 'LIQUIDATION' } as TravailExtractedData;
    component.situationEmployeur.set('IN_BONIS');
    expect(component.coherenceAlerts().some((a) => a.includes('Liquidation'))).toBe(true);
  });

  // --- helpers / labels ---

  it('verdictLabel / bannerClass / bannerIcon cover all 3 verdicts', () => {
    expect(component.verdictLabel('EXECUTION_DIRECTE')).toContain('Exécution directe');
    expect(component.verdictLabel('RELAIS_AGS')).toContain('AGS');
    expect(component.verdictLabel('BLOQUE_INFO_MANQUANTE')).toContain('manquante');
    expect(component.bannerClass('EXECUTION_DIRECTE')).toContain('ejc-banner--success');
    expect(component.bannerClass('RELAIS_AGS')).toContain('ejc-banner--info');
    expect(component.bannerClass('BLOQUE_INFO_MANQUANTE')).toContain('ejc-banner--warning');
    expect(component.bannerIcon('EXECUTION_DIRECTE')).toBe('check_circle');
    expect(component.bannerIcon('RELAIS_AGS')).toBe('account_balance');
    expect(component.bannerIcon('BLOQUE_INFO_MANQUANTE')).toBe('help_outline');
  });

  it('situationEmployeurLabel covers all values', () => {
    expect(component.situationEmployeurLabel('IN_BONIS')).toContain('In bonis');
    expect(component.situationEmployeurLabel('REDRESSEMENT')).toContain('Redressement');
    expect(component.situationEmployeurLabel('LIQUIDATION')).toContain('Liquidation');
  });

  it('showAgsBlock true only when agsEligible + agsInfo present', () => {
    expect(component.showAgsBlock(agsResponse())).toBe(true);
    expect(component.showAgsBlock(frResponse())).toBe(false);
    expect(component.showAgsBlock(agsResponse({ agsEligible: false }))).toBe(false);
    expect(component.showAgsBlock(null)).toBe(false);
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

  it('standaloneMode -> no GET, form visible, banner displayed', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Mode simulateur');
  });
});
