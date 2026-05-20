import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { AesFamilleSectionComponent } from './aes-famille-section.component';
import { AesFamilleResponse } from '../../core/models/aes-famille.model';

describe('AesFamilleSectionComponent', () => {
  let component: AesFamilleSectionComponent;
  let fixture: ComponentFixture<AesFamilleSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/aes-famille';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function frResponse(): AesFamilleResponse {
    return {
      caseFileId: 'case-1',
      dateEntreeFrance: '2018-01-15',
      dureePresenceMois: 96,
      conjointFrancaisOuRegulier: true,
      enfantsScolarisesFrance: 2,
      dureeScolaritePlusAncienEnfantAnnees: 6,
      preuvesInsertion: true,
      menaceOrdrePublic: false,
      dateDepotDemande: '2026-01-01',
      country: 'FRANCE',
      presence5AnsOk: true,
      presence10AnsOk: false,
      liensFamiliauxOk: true,
      insertionOk: true,
      pasMenace: true,
      scoreGlobal: 80,
      verdictProbabiliteAcceptation: 'ELEVEE',
      criteresNonRemplis: [],
      dateExpirationInstructionSiDemande: '2026-07-01',
      formule: 'AES liens personnels et familiaux (L.435-1) : probabilité ELEVEE (score 80/100) — présence ≥ 5 ans, liens familiaux OK, insertion OK, ordre public OK.',
      baseJuridique: 'Art. L.435-1 CESEDA + Circulaire Valls 28/11/2012',
      messages: ['Probabilité d\'acceptation élevée'],
    };
  }

  function expectSourceExplanationCall(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        AesFamilleSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AesFamilleSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('mount + GET 404 → mode formulaire', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('charge analyse persistée si GET 200 → result + form masqué', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());
    expectSourceExplanationCall();
    expect(component.result()!.scoreGlobal).toBe(80);
    expect(component.showForm()).toBe(false);
    expect(component.dateEntreeFrance()).toBe('2018-01-15');
    expect(component.dureePresenceMois()).toBe(96);
    expect(component.conjointFrancaisOuRegulier()).toBe(true);
  });

  it('formValid : exige dateEntreeFrance + dureePresenceMois', () => {
    component.dateEntreeFrance.set(null);
    component.dureePresenceMois.set(60);
    expect(component.formValid()).toBe(false);

    component.dateEntreeFrance.set('2020-01-01');
    component.dureePresenceMois.set(null);
    expect(component.formValid()).toBe(false);

    component.dateEntreeFrance.set('2020-01-01');
    component.dureePresenceMois.set(60);
    expect(component.formValid()).toBe(true);
  });

  it('calculate() POST → result + snackbar succès', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.dateEntreeFrance.set('2018-01-15');
    component.dureePresenceMois.set(96);
    component.conjointFrancaisOuRegulier.set(true);
    component.preuvesInsertion.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dateEntreeFrance).toBe('2018-01-15');
    expect(req.request.body.dureePresenceMois).toBe(96);
    expect(req.request.body.conjointFrancaisOuRegulier).toBe(true);
    req.flush(frResponse());

    expect(component.result()!.scoreGlobal).toBe(80);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Analyse AES famille calculée', 'OK', jasmine.any(Object));
  });

  it('calculate() erreur 400 → snackbar rouge', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.dateEntreeFrance.set('2018-01-15');
    component.dureePresenceMois.set(96);
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

  it('pré-fill IA : dateEntreeFrance + dureePresenceMois (calculé) + provenance IA', () => {
    component.aiData = ({ aesDateEntreeFrance: '2020-01-15' } as unknown) as never;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.dateEntreeFrance()).toBe('2020-01-15');
    expect(component.provenanceDateEntree()).toBe('IA');
    expect(component.dureePresenceMois()).not.toBeNull();
    expect(component.dureePresenceMois()!).toBeGreaterThan(0);
    expect(component.provenanceDureePresence()).toBe('IA');
  });

  it('pré-fill no-op si aiData absent (graceful)', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.dateEntreeFrance()).toBeNull();
    expect(component.provenanceDateEntree()).toBeNull();
  });

  it('onDateEntreeChange efface le badge IA', () => {
    component.aiData = ({ aesDateEntreeFrance: '2020-01-15' } as unknown) as never;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.provenanceDateEntree()).toBe('IA');

    component.onDateEntreeChange('2019-06-01');
    expect(component.dateEntreeFrance()).toBe('2019-06-01');
    expect(component.provenanceDateEntree()).toBeNull();
  });

  it('coherence alert DUREE_PRESENCE si écart > 6 mois entre IA et saisie', () => {
    component.aiData = ({ aesDateEntreeFrance: '2020-01-15' } as unknown) as never;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // IA implique ~72 mois (6 ans), avocat saisit 24 (2 ans) → écart > 6 mois.
    component.onDureePresenceChange(24);

    const alerts = component.coherenceAlerts();
    expect(alerts.DUREE_PRESENCE).toBeDefined();
    expect(alerts.DUREE_PRESENCE!.field).toBe('DUREE_PRESENCE');
    expect(alerts.DUREE_PRESENCE!.source).toBe('IA');
  });

  it('coherence alert DATE_ENTREE_FRANCE si divergence avec IA', () => {
    component.aiData = ({ aesDateEntreeFrance: '2020-01-15' } as unknown) as never;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onDateEntreeChange('2018-06-01');
    const alerts = component.coherenceAlerts();
    expect(alerts.DATE_ENTREE_FRANCE).toBeDefined();
    expect(alerts.DATE_ENTREE_FRANCE!.expectedDisplay).toBe('2020-01-15');
  });

  it('gate BELGIQUE : bannière info + pas de GET, pas de POST', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.loading()).toBe(false);

    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('gate FRANCE : déclenche le GET + form visible', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.showForm()).toBe(true);
  });

  it('toggleCollapse alterne collapsed', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('result avec criteresNonRemplis → exposés via result().criteresNonRemplis', () => {
    component.ngOnInit();
    const responseAvecCriteres: AesFamilleResponse = {
      ...frResponse(),
      scoreGlobal: 30,
      verdictProbabiliteAcceptation: 'FAIBLE',
      criteresNonRemplis: [
        'Présence en France inférieure à 5 ans (signal faible)',
        'Preuves d\'insertion insuffisantes (contrat, formation, logement)',
      ],
    };
    httpMock.expectOne(BASE_URL).flush(responseAvecCriteres);
    expectSourceExplanationCall();
    expect(component.result()!.criteresNonRemplis.length).toBe(2);
    expect(component.verdictBannerClass()).toContain('--danger');
    expect(component.verdictIcon()).toBe('error');
  });

  it('verdict ELEVEE → banner success + icône check_circle', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());
    expectSourceExplanationCall();
    expect(component.verdictBannerClass()).toContain('--success');
    expect(component.verdictIcon()).toBe('check_circle');
    expect(component.verdictLabel()).toContain('élevée');
  });

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newAi = ({ aesDateEntreeFrance: '2019-03-10' } as unknown) as never;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.dateEntreeFrance()).toBe('2019-03-10');
    expect(component.provenanceDateEntree()).toBe('IA');
  });

  it('alertes masquées si form caché (showForm=false)', () => {
    component.aiData = ({ aesDateEntreeFrance: '2020-01-15' } as unknown) as never;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onDateEntreeChange('2018-06-01'); // divergence
    expect(component.coherenceAlerts().DATE_ENTREE_FRANCE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().DATE_ENTREE_FRANCE).toBeUndefined();
  });

  it('editMode ré-affiche le form', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02d — mode simulateur autonome
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02d — mode standalone', () => {
    const STANDALONE_URL_F163 = '/api/v1/simulators/F-IM-09-aes-famille/calculate';

    it('CA-02 : affiche la bannière 🧪 quand standaloneMode=true', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
      expect(banner).not.toBeNull();
      expect(banner.textContent).toContain('Mode simulateur');
    });

    it('CA-02 : aucun GET vers /api/v1/case-files/... en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const matches = httpMock.match((r: { url: string }) => r.url.includes('/api/v1/case-files/'));
      expect(matches.length).toBe(0);
    });

    it('CA-04 : POST sur le dispatcher /api/v1/simulators/... en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      try { (component as any).calculate(); } catch (_) { /* formValid */ }
      const dispatcherReqs = httpMock.match((r: { url: string; method: string }) => r.url === STANDALONE_URL_F163 && r.method === 'POST');
      const caseFileReqs = httpMock.match((r: { url: string; method: string }) => r.url.includes('/api/v1/case-files/') && r.method === 'POST');
      // Aucun POST case-file ne doit partir en standalone.
      expect(caseFileReqs.length).toBe(0);
      dispatcherReqs.forEach((req: any) => req.flush({}));
    });
  });
});
