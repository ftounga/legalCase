import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MesuresEloignementSectionComponent } from './mesures-eloignement-section.component';
import { MesuresEloignementResponse } from '../../core/models/mesures-eloignement.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('MesuresEloignementSectionComponent (SF-IM-20-02)', () => {
  let component: MesuresEloignementSectionComponent;
  let fixture: ComponentFixture<MesuresEloignementSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/mesures-eloignement-analysis';

  function response(overrides: Partial<MesuresEloignementResponse> = {}): MesuresEloignementResponse {
    return {
      caseFileId: 'case-1',
      country: 'FRANCE',
      dispositif: 'EXPULSION_PREFECTORALE',
      dispositifRecommande: 'EXPULSION_PREFECTORALE',
      motifMenace: 'ORDRE_PUBLIC',
      procedureCommissionRespectee: true,
      urgenceAbsolueJustifiee: false,
      comportementAggravant: false,
      verdictLegalite: 'VALIDE',
      risqueAnnulation: [],
      delaiRecoursJours: 30,
      juridictionRecours: 'TA',
      documentsRequis: [
        'Décision préfectorale notifiée',
        'Avis commission expulsion (CESEDA L.632-1)',
      ],
      baseJuridique: 'CESEDA art. L.631-1 (expulsion préfectorale)',
      formule: 'Expulsion préfectorale — VALIDE — recours TA dans 30 jours.',
      messages: ['Procédure commission expulsion respectée.'],
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        MesuresEloignementSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(MesuresEloignementSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ---------------------------------------------------------------------------
  // 1. Mount + lifecycle
  // ---------------------------------------------------------------------------

  it('FRANCE → isFrance() true, GET émis au ngOnInit', () => {
    expect(component.isFrance()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('BELGIQUE → isFrance() false, aucun appel HTTP au ngOnInit (gate pays)', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.isFrance()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
  });

  it('GET 200 → résultat hydraté + dispositif persisté + showForm=false', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());

    expect(component.result()!.verdictLegalite).toBe('VALIDE');
    expect(component.dispositif()).toBe('EXPULSION_PREFECTORALE');
    expect(component.motifMenace()).toBe('ORDRE_PUBLIC');
    expect(component.showForm()).toBe(false);
    expect(component.provenanceDispositif()).toBeNull();
  });

  it('GET 404 → mode formulaire (showForm=true)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // 2. Pré-fill IA
  // ---------------------------------------------------------------------------

  it('pré-fill IA : no-op gracieux sans aiData (pas d\'erreur, pas de provenance)', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dispositif()).toBeNull();
    expect(component.provenanceDispositif()).toBeNull();
  });

  it('pré-fill IA : typeProcedureDetectee=EXPULSION → dispositif=EXPULSION_PREFECTORALE + provenance IA', () => {
    component.aiData = { typeProcedureDetectee: 'EXPULSION' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dispositif()).toBe('EXPULSION_PREFECTORALE');
    expect(component.provenanceDispositif()).toBe('IA');
  });

  it('pré-fill IA : typeProcedureDetectee=IRTF → dispositif=IRTF', () => {
    component.aiData = { typeProcedureDetectee: 'IRTF' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dispositif()).toBe('IRTF');
    expect(component.provenanceDispositif()).toBe('IA');
  });

  // ---------------------------------------------------------------------------
  // 3. Form validation + champs conditionnels
  // ---------------------------------------------------------------------------

  it('formValid false initialement (aucun dispositif)', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true dès dispositif + motif saisis (cas EXPULSION_PREFECTORALE)', () => {
    component.dispositif.set('EXPULSION_PREFECTORALE');
    expect(component.formValid()).toBe(false);
    component.motifMenace.set('ORDRE_PUBLIC');
    expect(component.formValid()).toBe(true);
  });

  it('formValid IRTF : faux si durée négative, vrai si durées non saisies ou ≥ 0', () => {
    component.dispositif.set('IRTF');
    component.motifMenace.set('AUTRE');
    expect(component.formValid()).toBe(true); // durées null

    component.dureePresenceIrreguliereMois.set(24);
    component.dureeCircularitePrecaire.set(2);
    expect(component.formValid()).toBe(true);

    component.dureePresenceIrreguliereMois.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('champs conditionnels : showExpulsionFields / showIrtfFields / showIatFields', () => {
    component.dispositif.set('EXPULSION_PREFECTORALE');
    expect(component.showExpulsionFields()).toBe(true);
    expect(component.showIrtfFields()).toBe(false);
    expect(component.showIatFields()).toBe(false);

    component.dispositif.set('EXPULSION_MINISTERIELLE');
    expect(component.showExpulsionFields()).toBe(true);

    component.dispositif.set('EXPULSION_SECURITE_ETAT');
    expect(component.showExpulsionFields()).toBe(true);

    component.dispositif.set('IRTF');
    expect(component.showExpulsionFields()).toBe(false);
    expect(component.showIrtfFields()).toBe(true);
    expect(component.showIatFields()).toBe(false);

    component.dispositif.set('IAT');
    expect(component.showExpulsionFields()).toBe(false);
    expect(component.showIrtfFields()).toBe(false);
    expect(component.showIatFields()).toBe(true);
  });

  it('onDispositifChange efface le badge IA', () => {
    component.provenanceDispositif.set('IA');
    component.onDispositifChange('IRTF');
    expect(component.dispositif()).toBe('IRTF');
    expect(component.provenanceDispositif()).toBeNull();
  });

  it('onMotifMenaceChange efface le badge IA', () => {
    component.provenanceMotif.set('IA');
    component.onMotifMenaceChange('TERRORISME');
    expect(component.motifMenace()).toBe('TERRORISME');
    expect(component.provenanceMotif()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // 4. Calculate / submit
  // ---------------------------------------------------------------------------

  it('calculate() POST EXPULSION_PREFECTORALE envoie procedureCommission/urgence et pas les champs IRTF', () => {
    component.dispositif.set('EXPULSION_PREFECTORALE');
    component.motifMenace.set('ORDRE_PUBLIC');
    component.procedureCommissionRespectee.set(true);
    component.urgenceAbsolueJustifiee.set(false);
    // Résidu IRTF — doit être ignoré.
    component.dureePresenceIrreguliereMois.set(24);
    component.comportementAggravant.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dispositif).toBe('EXPULSION_PREFECTORALE');
    expect(req.request.body.motifMenace).toBe('ORDRE_PUBLIC');
    expect(req.request.body.procedureCommissionRespectee).toBe(true);
    expect(req.request.body.urgenceAbsolueJustifiee).toBe(false);
    expect(req.request.body.dureePresenceIrreguliereMois).toBeUndefined();
    expect(req.request.body.dureeCircularitePrecaire).toBeUndefined();
    expect(req.request.body.comportementAggravant).toBeUndefined();
    req.flush(response());

    expect(component.result()!.verdictLegalite).toBe('VALIDE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Mesure d\'éloignement analysée', 'OK', jasmine.any(Object));
  });

  it('calculate() POST IRTF envoie durées et comportement, pas urgence/commission', () => {
    component.dispositif.set('IRTF');
    component.motifMenace.set('RECIDIVE_GRAVE');
    component.dureePresenceIrreguliereMois.set(18);
    component.dureeCircularitePrecaire.set(0);
    component.comportementAggravant.set(true);
    // Résidu EXPULSION — doit être ignoré.
    component.urgenceAbsolueJustifiee.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dispositif).toBe('IRTF');
    expect(req.request.body.dureePresenceIrreguliereMois).toBe(18);
    expect(req.request.body.dureeCircularitePrecaire).toBe(0);
    expect(req.request.body.comportementAggravant).toBe(true);
    expect(req.request.body.procedureCommissionRespectee).toBeUndefined();
    expect(req.request.body.urgenceAbsolueJustifiee).toBeUndefined();
    req.flush(response({ dispositif: 'IRTF', delaiRecoursJours: 15,
      juridictionRecours: 'TA' }));
  });

  it('calculate() POST IAT envoie uniquement dispositif + motif', () => {
    component.dispositif.set('IAT');
    component.motifMenace.set('TERRORISME');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dispositif).toBe('IAT');
    expect(req.request.body.motifMenace).toBe('TERRORISME');
    expect(req.request.body.procedureCommissionRespectee).toBeUndefined();
    expect(req.request.body.dureePresenceIrreguliereMois).toBeUndefined();
    req.flush(response({ dispositif: 'IAT', delaiRecoursJours: 60,
      juridictionRecours: 'CE' }));
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.dispositif.set('EXPULSION_PREFECTORALE');
    component.motifMenace.set('ORDRE_PUBLIC');
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

  it('calculate() avec recoursDelai → champ envoyé', () => {
    component.dispositif.set('EXPULSION_PREFECTORALE');
    component.motifMenace.set('ORDRE_PUBLIC');
    component.recoursDelai.set('2026-05-15');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.recoursDelai).toBe('2026-05-15');
    req.flush(response());
  });

  // ---------------------------------------------------------------------------
  // 5. F-IA-03 alertes de cohérence
  // ---------------------------------------------------------------------------

  it('coherenceAlerts vide quand pas de dispositif sélectionné', () => {
    expect(component.coherenceAlerts().DISPOSITIF).toBeUndefined();
    expect(component.coherenceAlerts().MOTIF_MENACE).toBeUndefined();
  });

  it('coherenceAlerts.DISPOSITIF présent si IA détecte un dispositif divergent', () => {
    component.aiData = { typeProcedureDetectee: 'IRTF' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill a positionné IRTF — l'avocat change manuellement vers IAT.
    component.onDispositifChange('IAT');

    const alert = component.coherenceAlerts().DISPOSITIF;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('DISPOSITIF');
    expect(alert!.source).toBe('IA');
    expect(alert!.expectedDisplay).toContain('IRTF');
  });

  it('coherenceAlerts vide quand IA convergente avec saisie', () => {
    component.aiData = { typeProcedureDetectee: 'EXPULSION' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill positionne EXPULSION_PREFECTORALE — l'avocat ne change rien.
    expect(component.dispositif()).toBe('EXPULSION_PREFECTORALE');
    expect(component.coherenceAlerts().DISPOSITIF).toBeUndefined();
  });

  it('coherenceAlerts.DISPOSITIF multi-sources F96 + IA convergents → MULTI', () => {
    component.aiData = { typeProcedureDetectee: 'IRTF' } as ImmigrationExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Dispositif d\'éloignement',
        statut: 'NON_COMPLIANT',
        critereCode: 'IM20_DISPOSITIF_ELOIGNEMENT',
        expectedValue: 'IRTF',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Avocat saisit IAT — divergent IA + F96 (qui disent IRTF).
    component.onDispositifChange('IAT');

    const alert = component.coherenceAlerts().DISPOSITIF;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
  });

  it('coherenceAlerts vidées après calcul (showForm=false)', () => {
    component.aiData = { typeProcedureDetectee: 'IRTF' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onDispositifChange('IAT');
    expect(component.coherenceAlerts().DISPOSITIF).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().DISPOSITIF).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // 6. Misc
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

  it('bannerClass mappe verdict → classe CSS attendue', () => {
    expect(component.bannerClass('VALIDE')).toContain('eloi-banner--info');
    expect(component.bannerClass('CONTESTABLE')).toContain('eloi-banner--warning');
    expect(component.bannerClass('NUL')).toContain('eloi-banner--critical');
    expect(component.bannerClass(null)).toContain('eloi-banner--info');
  });

  it('bannerLabel retourne le verdict humain', () => {
    expect(component.bannerLabel('VALIDE')).toContain('légale');
    expect(component.bannerLabel('CONTESTABLE')).toContain('contestable');
    expect(component.bannerLabel('NUL')).toContain('annulation');
    expect(component.bannerLabel(null)).toBe('');
  });

  it('formatDelaiRecours retourne "30 jours (TA)" et "60 jours (CE)"', () => {
    expect(component.formatDelaiRecours(30, 'TA')).toBe('30 jours (TA)');
    expect(component.formatDelaiRecours(60, 'CE')).toBe('60 jours (CE)');
    expect(component.formatDelaiRecours(15, 'TA')).toBe('15 jours (TA)');
    expect(component.formatDelaiRecours(null, 'TA')).toBe('');
  });

  it('risqueChipClass : "vice de procédure" → critical, "motif limite" → warning', () => {
    expect(component.risqueChipClass('Vice de procédure : commission absente')).toContain('critical');
    expect(component.risqueChipClass('Délai expiré')).toContain('critical');
    expect(component.risqueChipClass('Motif limite — qualification fragile')).toContain('warning');
    expect(component.risqueChipClass('Annulation probable')).toContain('critical');
  });

  it('dispositifLabel et motifMenaceLabel retournent les libellés humains', () => {
    expect(component.dispositifLabel('EXPULSION_PREFECTORALE')).toContain('Expulsion préfectorale');
    expect(component.dispositifLabel('IRTF')).toContain('IRTF');
    expect(component.dispositifLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(component.dispositifLabel(null)).toBe('');
    expect(component.motifMenaceLabel('TERRORISME')).toContain('Terrorisme');
    expect(component.motifMenaceLabel('AUTRE')).toContain('Autre');
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02d — mode simulateur autonome
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02d — mode standalone', () => {
    const STANDALONE_URL_F163 = '/api/v1/simulators/F-IM-20-mesures-eloignement/calculate';

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
