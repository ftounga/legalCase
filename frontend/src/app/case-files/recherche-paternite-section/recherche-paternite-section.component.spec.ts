import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { RecherchePaterniteSectionComponent } from './recherche-paternite-section.component';
import { RecherchePaterniteResponse } from '../../core/models/recherche-paternite.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('RecherchePaterniteSectionComponent', () => {
  let component: RecherchePaterniteSectionComponent;
  let fixture: ComponentFixture<RecherchePaterniteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/recherche-paternite-analysis';

  function response(overrides: Partial<RecherchePaterniteResponse> = {}): RecherchePaterniteResponse {
    return {
      caseFileId: 'case-1',
      qualiteDuDemandeur: 'ENFANT_MAJEUR',
      verdictRecevabilite: 'ELEVEE',
      scoreRecevabilite: 88,
      delaiPrescriptionAns: 10,
      delaiPrescriptionRestantMois: 96,
      expertiseAdnRecommandee: true,
      presomptionRefusADN: false,
      risquesRefus: [],
      documentsRequis: ['Acte de naissance', 'Demande d\'expertise ADN'],
      baseJuridique: 'Art. 327 + 340 + 16-11 + 321 Cciv',
      formule: 'Verdict ELEVEE — délai non prescrit + ADN demandée + motifs',
      messages: ['Action recevable'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        RecherchePaterniteSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RecherchePaterniteSectionComponent);
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

    expect(component.result()!.scoreRecevabilite).toBe(88);
    expect(component.showForm()).toBe(false);
    expect(component.provenanceQualite()).toBeNull();
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

  it('pré-fill IA : qualité + date naissance + booleans', () => {
    component.aiData = {
      qualiteDuDemandeurRechercheDetected: 'ENFANT_MAJEUR',
      dateNaissanceEnfantRechercheDetectee: '2001-04-15',
      presomptionPossessionEtatRechercheDetected: true,
      expertiseAdnDemandeeRechercheDetected: true,
      pereDesigneRefuseADNDetected: false,
      motifsSerieuxRechercheDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.qualiteDuDemandeur()).toBe('ENFANT_MAJEUR');
    expect(component.provenanceQualite()).toBe('IA');
    expect(component.dateNaissanceEnfant()).toBe('2001-04-15');
    expect(component.provenanceDateNaissance()).toBe('IA');
    expect(component.presomptionPossessionEtat()).toBe(true);
    expect(component.provenancePossessionEtat()).toBe('IA');
    expect(component.expertiseAdnDemandee()).toBe(true);
    expect(component.provenanceExpertiseAdn()).toBe('IA');
    expect(component.pereDesigneRefuseADN()).toBe(false);
    // Refus ADN false n'est pas pré-rempli sur ce field (la valeur par défaut
    // est déjà false ; on ne pousse l'IA que si elle dévie).
    expect(component.motifsSerieux()).toBe(true);
    expect(component.provenanceMotifsSerieux()).toBe('IA');
  });

  it('pré-fill sans aiData → aucun pré-remplissage, aucun badge', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.qualiteDuDemandeur()).toBeNull();
    expect(component.dateNaissanceEnfant()).toBeNull();
    expect(component.provenanceQualite()).toBeNull();
  });

  it('onQualiteChange efface le badge IA', () => {
    component.aiData = {
      qualiteDuDemandeurRechercheDetected: 'ENFANT_MAJEUR',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceQualite()).toBe('IA');
    component.onQualiteChange('MERE');
    expect(component.qualiteDuDemandeur()).toBe('MERE');
    expect(component.provenanceQualite()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true ssi qualité + dateNaissanceEnfant', () => {
    component.qualiteDuDemandeur.set('ENFANT_MAJEUR');
    component.dateNaissanceEnfant.set('2001-04-15');
    expect(component.formValid()).toBe(true);

    component.dateNaissanceEnfant.set(null);
    expect(component.formValid()).toBe(false);

    component.dateNaissanceEnfant.set('2001-04-15');
    component.qualiteDuDemandeur.set(null);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST + résultat + snackbar succès + dashboardRefresh', () => {
    const refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    (component as any).dashboardRefresh = refreshSpy;

    component.qualiteDuDemandeur.set('ENFANT_MAJEUR');
    component.dateNaissanceEnfant.set('2001-04-15');
    component.expertiseAdnDemandee.set(true);
    component.motifsSerieux.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.qualiteDuDemandeur).toBe('ENFANT_MAJEUR');
    expect(req.request.body.dateNaissanceEnfant).toBe('2001-04-15');
    expect(req.request.body.expertiseAdnDemandee).toBe(true);
    expect(req.request.body.motifsSerieux).toBe(true);
    expect(req.request.body.presomptionPossessionEtat).toBe(false);
    expect(req.request.body.pereDesigneRefuseADN).toBe(false);
    req.flush(response());

    expect(component.result()!.verdictRecevabilite).toBe('ELEVEE');
    expect(component.result()!.delaiPrescriptionAns).toBe(10);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Recherche de paternité analysée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.qualiteDuDemandeur.set('MERE');
    component.dateNaissanceEnfant.set('2018-04-15');
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

  it('coherenceAlerts.QUALITE_DEMANDEUR présent si IA diverge de saisie', () => {
    component.aiData = {
      qualiteDuDemandeurRechercheDetected: 'ENFANT_MAJEUR',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit ENFANT_MAJEUR. Avocat saisit MERE → divergence IA.
    component.onQualiteChange('MERE');

    const alerts = component.coherenceAlerts();
    expect(alerts.QUALITE_DEMANDEUR).toBeDefined();
    expect(alerts.QUALITE_DEMANDEUR!.field).toBe('QUALITE_DEMANDEUR');
    expect(alerts.QUALITE_DEMANDEUR!.source).toBe('IA');
    expect(alerts.QUALITE_DEMANDEUR!.expectedDisplay).toContain('Enfant');
  });

  it('coherenceAlerts.MOTIFS_SERIEUX présent si IA diverge', () => {
    component.aiData = { motifsSerieuxRechercheDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill a mis motifsSerieux=true. Avocat décoche.
    component.onMotifsSerieuxChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.MOTIFS_SERIEUX).toBeDefined();
    expect(alerts.MOTIFS_SERIEUX!.source).toBe('IA');
    expect(alerts.MOTIFS_SERIEUX!.expectedDisplay).toContain('Motifs sérieux');
  });

  it('coherenceAlerts.EXPERTISE_ADN présent si IA diverge', () => {
    component.aiData = { expertiseAdnDemandeeRechercheDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onExpertiseAdnChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.EXPERTISE_ADN).toBeDefined();
    expect(alerts.EXPERTISE_ADN!.source).toBe('IA');
  });

  it('coherenceAlerts.REFUS_ADN présent si IA diverge', () => {
    component.aiData = { pereDesigneRefuseADNDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onRefusAdnChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.REFUS_ADN).toBeDefined();
    expect(alerts.REFUS_ADN!.source).toBe('IA');
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = { motifsSerieuxRechercheDetected: true } as FamilleExtractedData;
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
    component.result.set(response({ delaiPrescriptionRestantMois: 96 }));
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
      qualiteDuDemandeurRechercheDetected: 'MERE',
    } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.qualiteDuDemandeur()).toBe('MERE');
    expect(component.provenanceQualite()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onQualiteChange('REPRESENTANT_LEGAL_MINEUR');
    expect(component.provenanceQualite()).toBeNull();

    const newAi = {
      qualiteDuDemandeurRechercheDetected: 'MERE',
    } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.qualiteDuDemandeur()).toBe('REPRESENTANT_LEGAL_MINEUR');
    expect(component.provenanceQualite()).toBeNull();
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
    expect(component.bannerClass('ELEVEE')).toContain('rch-pat-banner--info');
    expect(component.bannerClass('MOYENNE')).toContain('rch-pat-banner--warning');
    expect(component.bannerClass('FAIBLE')).toContain('rch-pat-banner--critical');
    expect(component.bannerClass(null)).toContain('rch-pat-banner--info');
  });

  it('qualiteLabel renvoie le libellé humain ou le code en fallback', () => {
    expect(component.qualiteLabel('ENFANT_MAJEUR')).toContain('Enfant');
    expect(component.qualiteLabel('MERE')).toContain('Mère');
    expect(component.qualiteLabel('REPRESENTANT_LEGAL_MINEUR')).toContain('Représentant');
    expect(component.qualiteLabel(null)).toBe('');
  });
});
