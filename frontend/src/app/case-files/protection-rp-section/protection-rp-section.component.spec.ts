import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { ProtectionRpSectionComponent } from './protection-rp-section.component';
import { ProtectionRpResponse } from '../../core/models/protection-rp.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('ProtectionRpSectionComponent', () => {
  let component: ProtectionRpSectionComponent;
  let fixture: ComponentFixture<ProtectionRpSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/protection-rp-analysis';

  function response(overrides: Partial<ProtectionRpResponse> = {}): ProtectionRpResponse {
    return {
      caseFileId: 'case-1',
      salarieEncoreProtege: true,
      scoreConformite: 100,
      verdictLegalite: 'VALIDE',
      criteresRemplis: [
        'STATUT_PROTEGE_VALIDE',
        'PROCEDURE_AUTORISATION_DEMANDEE',
        'AUTORISATION_OBTENUE',
        'LICENCIEMENT_HORS_PROCEDURE_EN_COURS',
      ],
      criteresManquants: [],
      indemniteForfaitaireMinEur: 0,
      salaireEvictionPotentielEur: 0,
      delaiContestationJours: 60,
      baseJuridique: 'Art. L.2411-1 + L.2411-3 + L.2411-22 + L.2422-1 + R.2422-1 Code du travail',
      formule: 'Salarié protégé (MEMBRE_CSE_TITULAIRE) + procédure AUTORISATION_OBTENUE → score 100 → VALIDE',
      messages: ['Statut protégé reconnu (L.2411-1) ✓'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        ProtectionRpSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ProtectionRpSectionComponent);
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

    expect(component.result()!.scoreConformite).toBe(100);
    expect(component.showForm()).toBe(false);
    expect(component.provenanceMotifLicenciement()).toBeNull();
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

  it('pré-fill IA : motifLicenciement ← aiData.motifLicenciement (cas exact FAUTE_GRAVE)', () => {
    component.aiData = { motifLicenciement: 'FAUTE_GRAVE' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.motifLicenciement()).toBe('FAUTE_GRAVE');
    expect(component.provenanceMotifLicenciement()).toBe('IA');
  });

  it('pré-fill IA : normalisation insensible casse/accents (inaptitude → INAPTITUDE)', () => {
    component.aiData = { motifLicenciement: 'inaptitude' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.motifLicenciement()).toBe('INAPTITUDE');
    expect(component.provenanceMotifLicenciement()).toBe('IA');
  });

  it('pré-fill sans aiData → aucun pré-remplissage, aucun badge', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.motifLicenciement()).toBeNull();
    expect(component.provenanceMotifLicenciement()).toBeNull();
  });

  it('pré-fill avec valeur IA non reconnue → no-op gracieux (aucun pré-remplissage)', () => {
    component.aiData = { motifLicenciement: 'FOO_BAR' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.motifLicenciement()).toBeNull();
    expect(component.provenanceMotifLicenciement()).toBeNull();
  });

  it('onMotifLicenciementChange efface le badge IA', () => {
    component.aiData = { motifLicenciement: 'FAUTE_GRAVE' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceMotifLicenciement()).toBe('IA');
    component.onMotifLicenciementChange('AUTRE');
    expect(component.motifLicenciement()).toBe('AUTRE');
    expect(component.provenanceMotifLicenciement()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true seulement quand tous les champs requis sont présents', () => {
    component.statutProtege.set('MEMBRE_CSE_TITULAIRE');
    component.dateExpirationMandat.set('2026-09-30');
    component.datePresumeeRupture.set('2026-04-15');
    component.procedureSuivie.set('AUTORISATION_OBTENUE');
    component.motifLicenciement.set('FAUTE_GRAVE');
    expect(component.formValid()).toBe(true);

    component.motifLicenciement.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si salaire négatif', () => {
    component.statutProtege.set('MEMBRE_CSE_TITULAIRE');
    component.dateExpirationMandat.set('2026-09-30');
    component.datePresumeeRupture.set('2026-04-15');
    component.procedureSuivie.set('AUTORISATION_OBTENUE');
    component.motifLicenciement.set('FAUTE_GRAVE');
    component.salaireMensuelBrutEur.set(-100);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST + résultat + snackbar succès + triggerRefresh', () => {
    component.statutProtege.set('MEMBRE_CSE_TITULAIRE');
    component.dateExpirationMandat.set('2026-09-30');
    component.datePresumeeRupture.set('2026-04-15');
    component.procedureSuivie.set('AUTORISATION_OBTENUE');
    component.motifLicenciement.set('FAUTE_GRAVE');
    component.salaireMensuelBrutEur.set(3500);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.statutProtege).toBe('MEMBRE_CSE_TITULAIRE');
    expect(req.request.body.salaireMensuelBrutEur).toBe(3500);
    expect(req.request.body.dateExpirationMandat).toBe('2026-09-30');
    req.flush(response());

    expect(component.result()!.verdictLegalite).toBe('VALIDE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Protection RP analysée', 'OK', jasmine.any(Object));
  });

  it('calculate() omet salaireMensuelBrutEur si null', () => {
    component.statutProtege.set('DELEGUE_SYNDICAL');
    component.dateExpirationMandat.set('2026-09-30');
    component.datePresumeeRupture.set('2026-04-15');
    component.procedureSuivie.set('AUCUNE_DEMANDE');
    component.motifLicenciement.set('FAUTE_GRAVE');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.salaireMensuelBrutEur).toBeUndefined();
    req.flush(response({ verdictLegalite: 'NUL', scoreConformite: 0 }));
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.statutProtege.set('MEMBRE_CSE_TITULAIRE');
    component.dateExpirationMandat.set('2026-09-30');
    component.datePresumeeRupture.set('2026-04-15');
    component.procedureSuivie.set('AUTORISATION_OBTENUE');
    component.motifLicenciement.set('FAUTE_GRAVE');
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

  it('coherenceAlerts.MOTIF_LICENCIEMENT présent si IA diverge de saisie', () => {
    component.aiData = { motifLicenciement: 'INAPTITUDE' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit 'INAPTITUDE'. Avocat saisit 'FAUTE_GRAVE' → divergence.
    component.onMotifLicenciementChange('FAUTE_GRAVE');

    const alerts = component.coherenceAlerts();
    expect(alerts.MOTIF_LICENCIEMENT).toBeDefined();
    expect(alerts.MOTIF_LICENCIEMENT!.field).toBe('MOTIF_LICENCIEMENT');
    expect(alerts.MOTIF_LICENCIEMENT!.source).toBe('IA');
    expect(alerts.MOTIF_LICENCIEMENT!.expectedDisplay).toContain('Inaptitude');
  });

  it('coherenceAlerts.MOTIF_LICENCIEMENT absent si IA convergent', () => {
    component.aiData = { motifLicenciement: 'FAUTE_GRAVE' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit 'FAUTE_GRAVE'. L'avocat conserve la même valeur.
    expect(component.motifLicenciement()).toBe('FAUTE_GRAVE');
    expect(component.coherenceAlerts().MOTIF_LICENCIEMENT).toBeUndefined();
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = { motifLicenciement: 'INAPTITUDE' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onMotifLicenciementChange('FAUTE_GRAVE');
    expect(component.coherenceAlerts().MOTIF_LICENCIEMENT).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().MOTIF_LICENCIEMENT).toBeUndefined();
  });

  it('coherenceAlerts multi-sources F96 + IA convergents → MULTI', () => {
    component.aiData = { motifLicenciement: 'INAPTITUDE' } as TravailExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Motif protection RP',
        statut: 'NON_COMPLIANT',
        critereCode: 'PROTECTION_RP_MOTIF',
        expectedValue: 'INAPTITUDE',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Avocat saisit 'FAUTE_GRAVE' → divergence IA + F96 même valeur.
    component.onMotifLicenciementChange('FAUTE_GRAVE');

    const alert = component.coherenceAlerts().MOTIF_LICENCIEMENT;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
    expect(alert!.reason).toContain(' ET ');
  });

  // ============================================================
  // ngOnChanges + non-régression
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = { motifLicenciement: 'ECONOMIQUE' } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.motifLicenciement()).toBe('ECONOMIQUE');
    expect(component.provenanceMotifLicenciement()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onMotifLicenciementChange('AUTRE');
    expect(component.provenanceMotifLicenciement()).toBeNull();

    const newAi = { motifLicenciement: 'ECONOMIQUE' } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.motifLicenciement()).toBe('AUTRE');
    expect(component.provenanceMotifLicenciement()).toBeNull();
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
    expect(component.bannerClass('VALIDE')).toContain('protection-rp-banner--info');
    expect(component.bannerClass('CONTESTABLE')).toContain('protection-rp-banner--warning');
    expect(component.bannerClass('NUL')).toContain('protection-rp-banner--critical');
    expect(component.bannerClass(null)).toContain('protection-rp-banner--info');
  });

  it('critereLabel renvoie le libellé humain ou le code en fallback', () => {
    expect(component.critereLabel('STATUT_PROTEGE_VALIDE')).toContain('Statut protégé');
    expect(component.critereLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(component.critereLabel(null)).toBe('');
  });

  it('motifLabel renvoie le libellé humain ou le code en fallback', () => {
    expect(component.motifLabel('FAUTE_GRAVE')).toBe('Faute grave');
    expect(component.motifLabel('INAPTITUDE')).toBe('Inaptitude');
  });
});
