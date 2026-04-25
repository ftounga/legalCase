import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AvantagesConventionnelsBeSectionComponent } from './avantages-conventionnels-be-section.component';
import { AvantagesConventionnelsBeResponse } from '../../core/models/avantages-conventionnels-be.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('AvantagesConventionnelsBeSectionComponent', () => {
  let component: AvantagesConventionnelsBeSectionComponent;
  let fixture: ComponentFixture<AvantagesConventionnelsBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/avantages-conventionnels-be';

  function response(overrides: Partial<AvantagesConventionnelsBeResponse> = {}): AvantagesConventionnelsBeResponse {
    return {
      caseFileId: 'case-1',
      salaireMensuelBrutEur: 3000,
      joursTravaillesAnneePrecedente: 220,
      anciennetteMois: 36,
      commissionParitaire: 'CP_200',
      annee: 2026,
      doublePeculeVacancesPercu: false,
      primeFinAnneePrevueCcCp: true,
      ecoChequesPrevuCcCp: true,
      ecoChequesUtilisationDansAn: true,
      chequesRepasPrevu: true,
      joursPrestesEffectifs: 220,
      peculeVacancesSimpleEur: 2400,
      doublePeculeVacancesEur: 2766,
      primeFinAnneeEur: 3000,
      ecoChequesValeurAnnuelleEur: 250,
      chequesRepasValeurAnnuelleEur: 1452,
      totalAvantagesAnnuelsEur: 9868,
      formule: 'pécule simple = 8 % × salaire brut × jours/250',
      baseJuridique: 'Art. 38 loi 28/06/1971 + AR 30/03/1967 + CCT 90 + CCT 98',
      messages: ['Pécule de vacances dû conformément à la loi du 28/06/1971.'],
      country: 'BELGIQUE',
      ...overrides,
    };
  }

  function flushSourceExplanations(): void {
    httpMock.match((r) => r.url.endsWith('/source-explanations')).forEach((r) => r.flush([]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        AvantagesConventionnelsBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AvantagesConventionnelsBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ============================================================
  // Gate pays + init
  // ============================================================

  it('BELGIQUE → isBelgium() true, GET appelé au ngOnInit', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.isBelgium()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
  });

  it('FRANCE → isBelgium() false, aucun appel HTTP au ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isBelgium()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
    httpMock.expectNone((r) => r.url.endsWith('/source-explanations'));
  });

  it('charge l\'analyse existante si GET 200 (mode résultat hydraté)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    flushSourceExplanations();

    expect(component.result()!.totalAvantagesAnnuelsEur).toBe(9868);
    expect(component.showForm()).toBe(false);
    expect(component.salaireMensuelBrutEur()).toBe(3000);
    expect(component.joursTravaillesAnneePrecedente()).toBe(220);
    expect(component.anciennetteMois()).toBe(36);
    expect(component.commissionParitaire()).toBe('CP_200');
    expect(component.annee()).toBe(2026);
    expect(component.doublePeculeVacancesPercu()).toBe(false);
    expect(component.primeFinAnneePrevueCcCp()).toBe(true);
    expect(component.ecoChequesPrevuCcCp()).toBe(true);
    expect(component.ecoChequesUtilisationDansAn()).toBe(true);
    expect(component.chequesRepasPrevu()).toBe(true);
    expect(component.joursPrestesEffectifs()).toBe(220);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ============================================================
  // formValid()
  // ============================================================

  function fillValidForm(): void {
    component.salaireMensuelBrutEur.set(3000);
    component.joursTravaillesAnneePrecedente.set(220);
    component.anciennetteMois.set(36);
    component.commissionParitaire.set('CP_200');
    component.annee.set(2026);
    component.joursPrestesEffectifs.set(220);
  }

  it('formValid true avec valeurs canoniques', () => {
    fillValidForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si salaire ≤ 0', () => {
    fillValidForm();
    component.salaireMensuelBrutEur.set(0);
    expect(component.formValid()).toBe(false);
    component.salaireMensuelBrutEur.set(-100);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si jours travaillés < 0 ou > 365', () => {
    fillValidForm();
    component.joursTravaillesAnneePrecedente.set(-1);
    expect(component.formValid()).toBe(false);
    component.joursTravaillesAnneePrecedente.set(366);
    expect(component.formValid()).toBe(false);
    component.joursTravaillesAnneePrecedente.set(0);
    expect(component.formValid()).toBe(true);
    component.joursTravaillesAnneePrecedente.set(365);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si ancienneté en mois < 0 ou non-entier', () => {
    fillValidForm();
    component.anciennetteMois.set(-1);
    expect(component.formValid()).toBe(false);
    component.anciennetteMois.set(2.5);
    expect(component.formValid()).toBe(false);
    component.anciennetteMois.set(0);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si commission paritaire absente', () => {
    fillValidForm();
    component.commissionParitaire.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si année < 2020 ou > 2030', () => {
    fillValidForm();
    component.annee.set(2019);
    expect(component.formValid()).toBe(false);
    component.annee.set(2031);
    expect(component.formValid()).toBe(false);
    component.annee.set(2020);
    expect(component.formValid()).toBe(true);
    component.annee.set(2030);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si joursPrestesEffectifs < 0 ou non-entier', () => {
    fillValidForm();
    component.joursPrestesEffectifs.set(-1);
    expect(component.formValid()).toBe(false);
    component.joursPrestesEffectifs.set(2.5);
    expect(component.formValid()).toBe(false);
    component.joursPrestesEffectifs.set(0);
    expect(component.formValid()).toBe(true);
  });

  // ============================================================
  // calculate() / POST
  // ============================================================

  it('calculate() POST body valide → succès hydrate result + snackbar + refresh', () => {
    fillValidForm();
    component.doublePeculeVacancesPercu.set(false);
    component.primeFinAnneePrevueCcCp.set(true);
    component.ecoChequesPrevuCcCp.set(true);
    component.ecoChequesUtilisationDansAn.set(true);
    component.chequesRepasPrevu.set(true);

    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      salaireMensuelBrutEur: 3000,
      joursTravaillesAnneePrecedente: 220,
      anciennetteMois: 36,
      commissionParitaire: 'CP_200',
      annee: 2026,
      doublePeculeVacancesPercu: false,
      primeFinAnneePrevueCcCp: true,
      ecoChequesPrevuCcCp: true,
      ecoChequesUtilisationDansAn: true,
      chequesRepasPrevu: true,
      joursPrestesEffectifs: 220,
    });
    req.flush(response());

    expect(component.result()!.totalAvantagesAnnuelsEur).toBe(9868);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Avantages conventionnels analysés',
      'OK',
      jasmine.any(Object),
    );
  });

  it('calculate() erreur backend → snackbar rouge + calculating reset', () => {
    fillValidForm();
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

  it('calculate() ignoré si form invalide (pas d\'appel HTTP POST)', () => {
    component.salaireMensuelBrutEur.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ============================================================
  // Pré-fill IA + handlers
  // ============================================================

  it('prefillFromAi : salaireBrutMensuel → salaireMensuelBrutEur + badge IA', () => {
    const ai: TravailExtractedData = { salaireBrutMensuel: 2500 };
    component.aiData = ai;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.salaireMensuelBrutEur()).toBe(2500);
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('prefillFromAi : aiData absent → aucun pré-fill, pas d\'erreur', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.salaireMensuelBrutEur()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('onSalaireChange efface provenanceSalaire', () => {
    component.provenanceSalaire.set('IA');
    component.onSalaireChange(4000);
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.salaireMensuelBrutEur()).toBe(4000);
  });

  // ============================================================
  // Coherence alerts (F-IA-03)
  // ============================================================

  it('coherenceAlerts : SALAIRE si divergence > 10 % entre aiData et saisie', () => {
    component.aiData = { salaireBrutMensuel: 3000 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(4000); // +33 %
    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE).toBeDefined();
    expect(alerts.SALAIRE!.expectedDisplay).toContain('€');
  });

  it('coherenceAlerts : pas d\'alerte SALAIRE si écart ≤ 10 %', () => {
    component.aiData = { salaireBrutMensuel: 3000 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(3100); // ~3 %
    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE).toBeUndefined();
  });

  it('coherenceAlerts retourne {} en mode résultat (showForm=false)', () => {
    component.aiData = { salaireBrutMensuel: 3000 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(5000);
    component.showForm.set(false); // mode résultat
    expect(component.coherenceAlerts()).toEqual({});
  });

  it('alertsSummary compte le nombre d\'alertes + blockers (jamais CRITICAL)', () => {
    component.aiData = { salaireBrutMensuel: 3000 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(5000);
    const summary = component.alertsSummary();
    expect(summary.total).toBe(1);
    expect(summary.blockers).toBe(0); // WARNING pas CRITICAL
  });

  // ============================================================
  // Banner / labels
  // ============================================================

  it('bannerClass total > 0 → avantages-banner--info (navy)', () => {
    expect(component.bannerClass(9868)).toContain('avantages-banner--info');
    expect(component.bannerClass(9868)).not.toContain('danger');
  });

  it('bannerClass total = 0 → avantages-banner--neutre — pas de rouge', () => {
    expect(component.bannerClass(0)).toContain('avantages-banner--neutre');
    expect(component.bannerClass(0)).not.toContain('danger');
    expect(component.bannerClass(null)).toContain('avantages-banner--neutre');
    expect(component.bannerClass(undefined)).toContain('avantages-banner--neutre');
  });

  it('commissionParitaireOptions expose au moins CP_200, CP_124, CP_111, CP_226, CP_337', () => {
    const codes = component.commissionParitaireOptions.map((o) => o.code);
    expect(codes).toContain('CP_200');
    expect(codes).toContain('CP_124');
    expect(codes).toContain('CP_111');
    expect(codes).toContain('CP_226');
    expect(codes).toContain('CP_337');
  });

  it('commissionParitaireLabel renvoie libellé humain ou code en fallback', () => {
    expect(component.commissionParitaireLabel('CP_200')).toContain('CP 200');
    expect(component.commissionParitaireLabel('CP_999')).toBe('CP_999');
    expect(component.commissionParitaireLabel(null)).toBe('');
    expect(component.commissionParitaireLabel(undefined)).toBe('');
  });

  // ============================================================
  // Divers
  // ============================================================

  it('editMode() → showForm true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('toggleCollapse() inverse l\'état collapsed', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('salaireEstDeduit reflète aiData.salaireEstDeduit', () => {
    component.aiData = { salaireEstDeduit: true };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.salaireEstDeduit()).toBe(true);
  });

  it('explanationFor SALAIRE renvoie tableau (fallback []) ', () => {
    expect(component.explanationFor('SALAIRE')).toEqual([]);
  });

  it('handlers toggles bool — set / unset', () => {
    component.onDoublePeculeChange(true);
    expect(component.doublePeculeVacancesPercu()).toBe(true);
    component.onPrimeFinAnneeChange(true);
    expect(component.primeFinAnneePrevueCcCp()).toBe(true);
    component.onEcoChequesPrevuChange(true);
    expect(component.ecoChequesPrevuCcCp()).toBe(true);
    component.onEcoChequesUtilisationChange(true);
    expect(component.ecoChequesUtilisationDansAn()).toBe(true);
    component.onChequesRepasChange(true);
    expect(component.chequesRepasPrevu()).toBe(true);

    component.onDoublePeculeChange(false);
    expect(component.doublePeculeVacancesPercu()).toBe(false);
  });
});
