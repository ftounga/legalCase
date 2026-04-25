import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { RevisionsPostDivorceSectionComponent } from './revisions-post-divorce-section.component';
import { RevisionsPostDivorceResponse } from '../../core/models/revisions-post-divorce.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ProcedureCheck } from '../../core/models/procedure-check.model';

describe('RevisionsPostDivorceSectionComponent', () => {
  let component: RevisionsPostDivorceSectionComponent;
  let fixture: ComponentFixture<RevisionsPostDivorceSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/revisions-post-divorce';

  function frResponse(): RevisionsPostDivorceResponse {
    return {
      caseFileId: 'case-1',
      typeRevision: 'PENSION_ALIMENTAIRE',
      dateDecisionInitiale: '2024-01-15',
      changementCirconstance: 'Perte d\'emploi du débiteur depuis trois mois suite à licenciement économique',
      revenusInitialsDebiteurEur: 50000,
      revenusActuelsDebiteurEur: 30000,
      revenusInitialsCreancierEur: 25000,
      revenusActuelsCreancierEur: 28000,
      nbEnfantsACharge: 2,
      ageEnfants: [8, 12],
      modeResidenceActuel: 'EXCLUSIVE_MERE',
      modeResidenceDemande: 'EXCLUSIVE_MERE',
      ecartRevenusPct: -40.0,
      modificationSubstantielle: true,
      motivationSuffisante: true,
      scoreGlobal: 85,
      verdictRevisionPossible: 'ELEVEE',
      baseJuridique: 'Art. 373-2-13 Cciv',
      formule: 'score = 85/100 (modification substantielle + motivation suffisante)',
      messages: ['Probabilité élevée d\'obtenir une révision'],
      country: 'FRANCE',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [
        RevisionsPostDivorceSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RevisionsPostDivorceSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Mount + base
  // ---------------------------------------------------------------------------

  it('mounts with collapsed=true initial', () => {
    expect(component).toBeTruthy();
    expect(component.collapsed()).toBe(true);
  });

  it('toggleCollapse switches collapsed signal', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('editMode re-shows the form', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Conditional fields visibility (typeRevision)
  // ---------------------------------------------------------------------------

  it('typeRevision PENSION_ALIMENTAIRE → showRevenusFields=true, showResidenceFields=false', () => {
    component.typeRevision.set('PENSION_ALIMENTAIRE');
    expect(component.showRevenusFields()).toBe(true);
    expect(component.showResidenceFields()).toBe(false);
    expect(component.showDemenagementHint()).toBe(false);
  });

  it('typeRevision PRESTATION_COMPENSATOIRE → showRevenusFields=true', () => {
    component.typeRevision.set('PRESTATION_COMPENSATOIRE');
    expect(component.showRevenusFields()).toBe(true);
    expect(component.showResidenceFields()).toBe(false);
  });

  it('typeRevision RESIDENCE → showResidenceFields=true, showRevenusFields=false', () => {
    component.typeRevision.set('RESIDENCE');
    expect(component.showRevenusFields()).toBe(false);
    expect(component.showResidenceFields()).toBe(true);
  });

  it('typeRevision DROIT_VISITE → showResidenceFields=true', () => {
    component.typeRevision.set('DROIT_VISITE');
    expect(component.showResidenceFields()).toBe(true);
    expect(component.showRevenusFields()).toBe(false);
  });

  it('typeRevision DEMENAGEMENT_PARENT → showDemenagementHint=true', () => {
    component.typeRevision.set('DEMENAGEMENT_PARENT');
    expect(component.showDemenagementHint()).toBe(true);
    expect(component.showRevenusFields()).toBe(false);
    expect(component.showResidenceFields()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Form validation
  // ---------------------------------------------------------------------------

  it('formValid: false when type / date / changement missing', () => {
    expect(component.formValid()).toBe(false);

    component.typeRevision.set('PENSION_ALIMENTAIRE');
    expect(component.formValid()).toBe(false); // date manquante

    component.dateDecisionInitiale.set('2024-01-15');
    expect(component.formValid()).toBe(false); // changement < 20 chars

    component.changementCirconstance.set('Trop court');
    expect(component.formValid()).toBe(false);

    component.changementCirconstance.set(
      'Perte d\'emploi suite licenciement économique en mars 2026'); // > 20
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Load (GET)
  // ---------------------------------------------------------------------------

  it('GET 200 → form hidden + result populated', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(frResponse());

    expect(component.result()!.scoreGlobal).toBe(85);
    expect(component.showForm()).toBe(false);
    expect(component.typeRevision()).toBe('PENSION_ALIMENTAIRE');
    expect(component.dateDecisionInitiale()).toBe('2024-01-15');
    expect(component.nbEnfantsACharge()).toBe(2);
    expect(component.ageEnfants()).toEqual([8, 12]);
  });

  it('GET 404 → form remains visible', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // POST (calculate)
  // ---------------------------------------------------------------------------

  it('calculate() POST → result + success snackbar + refresh', () => {
    component.typeRevision.set('PENSION_ALIMENTAIRE');
    component.dateDecisionInitiale.set('2024-01-15');
    component.changementCirconstance.set(
      'Perte d\'emploi du débiteur, baisse de 40 % des revenus'); // 53 chars
    component.revenusActuelsDebiteurEur.set(30000);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.typeRevision).toBe('PENSION_ALIMENTAIRE');
    expect(req.request.body.dateDecisionInitiale).toBe('2024-01-15');
    expect(req.request.body.changementCirconstance.length).toBeGreaterThanOrEqual(20);
    req.flush(frResponse());

    expect(component.result()!.verdictRevisionPossible).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse révision post-divorce calculée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() error → red snackbar', () => {
    component.typeRevision.set('PENSION_ALIMENTAIRE');
    component.dateDecisionInitiale.set('2024-01-15');
    component.changementCirconstance.set(
      'Perte d\'emploi du débiteur, baisse de 40 % des revenus');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
  });

  it('calculate() ignored if form invalid → no HTTP call', () => {
    component.typeRevision.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA
  // ---------------------------------------------------------------------------

  it('pré-fill IA on GET 404 → revenus + IA badges', () => {
    const ai: FamilleExtractedData = {
      revenusAnnuelsEpoux1Eur: 48000,
      revenusAnnuelsEpoux2Eur: 28000,
    };
    component.aiData = ai;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.revenusActuelsDebiteurEur()).toBe(48000);
    expect(component.revenusActuelsCreancierEur()).toBe(28000);
    expect(component.provenanceRevenusActuelsDebiteur()).toBe('IA');
    expect(component.provenanceRevenusActuelsCreancier()).toBe('IA');
  });

  it('pré-fill IA without aiData → no values, no badges', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.revenusActuelsDebiteurEur()).toBeNull();
    expect(component.revenusActuelsCreancierEur()).toBeNull();
    expect(component.provenanceRevenusActuelsDebiteur()).toBeNull();
  });

  it('manual change clears IA badge', () => {
    const ai: FamilleExtractedData = { revenusAnnuelsEpoux1Eur: 50000 };
    component.aiData = ai;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceRevenusActuelsDebiteur()).toBe('IA');
    component.onRevenusActuelsDebiteurChange(60000);
    expect(component.revenusActuelsDebiteurEur()).toBe(60000);
    expect(component.provenanceRevenusActuelsDebiteur()).toBeNull();
  });

  it('GET 200 → no IA badges (persisted values prevail)', () => {
    const ai: FamilleExtractedData = { revenusAnnuelsEpoux1Eur: 99999 };
    component.aiData = ai;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());

    expect(component.revenusActuelsDebiteurEur()).toBe(30000);
    expect(component.provenanceRevenusActuelsDebiteur()).toBeNull();
  });

  it('ngOnChanges(aiData) post-mount refreshes pré-fill if form empty', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi: FamilleExtractedData = { revenusAnnuelsEpoux1Eur: 35000 };
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.revenusActuelsDebiteurEur()).toBe(35000);
    expect(component.provenanceRevenusActuelsDebiteur()).toBe('IA');
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03
  // ---------------------------------------------------------------------------

  it('coherence alert REVENUS_DEBITEUR_ACTUEL if divergence > 10 % vs IA', () => {
    component.typeRevision.set('PENSION_ALIMENTAIRE');
    component.aiData = { revenusAnnuelsEpoux1Eur: 50000 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // IA = 50000 ; user types 70000 → 40 % gap
    component.onRevenusActuelsDebiteurChange(70000);

    const alerts = component.coherenceAlerts();
    expect(alerts.REVENUS_DEBITEUR_ACTUEL).toBeDefined();
    expect(alerts.REVENUS_DEBITEUR_ACTUEL!.field).toBe('REVENUS_DEBITEUR_ACTUEL');
    expect(alerts.REVENUS_DEBITEUR_ACTUEL!.source).toBe('IA');
  });

  it('no coherence alert if revenue gap ≤ 10 %', () => {
    component.typeRevision.set('PENSION_ALIMENTAIRE');
    component.aiData = { revenusAnnuelsEpoux1Eur: 50000 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onRevenusActuelsDebiteurChange(52000); // 4 % gap

    expect(component.coherenceAlerts().REVENUS_DEBITEUR_ACTUEL).toBeUndefined();
  });

  it('alerts hidden once result rendered (showForm=false)', () => {
    component.typeRevision.set('PENSION_ALIMENTAIRE');
    component.aiData = { revenusAnnuelsEpoux1Eur: 50000 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onRevenusActuelsDebiteurChange(70000);
    expect(component.coherenceAlerts().REVENUS_DEBITEUR_ACTUEL).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().REVENUS_DEBITEUR_ACTUEL).toBeUndefined();
  });

  it('coherence alert NB_ENFANTS multi-source (F96 + IA)', () => {
    component.typeRevision.set('RESIDENCE');
    component.aiData = { revenusAnnuelsEpoux1Eur: 0 } as Partial<FamilleExtractedData>;
    // Force IA divergente via cast :
    (component.aiData as { nbEnfantsACharge?: number }).nbEnfantsACharge = 3;
    component.procedureChecks = [{
      id: 'chk-1', ordre: 1, description: 'Nb enfants attendus',
      statut: 'NON_COMPLIANT',
      critereCode: 'FA13_NB_ENFANTS',
      expectedValue: '3',
    } as ProcedureCheck];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onNbEnfantsChange(5); // user dit 5 ; F96 + IA disent 3
    const alert = component.coherenceAlerts().NB_ENFANTS;
    expect(alert).toBeDefined();
    expect(alert!.contributors.length).toBe(2);
    expect(alert!.source).toBe('MULTI');
  });

  it('alerts skipped on non-applicable typeRevision (DEMENAGEMENT_PARENT)', () => {
    component.typeRevision.set('DEMENAGEMENT_PARENT');
    component.aiData = { revenusAnnuelsEpoux1Eur: 50000 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Aucune alerte revenus ni nbEnfants car typeRevision non applicable
    expect(component.coherenceAlerts().REVENUS_DEBITEUR_ACTUEL).toBeUndefined();
    expect(component.coherenceAlerts().NB_ENFANTS).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // Gate workspaceCountry
  // ---------------------------------------------------------------------------

  it('workspaceCountry BELGIQUE → no HTTP call + isFranceGate false', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.isFranceGate()).toBe(false);
    expect(component.formValid()).toBe(false);
  });

  it('workspaceCountry FRANCE → HTTP GET issued', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.isFranceGate()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  it('verdictClass maps ELEVEE/MOYENNE/FAIBLE correctly', () => {
    expect(component.verdictClass('ELEVEE')).toContain('--ok');
    expect(component.verdictClass('MOYENNE')).toContain('--warn');
    expect(component.verdictClass('FAIBLE')).toContain('--ko');
  });

  it('ecartFormatted formats positive / negative / null', () => {
    expect(component.ecartFormatted(-40.0)).toBe('-40.0 %');
    expect(component.ecartFormatted(15.0)).toBe('+15.0 %');
    expect(component.ecartFormatted(null)).toBe('—');
  });

  // ---------------------------------------------------------------------------
  // Age enfants chips handlers
  // ---------------------------------------------------------------------------

  it('onAgeEnfantsAdd accepts valid age, ignores invalid', () => {
    component.onAgeEnfantsAdd('8');
    component.onAgeEnfantsAdd('12');
    expect(component.ageEnfants()).toEqual([8, 12]);

    component.onAgeEnfantsAdd('abc');     // invalide
    component.onAgeEnfantsAdd('-1');      // négatif
    component.onAgeEnfantsAdd('99');      // > 30
    expect(component.ageEnfants()).toEqual([8, 12]);
  });

  it('onAgeEnfantRemove removes the right index', () => {
    component.ageEnfants.set([8, 10, 12]);
    component.onAgeEnfantRemove(1);
    expect(component.ageEnfants()).toEqual([8, 12]);
  });
});
