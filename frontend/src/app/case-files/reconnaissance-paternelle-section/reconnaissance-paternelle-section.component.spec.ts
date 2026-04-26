import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { ReconnaissancePaternelleSectionComponent } from './reconnaissance-paternelle-section.component';
import { ReconnaissancePaternelleResponse } from '../../core/models/reconnaissance-paternelle.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('ReconnaissancePaternelleSectionComponent', () => {
  let component: ReconnaissancePaternelleSectionComponent;
  let fixture: ComponentFixture<ReconnaissancePaternelleSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/reconnaissance-paternelle-analysis';

  function response(overrides: Partial<ReconnaissancePaternelleResponse> = {}): ReconnaissancePaternelleResponse {
    return {
      caseFileId: 'case-1',
      sousType: 'RECONNAISSANCE_POST_NATALE_NAISSANCE',
      verdictRecevabilite: 'ELEVEE',
      scoreEligibilite: 90,
      effetFiliation: '2024-03-15',
      risquesContestation: [],
      documentsRequis: ['Acte de naissance', 'Pièce identité père'],
      delaiContestationAns: 10,
      baseJuridique: 'Art. 316 Cciv + 332-335 + 372 Cciv',
      formule: 'Tous critères ELEVEE → score 90 → ELEVEE',
      messages: ['Reconnaissance recevable'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        ReconnaissancePaternelleSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ReconnaissancePaternelleSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ============================================================
  // Gate pays + init
  // ============================================================

  it('FRANCE → isFrance() true, GET appelé au ngOnInit', () => {
    expect(component.isFrance()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('BELGIQUE → isFrance() false, aucun appel HTTP au ngOnInit (gate pays)', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.isFrance()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
  });

  it('charge le résultat existant si GET 200 (mode résultat hydraté)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());

    expect(component.result()!.scoreEligibilite).toBe(90);
    expect(component.showForm()).toBe(false);
    expect(component.provenanceConsentementLibre()).toBeNull();
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ============================================================
  // Pré-fill IA
  // ============================================================

  it('pré-fill IA : consentementLibreDuPere ← aiData.consentementLibreDuPereDetected', () => {
    component.aiData = { consentementLibreDuPereDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.consentementLibreDuPere()).toBe(true);
    expect(component.provenanceConsentementLibre()).toBe('IA');
  });

  it('pré-fill IA : 4 booleans + dateNaissance', () => {
    component.aiData = {
      consentementLibreDuPereDetected: true,
      paterniteVraisemblableDetected: true,
      enfantNonReconnuParAutrePereDetected: true,
      procedureRespecteeReconnaissanceDetected: true,
      dateNaissanceEnfantDetectee: '2024-05-10',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.consentementLibreDuPere()).toBe(true);
    expect(component.provenanceConsentementLibre()).toBe('IA');
    expect(component.paterniteVraisemblable()).toBe(true);
    expect(component.provenancePaterniteVraisemblable()).toBe('IA');
    expect(component.enfantNonReconnuParAutrePere()).toBe(true);
    expect(component.provenanceEnfantNonReconnu()).toBe('IA');
    expect(component.procedureRespectee()).toBe(true);
    expect(component.provenanceProcedureRespectee()).toBe('IA');
    expect(component.dateNaissanceEnfant()).toBe('2024-05-10');
    expect(component.provenanceDateNaissance()).toBe('IA');
  });

  it('pré-fill sans aiData → aucun pré-remplissage, aucun badge', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.consentementLibreDuPere()).toBeNull();
    expect(component.paterniteVraisemblable()).toBeNull();
    expect(component.provenanceConsentementLibre()).toBeNull();
  });

  it('onConsentementLibreChange efface le badge IA', () => {
    component.aiData = { consentementLibreDuPereDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceConsentementLibre()).toBe('IA');
    component.onConsentementLibreChange(false);
    expect(component.consentementLibreDuPere()).toBe(false);
    expect(component.provenanceConsentementLibre()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true seulement quand tous les champs requis sont présents (post-natale)', () => {
    component.sousType.set('RECONNAISSANCE_POST_NATALE_NAISSANCE');
    component.dateNaissanceEnfant.set('2024-03-15');
    component.consentementLibreDuPere.set(true);
    component.paterniteVraisemblable.set(true);
    component.enfantNonReconnuParAutrePere.set(true);
    component.procedureRespectee.set(true);
    expect(component.formValid()).toBe(true);

    component.dateNaissanceEnfant.set(null);
    expect(component.formValid()).toBe(false);

    component.dateNaissanceEnfant.set('2024-03-15');
    component.consentementLibreDuPere.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid PRENATALE valide sans dateNaissanceEnfant', () => {
    component.sousType.set('RECONNAISSANCE_PRENATALE');
    component.consentementLibreDuPere.set(true);
    component.paterniteVraisemblable.set(true);
    component.enfantNonReconnuParAutrePere.set(true);
    component.procedureRespectee.set(true);
    // dateNaissance null OK pour prénatale.
    expect(component.formValid()).toBe(true);
    expect(component.isPrenatal()).toBe(true);
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST + résultat + snackbar succès + triggerRefresh', () => {
    component.sousType.set('RECONNAISSANCE_POST_NATALE_NAISSANCE');
    component.dateNaissanceEnfant.set('2024-03-15');
    component.dateReconnaissance.set('2024-03-15');
    component.consentementLibreDuPere.set(true);
    component.paterniteVraisemblable.set(true);
    component.enfantNonReconnuParAutrePere.set(true);
    component.procedureRespectee.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.sousType).toBe('RECONNAISSANCE_POST_NATALE_NAISSANCE');
    expect(req.request.body.consentementLibreDuPere).toBe(true);
    expect(req.request.body.paterniteVraisemblable).toBe(true);
    expect(req.request.body.enfantNonReconnuParAutrePere).toBe(true);
    expect(req.request.body.procedureRespectee).toBe(true);
    expect(req.request.body.dateNaissanceEnfant).toBe('2024-03-15');
    expect(req.request.body.presenceParProcuration).toBe(false);
    req.flush(response());

    expect(component.result()!.verdictRecevabilite).toBe('ELEVEE');
    expect(component.result()!.delaiContestationAns).toBe(10);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Reconnaissance paternelle analysée', 'OK', jasmine.any(Object));
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.sousType.set('RECONNAISSANCE_PRENATALE');
    component.consentementLibreDuPere.set(true);
    component.paterniteVraisemblable.set(true);
    component.enfantNonReconnuParAutrePere.set(true);
    component.procedureRespectee.set(true);
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

  // ============================================================
  // F-IA-03 alertes de cohérence
  // ============================================================

  it('coherenceAlerts.PATERNITE_VRAISEMBLABLE présent si IA diverge de saisie', () => {
    component.aiData = { paterniteVraisemblableDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit `true`. Avocat saisit `false` → divergence IA.
    component.onPaterniteVraisemblableChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.PATERNITE_VRAISEMBLABLE).toBeDefined();
    expect(alerts.PATERNITE_VRAISEMBLABLE!.field).toBe('PATERNITE_VRAISEMBLABLE');
    expect(alerts.PATERNITE_VRAISEMBLABLE!.source).toBe('IA');
    expect(alerts.PATERNITE_VRAISEMBLABLE!.expectedDisplay).toContain('vraisemblable');
  });

  it('coherenceAlerts.CONSENTEMENT_LIBRE absent si IA convergent', () => {
    component.aiData = { consentementLibreDuPereDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.consentementLibreDuPere()).toBe(true);
    expect(component.coherenceAlerts().CONSENTEMENT_LIBRE).toBeUndefined();
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = { paterniteVraisemblableDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onPaterniteVraisemblableChange(false);
    expect(component.coherenceAlerts().PATERNITE_VRAISEMBLABLE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().PATERNITE_VRAISEMBLABLE).toBeUndefined();
  });

  it('coherenceAlerts multi-sources F96 + IA convergents → MULTI', () => {
    component.aiData = { consentementLibreDuPereDetected: true } as FamilleExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Consentement libre',
        statut: 'NON_COMPLIANT',
        critereCode: 'RECONNAISSANCE_PATERNELLE_CONSENTEMENT',
        expectedValue: 'OUI',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onConsentementLibreChange(false);

    const alert = component.coherenceAlerts().CONSENTEMENT_LIBRE;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
    expect(alert!.reason).toContain(' ET ');
  });

  it('coherenceAlerts.ENFANT_NON_RECONNU indépendant de PATERNITE_VRAISEMBLABLE', () => {
    component.aiData = {
      paterniteVraisemblableDetected: true,
      enfantNonReconnuParAutrePereDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onEnfantNonReconnuChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.PATERNITE_VRAISEMBLABLE).toBeUndefined();
    expect(alerts.ENFANT_NON_RECONNU).toBeDefined();
    expect(alerts.ENFANT_NON_RECONNU!.source).toBe('IA');
  });

  // ============================================================
  // ngOnChanges + non-régression
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = { consentementLibreDuPereDetected: true } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.consentementLibreDuPere()).toBe(true);
    expect(component.provenanceConsentementLibre()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onConsentementLibreChange(false);
    expect(component.provenanceConsentementLibre()).toBeNull();

    const newAi = { consentementLibreDuPereDetected: true } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.consentementLibreDuPere()).toBe(false);
    expect(component.provenanceConsentementLibre()).toBeNull();
  });

  it('toggleCollapse fonctionne', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('editMode ré-affiche le form', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('bannerClass mappe verdict → classe CSS attendue', () => {
    expect(component.bannerClass('ELEVEE')).toContain('rec-pat-banner--info');
    expect(component.bannerClass('MOYENNE')).toContain('rec-pat-banner--warning');
    expect(component.bannerClass('FAIBLE')).toContain('rec-pat-banner--critical');
    expect(component.bannerClass(null)).toContain('rec-pat-banner--info');
  });

  it('sousTypeLabel renvoie le libellé humain ou le code en fallback', () => {
    expect(component.sousTypeLabel('RECONNAISSANCE_PRENATALE')).toContain('Prénatale');
    expect(component.sousTypeLabel('RECONNAISSANCE_POST_NATALE_NAISSANCE')).toContain('acte de naissance');
    expect(component.sousTypeLabel(null)).toBe('');
  });
});
