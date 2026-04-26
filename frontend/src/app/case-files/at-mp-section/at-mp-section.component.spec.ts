import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { AtMpSectionComponent } from './at-mp-section.component';
import { AtMpResponse } from '../../core/models/at-mp.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('AtMpSectionComponent', () => {
  let component: AtMpSectionComponent;
  let fixture: ComponentFixture<AtMpSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/at-mp-analysis';

  function response(overrides: Partial<AtMpResponse> = {}): AtMpResponse {
    return {
      caseFileId: 'case-1',
      country: 'FRANCE',
      dispositif: 'RECONNAISSANCE_AT',
      dispositifLibelle: 'Reconnaissance accident du travail (CSS L.411-1)',
      dateAccident: '2026-03-15',
      lieuTravail: true,
      declarationEmployeurDansLes48h: true,
      certificatMedicalInitial: true,
      numeroTableau: null,
      delaiPriseEnChargeRespecte: null,
      dateExposition: null,
      tauxFixeParCpam: null,
      tauxRevendique: null,
      expertiseMedicaleProduite: null,
      datePremierAvisCpam: null,
      verdictRecevabilite: 'ELEVEE',
      delaiInstructionJours: 90,
      competence: 'CPAM',
      expertiseRequise: false,
      documentsRequis: ['Certificat médical initial L.441-6'],
      risqueRefus: [],
      baseJuridique: 'CSS art. L.411-1 + L.441-1 + L.441-6 (présomption AT)',
      formule: 'Reconnaissance AT — verdict ELEVEE — instruction CPAM 90 jours.',
      messages: ['Présomption d\'imputabilité L.411-1 applicable.'],
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        AtMpSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AtMpSectionComponent);
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

    expect(component.result()!.verdictRecevabilite).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(component.provenanceDateAccident()).toBeNull();
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ============================================================
  // Pré-fill IA — proxy aiData.dateLicenciement → dateAccident
  // ============================================================

  it('pré-fill IA : aiData.dateLicenciement → dateAccident (RECONNAISSANCE_AT seul)', () => {
    component.aiData = { dateLicenciement: '2026-03-10' } as TravailExtractedData;
    component.dispositif.set('RECONNAISSANCE_AT');
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateAccident()).toBe('2026-03-10');
    expect(component.provenanceDateAccident()).toBe('IA');
  });

  it('pré-fill IA ne s\'applique pas pour RECONNAISSANCE_MP', () => {
    component.aiData = { dateLicenciement: '2026-03-10' } as TravailExtractedData;
    component.dispositif.set('RECONNAISSANCE_MP');
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateAccident()).toBeNull();
    expect(component.provenanceDateAccident()).toBeNull();
  });

  it('pré-fill sans aiData → aucun pré-remplissage', () => {
    component.aiData = null;
    component.dispositif.set('RECONNAISSANCE_AT');
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateAccident()).toBeNull();
    expect(component.provenanceDateAccident()).toBeNull();
  });

  it('onDateAccidentChange efface le badge IA', () => {
    component.aiData = { dateLicenciement: '2026-03-10' } as TravailExtractedData;
    component.dispositif.set('RECONNAISSANCE_AT');
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceDateAccident()).toBe('IA');
    component.onDateAccidentChange('2026-03-20');
    expect(component.dateAccident()).toBe('2026-03-20');
    expect(component.provenanceDateAccident()).toBeNull();
  });

  // ============================================================
  // Form validation par dispositif
  // ============================================================

  it('formValid false initialement (pas de dispositif)', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid AT : exige dispositif + dateAccident', () => {
    component.dispositif.set('RECONNAISSANCE_AT');
    expect(component.formValid()).toBe(false);
    component.dateAccident.set('2026-03-15');
    expect(component.formValid()).toBe(true);
  });

  it('formValid MP : exige dispositif + numeroTableau + dateExposition', () => {
    component.dispositif.set('RECONNAISSANCE_MP');
    expect(component.formValid()).toBe(false);
    component.numeroTableau.set('30');
    expect(component.formValid()).toBe(false);
    component.dateExposition.set('2026-01-01');
    expect(component.formValid()).toBe(true);
  });

  it('formValid MP : hors-tableau accepte sans numéro de tableau', () => {
    component.dispositif.set('RECONNAISSANCE_MP');
    component.numeroTableauIsHorsTableau.set(true);
    component.numeroTableau.set('HORS_TABLEAU');
    component.dateExposition.set('2026-01-01');
    expect(component.formValid()).toBe(true);
  });

  it('formValid IPP : exige tauxRevendique > tauxFixe', () => {
    component.dispositif.set('CONTESTATION_TAUX_IPP');
    component.tauxFixeParCpam.set(8);
    component.tauxRevendique.set(8);
    component.datePremierAvisCpam.set('2026-03-01');
    expect(component.formValid()).toBe(false);

    component.tauxRevendique.set(25);
    expect(component.formValid()).toBe(true);
  });

  it('formValid IPP : refuse taux hors [0,100]', () => {
    component.dispositif.set('CONTESTATION_TAUX_IPP');
    component.tauxFixeParCpam.set(120);
    component.tauxRevendique.set(150);
    component.datePremierAvisCpam.set('2026-03-01');
    expect(component.formValid()).toBe(false);
  });

  it('formValid IPP : exige datePremierAvisCpam', () => {
    component.dispositif.set('CONTESTATION_TAUX_IPP');
    component.tauxFixeParCpam.set(8);
    component.tauxRevendique.set(25);
    expect(component.formValid()).toBe(false);
    component.datePremierAvisCpam.set('2026-03-01');
    expect(component.formValid()).toBe(true);
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST AT → résultat ELEVEE + snackbar succès', () => {
    component.dispositif.set('RECONNAISSANCE_AT');
    component.dateAccident.set('2026-03-15');
    component.lieuTravail.set(true);
    component.declarationEmployeurDansLes48h.set(true);
    component.certificatMedicalInitial.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dispositif).toBe('RECONNAISSANCE_AT');
    expect(req.request.body.dateAccident).toBe('2026-03-15');
    expect(req.request.body.lieuTravail).toBe(true);
    expect(req.request.body.numeroTableau).toBeUndefined();
    req.flush(response());

    expect(component.result()!.verdictRecevabilite).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Recevabilité AT/MP analysée', 'OK', jasmine.any(Object));
  });

  it('calculate() POST IPP envoie tauxFixe + tauxRevendique + datePremierAvisCpam', () => {
    component.dispositif.set('CONTESTATION_TAUX_IPP');
    component.tauxFixeParCpam.set(8);
    component.tauxRevendique.set(25);
    component.expertiseMedicaleProduite.set(true);
    component.datePremierAvisCpam.set('2026-03-01');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.dispositif).toBe('CONTESTATION_TAUX_IPP');
    expect(req.request.body.tauxFixeParCpam).toBe(8);
    expect(req.request.body.tauxRevendique).toBe(25);
    expect(req.request.body.expertiseMedicaleProduite).toBe(true);
    expect(req.request.body.datePremierAvisCpam).toBe('2026-03-01');
    expect(req.request.body.dateAccident).toBeUndefined();
    req.flush(response({
      dispositif: 'CONTESTATION_TAUX_IPP',
      verdictRecevabilite: 'ELEVEE',
      delaiInstructionJours: 120,
      competence: 'CMRA',
      expertiseRequise: true,
    }));
    expect(component.result()!.competence).toBe('CMRA');
  });

  it('calculate() POST MP envoie numeroTableau + dateExposition + delaiPriseEnChargeRespecte', () => {
    component.dispositif.set('RECONNAISSANCE_MP');
    component.numeroTableau.set('30');
    component.delaiPriseEnChargeRespecte.set(true);
    component.dateExposition.set('2026-01-01');
    component.certificatMedicalInitial.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.dispositif).toBe('RECONNAISSANCE_MP');
    expect(req.request.body.numeroTableau).toBe('30');
    expect(req.request.body.delaiPriseEnChargeRespecte).toBe(true);
    expect(req.request.body.dateExposition).toBe('2026-01-01');
    expect(req.request.body.tauxFixeParCpam).toBeUndefined();
    req.flush(response({
      dispositif: 'RECONNAISSANCE_MP',
      verdictRecevabilite: 'ELEVEE',
      delaiInstructionJours: 120,
      competence: 'CPAM',
      numeroTableau: '30',
      delaiPriseEnChargeRespecte: true,
      dateExposition: '2026-01-01',
    }));
    expect(component.result()!.competence).toBe('CPAM');
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.dispositif.set('RECONNAISSANCE_AT');
    component.dateAccident.set('2026-03-15');
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

  it('coherenceAlerts.DATE_ACCIDENT présent si IA diverge de saisie (dispositif AT)', () => {
    component.aiData = { dateLicenciement: '2026-03-10' } as TravailExtractedData;
    component.dispositif.set('RECONNAISSANCE_AT');
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit '2026-03-10'. Avocat saisit autre date.
    component.onDateAccidentChange('2026-03-20');

    const alerts = component.coherenceAlerts();
    expect(alerts.DATE_ACCIDENT).toBeDefined();
    expect(alerts.DATE_ACCIDENT!.field).toBe('DATE_ACCIDENT');
    expect(alerts.DATE_ACCIDENT!.source).toBe('IA');
    expect(alerts.DATE_ACCIDENT!.expectedDisplay).toBe('10/03/2026');
  });

  it('coherenceAlerts vides si dispositif != AT (pas d\'alerte sur MP/IPP)', () => {
    component.aiData = { dateLicenciement: '2026-03-10' } as TravailExtractedData;
    component.dispositif.set('RECONNAISSANCE_MP');
    component.dateAccident.set('2026-03-20');
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.coherenceAlerts().DATE_ACCIDENT).toBeUndefined();
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = { dateLicenciement: '2026-03-10' } as TravailExtractedData;
    component.dispositif.set('RECONNAISSANCE_AT');
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onDateAccidentChange('2026-03-20');
    expect(component.coherenceAlerts().DATE_ACCIDENT).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().DATE_ACCIDENT).toBeUndefined();
  });

  it('coherenceAlerts multi-sources F96 + IA convergents → MULTI', () => {
    component.aiData = { dateLicenciement: '2026-03-10' } as TravailExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Date AT/MP',
        statut: 'NON_COMPLIANT',
        critereCode: 'AT_MP_DATE_ACCIDENT',
        expectedValue: '2026-03-10',
      },
    ];
    component.dispositif.set('RECONNAISSANCE_AT');
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Avocat saisit autre date → divergence IA + F96 même valeur attendue.
    component.onDateAccidentChange('2026-03-20');

    const alert = component.coherenceAlerts().DATE_ACCIDENT;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
  });

  // ============================================================
  // ngOnChanges + non-régression
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si AT et form vide', () => {
    component.aiData = null;
    component.dispositif.set('RECONNAISSANCE_AT');
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = { dateLicenciement: '2026-04-01' } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.dateAccident()).toBe('2026-04-01');
    expect(component.provenanceDateAccident()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas la saisie avocat', () => {
    component.aiData = null;
    component.dispositif.set('RECONNAISSANCE_AT');
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onDateAccidentChange('2026-03-20');
    expect(component.provenanceDateAccident()).toBeNull();

    const newAi = { dateLicenciement: '2026-04-01' } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.dateAccident()).toBe('2026-03-20');
    expect(component.provenanceDateAccident()).toBeNull();
  });

  // ============================================================
  // Helpers UI
  // ============================================================

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
    expect(component.bannerClass('ELEVEE')).toContain('at-mp-banner--info');
    expect(component.bannerClass('MOYENNE')).toContain('at-mp-banner--warning');
    expect(component.bannerClass('FAIBLE')).toContain('at-mp-banner--warning');
    expect(component.bannerClass(null)).toContain('at-mp-banner--info');
  });

  it('dispositifLabel renvoie le libellé humain ou le code en fallback', () => {
    expect(component.dispositifLabel('RECONNAISSANCE_AT')).toContain('accident du travail');
    expect(component.dispositifLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(component.dispositifLabel(null)).toBe('');
  });

  it('verdictLabel renvoie le libellé humain', () => {
    expect(component.verdictLabel('ELEVEE')).toBe('Recevabilité élevée');
    expect(component.verdictLabel('MOYENNE')).toBe('Recevabilité moyenne');
    expect(component.verdictLabel('FAIBLE')).toBe('Recevabilité faible');
  });

  it('competenceLabel renvoie le libellé humain (CRRMP, CMRA, TJ)', () => {
    expect(component.competenceLabel('CPAM')).toContain('Caisse primaire');
    expect(component.competenceLabel('CRRMP')).toContain('Comité régional');
    expect(component.competenceLabel('CMRA')).toContain('Commission médicale');
    expect(component.competenceLabel('TJ_POLE_SOCIAL')).toContain('Pôle Social');
  });

  it('onHorsTableauChange met le champ numeroTableau à HORS_TABLEAU et désactive le toggle', () => {
    component.dispositif.set('RECONNAISSANCE_MP');
    component.onHorsTableauChange(true);
    expect(component.numeroTableau()).toBe('HORS_TABLEAU');
    expect(component.numeroTableauIsHorsTableau()).toBe(true);

    component.onHorsTableauChange(false);
    expect(component.numeroTableau()).toBeNull();
    expect(component.numeroTableauIsHorsTableau()).toBe(false);
  });
});
