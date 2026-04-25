import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { OrdonnanceRequeteSectionComponent } from './ordonnance-requete-section.component';
import { OrdonnanceRequeteResponse } from '../../core/models/ordonnance-requete.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('OrdonnanceRequeteSectionComponent', () => {
  let component: OrdonnanceRequeteSectionComponent;
  let fixture: ComponentFixture<OrdonnanceRequeteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/ordonnance-requete-analysis';

  function response(overrides: Partial<OrdonnanceRequeteResponse> = {}): OrdonnanceRequeteResponse {
    return {
      caseFileId: 'case-1',
      motifRequete: 'DETOURNEMENT_PATRIMOINE',
      scoreEligibilite: 100,
      verdictAccordeProbabilite: 'ELEVEE',
      criteresRemplis: [
        'URGENCE_JUSTIFIEE',
        'DEROGATION_CONTRADICTOIRE_JUSTIFIEE',
        'PIECE_JUSTIFICATIVE_FOURNIE',
      ],
      criteresManquants: [],
      delaiTypiqueJoursMin: 5,
      delaiTypiqueJoursMax: 15,
      recoursAdverseDelaiJours: 15,
      baseJuridique: 'Art. 493 + 497 CPC',
      formule: 'Motif DETOURNEMENT_PATRIMOINE + 3 critères remplis → score 100 → ELEVEE',
      messages: ['Critères remplis ✓'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        OrdonnanceRequeteSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(OrdonnanceRequeteSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ============================================================
  // FR + BE actifs (pas de gate pays)
  // ============================================================

  it('FRANCE → GET appelé au ngOnInit', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('BELGIQUE → GET appelé au ngOnInit (pas de gate pays — outil FR + BE actifs)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('BELGIQUE — POST déclenché et réussi (pas de masquage UI)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.motifRequete.set('DETOURNEMENT_PATRIMOINE');
    component.urgenceJustifiee.set(true);
    component.derogationContradictoireJustifiee.set(true);
    component.pieceJustificativeFournie.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    req.flush(response({
      country: 'BELGIQUE',
      baseJuridique: 'Art. 1025 et s. CJ',
    }));

    expect(component.result()!.country).toBe('BELGIQUE');
    expect(component.result()!.baseJuridique).toContain('1025');
  });

  // ============================================================
  // GET 200 / GET 404 hydration
  // ============================================================

  it('charge le résultat existant si GET 200 (mode résultat hydraté)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());

    expect(component.result()!.scoreEligibilite).toBe(100);
    expect(component.showForm()).toBe(false);
    expect(component.provenancePresenceEnfants()).toBeNull();
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

  it('pré-fill IA : presenceEnfants ← aiData.presenceEnfantsDetected=true', () => {
    component.aiData = { presenceEnfantsDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.presenceEnfants()).toBe(true);
    expect(component.provenancePresenceEnfants()).toBe('IA');
  });

  it('pré-fill sans aiData → aucun pré-remplissage, aucun badge', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.presenceEnfants()).toBe(false);
    expect(component.provenancePresenceEnfants()).toBeNull();
  });

  it('onPresenceEnfantsChange efface le badge IA', () => {
    component.aiData = { presenceEnfantsDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenancePresenceEnfants()).toBe('IA');
    component.onPresenceEnfantsChange(false);
    expect(component.presenceEnfants()).toBe(false);
    expect(component.provenancePresenceEnfants()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement (pas de motif)', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true quand motif présent (3 critères booléens optionnels)', () => {
    component.motifRequete.set('DETOURNEMENT_PATRIMOINE');
    expect(component.formValid()).toBe(true);

    component.motifRequete.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si commentaire > 2000 caractères', () => {
    component.motifRequete.set('DETOURNEMENT_PATRIMOINE');
    component.commentaireUrgence.set('a'.repeat(2001));
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST + résultat + snackbar succès', () => {
    component.motifRequete.set('ENLEVEMENT_INTERNATIONAL');
    component.urgenceJustifiee.set(true);
    component.derogationContradictoireJustifiee.set(true);
    component.pieceJustificativeFournie.set(true);
    component.presenceEnfants.set(true);
    component.commentaireUrgence.set('Risque imminent');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.motifRequete).toBe('ENLEVEMENT_INTERNATIONAL');
    expect(req.request.body.urgenceJustifiee).toBe(true);
    expect(req.request.body.presenceEnfants).toBe(true);
    expect(req.request.body.commentaireUrgence).toBe('Risque imminent');
    req.flush(response({
      motifRequete: 'ENLEVEMENT_INTERNATIONAL',
      delaiTypiqueJoursMin: 1,
      delaiTypiqueJoursMax: 3,
    }));

    expect(component.result()!.delaiTypiqueJoursMin).toBe(1);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Ordonnance sur requête analysée', 'OK', jasmine.any(Object));
  });

  it('calculate() omet commentaireUrgence si vide ou whitespace', () => {
    component.motifRequete.set('DETOURNEMENT_PATRIMOINE');
    component.urgenceJustifiee.set(true);
    component.derogationContradictoireJustifiee.set(true);
    component.pieceJustificativeFournie.set(true);
    component.commentaireUrgence.set('   ');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.commentaireUrgence).toBeUndefined();
    req.flush(response());
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.motifRequete.set('DETOURNEMENT_PATRIMOINE');
    component.urgenceJustifiee.set(true);
    component.derogationContradictoireJustifiee.set(true);
    component.pieceJustificativeFournie.set(true);
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

  it('coherenceAlerts.PRESENCE_ENFANTS présent si IA dit true et avocat saisit false', () => {
    component.aiData = { presenceEnfantsDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit `true`. Avocat décoche → false → divergence.
    component.onPresenceEnfantsChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.PRESENCE_ENFANTS).toBeDefined();
    expect(alerts.PRESENCE_ENFANTS!.field).toBe('PRESENCE_ENFANTS');
    expect(alerts.PRESENCE_ENFANTS!.source).toBe('IA');
    expect(alerts.PRESENCE_ENFANTS!.expectedDisplay).toBe('Oui');
  });

  it('coherenceAlerts.PRESENCE_ENFANTS absent si IA convergent', () => {
    component.aiData = { presenceEnfantsDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit `true`. Conserve → pas de divergence.
    expect(component.presenceEnfants()).toBe(true);
    expect(component.coherenceAlerts().PRESENCE_ENFANTS).toBeUndefined();
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = { presenceEnfantsDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onPresenceEnfantsChange(false);
    expect(component.coherenceAlerts().PRESENCE_ENFANTS).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().PRESENCE_ENFANTS).toBeUndefined();
  });

  it('coherenceAlerts multi-sources F96 + IA convergents → MULTI', () => {
    component.aiData = { presenceEnfantsDetected: true } as FamilleExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Présence enfants',
        statut: 'NON_COMPLIANT',
        critereCode: 'PRESENCE_ENFANTS',
        expectedValue: 'true',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Avocat saisit `false` → divergence IA + F96 même valeur attendue (true).
    component.onPresenceEnfantsChange(false);

    const alert = component.coherenceAlerts().PRESENCE_ENFANTS;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
    expect(alert!.reason).toContain(' ET ');
  });

  // ============================================================
  // URGENT chip + bandeau
  // ============================================================

  it('isUrgent true si motif ENLEVEMENT_INTERNATIONAL (form mode)', () => {
    component.motifRequete.set('ENLEVEMENT_INTERNATIONAL');
    expect(component.isUrgent()).toBe(true);
  });

  it('isUrgent true si presenceEnfants=true même sur autre motif', () => {
    component.motifRequete.set('DETOURNEMENT_PATRIMOINE');
    component.presenceEnfants.set(true);
    expect(component.isUrgent()).toBe(true);
  });

  it('isUrgent false si motif non-IST + pas d\'enfants', () => {
    component.motifRequete.set('ACCES_PIECES');
    component.presenceEnfants.set(false);
    expect(component.isUrgent()).toBe(false);
  });

  it('isUrgentResult true si motif ENLEVEMENT_INTERNATIONAL en résultat', () => {
    component.result.set(response({
      motifRequete: 'ENLEVEMENT_INTERNATIONAL',
      delaiTypiqueJoursMin: 1,
      delaiTypiqueJoursMax: 3,
    }));
    expect(component.isUrgentResult()).toBe(true);
  });

  it('isUrgentResult true si délai 1-3 jours (IST forcé par enfants)', () => {
    component.result.set(response({
      motifRequete: 'ACCES_PIECES',
      delaiTypiqueJoursMin: 1,
      delaiTypiqueJoursMax: 3,
    }));
    expect(component.isUrgentResult()).toBe(true);
  });

  it('isUrgentResult false si délai 5-15 jours non-IST', () => {
    component.result.set(response({
      delaiTypiqueJoursMin: 5,
      delaiTypiqueJoursMax: 15,
    }));
    expect(component.isUrgentResult()).toBe(false);
  });

  // ============================================================
  // ngOnChanges + non-régression
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = { presenceEnfantsDetected: true } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.presenceEnfants()).toBe(true);
    expect(component.provenancePresenceEnfants()).toBe('IA');
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
    expect(component.bannerClass('ELEVEE')).toContain('ord-requete-banner--info');
    expect(component.bannerClass('MOYENNE')).toContain('ord-requete-banner--warning');
    expect(component.bannerClass('FAIBLE')).toContain('ord-requete-banner--critical');
    expect(component.bannerClass(null)).toContain('ord-requete-banner--info');
  });

  it('motifLabel renvoie le libellé humain ou le code en fallback', () => {
    expect(component.motifLabel('ENLEVEMENT_INTERNATIONAL')).toContain('enlèvement');
    expect(component.motifLabel('UNKNOWN_CODE' as any)).toBe('UNKNOWN_CODE');
    expect(component.motifLabel(null)).toBe('');
  });

  it('critereLabel renvoie le libellé humain ou le code en fallback', () => {
    expect(component.critereLabel('URGENCE_JUSTIFIEE')).toContain('Urgence');
    expect(component.critereLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(component.critereLabel(null)).toBe('');
  });
});
