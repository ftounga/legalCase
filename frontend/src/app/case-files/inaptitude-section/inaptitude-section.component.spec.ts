import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { InaptitudeSectionComponent } from './inaptitude-section.component';
import { InaptitudeResponse } from '../../core/models/inaptitude.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('InaptitudeSectionComponent', () => {
  let component: InaptitudeSectionComponent;
  let fixture: ComponentFixture<InaptitudeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/inaptitude';

  function frResponse(): InaptitudeResponse {
    return {
      caseFileId: 'case-1',
      salaireMensuelReference: 3000,
      ancienneteAnnees: 5,
      origineInaptitude: 'PROFESSIONNELLE',
      reclassementRespecte: true,
      avisMedecinTravailDate: '2026-01-15',
      country: 'FRANCE',
      indemniteLegale: 7500,
      indemniteCompensatricePreavis: 6000,
      damagesReclassement: 0,
      total: 13500,
      formule: 'Indemnité légale doublée + préavis (origine pro)',
      baseJuridique: 'Art. L1226-14 Code du travail',
      messages: ['Indemnité légale doublée pour inaptitude professionnelle'],
    };
  }

  function fullAiData(): TravailExtractedData {
    return {
      salaireBrutMensuel: 3000,
      dateEntree: '2020-01-01',
      origineInaptitudePressentie: 'ACCIDENT_TRAVAIL',
      avisMedecinTravailDate: '2026-01-15',
      reclassementRespecteDetected: { reponse: 'OUI', justification: 'Recherche documentée' },
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        InaptitudeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(InaptitudeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ==========================================================================
  // Tests existants conservés (pré-SF-155-04-A2)
  // ==========================================================================

  it('FRANCE → 2 origines FR disponibles', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.originesDisponibles().length).toBe(2);
    const codes = component.originesDisponibles().map(o => o.code);
    expect(codes).toContain('PROFESSIONNELLE');
    expect(codes).toContain('NON_PROFESSIONNELLE');
  });

  it('BELGIQUE → 2 origines BE disponibles', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.originesDisponibles().length).toBe(2);
    const codes = component.originesDisponibles().map(o => o.code);
    expect(codes).toContain('PROFESSIONNELLE_BE');
    expect(codes).toContain('NON_PROFESSIONNELLE_BE');
  });

  it('charge l\'analyse existante si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(frResponse());
    expect(component.result()!.total).toBe(13500);
    expect(component.showForm()).toBe(false);
    expect(component.salaireMensuelReference()).toBe(3000);
    expect(component.ancienneteAnnees()).toBe(5);
    expect(component.origineInaptitude()).toBe('PROFESSIONNELLE');
    expect(component.reclassementRespecte()).toBe(true);
    expect(component.avisMedecinTravailDate()).toBe('2026-01-15');
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false si salaire ou ancienneté ou origine manquants', () => {
    component.salaireMensuelReference.set(null);
    component.ancienneteAnnees.set(5);
    component.origineInaptitude.set('PROFESSIONNELLE');
    expect(component.formValid()).toBe(false);

    component.salaireMensuelReference.set(3000);
    component.ancienneteAnnees.set(null);
    expect(component.formValid()).toBe(false);

    component.ancienneteAnnees.set(5);
    component.origineInaptitude.set(null);
    expect(component.formValid()).toBe(false);

    component.origineInaptitude.set('PROFESSIONNELLE');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si salaire ≤ 0 ou ancienneté non entière ou négative', () => {
    component.salaireMensuelReference.set(0);
    component.ancienneteAnnees.set(5);
    component.origineInaptitude.set('PROFESSIONNELLE');
    expect(component.formValid()).toBe(false);

    component.salaireMensuelReference.set(3000);
    component.ancienneteAnnees.set(-1);
    expect(component.formValid()).toBe(false);

    component.ancienneteAnnees.set(5.5);
    expect(component.formValid()).toBe(false);

    component.ancienneteAnnees.set(0);
    expect(component.formValid()).toBe(true);
  });

  it('calculate() POST + affiche résultat + snackbar succès + omet date si vide', () => {
    component.salaireMensuelReference.set(3000);
    component.ancienneteAnnees.set(5);
    component.origineInaptitude.set('PROFESSIONNELLE');
    component.reclassementRespecte.set(true);
    component.avisMedecinTravailDate.set(null);
    component.calculate();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      salaireMensuelReference: 3000,
      ancienneteAnnees: 5,
      origineInaptitude: 'PROFESSIONNELLE',
      reclassementRespecte: true,
    });
    req.flush(frResponse());

    expect(component.result()!.total).toBe(13500);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Indemnité calculée', 'OK', jasmine.any(Object));
  });

  it('calculate() inclut avisMedecinTravailDate si renseignée', () => {
    component.salaireMensuelReference.set(3000);
    component.ancienneteAnnees.set(5);
    component.origineInaptitude.set('PROFESSIONNELLE');
    component.reclassementRespecte.set(true);
    component.avisMedecinTravailDate.set('2026-01-15');
    component.calculate();

    const req = httpMock.expectOne(r => r.method === 'POST');
    expect(req.request.body.avisMedecinTravailDate).toBe('2026-01-15');
    req.flush(frResponse());
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.salaireMensuelReference.set(3000);
    component.ancienneteAnnees.set(5);
    component.origineInaptitude.set('PROFESSIONNELLE');
    component.calculate();

    const req = httpMock.expectOne(r => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.salaireMensuelReference.set(null);
    component.ancienneteAnnees.set(null);
    component.origineInaptitude.set(null);
    component.calculate();
    httpMock.expectNone(r => r.method === 'POST');
  });

  // ==========================================================================
  // SF-155-04-A2 : tests pré-fill IA + validation F-IA-03
  // ==========================================================================

  it('pré-fill complet depuis aiData (5 champs + badges IA) sur 404', () => {
    component.aiData = fullAiData();
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

    expect(component.salaireMensuelReference()).toBe(3000);
    expect(component.provenanceSalaire()).toBe('IA');
    expect(component.origineInaptitude()).toBe('PROFESSIONNELLE');
    expect(component.provenanceOrigineInaptitude()).toBe('IA');
    expect(component.avisMedecinTravailDate()).toBe('2026-01-15');
    expect(component.provenanceAvisMedecinDate()).toBe('IA');
    expect(component.reclassementRespecte()).toBe(true);
    expect(component.provenanceReclassement()).toBe('IA');
    expect(component.ancienneteAnnees()).toBeGreaterThanOrEqual(6);
    expect(component.provenanceAnciennete()).toBe('IA');
  });

  it('pré-fill partiel : seul salaireBrutMensuel et origineInaptitudePressentie remplis', () => {
    component.aiData = {
      salaireBrutMensuel: 2500,
      origineInaptitudePressentie: 'MALADIE_ORDINAIRE',
    };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.salaireMensuelReference()).toBe(2500);
    expect(component.provenanceSalaire()).toBe('IA');
    expect(component.origineInaptitude()).toBe('NON_PROFESSIONNELLE');
    expect(component.provenanceOrigineInaptitude()).toBe('IA');
    expect(component.ancienneteAnnees()).toBeNull();
    expect(component.provenanceAnciennete()).toBeNull();
    expect(component.avisMedecinTravailDate()).toBeNull();
    expect(component.provenanceAvisMedecinDate()).toBeNull();
    expect(component.provenanceReclassement()).toBeNull();
  });

  it('pas d\'écrasement si GET 200 réussit (analyse déjà persistée)', () => {
    component.aiData = fullAiData();
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());

    // Les valeurs viennent de l'API (backend), pas de l'IA.
    expect(component.showForm()).toBe(false);
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.provenanceOrigineInaptitude()).toBeNull();
    expect(component.provenanceAvisMedecinDate()).toBeNull();
    expect(component.provenanceReclassement()).toBeNull();
    expect(component.provenanceAnciennete()).toBeNull();
  });

  it('origineInaptitudePressentie hors enum whitelist ignorée (pas de mapping)', () => {
    component.aiData = { origineInaptitudePressentie: 'FOO_BAR' as any };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.origineInaptitude()).toBeNull();
    expect(component.provenanceOrigineInaptitude()).toBeNull();
  });

  it('pré-fill skip salaire si salaireBrutMensuel ≤ 0', () => {
    component.aiData = { salaireBrutMensuel: 0 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.salaireMensuelReference()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('modification manuelle salaire → provenanceSalaire = null', () => {
    component.aiData = fullAiData();
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.provenanceSalaire()).toBe('IA');

    component.onSalaireChange(3500);
    expect(component.salaireMensuelReference()).toBe(3500);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('modifications manuelles (4 autres champs) → provenance correspondante = null', () => {
    component.aiData = fullAiData();
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onAncienneteChange(7);
    expect(component.provenanceAnciennete()).toBeNull();

    component.onOrigineChange('NON_PROFESSIONNELLE');
    expect(component.provenanceOrigineInaptitude()).toBeNull();

    component.onAvisMedecinDateChange('2026-02-01');
    expect(component.provenanceAvisMedecinDate()).toBeNull();

    component.onReclassementChange(false);
    expect(component.provenanceReclassement()).toBeNull();
  });

  it('divergence salaire > 10 % → coherenceAlerts().SALAIRE défini', () => {
    component.aiData = { salaireBrutMensuel: 3000 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // L'IA a pré-rempli à 3000, on passe à 3400 (écart ~13 %)
    component.onSalaireChange(3400);

    const alert = component.coherenceAlerts().SALAIRE;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('SALAIRE');
    expect(alert!.source).toBe('IA');
  });

  it('pas d\'alerte salaire si écart ≤ 10 %', () => {
    component.aiData = { salaireBrutMensuel: 3000 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onSalaireChange(3200); // ~6,67 %
    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  it('divergence origine inaptitude → coherenceAlerts().ORIGINE défini', () => {
    component.aiData = { origineInaptitudePressentie: 'ACCIDENT_TRAVAIL' };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.origineInaptitude()).toBe('PROFESSIONNELLE');
    component.onOrigineChange('NON_PROFESSIONNELLE');

    const alert = component.coherenceAlerts().ORIGINE;
    expect(alert).toBeDefined();
    expect(alert!.expectedDisplay).toContain('professionnelle');
  });

  it('divergence date avis médecin → coherenceAlerts().AVIS_DATE défini', () => {
    component.aiData = { avisMedecinTravailDate: '2026-01-10' };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onAvisMedecinDateChange('2026-02-01');
    const alert = component.coherenceAlerts().AVIS_DATE;
    expect(alert).toBeDefined();
    expect(alert!.expectedDisplay).toBe('2026-01-10');
  });

  it('contradiction reclassement avocat=true vs IA=NON → coherenceAlerts().RECLASSEMENT défini', () => {
    component.aiData = {
      reclassementRespecteDetected: { reponse: 'NON', justification: 'Aucune recherche documentée' },
    };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // IA a pré-rempli à false, on coche true
    component.onReclassementChange(true);
    const alert = component.coherenceAlerts().RECLASSEMENT;
    expect(alert).toBeDefined();
    expect(alert!.expectedDisplay).toContain('NON');
  });

  it('INCONNU → pas de prefill reclassement + pas d\'alerte', () => {
    component.aiData = {
      reclassementRespecteDetected: { reponse: 'INCONNU' },
    };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceReclassement()).toBeNull();
    expect(component.coherenceAlerts().RECLASSEMENT).toBeUndefined();
  });

  it('salaireEstDeduit=true → salaireEstDeduitNote() = true', () => {
    component.aiData = { salaireBrutMensuel: 3000, salaireEstDeduit: true };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.salaireEstDeduitNote()).toBe(true);
  });

  it('aucun aiData + 404 → form vide, aucune provenance, aucune alerte', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.salaireMensuelReference()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.origineInaptitude()).toBeNull();
    expect(component.provenanceOrigineInaptitude()).toBeNull();
    expect(Object.keys(component.coherenceAlerts()).length).toBe(0);
  });

  it('ngOnChanges(aiData) re-applique prefillFromAi si showForm et pas de result', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.salaireMensuelReference()).toBeNull();

    // Simule un changement d'input aiData post-init (ex. analyse IA qui arrive)
    component.aiData = fullAiData();
    component.ngOnChanges({
      aiData: new SimpleChange(null, fullAiData(), false),
    });

    expect(component.salaireMensuelReference()).toBe(3000);
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('calcul ancienneté depuis dateEntree (années entières floor)', () => {
    // On fige la référence temporelle autour de 2026-04-24 (aujourd'hui au
    // moment de l'écriture de la SF) : dateEntree 2020-01-01 → 6 ans révolus.
    component.aiData = { dateEntree: '2020-01-01' };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const computed = component.ancienneteAnnees();
    expect(computed).not.toBeNull();
    expect(computed! >= 5 && computed! <= 10).toBe(true);
    expect(Number.isInteger(computed!)).toBe(true);
    expect(component.provenanceAnciennete()).toBe('IA');
  });

  it('BELGIQUE : origineInaptitudePressentie FR ignorée (no prefill, no alerte)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = { origineInaptitudePressentie: 'ACCIDENT_TRAVAIL' };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Côté BE, le mapping FR ne s'applique pas (l'enum BE n'est pas encore
    // extrait par l'IA — cf. mini-spec backend §2.2).
    expect(component.origineInaptitude()).toBeNull();
    expect(component.provenanceOrigineInaptitude()).toBeNull();

    component.onOrigineChange('PROFESSIONNELLE_BE');
    expect(component.coherenceAlerts().ORIGINE).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // SF-155-05 — interface `CoherenceAlert<InaptitudeAlertField>` partagée
  // ---------------------------------------------------------------------------

  it('SF-155-05 : alerte SALAIRE expose contract CoherenceAlert — contributors=[IA], severity=WARNING', () => {
    component.aiData = { salaireBrutMensuel: 3000 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onSalaireChange(5000); // 66 % d'écart
    const alert = component.coherenceAlerts().SALAIRE;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('SALAIRE');
    expect(alert!.source).toBe('IA');
    expect(alert!.contributors).toEqual(['IA']);
    expect(alert!.severity).toBe('WARNING');
  });

  it('SF-155-05 : alertBadgeLabel + alertTooltip fonctionnent avec la nouvelle interface', () => {
    component.aiData = { salaireBrutMensuel: 3000 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onSalaireChange(5000);
    const alert = component.coherenceAlerts().SALAIRE!;
    expect(component.alertBadgeLabel(alert)).toContain('Incohérence');
    expect(component.alertTooltip(alert)).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // SF-155-06 — enrichissement 4-sources (ferme DIV-2)
  // ---------------------------------------------------------------------------

  it('SF-155-06 : F96 seul → alerte ORIGINE avec source F96', () => {
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Origine inaptitude',
        statut: 'NON_COMPLIANT',
        critereCode: 'INAPT_ORIGINE',
        expectedValue: 'PROFESSIONNELLE',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onOrigineChange('NON_PROFESSIONNELLE');
    const alert = component.coherenceAlerts().ORIGINE;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('F96');
    expect(alert!.contributors).toEqual(['F96']);
    expect(alert!.expectedDisplay).toContain('professionnelle');
  });

  it('SF-155-06 : QUESTION_IA seule (réponse oui) sur RECLASSEMENT → alerte QUESTION_IA', () => {
    component.aiQuestions = [
      {
        id: 'q-1', orderIndex: 1,
        questionText: 'L\'employeur a-t-il respecté l\'obligation de reclassement ?',
        answerText: 'oui, recherche documentée',
        critereCode: 'INAPT_RECLASSEMENT',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Avocat dit "reclassement non respecté" (false) alors que la question dit oui.
    component.onReclassementChange(false);
    const alert = component.coherenceAlerts().RECLASSEMENT;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('QUESTION_IA');
    expect(alert!.contributors).toEqual(['QUESTION_IA']);
    expect(alert!.expectedDisplay).toContain('respecté');
  });

  it('SF-155-06 : IA + F96 convergents sur ORIGINE → alerte MULTI avec 2 contributors', () => {
    component.aiData = { origineInaptitudePressentie: 'ACCIDENT_TRAVAIL' } as TravailExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Origine inaptitude',
        statut: 'NON_COMPLIANT',
        critereCode: 'INAPT_ORIGINE',
        expectedValue: 'PROFESSIONNELLE',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onOrigineChange('NON_PROFESSIONNELLE');
    const alert = component.coherenceAlerts().ORIGINE;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors.length).toBe(2);
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
    expect(alert!.reason).toContain(' ET ');
  });

  it('SF-155-06 : IA + PIECE_MANQUANTE sur RECLASSEMENT → contributors inclut PIECE_MANQUANTE + pieceTexte', () => {
    component.aiData = {
      reclassementRespecteDetected: { reponse: 'NON', justification: 'Aucune proposition' },
    } as TravailExtractedData;
    component.piecesManquantes = [
      { texte: 'Courrier de proposition reclassement', critereCode: 'INAPT_RECLASSEMENT' },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Avocat dit true (respecté), IA dit NON → divergence.
    component.onReclassementChange(true);
    const alert = component.coherenceAlerts().RECLASSEMENT;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('IA');
    expect(alert!.contributors).toContain('PIECE_MANQUANTE');
    expect(alert!.pieceTexte).toBe('Courrier de proposition reclassement');
  });
});
