import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CongesPayesIndemniteService } from '../../core/services/conges-payes-indemnite.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange, SimpleChanges } from '@angular/core';
import { CongesPayesSectionComponent } from './conges-payes-section.component';
import { CongesPayesIndemniteResponse } from '../../core/models/conges-payes-indemnite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('CongesPayesSectionComponent', () => {
  let component: CongesPayesSectionComponent;
  let fixture: ComponentFixture<CongesPayesSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-42/conges-payes-indemnite';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-42/source-explanations';

  function response(overrides: Partial<CongesPayesIndemniteResponse> = {}):
      CongesPayesIndemniteResponse {
    return {
      caseFileId: 'case-42',
      totalRemunerationPeriodeEur: 30000,
      joursAcquisAnnee: 30,
      joursPris: 10,
      salaireMensuelBrutEur: 2500,
      dateRupture: '2026-03-15',
      methodeForcee: null,
      joursDus: 20,
      montantMethodeDixPourcentEur: 2000,
      montantMethodeMaintienEur: 2000,
      methodeRetenue: 'DIX_POURCENT',
      montantIndemniteEur: 2000,
      baseJuridique: 'Art. L.3141-26 Code du travail',
      formule: '10 % × 30 000 € × (20/30) = 2 000 €',
      messages: ['Indemnité compensatrice due à l\'issue du contrat (art. L.3141-26).'],
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
        CongesPayesSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CongesPayesSectionComponent);
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
    flushSourceExplanations();
  });

  it('BELGIQUE → aucun appel HTTP, mode formulaire actif', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.showForm()).toBe(true);
  });

  it('GET 200 → mode résultat, valeurs hydratées', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    flushSourceExplanations();

    expect(component.result()!.montantIndemniteEur).toBe(2000);
    expect(component.showForm()).toBe(false);
    expect(component.totalRemunerationPeriodeEur()).toBe(30000);
    expect(component.joursAcquisAnnee()).toBe(30);
    expect(component.joursPris()).toBe(10);
    expect(component.salaireMensuelBrutEur()).toBe(2500);
    expect(component.dateRupture()).toBe('2026-03-15');
    expect(component.methodeForcee()).toBeNull();
  });

  it('GET 404 → reste en mode formulaire', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  it('formValid false si totalRemunerationPeriodeEur ≤ 0', () => {
    component.totalRemunerationPeriodeEur.set(null);
    component.joursAcquisAnnee.set(30);
    component.joursPris.set(10);
    component.salaireMensuelBrutEur.set(2500);
    component.dateRupture.set('2026-03-15');
    expect(component.formValid()).toBe(false);
    component.totalRemunerationPeriodeEur.set(0);
    expect(component.formValid()).toBe(false);
    component.totalRemunerationPeriodeEur.set(-100);
    expect(component.formValid()).toBe(false);
    component.totalRemunerationPeriodeEur.set(15000);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si salaireMensuelBrutEur ≤ 0', () => {
    component.totalRemunerationPeriodeEur.set(15000);
    component.joursAcquisAnnee.set(30);
    component.joursPris.set(10);
    component.salaireMensuelBrutEur.set(0);
    component.dateRupture.set('2026-03-15');
    expect(component.formValid()).toBe(false);
    component.salaireMensuelBrutEur.set(-50);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si dateRupture vide ou future', () => {
    component.totalRemunerationPeriodeEur.set(15000);
    component.joursAcquisAnnee.set(30);
    component.joursPris.set(10);
    component.salaireMensuelBrutEur.set(2500);
    component.dateRupture.set(null);
    expect(component.formValid()).toBe(false);
    // future
    component.dateRupture.set('2099-12-31');
    expect(component.formValid()).toBe(false);
    // today/passé OK
    component.dateRupture.set(component.todayIso);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si joursPris > joursAcquisAnnee (UI gate)', () => {
    component.totalRemunerationPeriodeEur.set(15000);
    component.joursAcquisAnnee.set(20);
    component.joursPris.set(25);
    component.salaireMensuelBrutEur.set(2500);
    component.dateRupture.set('2026-03-15');
    expect(component.formValid()).toBe(false);
    component.joursPris.set(20);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si joursAcquisAnnee ou joursPris null', () => {
    component.totalRemunerationPeriodeEur.set(15000);
    component.salaireMensuelBrutEur.set(2500);
    component.dateRupture.set('2026-03-15');
    component.joursAcquisAnnee.set(null);
    component.joursPris.set(10);
    expect(component.formValid()).toBe(false);
    component.joursAcquisAnnee.set(30);
    component.joursPris.set(null);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // POST
  // ---------------------------------------------------------------------------

  it('calculate() POST → résultat + snackbar + triggerRefresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.totalRemunerationPeriodeEur.set(30000);
    component.joursAcquisAnnee.set(30);
    component.joursPris.set(10);
    component.salaireMensuelBrutEur.set(2500);
    component.dateRupture.set('2026-03-15');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      totalRemunerationPeriodeEur: 30000,
      joursAcquisAnnee: 30,
      joursPris: 10,
      salaireMensuelBrutEur: 2500,
      dateRupture: '2026-03-15',
      methodeForcee: null,
    });
    req.flush(response());

    expect(component.result()!.montantIndemniteEur).toBe(2000);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Indemnité de congés payés calculée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.totalRemunerationPeriodeEur.set(30000);
    component.joursAcquisAnnee.set(30);
    component.joursPris.set(10);
    component.salaireMensuelBrutEur.set(2500);
    component.dateRupture.set('2026-03-15');
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'joursPris > joursAcquisAnnee' }, { status: 400, statusText: 'Bad Request' });

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

    // form vide : pas de POST.
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA
  // ---------------------------------------------------------------------------

  it('pré-fill IA salaire si aiData.salaireBrutMensuel > 0', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelBrutEur()).toBe(2500);
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('pré-fill IA dateRupture si aiData.dateLicenciement présent', () => {
    component.aiData = { dateLicenciement: '2026-02-28' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.dateRupture()).toBe('2026-02-28');
    expect(component.provenanceDateRupture()).toBe('IA');
  });

  it('aiData.salaireBrutMensuel = 0 → pas de pré-fill', () => {
    component.aiData = { salaireBrutMensuel: 0 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelBrutEur()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('aiData null → pas de pré-fill, pas de badge', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelBrutEur()).toBeNull();
    expect(component.dateRupture()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.provenanceDateRupture()).toBeNull();
  });

  it('onSalaireChange manuel efface le badge IA', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.provenanceSalaire()).toBe('IA');
    component.onSalaireChange(2800);
    expect(component.salaireMensuelBrutEur()).toBe(2800);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('onDateRuptureChange manuel efface le badge IA', () => {
    component.aiData = { dateLicenciement: '2026-02-28' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.provenanceDateRupture()).toBe('IA');
    component.onDateRuptureChange('2026-03-01');
    expect(component.dateRupture()).toBe('2026-03-01');
    expect(component.provenanceDateRupture()).toBeNull();
  });

  it('GET 200 → pas de badge IA même si aiData présent (persisté > IA)', () => {
    component.aiData = { salaireBrutMensuel: 9999, dateLicenciement: '2099-01-01' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response());
    flushSourceExplanations();

    expect(component.salaireMensuelBrutEur()).toBe(2500);
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.provenanceDateRupture()).toBeNull();
  });

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    const newAi = { salaireBrutMensuel: 2800 } as TravailExtractedData;
    component.aiData = newAi;
    const changes: SimpleChanges = { aiData: new SimpleChange(null, newAi, false) };
    component.ngOnChanges(changes);

    expect(component.salaireMensuelBrutEur()).toBe(2800);
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.onSalaireChange(4200);
    expect(component.provenanceSalaire()).toBeNull();

    const newAi = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    // Saisie avocat préservée.
    expect(component.salaireMensuelBrutEur()).toBe(4200);
    expect(component.provenanceSalaire()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Alertes F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.SALAIRE_MENSUEL présent si écart > 10 % vs IA', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(5000);

    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE_MENSUEL).toBeDefined();
    expect(alerts.SALAIRE_MENSUEL!.field).toBe('SALAIRE_MENSUEL');
    expect(alerts.SALAIRE_MENSUEL!.source).toBe('IA');
    expect(alerts.SALAIRE_MENSUEL!.severity).toBe('WARNING');
    expect(alerts.SALAIRE_MENSUEL!.expectedDisplay).toContain('€');
  });

  it('coherenceAlerts.SALAIRE_MENSUEL absent si écart ≤ 10 %', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(3100);
    expect(component.coherenceAlerts().SALAIRE_MENSUEL).toBeUndefined();
  });

  it('coherenceAlerts.DATE_RUPTURE présent si dateLicenciement IA != saisie', () => {
    component.aiData = { dateLicenciement: '2026-02-28' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onDateRuptureChange('2026-03-15');

    const alerts = component.coherenceAlerts();
    expect(alerts.DATE_RUPTURE).toBeDefined();
    expect(alerts.DATE_RUPTURE!.field).toBe('DATE_RUPTURE');
    expect(alerts.DATE_RUPTURE!.source).toBe('IA');
    expect(alerts.DATE_RUPTURE!.expectedDisplay).toBe('2026-02-28');
  });

  it('coherenceAlerts.SALAIRE_DEDUIT (note INFO) si aiData.salaireEstDeduit=true', () => {
    component.aiData = { salaireBrutMensuel: 2500, salaireEstDeduit: true } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE_DEDUIT).toBeDefined();
    expect(alerts.SALAIRE_DEDUIT!.severity).toBe('INFO');
    expect(alerts.SALAIRE_DEDUIT!.expectedDisplay).toContain('×1,30');
  });

  it('coherenceAlerts.SALAIRE_DEDUIT absent si salaireEstDeduit !== true', () => {
    component.aiData = { salaireBrutMensuel: 2500, salaireEstDeduit: false } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.coherenceAlerts().SALAIRE_DEDUIT).toBeUndefined();
  });

  it('coherenceAlerts.DATE_RUPTURE absent si dates identiques', () => {
    component.aiData = { dateLicenciement: '2026-02-28' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    // Pré-fill a posé '2026-02-28', alerte = absente.
    expect(component.dateRupture()).toBe('2026-02-28');
    expect(component.coherenceAlerts().DATE_RUPTURE).toBeUndefined();
  });

  it('alertes masquées après showForm=false (anti-bug SF-IA-03-12)', () => {
    component.aiData = { salaireBrutMensuel: 3000, dateLicenciement: '2026-02-28' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(5000);
    component.onDateRuptureChange('2026-03-15');
    expect(component.coherenceAlerts().SALAIRE_MENSUEL).toBeDefined();
    expect(component.coherenceAlerts().DATE_RUPTURE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().SALAIRE_MENSUEL).toBeUndefined();
    expect(component.coherenceAlerts().DATE_RUPTURE).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // Méthode forcée
  // ---------------------------------------------------------------------------

  it('méthode forcée DIX_POURCENT envoyée dans le POST', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.totalRemunerationPeriodeEur.set(30000);
    component.joursAcquisAnnee.set(30);
    component.joursPris.set(10);
    component.salaireMensuelBrutEur.set(2500);
    component.dateRupture.set('2026-03-15');
    component.onMethodeForceeChange('DIX_POURCENT');
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.methodeForcee).toBe('DIX_POURCENT');
    req.flush(response({ methodeForcee: 'DIX_POURCENT', methodeRetenue: 'DIX_POURCENT' }));
  });

  it('méthode forcée MAINTIEN envoyée dans le POST', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.totalRemunerationPeriodeEur.set(30000);
    component.joursAcquisAnnee.set(30);
    component.joursPris.set(10);
    component.salaireMensuelBrutEur.set(2500);
    component.dateRupture.set('2026-03-15');
    component.onMethodeForceeChange('MAINTIEN');
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.methodeForcee).toBe('MAINTIEN');
    req.flush(response({ methodeForcee: 'MAINTIEN', methodeRetenue: 'MAINTIEN' }));
  });

  it('méthode forcée Auto (null) envoyée dans le POST', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.totalRemunerationPeriodeEur.set(30000);
    component.joursAcquisAnnee.set(30);
    component.joursPris.set(10);
    component.salaireMensuelBrutEur.set(2500);
    component.dateRupture.set('2026-03-15');
    // Pas de surcharge — le radio reste à null.
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.methodeForcee).toBeNull();
    req.flush(response());
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

  it('methodeLabel + methodeArticle helpers', () => {
    expect(component.methodeLabel('DIX_POURCENT')).toBe('Méthode du dixième');
    expect(component.methodeLabel('MAINTIEN')).toBe('Méthode du maintien');
    expect(component.methodeArticle('DIX_POURCENT')).toContain('L.3141-24');
    expect(component.methodeArticle('MAINTIEN')).toContain('L.3141-22');
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
  // F-163 SF-163-02b — mode standalone (CA-08, CA-09, CA-10).
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02b — mode standalone', () => {
    const STANDALONE_URL = '/api/v1/simulators/F-DT-26-conges-payes-indemnite/calculate';

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
      expect(CongesPayesIndemniteService.STANDALONE_TOOL_ID).toBe('F-DT-26-conges-payes-indemnite');
      expect(STANDALONE_URL).toContain(CongesPayesIndemniteService.STANDALONE_TOOL_ID);
    });
  });

});
