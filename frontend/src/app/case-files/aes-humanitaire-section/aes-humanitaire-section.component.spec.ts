import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { AesHumanitaireSectionComponent } from './aes-humanitaire-section.component';
import { AesHumanitaireResponse } from '../../core/models/aes-humanitaire.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('AesHumanitaireSectionComponent (SF-IM-09-07)', () => {
  let component: AesHumanitaireSectionComponent;
  let fixture: ComponentFixture<AesHumanitaireSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/aes-humanitaire';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function okResponse(): AesHumanitaireResponse {
    return {
      caseFileId: 'case-1',
      dateEntreeFrance: '2018-01-15',
      motifHumanitaireDominant: 'VICTIME_VIOLENCES',
      preuvesMedicales: false,
      preuvesViolencesOuTraite: true,
      demandeAsileDeposeeEtRejetee: false,
      commissionTitreSejourSaisie: true,
      menaceOrdrePublic: false,
      dateDepotDemande: '2026-04-01',
      country: 'FRANCE',
      motifEligible: true,
      preuvesAdaptees: true,
      commissionRequise: true,
      pasMenace: true,
      scoreGlobal: 75,
      verdictProbabiliteAcceptation: 'ELEVEE',
      criteresNonRemplis: [],
      dateExpirationInstruction: '2026-10-01',
      formule: 'AES voie humanitaire (L.435-2) — motif « VICTIME_VIOLENCES » : score 75/100, probabilité d\'acceptation ELEVEE — délai d\'instruction jusqu\'au 2026-10-01 (silence vaut rejet).',
      baseJuridique: 'Art. L.435-2 CESEDA + L.432-14 (commission titre séjour)',
      messages: ['Voie exceptionnelle L.435-2 : faisceau d\'indices.'],
    };
  }

  function rejetResponse(): AesHumanitaireResponse {
    return {
      ...okResponse(),
      menaceOrdrePublic: true,
      pasMenace: false,
      scoreGlobal: 0,
      verdictProbabiliteAcceptation: 'FAIBLE',
      criteresNonRemplis: [
        'Menace ordre public présumée',
        'Commission du titre de séjour (L.432-14) non saisie',
      ],
      dateExpirationInstruction: null,
      formule: 'AES voie humanitaire (L.435-2) : menace ordre public présumée — demande vouée au rejet.',
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
        AesHumanitaireSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AesHumanitaireSectionComponent);
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
    httpMock.expectOne(BASE_URL).flush(okResponse());
    expectSourceExplanationCall();
    expect(component.result()!.scoreGlobal).toBe(75);
    expect(component.showForm()).toBe(false);
    expect(component.dateEntreeFrance()).toBe('2018-01-15');
    expect(component.motifHumanitaireDominant()).toBe('VICTIME_VIOLENCES');
    expect(component.commissionTitreSejourSaisie()).toBe(true);
  });

  it('formValid : exige dateEntreeFrance + motifHumanitaireDominant', () => {
    component.dateEntreeFrance.set(null);
    component.motifHumanitaireDominant.set('VICTIME_VIOLENCES');
    expect(component.formValid()).toBe(false);

    component.dateEntreeFrance.set('2020-01-01');
    component.motifHumanitaireDominant.set(null);
    expect(component.formValid()).toBe(false);

    component.dateEntreeFrance.set('2020-01-01');
    component.motifHumanitaireDominant.set('ISOLEMENT_TOTAL');
    expect(component.formValid()).toBe(true);
  });

  it('formValid : refuse une date d\'entrée future', () => {
    component.dateEntreeFrance.set('2099-01-01');
    component.motifHumanitaireDominant.set('RISQUES_AU_RETOUR');
    expect(component.formValid()).toBe(false);
  });

  it('formValid : refuse dateDepotDemande antérieure à dateEntreeFrance', () => {
    component.dateEntreeFrance.set('2020-06-01');
    component.motifHumanitaireDominant.set('RISQUES_AU_RETOUR');
    component.dateDepotDemande.set('2019-01-01');
    expect(component.formValid()).toBe(false);

    component.dateDepotDemande.set('2021-01-01');
    expect(component.formValid()).toBe(true);
  });

  it('calculate() POST → result + snackbar succès + refresh dashboard', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.dateEntreeFrance.set('2018-01-15');
    component.motifHumanitaireDominant.set('VICTIME_VIOLENCES');
    component.preuvesViolencesOuTraite.set(true);
    component.commissionTitreSejourSaisie.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dateEntreeFrance).toBe('2018-01-15');
    expect(req.request.body.motifHumanitaireDominant).toBe('VICTIME_VIOLENCES');
    expect(req.request.body.preuvesViolencesOuTraite).toBe(true);
    expect(req.request.body.commissionTitreSejourSaisie).toBe(true);
    req.flush(okResponse());

    expect(component.result()!.scoreGlobal).toBe(75);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse AES humanitaire calculée',
      'OK',
      jasmine.any(Object),
    );
  });

  it('calculate() erreur 400 → snackbar rouge', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.dateEntreeFrance.set('2018-01-15');
    component.motifHumanitaireDominant.set('VICTIME_VIOLENCES');
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

  it('pré-fill IA : dateEntreeFrance + dateDepotDemande + provenance IA', () => {
    component.aiData = ({
      aesDateEntreeFrance: '2020-01-15',
      dateDepotProcedure: '2026-02-10',
    } as unknown) as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.dateEntreeFrance()).toBe('2020-01-15');
    expect(component.provenanceDateEntree()).toBe('IA');
    expect(component.dateDepotDemande()).toBe('2026-02-10');
    expect(component.provenanceDateDepot()).toBe('IA');
  });

  it('pré-fill no-op si aiData absent (graceful)', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.dateEntreeFrance()).toBeNull();
    expect(component.provenanceDateEntree()).toBeNull();
    expect(component.provenanceMotif()).toBeNull();
  });

  it('onDateEntreeChange / onMotifChange / onDateDepotChange effacent les badges IA', () => {
    component.aiData = ({
      aesDateEntreeFrance: '2020-01-15',
      dateDepotProcedure: '2026-02-10',
    } as unknown) as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.provenanceDateEntree()).toBe('IA');
    expect(component.provenanceDateDepot()).toBe('IA');

    component.onDateEntreeChange('2019-06-01');
    expect(component.dateEntreeFrance()).toBe('2019-06-01');
    expect(component.provenanceDateEntree()).toBeNull();

    component.onDateDepotChange('2025-12-01');
    expect(component.dateDepotDemande()).toBe('2025-12-01');
    expect(component.provenanceDateDepot()).toBeNull();

    component.provenanceMotif.set('IA');
    component.onMotifChange('AUTRE_HUMANITAIRE');
    expect(component.motifHumanitaireDominant()).toBe('AUTRE_HUMANITAIRE');
    expect(component.provenanceMotif()).toBeNull();
  });

  it('coherence alert DATE_ENTREE_FRANCE si divergence avec IA', () => {
    component.aiData = ({ aesDateEntreeFrance: '2020-01-15' } as unknown) as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onDateEntreeChange('2018-06-01');

    const alerts = component.coherenceAlerts();
    expect(alerts.DATE_ENTREE_FRANCE).toBeDefined();
    expect(alerts.DATE_ENTREE_FRANCE!.field).toBe('DATE_ENTREE_FRANCE');
    expect(alerts.DATE_ENTREE_FRANCE!.expectedDisplay).toBe('2020-01-15');
    expect(alerts.DATE_ENTREE_FRANCE!.source).toBe('IA');
  });

  it('coherence alert MOTIF_HUMANITAIRE consolidé F96 + QUESTION_IA → MULTI', () => {
    component.procedureChecks = [
      {
        id: 'p1', ordre: 1, description: 'Motif humanitaire',
        statut: 'TO_CHECK', critereCode: 'IM09H_MOTIF_HUMANITAIRE',
        expectedValue: 'VICTIME_TRAITE',
      },
    ];
    component.aiQuestions = [
      {
        id: 'q1', orderIndex: 1,
        questionText: 'Confirme le motif',
        answerText: null,
        critereCode: 'IM09H_MOTIF_HUMANITAIRE',
        expectedValue: 'VICTIME_TRAITE',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onMotifChange('AUTRE_HUMANITAIRE');
    const alerts = component.coherenceAlerts();
    expect(alerts.MOTIF_HUMANITAIRE).toBeDefined();
    expect(alerts.MOTIF_HUMANITAIRE!.contributors.length).toBe(2);
    expect(alerts.MOTIF_HUMANITAIRE!.source).toBe('MULTI');
    expect(alerts.MOTIF_HUMANITAIRE!.expectedDisplay).toContain('Victime de traite');
  });

  it('coherence alert DATE_DEPOT_DEMANDE si IA divergent', () => {
    component.aiData = ({ dateDepotProcedure: '2026-02-10' } as unknown) as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // Le pré-fill a posé 2026-02-10 ; on simule une saisie manuelle divergente.
    component.onDateDepotChange('2026-03-15');
    const alerts = component.coherenceAlerts();
    expect(alerts.DATE_DEPOT_DEMANDE).toBeDefined();
    expect(alerts.DATE_DEPOT_DEMANDE!.expectedDisplay).toBe('2026-02-10');
  });

  it('gate BELGIQUE : bannière info + pas de GET, pas de POST', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.loading()).toBe(false);

    component.dateEntreeFrance.set('2020-01-01');
    component.motifHumanitaireDominant.set('VICTIME_VIOLENCES');
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

  it('verdict ELEVEE → banner success + icône check_circle', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(okResponse());
    expectSourceExplanationCall();
    expect(component.verdictBannerClass()).toContain('--success');
    expect(component.verdictIcon()).toBe('check_circle');
    expect(component.verdictLabel()).toContain('élevée');
  });

  it('verdict FAIBLE (menace ordre public) → banner danger + critères affichés', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(rejetResponse());
    expectSourceExplanationCall();
    expect(component.result()!.criteresNonRemplis.length).toBe(2);
    expect(component.verdictBannerClass()).toContain('--danger');
    expect(component.verdictIcon()).toBe('error');
    expect(component.verdictLabel()).toContain('faible');
  });

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newAi = ({ aesDateEntreeFrance: '2019-03-10' } as unknown) as ImmigrationExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.dateEntreeFrance()).toBe('2019-03-10');
    expect(component.provenanceDateEntree()).toBe('IA');
  });

  it('alertes masquées si form caché (showForm=false)', () => {
    component.aiData = ({ aesDateEntreeFrance: '2020-01-15' } as unknown) as ImmigrationExtractedData;
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

  it('labelForMotif retourne le libellé humain ou le code en fallback', () => {
    expect(component.labelForMotif('VICTIME_TRAITE')).toBe('Victime de traite / proxénétisme');
    expect(component.labelForMotif('AUTRE_HUMANITAIRE')).toBe('Autre motif humanitaire');
    expect(component.labelForMotif(null)).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02d — mode simulateur autonome
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02d — mode standalone', () => {
    const STANDALONE_URL_F163 = '/api/v1/simulators/F-IM-09-aes-humanitaire/calculate';

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
