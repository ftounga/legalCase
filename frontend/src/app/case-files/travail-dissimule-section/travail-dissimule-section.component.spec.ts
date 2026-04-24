import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange, SimpleChanges } from '@angular/core';
import { TravailDissimuleSectionComponent } from './travail-dissimule-section.component';
import { TravailDissimuleResponse } from '../../core/models/travail-dissimule.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('TravailDissimuleSectionComponent', () => {
  let component: TravailDissimuleSectionComponent;
  let fixture: ComponentFixture<TravailDissimuleSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/travail-dissimule';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function okResponse(): TravailDissimuleResponse {
    return {
      caseFileId: 'case-1',
      salaireMensuelReference: 2500,
      indemniteForfaitaire: 15000,
      formule: '6 mois × 2 500,00 € = 15 000,00 €',
      baseJuridique: 'Art. L.8223-1 Code du travail',
      messages: [
        'Indemnité forfaitaire cumulable avec les indemnités de rupture (licenciement, préavis, congés payés) selon la jurisprudence constante (Cass. soc., ch. mixte, 26 mars 2010).',
        "Condition d'application : rupture de la relation de travail ET infraction caractérisée (intention de l'employeur, méconnaissance L.8221-3 non-déclaration URSSAF ou L.8221-5 dissimulation d'heures).",
        "Non cumulable avec l'indemnité forfaitaire pour défaut de visite médicale (art. L.4624-1).",
      ],
    };
  }

  /** Absorbe la requête source-explanations émise par ngOnInit. */
  function expectSourceExplanationCall(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        TravailDissimuleSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(TravailDissimuleSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Non-régression — chargement et formulaire de base
  // ---------------------------------------------------------------------------

  it('charge l\'analyse existante si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(okResponse());
    expectSourceExplanationCall();
    expect(component.result()!.indemniteForfaitaire).toBe(15000);
    expect(component.showForm()).toBe(false);
    expect(component.salaireMensuelReference()).toBe(2500);
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false si salaire null ou ≤ 0', () => {
    component.salaireMensuelReference.set(null);
    expect(component.formValid()).toBe(false);
    component.salaireMensuelReference.set(0);
    expect(component.formValid()).toBe(false);
    component.salaireMensuelReference.set(-100);
    expect(component.formValid()).toBe(false);
    component.salaireMensuelReference.set(2500);
    expect(component.formValid()).toBe(true);
  });

  it('calculate() POST + affiche résultat + snackbar succès', () => {
    component.salaireMensuelReference.set(2500);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({ salaireMensuelReference: 2500 });
    req.flush(okResponse());

    expect(component.result()!.indemniteForfaitaire).toBe(15000);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Indemnité calculée', 'OK', jasmine.any(Object));
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.salaireMensuelReference.set(2500);
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

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.salaireMensuelReference.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // SF-DT-21-02 — Gate pays Belgique (bannière info, pas de masquage silencieux)
  // ---------------------------------------------------------------------------

  it('workspaceCountry BELGIQUE → bannière info + aucun appel HTTP', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    // Pas de GET attendu — le gate pays évite l'appel.
    httpMock.expectNone(BASE_URL);
    httpMock.expectNone(SOURCE_EXPL_URL);
    expect(component.isBelgium()).toBe(true);
  });

  it('workspaceCountry FRANCE → appel GET normal', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.isBelgium()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // SF-DT-21-02 — Pré-fill IA salaire
  // ---------------------------------------------------------------------------

  it('prefill IA (salaireBrutMensuel > 0) + GET 404 → valeur + badge IA', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireMensuelReference()).toBe(2500);
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('prefill sans aiData → pas de pré-remplissage, aucun badge', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireMensuelReference()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('salaireBrutMensuel ≤ 0 → pas de pré-fill salaire', () => {
    component.aiData = { salaireBrutMensuel: 0 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireMensuelReference()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('onSalaireChange efface le badge IA salaire', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceSalaire()).toBe('IA');
    component.onSalaireChange(3000);
    expect(component.salaireMensuelReference()).toBe(3000);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('loadExisting (GET 200) → pas de badge IA (valeurs persistées prioritaires)', () => {
    component.aiData = { salaireBrutMensuel: 9999 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(okResponse());
    expectSourceExplanationCall();

    expect(component.salaireMensuelReference()).toBe(2500);
    expect(component.provenanceSalaire()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // SF-DT-21-02 — Alertes de cohérence F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.SALAIRE présent si divergence > 10 %', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // IA = 2500, avocat 4000 → écart 60 %.
    component.onSalaireChange(4000);

    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE).toBeDefined();
    expect(alerts.SALAIRE!.field).toBe('SALAIRE');
    expect(alerts.SALAIRE!.source).toBe('IA');
    expect(alerts.SALAIRE!.severity).toBe('WARNING');
  });

  it('coherenceAlerts.SALAIRE absent si écart ≤ 10 %', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // IA = 2500, avocat 2600 → écart 4 %.
    component.onSalaireChange(2600);

    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  it('alertes masquées après résultat affiché (showForm=false) — anti-bug SF-IA-03-12', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(4000); // divergent
    expect(component.coherenceAlerts().SALAIRE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  it('SF-155-06 : IA + PIECE_MANQUANTE → alerte avec pieceTexte rempli', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.piecesManquantes = [
      { texte: 'Bulletins de salaire des 12 derniers mois', critereCode: 'SALAIRE_BRUT_MENSUEL' },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(4000); // divergent

    const alert = component.coherenceAlerts().SALAIRE;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('IA');
    expect(alert!.contributors).toContain('PIECE_MANQUANTE');
    expect(alert!.pieceTexte).toBe('Bulletins de salaire des 12 derniers mois');
  });

  // ---------------------------------------------------------------------------
  // SF-DT-21-02 — ngOnChanges
  // ---------------------------------------------------------------------------

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newAi = { salaireBrutMensuel: 2800 } as TravailExtractedData;
    component.aiData = newAi;
    const changes: SimpleChanges = {
      aiData: new SimpleChange(null, newAi, false),
    };
    component.ngOnChanges(changes);

    expect(component.salaireMensuelReference()).toBe(2800);
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onSalaireChange(4200);
    expect(component.provenanceSalaire()).toBeNull();

    const newAi = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.salaireMensuelReference()).toBe(4200);
    expect(component.provenanceSalaire()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // SF-DT-21-02 — salaireEstDeduit hint
  // ---------------------------------------------------------------------------

  it('salaireEstDeduit=true → note déduction exposée', () => {
    component.aiData = {
      salaireBrutMensuel: 2500,
      salaireEstDeduit: true,
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireEstDeduit()).toBe(true);
  });

  it('salaireEstDeduit=false/undefined → pas de note', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireEstDeduit()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // SF-DT-21-02 — Scenarios métier F-DT-21 (messages L.8223-1)
  // ---------------------------------------------------------------------------

  it('F-DT-21 : résultat persisté expose les 3 messages L.8223-1 (cumul Cass. soc., condition, non-cumul L.4624-1)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(okResponse());
    expectSourceExplanationCall();

    const messages = component.result()!.messages;
    expect(messages.length).toBe(3);
    expect(messages[0]).toContain('Cass. soc.');
    expect(messages[0]).toContain('26 mars 2010');
    expect(messages[1]).toContain('rupture');
    expect(messages[1]).toContain('infraction');
    expect(messages[2]).toContain('L.4624-1');
  });

  it('F-DT-21 : formule indemnité = 6 × salaire (vérifiée sur résultat)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(okResponse());
    expectSourceExplanationCall();

    expect(component.result()!.formule).toContain('6 mois');
    expect(component.result()!.indemniteForfaitaire).toBe(
      6 * component.result()!.salaireMensuelReference,
    );
  });

  it('F-DT-21 : base juridique L.8223-1 exposée', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(okResponse());
    expectSourceExplanationCall();

    expect(component.result()!.baseJuridique).toBe('Art. L.8223-1 Code du travail');
  });

  // ---------------------------------------------------------------------------
  // SF-DT-21-02 — toggleCollapse / editMode (non-régression)
  // ---------------------------------------------------------------------------

  it('toggleCollapse bascule l\'état collapsed', () => {
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

  // ---------------------------------------------------------------------------
  // SF-DT-21-02 — Contrat CoherenceAlert<TDAlertField> (SF-155-05)
  // ---------------------------------------------------------------------------

  it('SF-155-05 : alerte SALAIRE expose contract CoherenceAlert<F>', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(4000);

    const alert = component.coherenceAlerts().SALAIRE;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('SALAIRE');
    expect(alert!.source).toBe('IA');
    expect(alert!.contributors).toEqual(['IA']);
    expect(alert!.severity).toBe('WARNING');
    expect(alert!.expectedDisplay).toContain('€');
    expect(alert!.reason).toContain('Analyse du dossier');
  });

  it('SF-155-05 : alertBadgeLabel / alertTooltip fonctionnent', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(4000);

    const alert = component.coherenceAlerts().SALAIRE!;
    expect(component.alertBadgeLabel(alert)).toContain('Incohérence');
    expect(component.alertTooltip(alert)).toBeTruthy();
  });
});
