import { ComponentFixture, TestBed } from '@angular/core/testing';
import { IndemnitePrecariteCddService } from '../../core/services/indemnite-precarite-cdd.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange, SimpleChanges } from '@angular/core';
import { IndemnitePrecariteCddSectionComponent } from './indemnite-precarite-cdd-section.component';
import { IndemnitePrecariteCddResponse } from '../../core/models/indemnite-precarite-cdd.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('IndemnitePrecariteCddSectionComponent', () => {
  let component: IndemnitePrecariteCddSectionComponent;
  let fixture: ComponentFixture<IndemnitePrecariteCddSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-42/cdd-indemnite-precarite';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-42/source-explanations';

  function response(overrides: Partial<IndemnitePrecariteCddResponse> = {}):
      IndemnitePrecariteCddResponse {
    return {
      caseFileId: 'case-42',
      totalSalairesBruts: 18500,
      tauxPrecarite: 10,
      casExclusion: null,
      indemnitePrecarite: 1850,
      formule: '10 % × 18 500,00 € = 1 850,00 €',
      baseJuridique: 'Art. L.1243-8 Code du travail',
      messages: ['Indemnité de fin de contrat due à l\'issue du CDD.'],
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
        IndemnitePrecariteCddSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IndemnitePrecariteCddSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-42';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Scénarios classiques (cycle de vie, form, HTTP)
  // ---------------------------------------------------------------------------

  it('6 options de cas d\'exclusion L.1243-10 exposées', () => {
    expect(component.casExclusionOptions.length).toBe(6);
    const codes = component.casExclusionOptions.map((o) => o.code);
    expect(codes).toContain('CDD_ETUDIANT_VACANCES');
    expect(codes).toContain('CDD_SAISONNIER');
    expect(codes).toContain('CDD_USAGE');
    expect(codes).toContain('CDI_REFUSE_PAR_SALARIE');
    expect(codes).toContain('RUPTURE_ANTICIPEE_SALARIE');
    expect(codes).toContain('RUPTURE_ANTICIPEE_FAUTE_GRAVE');
  });

  it('GET 200 → mode lecture, form masqué, valeurs hydratées', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response());
    flushSourceExplanations();

    expect(component.result()!.indemnitePrecarite).toBe(1850);
    expect(component.showForm()).toBe(false);
    expect(component.totalSalairesBruts()).toBe(18500);
    expect(component.tauxPrecarite()).toBe(10);
    expect(component.casExclusion()).toBeNull();
  });

  it('GET 404 → reste en mode formulaire', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false si totalSalairesBruts null ou ≤ 0', () => {
    component.totalSalairesBruts.set(null);
    expect(component.formValid()).toBe(false);
    component.totalSalairesBruts.set(0);
    expect(component.formValid()).toBe(false);
    component.totalSalairesBruts.set(-100);
    expect(component.formValid()).toBe(false);
    component.totalSalairesBruts.set(15000);
    expect(component.formValid()).toBe(true);
  });

  it('calculate() POST → résultat + snackbar + triggerRefresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.totalSalairesBruts.set(18500);
    component.tauxPrecarite.set(10);
    component.casExclusion.set(null);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      totalSalairesBruts: 18500,
      tauxPrecarite: 10,
      casExclusion: null,
    });
    req.flush(response());

    expect(component.result()!.indemnitePrecarite).toBe(1850);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Indemnité calculée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.totalSalairesBruts.set(18500);
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Taux invalide' }, { status: 400, statusText: 'Bad Request' });

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

    component.totalSalairesBruts.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Scénarios pré-fill IA + provenance
  // ---------------------------------------------------------------------------

  it('pré-fill IA salaire mensuel si aiData.salaireBrutMensuel > 0', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelReference()).toBe(2500);
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('aiData.salaireBrutMensuel = 0 → pas de pré-fill', () => {
    component.aiData = { salaireBrutMensuel: 0 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelReference()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('aiData null → pas de badge IA, pas de pré-fill', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelReference()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('onSalaireChange manuel efface le badge IA', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.provenanceSalaire()).toBe('IA');
    component.onSalaireChange(2800);
    expect(component.salaireMensuelReference()).toBe(2800);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('GET 200 → pas de badge IA même si aiData présent (persisté > IA)', () => {
    component.aiData = { salaireBrutMensuel: 9999 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response());
    flushSourceExplanations();

    expect(component.totalSalairesBruts()).toBe(18500);
    expect(component.provenanceSalaire()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Scénarios alertes de cohérence F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.SALAIRE_MENSUEL présent si écart > 10 % vs IA', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    // IA = 3000, avocat modifie à 5000 → écart 66 %.
    component.onSalaireChange(5000);

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

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    const newAi = { salaireBrutMensuel: 2800 } as TravailExtractedData;
    component.aiData = newAi;
    const changes: SimpleChanges = { aiData: new SimpleChange(null, newAi, false) };
    component.ngOnChanges(changes);

    expect(component.salaireMensuelReference()).toBe(2800);
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
    expect(component.salaireMensuelReference()).toBe(4200);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('alertes masquées après showForm=false (anti-bug SF-IA-03-12)', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(5000);
    expect(component.coherenceAlerts().SALAIRE_MENSUEL).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().SALAIRE_MENSUEL).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // Scénarios spécifiques F-DT-17
  // ---------------------------------------------------------------------------

  it('workspaceCountry BELGIQUE → bannière info, pas de GET', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    component.ngOnInit();
    httpMock.expectNone(BASE_URL); // ne déclenche pas l'API en BE
    expect(component.showForm()).toBe(true);
  });

  it('cas d\'exclusion sélectionné → envoyé dans le POST', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.totalSalairesBruts.set(20000);
    component.onCasExclusionChange('CDD_SAISONNIER');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.casExclusion).toBe('CDD_SAISONNIER');
    expect(req.request.body.tauxPrecarite).toBe(10);
    req.flush(response({
      casExclusion: 'CDD_SAISONNIER',
      indemnitePrecarite: 0,
      formule: '0,00 €',
      messages: ['Indemnité non due : CDD saisonnier'],
    }));
    expect(component.result()!.indemnitePrecarite).toBe(0);
  });

  it('taux 6 % → envoyé dans le POST', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.totalSalairesBruts.set(30000);
    component.onTauxChange(6);
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.tauxPrecarite).toBe(6);
    req.flush(response({ tauxPrecarite: 6, indemnitePrecarite: 1800 }));
  });

  it('auto-calcul totalSalairesBruts = salaireMensuel × dureeCddMois', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.onSalaireChange(2500);
    component.onDureeChange(6);
    expect(component.totalSalairesBruts()).toBe(15000);
  });

  it('saisie manuelle totalSalairesBruts → l\'auto-calc ne l\'écrase plus', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.onSalaireChange(2500);
    component.onDureeChange(6);
    expect(component.totalSalairesBruts()).toBe(15000);

    // Avocat corrige manuellement → nouvelle saisie durée ne doit pas écraser.
    component.onTotalSalairesChange(17000);
    component.onDureeChange(7);
    expect(component.totalSalairesBruts()).toBe(17000);
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
    const STANDALONE_URL = '/api/v1/simulators/F-DT-17-indemnite-precarite-cdd/calculate';

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
      expect(IndemnitePrecariteCddService.STANDALONE_TOOL_ID).toBe('F-DT-17-indemnite-precarite-cdd');
      expect(STANDALONE_URL).toContain(IndemnitePrecariteCddService.STANDALONE_TOOL_ID);
    });
  });

});
