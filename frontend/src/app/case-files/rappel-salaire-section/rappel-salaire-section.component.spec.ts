import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange, SimpleChanges } from '@angular/core';
import { RappelSalaireSectionComponent } from './rappel-salaire-section.component';
import { RappelSalaireResponse } from '../../core/models/rappel-salaire.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('RappelSalaireSectionComponent', () => {
  let component: RappelSalaireSectionComponent;
  let fixture: ComponentFixture<RappelSalaireSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-42/rappel-salaire';
  const CONVENTIONS_URL = '/api/v1/referentials/conventions';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-42/source-explanations';

  function response(overrides: Partial<RappelSalaireResponse> = {}):
      RappelSalaireResponse {
    return {
      caseFileId: 'case-42',
      periodeDebut: '2023-01-01',
      periodeFin: '2024-12-31',
      montantSalaireDuMensuelEur: 2500,
      montantSalairePerVerseMensuelEur: 2200,
      conventionCollectiveCode: null,
      ancienneteAnneesPrime: 0,
      indexInseeRevalorise: false,
      tauxRevalorisationPct: null,
      methodeCpSurRappel: 'DIX_POURCENT',
      nbMoisPeriode: 24,
      differentielMensuelEur: 300,
      totalRappelBrutHorsRevalorisationEur: 7200,
      montantRevalorisationEur: 0,
      primeAncienneteEur: 0,
      totalRappelBrutEur: 7200,
      congesPayesSurRappelEur: 720,
      totalAvecCpEur: 7920,
      baseJuridique: 'Art. L.3242-1 + L.3245-1 + L.3141-26 Code du travail',
      formule: 'Différentiel 300 €/mois × 24 mois + CP 10 % = 7 920 €',
      messages: ['Prescription action 3 ans (art. L.3245-1).'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  function flushSourceExplanations(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  function flushConventions(): void {
    const reqs = httpMock.match(CONVENTIONS_URL);
    reqs.forEach((r) => r.flush([
      { code: 'IDCC_2120', label: 'Banque (IDCC 2120)', country: 'FRANCE' },
      { code: 'IDCC_3248', label: 'Métallurgie (IDCC 3248)', country: 'FRANCE' },
      { code: 'CP200', label: 'CP 200 — Employés', country: 'BELGIQUE' },
    ]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [
        RappelSalaireSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RappelSalaireSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-42';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Cycle de vie + gate pays
  // ---------------------------------------------------------------------------

  it('FRANCE → GET appelé au ngOnInit', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();
  });

  it('BELGIQUE → aucun appel HTTP, mode formulaire actif', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    httpMock.expectNone(CONVENTIONS_URL);
    expect(component.showForm()).toBe(true);
  });

  it('GET 200 → mode résultat, valeurs hydratées', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      conventionCollectiveCode: 'IDCC_2120',
      ancienneteAnneesPrime: 8,
      indexInseeRevalorise: true,
      tauxRevalorisationPct: 3.5,
      montantRevalorisationEur: 252,
    }));
    flushConventions();
    flushSourceExplanations();

    expect(component.result()!.totalAvecCpEur).toBe(7920);
    expect(component.showForm()).toBe(false);
    expect(component.periodeDebut()).toBe('2023-01-01');
    expect(component.periodeFin()).toBe('2024-12-31');
    expect(component.montantSalaireDuMensuelEur()).toBe(2500);
    expect(component.montantSalairePerVerseMensuelEur()).toBe(2200);
    expect(component.conventionCollectiveCode()).toBe('IDCC_2120');
    expect(component.ancienneteAnneesPrime()).toBe(8);
    expect(component.indexInseeRevalorise()).toBe(true);
    expect(component.tauxRevalorisationPct()).toBe(3.5);
    expect(component.methodeCpSurRappel()).toBe('DIX_POURCENT');
  });

  it('GET 404 → reste en mode formulaire', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();

    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  function fillValidForm(): void {
    component.periodeDebut.set('2023-01-01');
    component.periodeFin.set('2024-12-31');
    component.montantSalaireDuMensuelEur.set(2500);
    component.montantSalairePerVerseMensuelEur.set(2200);
    component.ancienneteAnneesPrime.set(0);
    component.indexInseeRevalorise.set(false);
    component.tauxRevalorisationPct.set(null);
    component.methodeCpSurRappel.set('DIX_POURCENT');
  }

  it('formValid true sur formulaire complet minimal', () => {
    fillValidForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si periodeFin < periodeDebut', () => {
    fillValidForm();
    component.periodeFin.set('2022-06-01');
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si dates manquantes', () => {
    fillValidForm();
    component.periodeDebut.set(null);
    expect(component.formValid()).toBe(false);
    component.periodeDebut.set('2023-01-01');
    component.periodeFin.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si montantSalaireDuMensuelEur ≤ 0', () => {
    fillValidForm();
    component.montantSalaireDuMensuelEur.set(0);
    expect(component.formValid()).toBe(false);
    component.montantSalaireDuMensuelEur.set(-100);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si versé ≥ dû (UI gate)', () => {
    fillValidForm();
    component.montantSalairePerVerseMensuelEur.set(2500);
    expect(component.formValid()).toBe(false);
    component.montantSalairePerVerseMensuelEur.set(3000);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si versé négatif', () => {
    fillValidForm();
    component.montantSalairePerVerseMensuelEur.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si ancienneté négative', () => {
    fillValidForm();
    component.ancienneteAnneesPrime.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si indexInseeRevalorise=true et taux invalide', () => {
    fillValidForm();
    component.indexInseeRevalorise.set(true);
    component.tauxRevalorisationPct.set(null);
    expect(component.formValid()).toBe(false);
    component.tauxRevalorisationPct.set(-1);
    expect(component.formValid()).toBe(false);
    component.tauxRevalorisationPct.set(150);
    expect(component.formValid()).toBe(false);
    component.tauxRevalorisationPct.set(3.5);
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // POST
  // ---------------------------------------------------------------------------

  it('calculate() POST → contrat correct + résultat + snackbar + triggerRefresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();

    fillValidForm();
    component.indexInseeRevalorise.set(true);
    component.tauxRevalorisationPct.set(3.5);
    component.conventionCollectiveCode.set('IDCC_2120');
    component.ancienneteAnneesPrime.set(8);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      periodeDebut: '2023-01-01',
      periodeFin: '2024-12-31',
      montantSalaireDuMensuelEur: 2500,
      montantSalairePerVerseMensuelEur: 2200,
      conventionCollectiveCode: 'IDCC_2120',
      ancienneteAnneesPrime: 8,
      indexInseeRevalorise: true,
      tauxRevalorisationPct: 3.5,
      methodeCpSurRappel: 'DIX_POURCENT',
    });
    req.flush(response({ totalAvecCpEur: 8197.20 }));

    expect(component.result()!.totalAvecCpEur).toBe(8197.20);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Rappel de salaire calculé', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() envoie tauxRevalorisationPct=null si indexInseeRevalorise=false', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();

    fillValidForm();
    // Avocat avait saisi un taux puis désactivé le toggle — on doit envoyer null.
    component.indexInseeRevalorise.set(true);
    component.tauxRevalorisationPct.set(3.5);
    component.onIndexInseeChange(false);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.indexInseeRevalorise).toBe(false);
    expect(req.request.body.tauxRevalorisationPct).toBeNull();
    req.flush(response());
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();

    fillValidForm();
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Aucun différentiel à réclamer' }, { status: 400, statusText: 'Bad Request' });

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
    flushConventions();
    flushSourceExplanations();

    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() méthode AUCUN propagée dans le POST', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();

    fillValidForm();
    component.onMethodeCpChange('AUCUN');
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.methodeCpSurRappel).toBe('AUCUN');
    req.flush(response({ methodeCpSurRappel: 'AUCUN', congesPayesSurRappelEur: 0, totalAvecCpEur: 7200 }));
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA
  // ---------------------------------------------------------------------------

  it('pré-fill IA salaire dû si aiData.salaireBrutMensuel > 0', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();

    expect(component.montantSalaireDuMensuelEur()).toBe(2500);
    expect(component.provenanceMontantDu()).toBe('IA');
  });

  it('pré-fill IA convention si aiData.conventionCollective normalisable et matchée', () => {
    component.aiData = { conventionCollective: 'IDCC 2120' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();

    expect(component.conventionCollectiveCode()).toBe('IDCC_2120');
    expect(component.provenanceConvention()).toBe('IA');
  });

  it('aiData.salaireBrutMensuel = 0 → pas de pré-fill', () => {
    component.aiData = { salaireBrutMensuel: 0 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();

    expect(component.montantSalaireDuMensuelEur()).toBeNull();
    expect(component.provenanceMontantDu()).toBeNull();
  });

  it('aiData null → pas de pré-fill, pas de badge', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();

    expect(component.montantSalaireDuMensuelEur()).toBeNull();
    expect(component.conventionCollectiveCode()).toBeNull();
    expect(component.provenanceMontantDu()).toBeNull();
    expect(component.provenanceConvention()).toBeNull();
  });

  it('onMontantDuChange manuel efface le badge IA', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();

    expect(component.provenanceMontantDu()).toBe('IA');
    component.onMontantDuChange(2800);
    expect(component.montantSalaireDuMensuelEur()).toBe(2800);
    expect(component.provenanceMontantDu()).toBeNull();
  });

  it('onConventionChange manuel efface le badge IA', () => {
    component.aiData = { conventionCollective: 'IDCC 2120' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();

    expect(component.provenanceConvention()).toBe('IA');
    component.onConventionChange('IDCC_3248');
    expect(component.conventionCollectiveCode()).toBe('IDCC_3248');
    expect(component.provenanceConvention()).toBeNull();
  });

  it('GET 200 → pas de badge IA même si aiData présent (persisté > IA)', () => {
    component.aiData = { salaireBrutMensuel: 9999, conventionCollective: 'IDCC_2120' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response());
    flushConventions();
    flushSourceExplanations();

    expect(component.montantSalaireDuMensuelEur()).toBe(2500);
    expect(component.provenanceMontantDu()).toBeNull();
    expect(component.provenanceConvention()).toBeNull();
  });

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();

    const newAi = { salaireBrutMensuel: 2800 } as TravailExtractedData;
    component.aiData = newAi;
    const changes: SimpleChanges = { aiData: new SimpleChange(null, newAi, false) };
    component.ngOnChanges(changes);

    expect(component.montantSalaireDuMensuelEur()).toBe(2800);
    expect(component.provenanceMontantDu()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();

    component.onMontantDuChange(4200);
    expect(component.provenanceMontantDu()).toBeNull();

    const newAi = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.montantSalaireDuMensuelEur()).toBe(4200);
    expect(component.provenanceMontantDu()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Alertes F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.SALAIRE présent si écart > 10 % vs IA', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();
    component.onMontantDuChange(5000);

    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE).toBeDefined();
    expect(alerts.SALAIRE!.field).toBe('SALAIRE');
    expect(alerts.SALAIRE!.source).toBe('IA');
    expect(alerts.SALAIRE!.severity).toBe('WARNING');
    expect(alerts.SALAIRE!.expectedDisplay).toContain('€');
  });

  it('coherenceAlerts.SALAIRE absent si écart ≤ 10 %', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();
    component.onMontantDuChange(3100);
    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  it('coherenceAlerts.CONVENTION présent si codes diffèrent', () => {
    component.aiData = { conventionCollective: 'IDCC_2120' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();
    // L'avocat sélectionne une CCN différente.
    component.onConventionChange('IDCC_3248');

    const alerts = component.coherenceAlerts();
    expect(alerts.CONVENTION).toBeDefined();
    expect(alerts.CONVENTION!.field).toBe('CONVENTION');
    expect(alerts.CONVENTION!.source).toBe('IA');
  });

  it('coherenceAlerts.CONVENTION absent si codes identiques (après normalize)', () => {
    component.aiData = { conventionCollective: 'IDCC 2120' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();
    // Pré-fill a posé 'IDCC_2120'.
    expect(component.conventionCollectiveCode()).toBe('IDCC_2120');
    expect(component.coherenceAlerts().CONVENTION).toBeUndefined();
  });

  it('alertes masquées après showForm=false (anti-bug SF-IA-03-12)', () => {
    component.aiData = { salaireBrutMensuel: 3000, conventionCollective: 'IDCC_2120' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();
    component.onMontantDuChange(5000);
    component.onConventionChange('IDCC_3248');
    expect(component.coherenceAlerts().SALAIRE).toBeDefined();
    expect(component.coherenceAlerts().CONVENTION).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
    expect(component.coherenceAlerts().CONVENTION).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // Helpers + UI
  // ---------------------------------------------------------------------------

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

  it('onIndexInseeChange(false) nettoie le taux de revalorisation', () => {
    component.indexInseeRevalorise.set(true);
    component.tauxRevalorisationPct.set(3.5);
    component.onIndexInseeChange(false);
    expect(component.indexInseeRevalorise()).toBe(false);
    expect(component.tauxRevalorisationPct()).toBeNull();
  });

  it('methodeLabel + methodeArticle helpers', () => {
    expect(component.methodeLabel('DIX_POURCENT')).toContain('dixième');
    expect(component.methodeLabel('MAINTIEN')).toContain('maintien');
    expect(component.methodeLabel('AUCUN')).toContain('Aucune');
    expect(component.methodeArticle('DIX_POURCENT')).toContain('L.3141-24');
    expect(component.methodeArticle('MAINTIEN')).toContain('L.3141-22');
    expect(component.methodeArticle('AUCUN')).toContain('L.1243-8');
  });

  it('alertBadgeLabel et alertTooltip exposent un texte pertinent', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushConventions();
    flushSourceExplanations();
    component.onMontantDuChange(5000);
    const alert = component.coherenceAlerts().SALAIRE!;
    expect(component.alertBadgeLabel(alert)).toContain('Incohérence');
    expect(component.alertTooltip(alert)).toBeTruthy();
    expect(alert.contributors).toEqual(['IA']);
  });
});
