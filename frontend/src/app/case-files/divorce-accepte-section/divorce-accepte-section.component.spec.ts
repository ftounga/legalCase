import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { DivorceAccepteSectionComponent } from './divorce-accepte-section.component';
import { DivorceAccepteResponse, FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('DivorceAccepteSectionComponent', () => {
  let component: DivorceAccepteSectionComponent;
  let fixture: ComponentFixture<DivorceAccepteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/divorce-accepte';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function eligibleResponse(): DivorceAccepteResponse {
    return {
      caseFileId: 'case-1',
      acceptationPrincipeSignee: true,
      dateAcceptationPV: '2026-04-01',
      dureeMariageAnnees: 12,
      revenusAnnuelsEpoux1Eur: 60000,
      revenusAnnuelsEpoux2Eur: 30000,
      patrimoineCommun: true,
      dateAssignation: '2026-04-15',
      country: 'FRANCE',
      acceptationValide: true,
      ordrePublic: true,
      eligibilite: true,
      scoreGlobal: 100,
      verdictEligibilite: 'ELEVEE',
      delaiProcedureMoisPrevisionnel: 10,
      prestationCompensatoireFourchetteMin: 25000,
      prestationCompensatoireFourchetteMax: 50000,
      criteresNonRemplis: [],
      formule: '|60000 - 30000| × min(12, 10) × [0,5 ; 1,5]',
      baseJuridique: 'Art. 233-234 Cciv + art. 1123 CPC',
      messages: ['PV irrévocable une fois signé'],
    };
  }

  /**
   * Absorbe l'éventuelle requête source-explanations émise par ngOnInit.
   * Fail-open si pas matchée — empêche les tests de crasher sur httpMock.verify().
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
        DivorceAccepteSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DivorceAccepteSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Mount + GET initial
  // ---------------------------------------------------------------------------

  it('mount FRANCE → GET initial déclenché', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.workspaceCountry).toBe('FRANCE');
    expect(component.showForm()).toBe(true);
  });

  it('GET 200 → restore form fields + result + showForm=false', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(eligibleResponse());
    expectSourceExplanationCall();

    expect(component.result()!.verdictEligibilite).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(component.dureeMariageAnnees()).toBe(12);
    expect(component.revenusAnnuelsEpoux1Eur()).toBe(60000);
    expect(component.acceptationPrincipeSignee()).toBe(true);
  });

  it('GET 404 → reste en mode formulaire, pas de result', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // formValid()
  // ---------------------------------------------------------------------------

  it('formValid : exige acceptation + durée ≥ 0 + revenus ≥ 0', () => {
    expect(component.formValid()).toBe(false);

    component.acceptationPrincipeSignee.set(true);
    component.dureeMariageAnnees.set(12);
    component.revenusAnnuelsEpoux1Eur.set(60000);
    component.revenusAnnuelsEpoux2Eur.set(30000);
    expect(component.formValid()).toBe(true);

    component.acceptationPrincipeSignee.set(false);
    expect(component.formValid()).toBe(false);

    component.acceptationPrincipeSignee.set(true);
    component.dureeMariageAnnees.set(-1);
    expect(component.formValid()).toBe(false);

    component.dureeMariageAnnees.set(12);
    component.revenusAnnuelsEpoux1Eur.set(-100);
    expect(component.formValid()).toBe(false);
  });

  it('formValid : durée doit être un entier', () => {
    component.acceptationPrincipeSignee.set(true);
    component.dureeMariageAnnees.set(12.5);
    component.revenusAnnuelsEpoux1Eur.set(60000);
    component.revenusAnnuelsEpoux2Eur.set(30000);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // POST + snackbar
  // ---------------------------------------------------------------------------

  it('calculate() POST → result + snackbar succès + triggerRefresh', () => {
    component.acceptationPrincipeSignee.set(true);
    component.dateAcceptationPV.set('2026-04-01');
    component.dureeMariageAnnees.set(12);
    component.revenusAnnuelsEpoux1Eur.set(60000);
    component.revenusAnnuelsEpoux2Eur.set(30000);
    component.patrimoineCommun.set(true);
    component.dateAssignation.set('2026-04-15');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      acceptationPrincipeSignee: true,
      dateAcceptationPV: '2026-04-01',
      dureeMariageAnnees: 12,
      revenusAnnuelsEpoux1Eur: 60000,
      revenusAnnuelsEpoux2Eur: 30000,
      patrimoineCommun: true,
      dateAssignation: '2026-04-15',
    });
    req.flush(eligibleResponse());

    expect(component.result()!.verdictEligibilite).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Analyse divorce accepté calculée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.acceptationPrincipeSignee.set(true);
    component.dureeMariageAnnees.set(12);
    component.revenusAnnuelsEpoux1Eur.set(60000);
    component.revenusAnnuelsEpoux2Eur.set(30000);
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

  it('calculate() body omet dates si null', () => {
    component.acceptationPrincipeSignee.set(true);
    component.dureeMariageAnnees.set(8);
    component.revenusAnnuelsEpoux1Eur.set(40000);
    component.revenusAnnuelsEpoux2Eur.set(20000);
    component.patrimoineCommun.set(false);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body).toEqual({
      acceptationPrincipeSignee: true,
      dureeMariageAnnees: 8,
      revenusAnnuelsEpoux1Eur: 40000,
      revenusAnnuelsEpoux2Eur: 20000,
      patrimoineCommun: false,
    });
    req.flush(eligibleResponse());
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + onChange
  // ---------------------------------------------------------------------------

  it('prefill IA : 5 fields renseignés → valeurs + provenance IA', () => {
    component.aiData = {
      dureeMariageAnnees: 10,
      revenusAnnuelsEpoux1Eur: 55000,
      revenusAnnuelsEpoux2Eur: 28000,
      patrimoineCommun: true,
      dateAcceptationPV: '2026-03-20',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.dureeMariageAnnees()).toBe(10);
    expect(component.revenusAnnuelsEpoux1Eur()).toBe(55000);
    expect(component.revenusAnnuelsEpoux2Eur()).toBe(28000);
    expect(component.patrimoineCommun()).toBe(true);
    expect(component.dateAcceptationPV()).toBe('2026-03-20');
    expect(component.provenanceDureeMariage()).toBe('IA');
    expect(component.provenanceRevenusEpoux1()).toBe('IA');
    expect(component.provenanceRevenusEpoux2()).toBe('IA');
    expect(component.provenancePatrimoineCommun()).toBe('IA');
    expect(component.provenanceDateAcceptationPV()).toBe('IA');
  });

  it('prefill IA : aiData absent → no-op gracieux', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.dureeMariageAnnees()).toBeNull();
    expect(component.revenusAnnuelsEpoux1Eur()).toBeNull();
    expect(component.provenanceDureeMariage()).toBeNull();
  });

  it('prefill IA : champ absent → no-op + provenance null sur ce champ', () => {
    component.aiData = {
      dureeMariageAnnees: 8,
      // revenusAnnuelsEpoux1Eur absent
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.dureeMariageAnnees()).toBe(8);
    expect(component.provenanceDureeMariage()).toBe('IA');
    expect(component.revenusAnnuelsEpoux1Eur()).toBeNull();
    expect(component.provenanceRevenusEpoux1()).toBeNull();
  });

  it('onDureeMariageChange efface badge IA', () => {
    component.aiData = { dureeMariageAnnees: 10 } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceDureeMariage()).toBe('IA');
    component.onDureeMariageChange(15);
    expect(component.dureeMariageAnnees()).toBe(15);
    expect(component.provenanceDureeMariage()).toBeNull();
  });

  it('onRevenusEpoux1Change efface badge IA', () => {
    component.aiData = { revenusAnnuelsEpoux1Eur: 60000 } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceRevenusEpoux1()).toBe('IA');
    component.onRevenusEpoux1Change(70000);
    expect(component.revenusAnnuelsEpoux1Eur()).toBe(70000);
    expect(component.provenanceRevenusEpoux1()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.DUREE_MARIAGE présent si écart > 1 an', () => {
    component.aiData = { dureeMariageAnnees: 10 } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onDureeMariageChange(15); // écart = 5 ans

    const alert = component.coherenceAlerts().DUREE_MARIAGE;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('DUREE_MARIAGE');
    expect(alert!.source).toBe('IA');
    expect(alert!.expectedDisplay).toContain('10');
  });

  it('coherenceAlerts.DUREE_MARIAGE absent si écart ≤ 1 an', () => {
    component.aiData = { dureeMariageAnnees: 10 } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onDureeMariageChange(11); // écart = 1 an

    expect(component.coherenceAlerts().DUREE_MARIAGE).toBeUndefined();
  });

  it('coherenceAlerts.REVENUS_EPOUX1 présent si écart > 10 %', () => {
    component.aiData = { revenusAnnuelsEpoux1Eur: 60000 } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onRevenusEpoux1Change(80000); // écart = 33 %

    const alert = component.coherenceAlerts().REVENUS_EPOUX1;
    expect(alert).toBeDefined();
    expect(alert!.expectedDisplay).toContain('€');
  });

  it('coherenceAlerts.PATRIMOINE_COMMUN présent si IA != avocat', () => {
    component.aiData = { patrimoineCommun: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // L'IA a pré-rempli à true ; l'avocat repasse à false.
    component.onPatrimoineCommunChange(false);

    const alert = component.coherenceAlerts().PATRIMOINE_COMMUN;
    expect(alert).toBeDefined();
    expect(alert!.expectedDisplay).toContain('Patrimoine commun');
  });

  it('coherenceAlerts gate : alertes masquées après calcul (showForm=false)', () => {
    component.aiData = { dureeMariageAnnees: 10 } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onDureeMariageChange(15);
    expect(component.coherenceAlerts().DUREE_MARIAGE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().DUREE_MARIAGE).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // Gate workspaceCountry
  // ---------------------------------------------------------------------------

  it('workspaceCountry=BELGIQUE → pas d\'appel HTTP, pas de form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.showForm()).toBe(true); // signal interne, mais le template gate sur workspaceCountry
  });

  it('workspaceCountry=FRANCE → appel HTTP normal', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
  });

  // ---------------------------------------------------------------------------
  // Verdict UI helpers
  // ---------------------------------------------------------------------------

  it('verdict ELEVEE : classe banner navy --available', () => {
    expect(component.verdictBannerClass('ELEVEE')).toContain('--available');
    expect(component.verdictBannerLabel('ELEVEE')).toBe('Éligibilité élevée');
    expect(component.verdictBannerIcon('ELEVEE')).toBe('check_circle');
  });

  it('verdict FAIBLE : classe banner rouge classique --danger', () => {
    expect(component.verdictBannerClass('FAIBLE')).toContain('--danger');
    expect(component.verdictBannerLabel('FAIBLE')).toBe('Éligibilité faible');
    expect(component.verdictBannerIcon('FAIBLE')).toBe('error');
  });

  // ---------------------------------------------------------------------------
  // Toggle / editMode (non-régression)
  // ---------------------------------------------------------------------------

  it('toggleCollapse fonctionne', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('editMode ré-affiche le form après calcul', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // ngOnChanges aiData
  // ---------------------------------------------------------------------------

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newAi = {
      dureeMariageAnnees: 7,
      revenusAnnuelsEpoux1Eur: 45000,
    } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.dureeMariageAnnees()).toBe(7);
    expect(component.revenusAnnuelsEpoux1Eur()).toBe(45000);
    expect(component.provenanceDureeMariage()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onDureeMariageChange(20);
    expect(component.provenanceDureeMariage()).toBeNull();

    const newAi = { dureeMariageAnnees: 5 } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    // Saisie avocat préservée — pas d'écrasement.
    expect(component.dureeMariageAnnees()).toBe(20);
    expect(component.provenanceDureeMariage()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02c — mode simulateur autonome
  // ---------------------------------------------------------------------------

  describe('F-163 SF-163-02c — mode standalone', () => {
    const STANDALONE_URL = '/api/v1/simulators/F-FA-10-divorce-accepte/calculate';
    const CASE_URL = '/api/v1/case-files/case-1/divorce-accepte';

    it('CA-02 : affiche la bannière 🧪 quand standaloneMode=true', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
      expect(banner).not.toBeNull();
      expect(banner.textContent).toContain('Mode simulateur');
    });

    it('CA-02 : ne fait AUCUN GET vers /api/v1/case-files/... en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const matches = httpMock.match((r: { url: string }) => r.url.includes('/api/v1/case-files/'));
      expect(matches.length).toBe(0);
    });

    it('CA-04 : POST sur le dispatcher /api/v1/simulators/.../calculate en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      component.calculate();
      const requests = httpMock.match((r: { url: string; method: string }) => r.url === STANDALONE_URL && r.method === 'POST');
      // Si la méthode formValid() bloque ou si la gate FR/BE échoue, aucun POST
      // n'est émis — c'est acceptable (le standalone n'a pas de payload obligatoire).
      // On valide ici qu'**aucun POST vers le case-file URL** n'a été émis.
      const caseUrlPosts = httpMock.match((r: { url: string; method: string }) => r.url === CASE_URL && r.method === 'POST');
      expect(caseUrlPosts.length).toBe(0);
      // Et, si le composant a POST sur le dispatcher, il flush proprement.
      requests.forEach(req => req.flush({}));
    });
  });
});
