import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { ContestationPaterniteSectionComponent } from './contestation-paternite-section.component';
import { ContestationPaterniteResponse } from '../../core/models/contestation-paternite.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('ContestationPaterniteSectionComponent', () => {
  let component: ContestationPaterniteSectionComponent;
  let fixture: ComponentFixture<ContestationPaterniteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/contestation-paternite-analysis';

  function response(overrides: Partial<ContestationPaterniteResponse> = {}): ContestationPaterniteResponse {
    return {
      caseFileId: 'case-1',
      qualiteAagir: 'PERE_DECLARE',
      verdictRecevabilite: 'ELEVEE',
      scoreRecevabilite: 88,
      delaiPrescriptionAns: 5,
      delaiPrescriptionRestantMois: 42,
      expertiseAdnRecommandee: true,
      risquesRefus: [],
      documentsRequis: ['Acte de naissance', 'Demande d\'expertise ADN'],
      baseJuridique: 'Art. 332-335 + 311-1 + 321 + 372 Cciv',
      formule: 'Verdict ELEVEE — délai non prescrit + motifs sérieux',
      messages: ['Action recevable'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        ContestationPaterniteSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ContestationPaterniteSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

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

    expect(component.result()!.scoreRecevabilite).toBe(88);
    expect(component.showForm()).toBe(false);
    expect(component.provenanceQualiteAagir()).toBeNull();
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

  it('pré-fill IA : qualité + dates + booleans', () => {
    component.aiData = {
      qualiteAagirContestationDetected: 'PERE_DECLARE',
      dateEtablissementFiliationDetectee: '2018-04-15',
      dateConnaissanceVeriteDetectee: '2025-01-20',
      dateMajoriteEnfantDetectee: '2026-06-01',
      possessionEtatConforme5AnsDetected: false,
      expertiseAdnDemandeeDetected: true,
      motifsSerieuxDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.qualiteAagir()).toBe('PERE_DECLARE');
    expect(component.provenanceQualiteAagir()).toBe('IA');
    expect(component.dateEtablissementFiliation()).toBe('2018-04-15');
    expect(component.provenanceDateEtablissement()).toBe('IA');
    expect(component.dateConnaissanceVerite()).toBe('2025-01-20');
    expect(component.provenanceDateConnaissance()).toBe('IA');
    expect(component.possessionEtatConforme5Ans()).toBe(false);
    expect(component.provenancePossessionEtat()).toBe('IA');
    expect(component.expertiseAdnDemandee()).toBe(true);
    expect(component.provenanceExpertiseAdn()).toBe('IA');
    expect(component.motifsSerieux()).toBe(true);
    expect(component.provenanceMotifsSerieux()).toBe('IA');
  });

  it('pré-fill sans aiData → aucun pré-remplissage, aucun badge', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.qualiteAagir()).toBeNull();
    expect(component.motifsSerieux()).toBeNull();
    expect(component.provenanceQualiteAagir()).toBeNull();
  });

  it('onQualiteAagirChange efface le badge IA', () => {
    component.aiData = {
      qualiteAagirContestationDetected: 'PERE_DECLARE',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceQualiteAagir()).toBe('IA');
    component.onQualiteAagirChange('MERE');
    expect(component.qualiteAagir()).toBe('MERE');
    expect(component.provenanceQualiteAagir()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true seulement quand tous les champs requis sont présents', () => {
    component.qualiteAagir.set('PERE_DECLARE');
    component.dateEtablissementFiliation.set('2018-04-15');
    component.dateConnaissanceVerite.set('2025-01-20');
    component.motifsSerieux.set(true);
    component.possessionEtatConforme5Ans.set(false);
    expect(component.formValid()).toBe(true);

    component.dateConnaissanceVerite.set(null);
    expect(component.formValid()).toBe(false);

    component.dateConnaissanceVerite.set('2025-01-20');
    component.qualiteAagir.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid ENFANT_MAJEUR exige dateMajoriteEnfant', () => {
    component.qualiteAagir.set('ENFANT_MAJEUR');
    component.dateEtablissementFiliation.set('2018-04-15');
    component.dateConnaissanceVerite.set('2025-01-20');
    component.motifsSerieux.set(true);
    component.possessionEtatConforme5Ans.set(false);
    // dateMajoriteEnfant non renseignée → invalide
    expect(component.isEnfantMajeur()).toBe(true);
    expect(component.formValid()).toBe(false);

    component.dateMajoriteEnfant.set('2026-06-01');
    expect(component.formValid()).toBe(true);
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST + résultat + snackbar succès', () => {
    component.qualiteAagir.set('PERE_DECLARE');
    component.dateEtablissementFiliation.set('2018-04-15');
    component.dateConnaissanceVerite.set('2025-01-20');
    component.motifsSerieux.set(true);
    component.possessionEtatConforme5Ans.set(false);
    component.expertiseAdnDemandee.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.qualiteAagir).toBe('PERE_DECLARE');
    expect(req.request.body.dateEtablissementFiliation).toBe('2018-04-15');
    expect(req.request.body.dateConnaissanceVerite).toBe('2025-01-20');
    expect(req.request.body.motifsSerieux).toBe(true);
    expect(req.request.body.possessionEtatConforme5Ans).toBe(false);
    expect(req.request.body.expertiseAdnDemandee).toBe(true);
    req.flush(response());

    expect(component.result()!.verdictRecevabilite).toBe('ELEVEE');
    expect(component.result()!.delaiPrescriptionAns).toBe(5);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Contestation de paternité analysée', 'OK', jasmine.any(Object));
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.qualiteAagir.set('MERE');
    component.dateEtablissementFiliation.set('2018-04-15');
    component.dateConnaissanceVerite.set('2025-01-20');
    component.motifsSerieux.set(true);
    component.possessionEtatConforme5Ans.set(false);
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

  it('coherenceAlerts.QUALITE_AAGIR présent si IA diverge de saisie', () => {
    component.aiData = {
      qualiteAagirContestationDetected: 'PERE_DECLARE',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit PERE_DECLARE. Avocat saisit MERE → divergence IA.
    component.onQualiteAagirChange('MERE');

    const alerts = component.coherenceAlerts();
    expect(alerts.QUALITE_AAGIR).toBeDefined();
    expect(alerts.QUALITE_AAGIR!.field).toBe('QUALITE_AAGIR');
    expect(alerts.QUALITE_AAGIR!.source).toBe('IA');
    expect(alerts.QUALITE_AAGIR!.expectedDisplay).toContain('Père');
  });

  it('coherenceAlerts.MOTIFS_SERIEUX présent si IA diverge', () => {
    component.aiData = { motifsSerieuxDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onMotifsSerieuxChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.MOTIFS_SERIEUX).toBeDefined();
    expect(alerts.MOTIFS_SERIEUX!.source).toBe('IA');
    expect(alerts.MOTIFS_SERIEUX!.expectedDisplay).toContain('Motifs sérieux');
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = { motifsSerieuxDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onMotifsSerieuxChange(false);
    expect(component.coherenceAlerts().MOTIFS_SERIEUX).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().MOTIFS_SERIEUX).toBeUndefined();
  });

  // ============================================================
  // Délai prescription chip alert
  // ============================================================

  it('delaiPrescriptionAlert null si > 12 mois', () => {
    component.result.set(response({ delaiPrescriptionRestantMois: 42 }));
    expect(component.delaiPrescriptionAlert()).toBeNull();
  });

  it('delaiPrescriptionAlert warning si > 6 et < 12 mois', () => {
    component.result.set(response({ delaiPrescriptionRestantMois: 8 }));
    expect(component.delaiPrescriptionAlert()).toBe('warning');
  });

  it('delaiPrescriptionAlert critical si <= 6 mois', () => {
    component.result.set(response({ delaiPrescriptionRestantMois: 3 }));
    expect(component.delaiPrescriptionAlert()).toBe('critical');
  });

  it('delaiPrescriptionAlert critical si prescrit (négatif)', () => {
    component.result.set(response({
      delaiPrescriptionRestantMois: -5,
      verdictRecevabilite: 'FAIBLE',
    }));
    expect(component.delaiPrescriptionAlert()).toBe('critical');
  });

  // ============================================================
  // ngOnChanges + non-régression
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = {
      qualiteAagirContestationDetected: 'MERE',
    } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.qualiteAagir()).toBe('MERE');
    expect(component.provenanceQualiteAagir()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onQualiteAagirChange('PERE_BIOLOGIQUE_PRESUME');
    expect(component.provenanceQualiteAagir()).toBeNull();

    const newAi = {
      qualiteAagirContestationDetected: 'MERE',
    } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.qualiteAagir()).toBe('PERE_BIOLOGIQUE_PRESUME');
    expect(component.provenanceQualiteAagir()).toBeNull();
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
    expect(component.bannerClass('ELEVEE')).toContain('cont-pat-banner--info');
    expect(component.bannerClass('MOYENNE')).toContain('cont-pat-banner--warning');
    expect(component.bannerClass('FAIBLE')).toContain('cont-pat-banner--critical');
    expect(component.bannerClass(null)).toContain('cont-pat-banner--info');
  });

  it('qualiteLabel renvoie le libellé humain ou le code en fallback', () => {
    expect(component.qualiteLabel('PERE_DECLARE')).toContain('Père');
    expect(component.qualiteLabel('ENFANT_MAJEUR')).toContain('Enfant');
    expect(component.qualiteLabel(null)).toBe('');
  });
});
