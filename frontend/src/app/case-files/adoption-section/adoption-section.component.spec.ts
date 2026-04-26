import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { AdoptionSectionComponent } from './adoption-section.component';
import { AdoptionResponse } from '../../core/models/adoption.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('AdoptionSectionComponent', () => {
  let component: AdoptionSectionComponent;
  let fixture: ComponentFixture<AdoptionSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/adoption-analysis';

  function response(overrides: Partial<AdoptionResponse> = {}): AdoptionResponse {
    return {
      caseFileId: 'case-1',
      formeAdoption: 'PLENIERE',
      formeRecommandee: 'PLENIERE',
      verdictRecevabilite: 'ELEVEE',
      ageAdoptant: 35,
      ageAdopte: 4,
      differenceAgeAns: 31,
      criteresNonRemplis: [],
      delaiInstructionMois: 6,
      documentsRequis: ['Requête en adoption signée par avocat'],
      risqueRefus: [],
      baseJuridique: 'Art. 343-370-2 Cciv',
      formule: 'Forme demandée=PLENIERE → Verdict=ELEVEE',
      messages: ['Adoption recevable'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        AdoptionSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AdoptionSectionComponent);
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
    expect(component.provenanceFormeAdoption()).toBeNull();
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

  it('pré-fill IA depuis formeAdoptionDemandeeDetected=SIMPLE + pupilleEtatDetected=true', () => {
    component.aiData = {
      formeAdoptionDemandeeDetected: 'SIMPLE',
      pupilleEtatDetected: true,
      adoptantMarieDetected: true,
      ageAdoptantDetecte: 40,
      ageAdopteDetecte: 6,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.formeAdoption()).toBe('SIMPLE');
    expect(component.provenanceFormeAdoption()).toBe('IA');
    expect(component.pupilleEtat()).toBe(true);
    expect(component.provenancePupilleEtat()).toBe('IA');
    expect(component.adoptantMarie()).toBe(true);
    expect(component.provenanceAdoptantMarie()).toBe('IA');
    expect(component.ageAdoptant()).toBe(40);
    expect(component.provenanceAgeAdoptant()).toBe('IA');
    expect(component.ageAdopte()).toBe(6);
    expect(component.provenanceAgeAdopte()).toBe('IA');
  });

  it('pré-fill sans aiData → aucun pré-remplissage, aucun badge', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.formeAdoption()).toBe('PLENIERE'); // valeur par défaut
    expect(component.provenanceFormeAdoption()).toBeNull();
    expect(component.pupilleEtat()).toBe(false);
    expect(component.provenancePupilleEtat()).toBeNull();
    expect(component.ageAdoptant()).toBeNull();
  });

  it('pré-fill non déclenché si pupilleEtatDetected=false', () => {
    component.aiData = {
      pupilleEtatDetected: false,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.pupilleEtat()).toBe(false);
    expect(component.provenancePupilleEtat()).toBeNull();
  });

  it('onPupilleEtatChange efface le badge IA', () => {
    component.aiData = {
      pupilleEtatDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenancePupilleEtat()).toBe('IA');
    component.onPupilleEtatChange(false);
    expect(component.pupilleEtat()).toBe(false);
    expect(component.provenancePupilleEtat()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement (âges null)', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true ssi ageAdoptant + ageAdopte ≥ 0', () => {
    component.ageAdoptant.set(35);
    component.ageAdopte.set(4);
    expect(component.formValid()).toBe(true);

    component.ageAdoptant.set(null);
    expect(component.formValid()).toBe(false);

    component.ageAdoptant.set(35);
    component.ageAdopte.set(null);
    expect(component.formValid()).toBe(false);

    component.ageAdopte.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('differenceAgeAns calculée live (signal computed)', () => {
    component.ageAdoptant.set(40);
    component.ageAdopte.set(8);
    expect(component.differenceAgeAns()).toBe(32);

    component.ageAdoptant.set(null);
    expect(component.differenceAgeAns()).toBeNull();
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST + résultat + snackbar succès + dashboardRefresh', () => {
    const refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    (component as any).dashboardRefresh = refreshSpy;

    component.ageAdoptant.set(35);
    component.ageAdopte.set(4);
    component.formeAdoption.set('PLENIERE');
    component.consentementParents.set(true);
    component.placement6mois.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.formeAdoption).toBe('PLENIERE');
    expect(req.request.body.ageAdoptant).toBe(35);
    expect(req.request.body.ageAdopte).toBe(4);
    expect(req.request.body.consentementParents).toBe(true);
    expect(req.request.body.placement6mois).toBe(true);
    req.flush(response());

    expect(component.result()!.verdictRecevabilite).toBe('ELEVEE');
    expect(component.result()!.formeRecommandee).toBe('PLENIERE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Adoption analysée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.ageAdoptant.set(35);
    component.ageAdopte.set(4);
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

  it('coherenceAlerts.PUPILLE_ETAT présent si IA diverge de saisie', () => {
    component.aiData = {
      pupilleEtatDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit pupilleEtat=true. Avocat décoche → divergence IA.
    component.onPupilleEtatChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.PUPILLE_ETAT).toBeDefined();
    expect(alerts.PUPILLE_ETAT!.field).toBe('PUPILLE_ETAT');
    expect(alerts.PUPILLE_ETAT!.source).toBe('IA');
    expect(alerts.PUPILLE_ETAT!.expectedDisplay).toContain('pupille');
  });

  it('coherenceAlerts.FORME_ADOPTION présent si IA détecte SIMPLE et avocat saisit PLENIERE', () => {
    component.aiData = {
      formeAdoptionDemandeeDetected: 'SIMPLE',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill a mis SIMPLE. Avocat repasse à PLENIERE → divergence.
    component.onFormeAdoptionChange('PLENIERE');

    const alerts = component.coherenceAlerts();
    expect(alerts.FORME_ADOPTION).toBeDefined();
    expect(alerts.FORME_ADOPTION!.source).toBe('IA');
    expect(alerts.FORME_ADOPTION!.expectedDisplay).toBe('SIMPLE');
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = {
      pupilleEtatDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onPupilleEtatChange(false);
    expect(component.coherenceAlerts().PUPILLE_ETAT).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().PUPILLE_ETAT).toBeUndefined();
  });

  // ============================================================
  // Forme recommandée alert chip
  // ============================================================

  it('formeRecommandeeAlert null si plénière demandée + recommandée', () => {
    component.result.set(response({
      formeAdoption: 'PLENIERE', formeRecommandee: 'PLENIERE',
    }));
    expect(component.formeRecommandeeAlert()).toBeNull();
  });

  it('formeRecommandeeAlert warning si bascule plénière → simple', () => {
    component.result.set(response({
      formeAdoption: 'PLENIERE', formeRecommandee: 'SIMPLE',
      verdictRecevabilite: 'ELEVEE',
    }));
    expect(component.formeRecommandeeAlert()).toBe('warning');
  });

  it('formeRecommandeeAlert critical si AUCUNE', () => {
    component.result.set(response({
      formeAdoption: 'PLENIERE', formeRecommandee: 'AUCUNE',
      verdictRecevabilite: 'FAIBLE',
    }));
    expect(component.formeRecommandeeAlert()).toBe('critical');
  });

  // ============================================================
  // ngOnChanges + non-régression
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = {
      pupilleEtatDetected: true,
      ageAdoptantDetecte: 38,
    } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.pupilleEtat()).toBe(true);
    expect(component.provenancePupilleEtat()).toBe('IA');
    expect(component.ageAdoptant()).toBe(38);
    expect(component.provenanceAgeAdoptant()).toBe('IA');
  });

  it('ngOnChanges(aiData) ne réécrit pas un champ déjà coché par l\'avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Avocat coche pupille manuellement (provenance reste null).
    component.onPupilleEtatChange(true);
    expect(component.provenancePupilleEtat()).toBeNull();
    expect(component.pupilleEtat()).toBe(true);

    const newAi = {
      pupilleEtatDetected: true,
    } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    // pupilleEtat = true déjà saisi par l'avocat — la condition
    // `!this.pupilleEtat()` est false donc l'IA ne réécrit pas.
    expect(component.pupilleEtat()).toBe(true);
    expect(component.provenancePupilleEtat()).toBeNull();
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
    expect(component.bannerClass('ELEVEE')).toContain('adoption-banner--info');
    expect(component.bannerClass('MOYENNE')).toContain('adoption-banner--warning');
    expect(component.bannerClass('FAIBLE')).toContain('adoption-banner--critical');
    expect(component.bannerClass(null)).toContain('adoption-banner--info');
  });

  it('formeLabel renvoie le libellé humain', () => {
    expect(component.formeLabel('PLENIERE')).toContain('plénière');
    expect(component.formeLabel('SIMPLE')).toContain('simple');
    expect(component.formeLabel('AUCUNE')).toContain('Aucune');
    expect(component.formeLabel(null)).toBe('');
  });

  it('alertsSummary computed total sur les alertes actives', () => {
    component.aiData = {
      pupilleEtatDetected: true,
      adoptantMarieDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Pré-fill activé. Avocat décoche pour simuler divergences.
    component.onPupilleEtatChange(false);
    component.onAdoptantMarieChange(false);

    const summary = component.alertsSummary();
    expect(summary.total).toBeGreaterThanOrEqual(2);
  });
});
