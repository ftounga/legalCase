import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TransactionService } from '../../core/services/transaction.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { TransactionSectionComponent } from './transaction-section.component';
import { TransactionResponse } from '../../core/models/transaction.model';
import { TravailExtractedData, PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('TransactionSectionComponent', () => {
  let component: TransactionSectionComponent;
  let fixture: ComponentFixture<TransactionSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/transaction';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function defaultResponse(): TransactionResponse {
    return {
      caseFileId: 'case-1',
      dateSignature: '2026-04-25',
      concessionsEmployeur: ['INDEMNITE_TRANSACTIONNELLE', 'LETTRE_RECOMMANDATION'],
      concessionsSalarie: ['RENONCIATION_ACTION_PRUDHOMALE'],
      indemniteTransactionnelleEur: 12000,
      salaireMensuelBrutEur: 3000,
      ancienneteAnnees: 6,
      renonciationActionExpresse: true,
      delaiReflexion15jOk: true,
      rupturePrealable: 'LICENCIEMENT_PERSONNEL',
      presenceAvocatAssistance: false,
      viceConsentementAllégué: false,
      concessionsReciproquesCaracterisees: true,
      ratioConcessionsEmployeurPct: 0.42,
      indemniteTransactionnelleSuperieureMacron: true,
      scoreValidite: 78,
      verdictValiditeContrat: 'VALIDE',
      risqueNulliteRetenu: false,
      baseJuridique: 'Art. 2044-2058 Cciv',
      formule: 'score = 78/100',
      messages: ['Concessions réciproques caractérisées.'],
      country: 'FRANCE',
    };
  }

  /**
   * Absorbe la requête source-explanations émise par `loadSourceExplanations()`
   * dans `ngOnInit` (fail-open). Empêche `httpMock.verify()` de crasher.
   */
  function expectSourceExplanationCall(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [
        TransactionSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TransactionSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Mount + form validators
  // ---------------------------------------------------------------------------

  it('mount sans erreur (FRANCE)', () => {
    expect(component).toBeTruthy();
    expect(component.concessionsEmployeurOptions.length).toBe(6);
    expect(component.concessionsSalarieOptions.length).toBe(3);
    expect(component.rupturesPrealableOptions.length).toBe(6);
  });

  it('formValid faux si concessionsEmployeur vide', () => {
    component.dateSignature.set('2026-04-25');
    component.concessionsEmployeur.set([]);
    component.concessionsSalarie.set(['RENONCIATION_ACTION_PRUDHOMALE']);
    component.indemniteTransactionnelleEur.set(10000);
    component.salaireMensuelBrutEur.set(3000);
    component.ancienneteAnnees.set(5);
    component.rupturePrealable.set('LICENCIEMENT_PERSONNEL');
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si concessionsSalarie vide', () => {
    component.dateSignature.set('2026-04-25');
    component.concessionsEmployeur.set(['INDEMNITE_TRANSACTIONNELLE']);
    component.concessionsSalarie.set([]);
    component.indemniteTransactionnelleEur.set(10000);
    component.salaireMensuelBrutEur.set(3000);
    component.ancienneteAnnees.set(5);
    component.rupturePrealable.set('LICENCIEMENT_PERSONNEL');
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si salaireMensuelBrutEur ≤ 0', () => {
    component.dateSignature.set('2026-04-25');
    component.concessionsEmployeur.set(['INDEMNITE_TRANSACTIONNELLE']);
    component.concessionsSalarie.set(['RENONCIATION_ACTION_PRUDHOMALE']);
    component.indemniteTransactionnelleEur.set(10000);
    component.salaireMensuelBrutEur.set(0);
    component.ancienneteAnnees.set(5);
    component.rupturePrealable.set('LICENCIEMENT_PERSONNEL');
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si rupturePrealable null', () => {
    component.dateSignature.set('2026-04-25');
    component.concessionsEmployeur.set(['INDEMNITE_TRANSACTIONNELLE']);
    component.concessionsSalarie.set(['RENONCIATION_ACTION_PRUDHOMALE']);
    component.indemniteTransactionnelleEur.set(10000);
    component.salaireMensuelBrutEur.set(3000);
    component.ancienneteAnnees.set(5);
    component.rupturePrealable.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai si tous champs requis renseignés', () => {
    component.dateSignature.set('2026-04-25');
    component.concessionsEmployeur.set(['INDEMNITE_TRANSACTIONNELLE']);
    component.concessionsSalarie.set(['RENONCIATION_ACTION_PRUDHOMALE']);
    component.indemniteTransactionnelleEur.set(0); // 0 OK pour indemnité
    component.salaireMensuelBrutEur.set(3000);
    component.ancienneteAnnees.set(0);
    component.rupturePrealable.set('LICENCIEMENT_PERSONNEL');
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // HTTP : load + calculate
  // ---------------------------------------------------------------------------

  it('GET 200 → form masqué, valeurs persistées affichées, pas de badge IA', () => {
    component.aiData = {
      salaireBrutMensuel: 9999,
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(defaultResponse());
    expectSourceExplanationCall();

    expect(component.result()!.scoreValidite).toBe(78);
    expect(component.showForm()).toBe(false);
    expect(component.salaireMensuelBrutEur()).toBe(3000); // valeur persistée
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.rupturePrealable()).toBe('LICENCIEMENT_PERSONNEL');
  });

  it('GET 404 → reste en mode formulaire ; pré-fill IA appliqué', () => {
    component.aiData = {
      salaireBrutMensuel: 3500,
      dateEntree: '2018-01-01',
      dateLicenciement: '2026-01-01',
      motifLicenciement: 'LICENCIEMENT_PERSONNEL',
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.showForm()).toBe(true);
    expect(component.salaireMensuelBrutEur()).toBe(3500);
    expect(component.provenanceSalaire()).toBe('IA');
    expect(component.ancienneteAnnees()).toBe(8); // 2018 → 2026
    expect(component.provenanceAnciennete()).toBe('IA');
    expect(component.rupturePrealable()).toBe('LICENCIEMENT_PERSONNEL');
    expect(component.provenanceRupturePrealable()).toBe('IA');
  });

  it('calculate() POST → résultat affiché + snackbar succès + dashboardRefresh', () => {
    component.dateSignature.set('2026-04-25');
    component.concessionsEmployeur.set(['INDEMNITE_TRANSACTIONNELLE']);
    component.concessionsSalarie.set(['RENONCIATION_ACTION_PRUDHOMALE']);
    component.indemniteTransactionnelleEur.set(12000);
    component.salaireMensuelBrutEur.set(3000);
    component.ancienneteAnnees.set(6);
    component.renonciationActionExpresse.set(true);
    component.delaiReflexion15jOk.set(true);
    component.rupturePrealable.set('LICENCIEMENT_PERSONNEL');
    component.presenceAvocatAssistance.set(false);
    // viceConsentementAllegue défaut false

    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateSignature: '2026-04-25',
      concessionsEmployeur: ['INDEMNITE_TRANSACTIONNELLE'],
      concessionsSalarie: ['RENONCIATION_ACTION_PRUDHOMALE'],
      indemniteTransactionnelleEur: 12000,
      salaireMensuelBrutEur: 3000,
      ancienneteAnnees: 6,
      renonciationActionExpresse: true,
      delaiReflexion15jOk: true,
      rupturePrealable: 'LICENCIEMENT_PERSONNEL',
      presenceAvocatAssistance: false,
      viceConsentementAllégué: false,
    });
    req.flush(defaultResponse());

    expect(component.result()!.verdictValiditeContrat).toBe('VALIDE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Validité de la transaction calculée',
      'OK',
      jasmine.any(Object),
    );
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() erreur 400 → snackbar rouge', () => {
    component.dateSignature.set('2026-04-25');
    component.concessionsEmployeur.set(['INDEMNITE_TRANSACTIONNELLE']);
    component.concessionsSalarie.set(['RENONCIATION_ACTION_PRUDHOMALE']);
    component.indemniteTransactionnelleEur.set(0);
    component.salaireMensuelBrutEur.set(3000);
    component.ancienneteAnnees.set(5);
    component.rupturePrealable.set('LICENCIEMENT_PERSONNEL');

    component.calculate();
    httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL)
      .flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
  });

  it('calculate() ignoré si form invalide (pas de POST)', () => {
    component.concessionsEmployeur.set([]);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Provenance handlers (badge IA effacé sur edit manuel)
  // ---------------------------------------------------------------------------

  it('onSalaireChange efface le badge IA salaire', () => {
    component.aiData = {
      salaireBrutMensuel: 3000,
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceSalaire()).toBe('IA');
    component.onSalaireChange(5000);
    expect(component.salaireMensuelBrutEur()).toBe(5000);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('onAncienneteChange efface le badge IA ancienneté', () => {
    component.aiData = {
      dateEntree: '2018-01-01',
      dateLicenciement: '2026-01-01',
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceAnciennete()).toBe('IA');
    component.onAncienneteChange(15);
    expect(component.ancienneteAnnees()).toBe(15);
    expect(component.provenanceAnciennete()).toBeNull();
  });

  it('onRupturePrealableChange efface le badge IA rupture', () => {
    component.aiData = {
      motifLicenciement: 'LICENCIEMENT_PERSONNEL',
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceRupturePrealable()).toBe('IA');
    component.onRupturePrealableChange('DEMISSION');
    expect(component.rupturePrealable()).toBe('DEMISSION');
    expect(component.provenanceRupturePrealable()).toBeNull();
  });

  it('pré-fill mappe motifLicenciement libre "rupture conventionnelle" → RUPTURE_CONVENTIONNELLE', () => {
    component.aiData = {
      motifLicenciement: 'rupture conventionnelle entre les parties',
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.rupturePrealable()).toBe('RUPTURE_CONVENTIONNELLE');
    expect(component.provenanceRupturePrealable()).toBe('IA');
  });

  it('pré-fill mappe motifLicenciement "économique" → LICENCIEMENT_ECONOMIQUE', () => {
    component.aiData = {
      motifLicenciement: 'Licenciement pour motif économique',
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.rupturePrealable()).toBe('LICENCIEMENT_ECONOMIQUE');
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03 (CoherenceAlertBuilder + multi-sources)
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.SALAIRE_MENSUEL présent si divergence > 10 %', () => {
    component.aiData = {
      salaireBrutMensuel: 3000,
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(5000); // écart 66 %

    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE_MENSUEL).toBeDefined();
    expect(alerts.SALAIRE_MENSUEL!.field).toBe('SALAIRE_MENSUEL');
    expect(alerts.SALAIRE_MENSUEL!.expectedDisplay).toContain('3');
    expect(alerts.SALAIRE_MENSUEL!.source).toBe('IA');
    expect(alerts.SALAIRE_MENSUEL!.contributors).toEqual(['IA']);
    expect(alerts.SALAIRE_MENSUEL!.severity).toBe('WARNING');
  });

  it('coherenceAlerts.SALAIRE_MENSUEL absent si écart ≤ 10 %', () => {
    component.aiData = {
      salaireBrutMensuel: 3000,
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(3100); // écart 3 %

    expect(component.coherenceAlerts().SALAIRE_MENSUEL).toBeUndefined();
  });

  it('coherenceAlerts.SALAIRE_MENSUEL enrichi par F96 → contributors IA + F96 + MULTI', () => {
    component.aiData = {
      salaireBrutMensuel: 3000,
    } as unknown as TravailExtractedData;
    component.procedureChecks = [
      {
        id: 'c1', ordre: 1, description: 'Salaire', statut: 'NON_COMPLIANT',
        critereCode: 'DT31_SALAIRE_MENSUEL', expectedValue: '3000 €', raison: 'fiche paie',
      },
    ] as ProcedureCheck[];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(6000); // divergence > 10 %

    const alert = component.coherenceAlerts().SALAIRE_MENSUEL;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('IA');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.source).toBe('MULTI');
  });

  it('coherenceAlerts.SALAIRE_MENSUEL enrichi par PIECE_MANQUANTE (fiche paie)', () => {
    component.aiData = {
      salaireBrutMensuel: 3000,
    } as unknown as TravailExtractedData;
    component.piecesManquantes = [
      { texte: 'Bulletins de paie 12 derniers mois', critereCode: 'FICHE_PAIE' },
    ] as PieceManquanteEntry[];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(6000);

    const alert = component.coherenceAlerts().SALAIRE_MENSUEL;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('PIECE_MANQUANTE');
    expect(alert!.pieceTexte).toBe('Bulletins de paie 12 derniers mois');
  });

  it('coherenceAlerts.ANCIENNETE présent si divergence > 1 an', () => {
    component.aiData = {
      dateEntree: '2018-01-01',
      dateLicenciement: '2026-01-01', // 8 ans IA
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onAncienneteChange(2); // saisie avocat divergente
    const alert = component.coherenceAlerts().ANCIENNETE;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('ANCIENNETE');
    expect(alert!.expectedDisplay).toContain('8');
  });

  it('coherenceAlerts.RUPTURE_PREALABLE présent si IA mappe à code différent', () => {
    component.aiData = {
      motifLicenciement: 'rupture conventionnelle',
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // L'avocat change manuellement vers DEMISSION (≠ IA mapping).
    component.onRupturePrealableChange('DEMISSION');
    const alert = component.coherenceAlerts().RUPTURE_PREALABLE;
    expect(alert).toBeDefined();
    expect(alert!.expectedDisplay).toContain('Rupture conventionnelle');
  });

  it('alertes masquées après résultat affiché (showForm=false)', () => {
    component.aiData = {
      salaireBrutMensuel: 3000,
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(6000);
    expect(component.coherenceAlerts().SALAIRE_MENSUEL).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().SALAIRE_MENSUEL).toBeUndefined();
  });

  it('alertBadgeLabel reflète le type de source (IA / MULTI)', () => {
    const alertIA: import('./transaction-section.component').TransactionCoherenceAlert = {
      field: 'SALAIRE_MENSUEL',
      source: 'IA',
      contributors: ['IA'],
      severity: 'WARNING',
      expectedDisplay: '3 000 €',
      reason: 'r',
    };
    const alertMulti = { ...alertIA, source: 'MULTI' as const, contributors: ['IA' as const, 'F96' as const] };
    expect(component.alertBadgeLabel(alertIA)).toContain('Incohérence détectée');
    expect(component.alertBadgeLabel(alertIA)).toContain('3 000 €');
    expect(component.alertBadgeLabel(alertMulti)).toContain('multiple');
  });

  it('alertTooltip ajoute "Contredit" si > 1 contributor', () => {
    const single: import('./transaction-section.component').TransactionCoherenceAlert = {
      field: 'SALAIRE_MENSUEL',
      source: 'IA',
      contributors: ['IA'],
      severity: 'WARNING',
      expectedDisplay: '3 000 €',
      reason: 'simple',
    };
    const multi = { ...single, source: 'MULTI' as const, contributors: ['IA' as const, 'F96' as const] };
    expect(component.alertTooltip(single)).toBe('simple');
    expect(component.alertTooltip(multi)).toContain('Contredit');
  });

  it('explanationFor retourne [] (fail-open empty)', () => {
    expect(component.explanationFor('SALAIRE_MENSUEL')).toEqual([]);
    expect(component.explanationFor('ANCIENNETE')).toEqual([]);
    expect(component.explanationFor('RUPTURE_PREALABLE')).toEqual([]);
  });

  it('ngOnChanges propage procedureChecks et permet d\'enrichir alertes', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newChecks: ProcedureCheck[] = [
      {
        id: 'c2', ordre: 2, description: 'Salaire', statut: 'NON_COMPLIANT',
        critereCode: 'DT31_SALAIRE_MENSUEL', expectedValue: '3000',
      } as ProcedureCheck,
    ];
    component.procedureChecks = newChecks;
    component.ngOnChanges({
      procedureChecks: new SimpleChange(null, newChecks, false),
    });
    component.onSalaireChange(6000);

    const alert = component.coherenceAlerts().SALAIRE_MENSUEL;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('F96');
  });

  // ---------------------------------------------------------------------------
  // Gate workspaceCountry
  // ---------------------------------------------------------------------------

  it('gate BELGIQUE → form non rendu, GET non appelé', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    httpMock.expectNone(SOURCE_EXPL_URL);
    expect(component.isFrance()).toBe(false);
  });

  it('gate FRANCE → load() appelé', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.isFrance()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // ngOnChanges + UI helpers
  // ---------------------------------------------------------------------------

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newAi = {
      salaireBrutMensuel: 3500,
      motifLicenciement: 'DEMISSION',
    } as unknown as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({
      aiData: new SimpleChange(null, newAi, false),
    });

    expect(component.salaireMensuelBrutEur()).toBe(3500);
    expect(component.rupturePrealable()).toBe('DEMISSION');
    expect(component.provenanceSalaire()).toBe('IA');
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

  it('verdictBannerClass renvoie la classe attendue (valid/medium/danger)', () => {
    expect(component.verdictBannerClass('VALIDE')).toContain('valid');
    expect(component.verdictBannerClass('CONTESTABLE')).toContain('medium');
    expect(component.verdictBannerClass('NULLE')).toContain('danger');
  });

  it('verdictIconName mappe les 3 verdicts', () => {
    expect(component.verdictIconName('VALIDE')).toBe('verified');
    expect(component.verdictIconName('CONTESTABLE')).toBe('gavel');
    expect(component.verdictIconName('NULLE')).toBe('error');
  });

  it('verdictChipClass mappe les 3 verdicts', () => {
    expect(component.verdictChipClass('VALIDE')).toContain('valid');
    expect(component.verdictChipClass('CONTESTABLE')).toContain('medium');
    expect(component.verdictChipClass('NULLE')).toContain('danger');
  });

  it('ratioPct convertit ratio 0-1 en pourcentage', () => {
    expect(component.ratioPct(0.42)).toBe(42);
    expect(component.ratioPct(0)).toBe(0);
    expect(component.ratioPct(1)).toBe(100);
    expect(component.ratioPct(NaN)).toBe(0);
  });

  it('handlers booléens mettent à jour les signals', () => {
    component.onRenonciationChange(true);
    expect(component.renonciationActionExpresse()).toBe(true);
    component.onDelaiReflexionChange(true);
    expect(component.delaiReflexion15jOk()).toBe(true);
    component.onPresenceAvocatChange(true);
    expect(component.presenceAvocatAssistance()).toBe(true);
    component.onViceConsentementChange(true);
    expect(component.viceConsentementAllegue()).toBe(true);
  });

  it('handlers multi-select mettent à jour les listes', () => {
    component.onConcessionsEmployeurChange(['LETTRE_RECOMMANDATION', 'AUTRE']);
    expect(component.concessionsEmployeur()).toEqual(['LETTRE_RECOMMANDATION', 'AUTRE']);
    component.onConcessionsSalarieChange(['RENONCIATION_PARTAGE_BENEFICES']);
    expect(component.concessionsSalarie()).toEqual(['RENONCIATION_PARTAGE_BENEFICES']);
  });

  it('onDateSignatureChange normalise null/empty', () => {
    component.onDateSignatureChange('');
    expect(component.dateSignature()).toBeNull();
    component.onDateSignatureChange('2026-04-25');
    expect(component.dateSignature()).toBe('2026-04-25');
    component.onDateSignatureChange(null);
    expect(component.dateSignature()).toBeNull();
  });

  // SF-DT-31-02 : suppression d'un test inutile qui aurait nécessité aiQuestion
  // sans branchement front. Test couvert par F-IA-03 multi-source canonique.
  it('aiQuestion enrichit l\'alerte salaire si réponse "oui" + expectedValue', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as unknown as TravailExtractedData;
    component.aiQuestions = [
      {
        id: 'q1',
        orderIndex: 1,
        questionText: 'Confirmez-vous le salaire 3000 €  ?',
        critereCode: 'DT31_SALAIRE_MENSUEL',
        answerText: 'oui, c\'est exact',
        expectedValue: '3000',
      } as AiQuestion,
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(6000);

    const alert = component.coherenceAlerts().SALAIRE_MENSUEL;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('QUESTION_IA');
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02b — mode standalone (CA-08, CA-09, CA-10).
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02b — mode standalone', () => {
    const STANDALONE_URL = '/api/v1/simulators/F-DT-31-transaction/calculate';

    it('CA-08 : affiche la bannière 🧪 quand standaloneMode=true', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
      expect(banner).not.toBeNull();
      expect(banner.textContent).toContain('Mode simulateur');
    });

    it('CA-08 : pas de bannière en mode case-file (standaloneMode=false)', () => {
      component.standaloneMode = false;
      fixture.detectChanges();
      const matches = httpMock.match(() => true);
      matches.forEach((r) => { try { r.flush({}, { status: 404, statusText: 'Not Found' }); } catch {} });
      const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
      expect(banner).toBeNull();
    });

    it("CA-08 : coherenceAlerts() retourne vide en standalone", () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const alerts = (component as any).coherenceAlerts ? (component as any).coherenceAlerts() : {};
      expect(Object.keys(alerts)).toHaveLength(0);
    });

    it("CA-09 : exposition du service standalone (route dispatcher)", () => {
      // Garde-fou statique : le service expose le toolId du dispatcher.
      // L'intégration runtime est couverte par CA-09 manuel sur staging
      // (3 outils échantillonnés — cf. mini-spec).
      expect(TransactionService.STANDALONE_TOOL_ID).toBe('F-DT-31-transaction');
      expect(STANDALONE_URL).toContain(TransactionService.STANDALONE_TOOL_ID);
    });
  });

});
