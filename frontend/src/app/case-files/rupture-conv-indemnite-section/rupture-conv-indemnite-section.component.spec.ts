import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange, SimpleChanges } from '@angular/core';
import { RuptureConvIndemniteSectionComponent } from './rupture-conv-indemnite-section.component';
import {
  CaseAnalysisResult,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';

const BASE_URL = '/api/v1/case-files/case-1/rupture-conv-indemnite';
const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

function createSynthesis(overrides: Partial<CaseAnalysisResult['compensationEstimate']>): CaseAnalysisResult {
  return {
    compensationEstimate: {
      indemnite: 0,
      salaireReference: 2979,
      ancienneteAnnees: 4,
      ancienneteMois: 0,
      typeRupture: 'RUPTURE_CONVENTIONNELLE',
      plafondMinMois: 0,
      plafondMaxMois: 0,
      donneesPartielles: false,
      ...(overrides ?? {}),
    },
  } as CaseAnalysisResult;
}

describe('RuptureConvIndemniteSectionComponent', () => {
  let component: RuptureConvIndemniteSectionComponent;
  let fixture: ComponentFixture<RuptureConvIndemniteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  /**
   * SF-155-12 : absorbe la requête source-explanations émise par ngOnInit.
   * Empêche les tests de crash sur httpMock.verify().
   */
  function expectSourceExplanationCall(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [RuptureConvIndemniteSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RuptureConvIndemniteSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Tests existants SF-132-02 (non-régression).
  // ---------------------------------------------------------------------------

  it('charge l\'analyse existante si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({
      caseFileId: 'case-1', ancienneteAnnees: 4, salaireMensuel: 2979,
      indemniteLegaleMinimum: 2979, formule: '¼ × 4 ans × 2979 €',
      baseJuridique: 'Art. R1234-2 Code du travail', messages: ['L1237-13'],
    });
    expectSourceExplanationCall();
    expect(component.result()!.indemniteLegaleMinimum).toBe(2979);
    expect(component.showForm()).toBe(false);
  });

  it('reste en mode formulaire si GET 404, puis prefill IA', () => {
    component.synthesis = createSynthesis({});
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.showForm()).toBe(true);
    expect(component.ancienneteAnnees()).toBe(4);
    expect(component.salaireMensuel()).toBe(2979);
  });

  it('formValid false si inputs manquants ou négatifs', () => {
    component.ancienneteAnnees.set(null);
    component.salaireMensuel.set(3000);
    expect(component.formValid()).toBe(false);

    component.ancienneteAnnees.set(5);
    component.salaireMensuel.set(0);
    expect(component.formValid()).toBe(false);

    component.ancienneteAnnees.set(5);
    component.salaireMensuel.set(3000);
    expect(component.formValid()).toBe(true);

    component.ancienneteAnnees.set(-1);
    component.salaireMensuel.set(3000);
    expect(component.formValid()).toBe(false);
  });

  it('calculate() POST + affiche résultat + snackbar succès', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.ancienneteAnnees.set(4);
    component.salaireMensuel.set(2979);
    component.calculate();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({ ancienneteAnnees: 4, salaireMensuel: 2979 });
    req.flush({
      caseFileId: 'case-1', ancienneteAnnees: 4, salaireMensuel: 2979,
      indemniteLegaleMinimum: 2979, formule: '¼ × 4 ans × 2979 €',
      baseJuridique: 'Art. R1234-2 Code du travail', messages: [],
    });

    expect(component.result()!.indemniteLegaleMinimum).toBe(2979);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Indemnité calculée', 'OK', jasmine.any(Object));
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.ancienneteAnnees.set(5);
    component.salaireMensuel.set(3000);
    component.calculate();

    const req = httpMock.expectOne(r => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(jasmine.any(String), 'Fermer', jasmine.objectContaining({ panelClass: 'snack-error' }));
    expect(component.calculating()).toBe(false);
  });

  it('ignore le prefill IA si des inputs sont déjà remplis (GET 200 prioritaire)', () => {
    // L'analyse GET 200 signale "déjà pré-rempli depuis la persistance"
    component.synthesis = createSynthesis({ ancienneteAnnees: 2, salaireReference: 1500 });
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({
      caseFileId: 'case-1', ancienneteAnnees: 10, salaireMensuel: 4000,
      indemniteLegaleMinimum: 10000, formule: '…',
      baseJuridique: 'Art. R1234-2', messages: [],
    });
    expectSourceExplanationCall();

    expect(component.ancienneteAnnees()).toBe(10);
    expect(component.salaireMensuel()).toBe(4000);
    // SF-155-12 : pas de badge IA pour des valeurs persistées.
    expect(component.provenanceAncienneteAnnees()).toBeNull();
    expect(component.provenanceSalaireMensuel()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // SF-155-12 : pré-fill IA + validation F-IA-03 (nouveaux tests).
  // ---------------------------------------------------------------------------

  it('SF-155-12 : prefill IA (aiData.salaireBrutMensuel + synthesis.compensationEstimate) + GET 404 → badges IA', () => {
    component.synthesis = createSynthesis({});
    component.aiData = { salaireBrutMensuel: 3100 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // Salaire IA prioritaire sur compensationEstimate.salaireReference.
    expect(component.salaireMensuel()).toBe(3100);
    expect(component.ancienneteAnnees()).toBe(4);
    expect(component.provenanceSalaireMensuel()).toBe('IA');
    expect(component.provenanceAncienneteAnnees()).toBe('IA');
  });

  it('SF-155-12 : prefill sans aiData ni synthesis → pas de pré-remplissage, aucun badge', () => {
    component.aiData = null;
    component.synthesis = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireMensuel()).toBeNull();
    expect(component.ancienneteAnnees()).toBeNull();
    expect(component.provenanceSalaireMensuel()).toBeNull();
    expect(component.provenanceAncienneteAnnees()).toBeNull();
  });

  it('SF-155-12 : aiData.salaireBrutMensuel ≤ 0 → fallback sur synthesis.salaireReference', () => {
    component.synthesis = createSynthesis({ salaireReference: 2500 });
    component.aiData = { salaireBrutMensuel: 0 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // Fallback compensationEstimate quand aiData absent / invalide.
    expect(component.salaireMensuel()).toBe(2500);
    expect(component.provenanceSalaireMensuel()).toBe('IA');
  });

  it('SF-155-12 : onAncienneteAnneesChange efface le badge IA', () => {
    component.synthesis = createSynthesis({});
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.provenanceAncienneteAnnees()).toBe('IA');

    component.onAncienneteAnneesChange(7);
    expect(component.ancienneteAnnees()).toBe(7);
    expect(component.provenanceAncienneteAnnees()).toBeNull();
  });

  it('SF-155-12 : onSalaireMensuelChange efface le badge IA', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.provenanceSalaireMensuel()).toBe('IA');

    component.onSalaireMensuelChange(3500);
    expect(component.salaireMensuel()).toBe(3500);
    expect(component.provenanceSalaireMensuel()).toBeNull();
  });

  it('SF-155-12 : coherenceAlerts.SALAIRE présent si divergence > 10 % vs aiData', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // Pré-fill = 3000 → on saisit 5000 (66 % d'écart).
    component.onSalaireMensuelChange(5000);

    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE).toBeDefined();
    expect(alerts.SALAIRE!.field).toBe('SALAIRE');
    expect(alerts.SALAIRE!.source).toBe('IA');
    expect(alerts.SALAIRE!.expectedDisplay).toContain('€');
    expect(alerts.SALAIRE!.severity).toBe('WARNING');
  });

  it('SF-155-12 : coherenceAlerts.SALAIRE absent si écart ≤ 10 %', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // Salaire IA = 3000, avocat saisit 3100 → 3.3 % d'écart.
    component.onSalaireMensuelChange(3100);

    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  it('SF-155-12 : coherenceAlerts.ANCIENNETE présent si divergence ≥ 1 an vs synthesis', () => {
    component.synthesis = createSynthesis({ ancienneteAnnees: 4 });
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // Pré-fill = 4 → on saisit 10 (écart 6 ans).
    component.onAncienneteAnneesChange(10);

    const alerts = component.coherenceAlerts();
    expect(alerts.ANCIENNETE).toBeDefined();
    expect(alerts.ANCIENNETE!.field).toBe('ANCIENNETE');
    expect(alerts.ANCIENNETE!.source).toBe('IA');
    expect(alerts.ANCIENNETE!.expectedDisplay).toContain('4');
  });

  it('SF-155-12 : coherenceAlerts.ANCIENNETE absent si écart < 1 an', () => {
    component.synthesis = createSynthesis({ ancienneteAnnees: 4 });
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onAncienneteAnneesChange(4); // identique → pas d'alerte

    expect(component.coherenceAlerts().ANCIENNETE).toBeUndefined();
  });

  it('SF-155-12 : F96 + IA convergents salaire → alerte MULTI avec 2 contributors', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Salaire de référence',
        statut: 'NON_COMPLIANT',
        critereCode: 'RCI_SALAIRE',
        expectedValue: '3000',
      } as any,
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireMensuelChange(5000); // divergent vs 3000

    const alert = component.coherenceAlerts().SALAIRE;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors.length).toBe(2);
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
    expect(alert!.reason).toContain(' ET ');
  });

  it('SF-155-12 : QUESTION_IA "oui" sur ancienneté → alerte ANCIENNETE source MULTI', () => {
    component.synthesis = createSynthesis({ ancienneteAnnees: 4 });
    component.aiQuestions = [
      {
        id: 'q-1', orderIndex: 1,
        questionText: 'Le salarié a-t-il bien 4 ans d\'ancienneté ?',
        answerText: 'oui, depuis 2021',
        critereCode: 'RCI_ANCIENNETE',
        expectedValue: '4',
      } as any,
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onAncienneteAnneesChange(10);

    const alert = component.coherenceAlerts().ANCIENNETE;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('QUESTION_IA');
    expect(alert!.contributors).toContain('IA');
  });

  it('SF-155-12 : PIECE_MANQUANTE seule sur salaire ne déclenche PAS d\'alerte', () => {
    component.piecesManquantes = [
      { texte: 'Bulletins de paie 12 derniers mois', critereCode: 'RCI_SALAIRE' },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireMensuelChange(3000);

    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  it('SF-155-12 : IA + PIECE_MANQUANTE salaire → alerte avec pieceTexte rempli', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.piecesManquantes = [
      { texte: 'Bulletins de paie 12 derniers mois', critereCode: 'RCI_SALAIRE' },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireMensuelChange(5000); // divergent

    const alert = component.coherenceAlerts().SALAIRE;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('IA');
    expect(alert!.contributors).toContain('PIECE_MANQUANTE');
    expect(alert!.pieceTexte).toBe('Bulletins de paie 12 derniers mois');
  });

  it('SF-155-12 : alertes masquées après résultat affiché (showForm=false)', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireMensuelChange(5000);
    expect(component.coherenceAlerts().SALAIRE).toBeDefined();

    // Après calcul, form masqué → alertes doivent disparaître.
    component.showForm.set(false);
    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  it('SF-155-12 : ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.synthesis = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireMensuel()).toBeNull();

    // Nouvel aiData arrive tardivement.
    const newAi = { salaireBrutMensuel: 2800 } as TravailExtractedData;
    component.aiData = newAi;
    const changes: SimpleChanges = {
      aiData: new SimpleChange(null, newAi, false),
    };
    component.ngOnChanges(changes);

    expect(component.salaireMensuel()).toBe(2800);
    expect(component.provenanceSalaireMensuel()).toBe('IA');
  });

  it('SF-155-12 : alertBadgeLabel et alertTooltip fonctionnent avec la nouvelle interface', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireMensuelChange(5000);

    const alert = component.coherenceAlerts().SALAIRE!;
    expect(component.alertBadgeLabel(alert)).toContain('Incohérence');
    expect(component.alertTooltip(alert)).toBeTruthy();
  });

  it('SF-155-12 : explanationFor mappe field → sourceKey', () => {
    expect(component.explanationFor('SALAIRE')).toEqual([]);
    expect(component.explanationFor('ANCIENNETE')).toEqual([]);
  });
});
