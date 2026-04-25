import { ComponentFixture, TestBed } from '@angular/core/testing';
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
});
