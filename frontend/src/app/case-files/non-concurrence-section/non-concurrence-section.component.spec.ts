import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NonConcurrenceService } from '../../core/services/non-concurrence.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange, SimpleChanges } from '@angular/core';
import { NonConcurrenceSectionComponent } from './non-concurrence-section.component';
import { NonConcurrenceResponse } from '../../core/models/non-concurrence.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('NonConcurrenceSectionComponent', () => {
  let component: NonConcurrenceSectionComponent;
  let fixture: ComponentFixture<NonConcurrenceSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-24/non-concurrence';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-24/source-explanations';

  function defaultResponse(overrides: Partial<NonConcurrenceResponse> = {}):
      NonConcurrenceResponse {
    return {
      caseFileId: 'case-24',
      clausePresenteContrat: true,
      limiteTerritoireDefini: true,
      territoireDescription: 'Île-de-France',
      limiteDureeDefinie: true,
      dureeMois: 12,
      limiteObjetDefini: true,
      objetDescription: 'CRM logiciel',
      contrepartieFinancierePresente: true,
      contrepartieMontantMensuelEur: 750,
      salaireMensuelBrutEur: 3000,
      secteurActivite: 'INFORMATIQUE',
      datePriseEffet: '2026-04-15',
      critere1TerritoireOk: true,
      critere2DureeOk: true,
      critere3ObjetOk: true,
      critere4ContrepartieOk: true,
      ratioContrepartiePct: 25,
      scoreValidite: 100,
      verdictValidite: 'VALIDE',
      indemniteContrepartieDueEur: 750,
      indemnitePotentielleNulliteEur: 0,
      baseJuridique: 'Cass. soc. 10/07/2002, art. L.1121-1 Code du travail',
      formule: 'Indemnité contrepartie = 25 % × 3000 = 750 €/mois',
      messages: ['Clause valide selon les 4 critères Cass. soc. 10/07/2002.'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  function flushSourceExplanations(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [
        NonConcurrenceSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NonConcurrenceSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-24';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Mount + enums
  // ---------------------------------------------------------------------------

  it('mount sans erreur (FRANCE) + 5 secteurActivite options exposées', () => {
    expect(component).toBeTruthy();
    expect(component.secteurOptions.length).toBe(5);
    const codes = component.secteurOptions.map((o) => o.code);
    expect(codes).toContain('INFORMATIQUE');
    expect(codes).toContain('COMMERCE');
    expect(codes).toContain('INDUSTRIE');
    expect(codes).toContain('SERVICES');
    expect(codes).toContain('AUTRE');
  });

  // ---------------------------------------------------------------------------
  // Form validators
  // ---------------------------------------------------------------------------

  it('formValid faux si salaireMensuelBrutEur null/0/négatif', () => {
    component.secteurActivite.set('INFORMATIQUE');
    component.datePriseEffet.set('2026-04-15');
    component.salaireMensuelBrutEur.set(null);
    expect(component.formValid()).toBe(false);
    component.salaireMensuelBrutEur.set(0);
    expect(component.formValid()).toBe(false);
    component.salaireMensuelBrutEur.set(-100);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si secteurActivite null', () => {
    component.salaireMensuelBrutEur.set(3000);
    component.datePriseEffet.set('2026-04-15');
    component.secteurActivite.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si datePriseEffet vide', () => {
    component.salaireMensuelBrutEur.set(3000);
    component.secteurActivite.set('INFORMATIQUE');
    component.datePriseEffet.set(null);
    expect(component.formValid()).toBe(false);
    component.datePriseEffet.set('');
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si limiteTerritoireDefini=true sans territoireDescription', () => {
    component.salaireMensuelBrutEur.set(3000);
    component.secteurActivite.set('INFORMATIQUE');
    component.datePriseEffet.set('2026-04-15');
    component.limiteTerritoireDefini.set(true);
    component.territoireDescription.set('');
    expect(component.formValid()).toBe(false);
    component.territoireDescription.set('   ');
    expect(component.formValid()).toBe(false);
    component.territoireDescription.set('Île-de-France');
    expect(component.formValid()).toBe(true);
  });

  it('formValid faux si limiteDureeDefinie=true avec dureeMois ≤ 0 ou null', () => {
    component.salaireMensuelBrutEur.set(3000);
    component.secteurActivite.set('INFORMATIQUE');
    component.datePriseEffet.set('2026-04-15');
    component.limiteDureeDefinie.set(true);
    component.dureeMois.set(null);
    expect(component.formValid()).toBe(false);
    component.dureeMois.set(0);
    expect(component.formValid()).toBe(false);
    component.dureeMois.set(-1);
    expect(component.formValid()).toBe(false);
    component.dureeMois.set(12);
    expect(component.formValid()).toBe(true);
  });

  it('formValid faux si limiteObjetDefini=true sans objetDescription', () => {
    component.salaireMensuelBrutEur.set(3000);
    component.secteurActivite.set('INFORMATIQUE');
    component.datePriseEffet.set('2026-04-15');
    component.limiteObjetDefini.set(true);
    component.objetDescription.set('');
    expect(component.formValid()).toBe(false);
    component.objetDescription.set('CRM logiciel');
    expect(component.formValid()).toBe(true);
  });

  it('formValid faux si contrepartieFinancierePresente=true avec montant ≤ 0/null', () => {
    component.salaireMensuelBrutEur.set(3000);
    component.secteurActivite.set('INFORMATIQUE');
    component.datePriseEffet.set('2026-04-15');
    component.contrepartieFinancierePresente.set(true);
    component.contrepartieMontantMensuelEur.set(null);
    expect(component.formValid()).toBe(false);
    component.contrepartieMontantMensuelEur.set(0);
    expect(component.formValid()).toBe(false);
    component.contrepartieMontantMensuelEur.set(750);
    expect(component.formValid()).toBe(true);
  });

  it('formValid vrai sur cas nominal complet sans toggles définis', () => {
    component.salaireMensuelBrutEur.set(3000);
    component.secteurActivite.set('INFORMATIQUE');
    component.datePriseEffet.set('2026-04-15');
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // HTTP lifecycle
  // ---------------------------------------------------------------------------

  it('GET 200 → form masqué, valeurs hydratées, pas de badge IA', () => {
    component.aiData = { salaireBrutMensuel: 9999 } as TravailExtractedData;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(defaultResponse({ verdictValidite: 'RISQUE_NULLITE_PARTIELLE', scoreValidite: 50 }));
    flushSourceExplanations();

    expect(component.result()!.scoreValidite).toBe(50);
    expect(component.showForm()).toBe(false);
    expect(component.salaireMensuelBrutEur()).toBe(3000);
    expect(component.territoireDescription()).toBe('Île-de-France');
    expect(component.dureeMois()).toBe(12);
    expect(component.objetDescription()).toBe('CRM logiciel');
    expect(component.contrepartieMontantMensuelEur()).toBe(750);
    expect(component.secteurActivite()).toBe('INFORMATIQUE');
    expect(component.datePriseEffet()).toBe('2026-04-15');
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('GET 404 → reste en mode formulaire ; pré-fill IA appliqué', () => {
    component.aiData = { salaireBrutMensuel: 2700 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.showForm()).toBe(true);
    expect(component.salaireMensuelBrutEur()).toBe(2700);
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('calculate() POST → résultat affiché + snackbar + dashboardRefresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.salaireMensuelBrutEur.set(3000);
    component.secteurActivite.set('INFORMATIQUE');
    component.datePriseEffet.set('2026-04-15');
    component.limiteTerritoireDefini.set(true);
    component.territoireDescription.set('Île-de-France');
    component.limiteDureeDefinie.set(true);
    component.dureeMois.set(12);
    component.limiteObjetDefini.set(true);
    component.objetDescription.set('CRM logiciel');
    component.contrepartieFinancierePresente.set(true);
    component.contrepartieMontantMensuelEur.set(750);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      clausePresenteContrat: true,
      limiteTerritoireDefini: true,
      territoireDescription: 'Île-de-France',
      limiteDureeDefinie: true,
      dureeMois: 12,
      limiteObjetDefini: true,
      objetDescription: 'CRM logiciel',
      contrepartieFinancierePresente: true,
      contrepartieMontantMensuelEur: 750,
      salaireMensuelBrutEur: 3000,
      secteurActivite: 'INFORMATIQUE',
      datePriseEffet: '2026-04-15',
    });
    req.flush(defaultResponse());

    expect(component.result()!.verdictValidite).toBe('VALIDE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse de validité de la clause calculée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() erreur 400 → snackbar rouge, pas de refresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.salaireMensuelBrutEur.set(3000);
    component.secteurActivite.set('INFORMATIQUE');
    component.datePriseEffet.set('2026-04-15');
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Secteur inconnu' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
    expect(refreshSpy.triggerRefresh).not.toHaveBeenCalled();
  });

  it('calculate() ignoré si form invalide (pas de POST)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + provenance
  // ---------------------------------------------------------------------------

  it('pré-fill IA salaireMensuelBrutEur si aiData.salaireBrutMensuel > 0', () => {
    component.aiData = { salaireBrutMensuel: 3200 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelBrutEur()).toBe(3200);
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('aiData.salaireBrutMensuel = 0 → pas de pré-fill', () => {
    component.aiData = { salaireBrutMensuel: 0 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelBrutEur()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('aiData null → pas de badge IA, pas de pré-fill', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelBrutEur()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('onSalaireChange manuel efface le badge IA', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.provenanceSalaire()).toBe('IA');
    component.onSalaireChange(3500);
    expect(component.salaireMensuelBrutEur()).toBe(3500);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    const newAi = { salaireBrutMensuel: 2700 } as TravailExtractedData;
    component.aiData = newAi;
    const changes: SimpleChanges = { aiData: new SimpleChange(null, newAi, false) };
    component.ngOnChanges(changes);

    expect(component.salaireMensuelBrutEur()).toBe(2700);
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.onSalaireChange(4500);
    expect(component.provenanceSalaire()).toBeNull();

    const newAi = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.salaireMensuelBrutEur()).toBe(4500);
    expect(component.provenanceSalaire()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // F-IA-03 cohérence
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.SALAIRE_MENSUEL présent si écart > 10 % vs IA', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.onSalaireChange(4000);
    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE_MENSUEL).toBeDefined();
    expect(alerts.SALAIRE_MENSUEL!.field).toBe('SALAIRE_MENSUEL');
    expect(alerts.SALAIRE_MENSUEL!.source).toBe('IA');
    expect(alerts.SALAIRE_MENSUEL!.severity).toBe('WARNING');
    expect(alerts.SALAIRE_MENSUEL!.expectedDisplay).toContain('€');
  });

  it('coherenceAlerts absent si écart ≤ 10 %', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.onSalaireChange(3100);
    expect(component.coherenceAlerts().SALAIRE_MENSUEL).toBeUndefined();
  });

  it('alertes masquées après showForm=false (anti-bug SF-IA-03-12)', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.onSalaireChange(4000);
    expect(component.coherenceAlerts().SALAIRE_MENSUEL).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().SALAIRE_MENSUEL).toBeUndefined();
  });

  it('alertBadgeLabel et alertTooltip exposent un texte pertinent', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(5000);
    const alert = component.coherenceAlerts().SALAIRE_MENSUEL!;
    expect(component.alertBadgeLabel(alert)).toContain('Incohérence');
    expect(component.alertTooltip(alert)).toBeTruthy();
    expect(alert.contributors).toEqual(['IA']);
  });

  // ---------------------------------------------------------------------------
  // SF-246-02 : pré-fill IA étendu (8 champs clause de non-concurrence)
  // ---------------------------------------------------------------------------

  const FULL_NC_AI: TravailExtractedData = {
    salaireBrutMensuel: 3000,
    clauseNonConcurrenceDetectee: true,
    nonConcurrenceDureeMois: 24,
    nonConcurrenceZoneGeographique: 'France métropolitaine',
    nonConcurrenceContrepartieMontantEur: 900,
  } as TravailExtractedData;

  function mountWith(ai: TravailExtractedData | null): void {
    component.aiData = ai;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
  }

  it('SF-246-02 : clause complète → 8 champs pré-remplis + badges', () => {
    mountWith(FULL_NC_AI);

    expect(component.salaireMensuelBrutEur()).toBe(3000);
    expect(component.provenanceSalaire()).toBe('IA');
    expect(component.clausePresenteContrat()).toBe(true);
    expect(component.provenanceClausePresente()).toBe('IA');
    expect(component.dureeMois()).toBe(24);
    expect(component.provenanceDureeMois()).toBe('IA');
    expect(component.limiteDureeDefinie()).toBe(true);
    expect(component.provenanceLimiteDuree()).toBe('IA');
    expect(component.territoireDescription()).toBe('France métropolitaine');
    expect(component.provenanceTerritoire()).toBe('IA');
    expect(component.limiteTerritoireDefini()).toBe(true);
    expect(component.provenanceLimiteTerritoire()).toBe('IA');
    expect(component.contrepartieMontantMensuelEur()).toBe(900);
    expect(component.provenanceContrepartieMontant()).toBe('IA');
    expect(component.contrepartieFinancierePresente()).toBe(true);
    expect(component.provenanceContrepartiePresente()).toBe('IA');
  });

  it('SF-246-02 : getPrefillCount statique en parité — 8 sur clause complète', () => {
    expect(NonConcurrenceSectionComponent.getPrefillCount({
      aiData: FULL_NC_AI, workspaceCountry: 'FRANCE',
    })).toBe(8);
  });

  it('SF-246-02 : getPrefillCount statique — 0 pour la Belgique', () => {
    expect(NonConcurrenceSectionComponent.getPrefillCount({
      aiData: FULL_NC_AI, workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('SF-246-02 : champs partiels (durée + salaire seuls) → 3 pré-remplis', () => {
    // salaire + durée + booléen dérivé limiteDuree = 3.
    const partial = {
      salaireBrutMensuel: 2500,
      nonConcurrenceDureeMois: 12,
    } as TravailExtractedData;
    expect(NonConcurrenceSectionComponent.getPrefillCount({
      aiData: partial, workspaceCountry: 'FRANCE',
    })).toBe(3);
  });

  it('SF-246-02 : parité runtime ↔ getPrefillCount sur clause complète', () => {
    mountWith(FULL_NC_AI);
    let runtimeCount = 0;
    if (component.provenanceSalaire() === 'IA') runtimeCount++;
    if (component.provenanceClausePresente() === 'IA') runtimeCount++;
    if (component.provenanceDureeMois() === 'IA') runtimeCount++;
    if (component.provenanceLimiteDuree() === 'IA') runtimeCount++;
    if (component.provenanceTerritoire() === 'IA') runtimeCount++;
    if (component.provenanceLimiteTerritoire() === 'IA') runtimeCount++;
    if (component.provenanceContrepartieMontant() === 'IA') runtimeCount++;
    if (component.provenanceContrepartiePresente() === 'IA') runtimeCount++;
    expect(runtimeCount).toBe(NonConcurrenceSectionComponent.getPrefillCount({
      aiData: FULL_NC_AI, workspaceCountry: 'FRANCE',
    }));
  });

  it('SF-246-02 : durée aberrante (> 600) → durée non pré-remplie', () => {
    mountWith({
      nonConcurrenceDureeMois: 720,
      nonConcurrenceZoneGeographique: 'Lyon',
    } as TravailExtractedData);
    expect(component.dureeMois()).toBeNull();
    expect(component.provenanceDureeMois()).toBeNull();
    expect(component.territoireDescription()).toBe('Lyon');
  });

  it('SF-246-02 : modification manuelle de la durée efface le badge IA', () => {
    mountWith(FULL_NC_AI);
    expect(component.provenanceDureeMois()).toBe('IA');
    component.onDureeMoisChange(18);
    expect(component.dureeMois()).toBe(18);
    expect(component.provenanceDureeMois()).toBeNull();
  });

  it('SF-246-02 : modification manuelle de la zone efface le badge IA', () => {
    mountWith(FULL_NC_AI);
    expect(component.provenanceTerritoire()).toBe('IA');
    component.onTerritoireDescriptionChange('Paris intra-muros');
    expect(component.provenanceTerritoire()).toBeNull();
  });

  it('SF-246-02 : modification manuelle de la contrepartie efface le badge IA', () => {
    mountWith(FULL_NC_AI);
    expect(component.provenanceContrepartieMontant()).toBe('IA');
    component.onContrepartieMontantChange(1100);
    expect(component.provenanceContrepartieMontant()).toBeNull();
  });

  it('SF-246-02 : alerte DUREE_CLAUSE si durée saisie diverge > 10 % de l\'IA', () => {
    mountWith(FULL_NC_AI);
    component.onDureeMoisChange(36); // 36 vs 24 → écart 50 %
    const alerts = component.coherenceAlerts();
    expect(alerts.DUREE_CLAUSE).toBeDefined();
    expect(alerts.DUREE_CLAUSE!.field).toBe('DUREE_CLAUSE');
    expect(alerts.DUREE_CLAUSE!.expectedDisplay).toContain('mois');
  });

  it('SF-246-02 : alerte CONTREPARTIE si montant saisi diverge > 10 % de l\'IA', () => {
    mountWith(FULL_NC_AI);
    component.onContrepartieMontantChange(1500); // 1500 vs 900
    expect(component.coherenceAlerts().CONTREPARTIE).toBeDefined();
  });

  it('SF-246-02 : alerte ZONE_GEOGRAPHIQUE si zone saisie diffère de l\'IA', () => {
    mountWith(FULL_NC_AI);
    component.onTerritoireDescriptionChange('Région PACA');
    expect(component.coherenceAlerts().ZONE_GEOGRAPHIQUE).toBeDefined();
  });

  it('SF-246-02 : pas d\'alerte ZONE si zone identique (casse/espaces ignorés)', () => {
    mountWith(FULL_NC_AI);
    component.onTerritoireDescriptionChange('  france  MÉTROPOLITAINE ');
    expect(component.coherenceAlerts().ZONE_GEOGRAPHIQUE).toBeUndefined();
  });

  it('SF-246-02 : aiData sans bloc clause → pré-fill no-op gracieux (salaire seul)', () => {
    mountWith({ salaireBrutMensuel: 2800 } as TravailExtractedData);
    expect(component.salaireMensuelBrutEur()).toBe(2800);
    expect(component.dureeMois()).toBeNull();
    expect(component.provenanceDureeMois()).toBeNull();
    expect(component.territoireDescription()).toBe('');
    expect(component.provenanceTerritoire()).toBeNull();
    expect(component.contrepartieMontantMensuelEur()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Specifics F-DT-24
  // ---------------------------------------------------------------------------

  it('toggle limiteTerritoireDefini=false vide la description', () => {
    component.onLimiteTerritoireDefiniChange(true);
    component.onTerritoireDescriptionChange('Paris');
    expect(component.territoireDescription()).toBe('Paris');
    component.onLimiteTerritoireDefiniChange(false);
    expect(component.territoireDescription()).toBe('');
    expect(component.limiteTerritoireDefini()).toBe(false);
  });

  it('toggle limiteDureeDefinie=false remet dureeMois à null', () => {
    component.onLimiteDureeDefinieChange(true);
    component.onDureeMoisChange(18);
    expect(component.dureeMois()).toBe(18);
    component.onLimiteDureeDefinieChange(false);
    expect(component.dureeMois()).toBeNull();
  });

  it('toggle limiteObjetDefini=false vide la description', () => {
    component.onLimiteObjetDefiniChange(true);
    component.onObjetDescriptionChange('CRM');
    expect(component.objetDescription()).toBe('CRM');
    component.onLimiteObjetDefiniChange(false);
    expect(component.objetDescription()).toBe('');
  });

  it('toggle contrepartieFinancierePresente=false remet montant à null', () => {
    component.onContrepartieFinancierePresenteChange(true);
    component.onContrepartieMontantChange(800);
    expect(component.contrepartieMontantMensuelEur()).toBe(800);
    component.onContrepartieFinancierePresenteChange(false);
    expect(component.contrepartieMontantMensuelEur()).toBeNull();
  });

  it('POST envoie tous les champs y compris ceux issus des paires off (defaults 0/empty)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    // Cas minimal valide : aucun toggle de critère activé.
    component.salaireMensuelBrutEur.set(3000);
    component.secteurActivite.set('SERVICES');
    component.datePriseEffet.set('2026-05-01');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.limiteTerritoireDefini).toBe(false);
    expect(req.request.body.territoireDescription).toBe('');
    expect(req.request.body.limiteDureeDefinie).toBe(false);
    expect(req.request.body.dureeMois).toBe(0);
    expect(req.request.body.limiteObjetDefini).toBe(false);
    expect(req.request.body.objetDescription).toBe('');
    expect(req.request.body.contrepartieFinancierePresente).toBe(false);
    expect(req.request.body.contrepartieMontantMensuelEur).toBe(0);
    expect(req.request.body.clausePresenteContrat).toBe(true);
    req.flush(defaultResponse({
      limiteTerritoireDefini: false,
      territoireDescription: '',
      limiteDureeDefinie: false,
      dureeMois: 0,
      limiteObjetDefini: false,
      objetDescription: '',
      contrepartieFinancierePresente: false,
      contrepartieMontantMensuelEur: 0,
      ratioContrepartiePct: 0,
      verdictValidite: 'NULLE',
      scoreValidite: 10,
    }));
  });

  it('workspaceCountry BELGIQUE → bannière info, pas de GET', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.showForm()).toBe(true);
    expect(component.isFrance()).toBe(false);
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

  it('verdictBannerClass mappe NULLE→danger, RISQUE_NULLITE_PARTIELLE→warn, VALIDE→info', () => {
    expect(component.verdictBannerClass('NULLE')).toContain('--danger');
    expect(component.verdictBannerClass('RISQUE_NULLITE_PARTIELLE')).toContain('--warn');
    expect(component.verdictBannerClass('VALIDE')).toContain('--info');
  });

  it('verdictIcon mappe NULLE→gavel, RISQUE_NULLITE_PARTIELLE→balance, VALIDE→verified', () => {
    expect(component.verdictIcon('NULLE')).toBe('gavel');
    expect(component.verdictIcon('RISQUE_NULLITE_PARTIELLE')).toBe('balance');
    expect(component.verdictIcon('VALIDE')).toBe('verified');
  });

  it('verdictValiditeNcLabel produit un label francisé pour chaque valeur', () => {
    expect(component.verdictValiditeNcLabel('VALIDE')).toBe('Clause valide');
    expect(component.verdictValiditeNcLabel('RISQUE_NULLITE_PARTIELLE')).toBe('Risque de nullité partielle');
    expect(component.verdictValiditeNcLabel('NULLE')).toBe('Clause nulle');
  });

  it('secteurActiviteLabel résout les libellés pour les codes connus', () => {
    expect(component.secteurActiviteLabel('INFORMATIQUE')).toBe('Informatique / IT');
    expect(component.secteurActiviteLabel('AUTRE')).toBe('Autre secteur');
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02b — mode standalone (CA-08, CA-09, CA-10).
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02b — mode standalone', () => {
    const STANDALONE_URL = '/api/v1/simulators/F-DT-24-non-concurrence/calculate';

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
      expect(NonConcurrenceService.STANDALONE_TOOL_ID).toBe('F-DT-24-non-concurrence');
      expect(STANDALONE_URL).toContain(NonConcurrenceService.STANDALONE_TOOL_ID);
    });
  });

});
