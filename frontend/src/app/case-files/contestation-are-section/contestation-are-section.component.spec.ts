import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { ContestationAreSectionComponent } from './contestation-are-section.component';
import { ContestationAreResponse } from '../../core/models/contestation-are.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('ContestationAreSectionComponent', () => {
  let component: ContestationAreSectionComponent;
  let fixture: ComponentFixture<ContestationAreSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/contestation-are';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function flushSourceExplanations(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  function frResponse(overrides: Partial<ContestationAreResponse> = {}): ContestationAreResponse {
    return {
      caseFileId: 'case-1',
      typeDecisionContestee: 'REFUS_OUVERTURE_DROITS',
      motifContestation: 'ERREUR_CALCUL_REMUNERATION_REFERENCE',
      dateNotificationDecision: '2026-03-10',
      dateRecoursHierarchiquePropose: '2026-03-25',
      preuvesProduites: ['BULLETIN_PAIE', 'ATTESTATION_EMPLOYEUR'],
      montantContesteEur: 1500,
      demandeurDejaSaisiTribunal: false,
      delaiContestationRespecte: true,
      delaiRecoursHierarchiqueJoursOk: true,
      delaiRecoursContentieuxTaJoursOk: true,
      scoreSuccessProbable: 70,
      verdictRecommandation: 'RECOURS_HIERARCHIQUE_PRIORITAIRE',
      delaiInstructionMoisPrevisionnel: 4,
      expertiseTraitementSalaireRecommandee: true,
      baseJuridique: 'Art. R.5422-9 Code du travail, RPGE Unédic',
      formule: 'Score = base 50 + délai 20 + preuves 20 = 70/100',
      messages: ['Recours hiérarchique recevable'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        ContestationAreSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ContestationAreSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    flushSourceExplanations();
    httpMock.verify();
  });

  // ============================================================
  // Tests de structure
  // ============================================================

  it('mount — composant créé, formValid() = false par défaut', () => {
    expect(component).toBeTruthy();
    expect(component.formValid()).toBe(false);
  });

  it('FRANCE — 6 types décision / 5 motifs / 6 preuves', () => {
    expect(component.typesDecision.length).toBe(6);
    expect(component.motifsContestation.length).toBe(5);
    expect(component.preuvesOptions.length).toBe(6);
  });

  it('FRANCE → isFrance() true, GET appelé au ngOnInit', () => {
    expect(component.isFrance()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('BELGIQUE → isFrance() false, pas d\'appel HTTP', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.isFrance()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
  });

  it('charge l\'analyse existante si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse());
    expect(component.result()!.verdictRecommandation).toBe('RECOURS_HIERARCHIQUE_PRIORITAIRE');
    expect(component.showForm()).toBe(false);
    expect(component.typeDecisionContestee()).toBe('REFUS_OUVERTURE_DROITS');
    expect(component.motifContestation()).toBe('ERREUR_CALCUL_REMUNERATION_REFERENCE');
    expect(component.dateNotificationDecision()).toBe('2026-03-10');
    expect(component.preuvesProduites()).toEqual(['BULLETIN_PAIE', 'ATTESTATION_EMPLOYEUR']);
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid : exige type + motif + 2 dates + montant', () => {
    component.typeDecisionContestee.set('REFUS_OUVERTURE_DROITS');
    component.motifContestation.set('ERREUR_CALCUL_REMUNERATION_REFERENCE');
    component.dateNotificationDecision.set('2026-03-10');
    component.dateRecoursHierarchiquePropose.set('2026-03-25');
    component.montantContesteEur.set(1500);
    expect(component.formValid()).toBe(true);

    component.montantContesteEur.set(null);
    expect(component.formValid()).toBe(false);

    component.montantContesteEur.set(-10);
    expect(component.formValid()).toBe(false);

    component.montantContesteEur.set(0);
    component.dateNotificationDecision.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si dateNotification dans le futur', () => {
    const future = new Date();
    future.setDate(future.getDate() + 5);
    const futureIso = future.toISOString().slice(0, 10);
    component.typeDecisionContestee.set('REFUS_OUVERTURE_DROITS');
    component.motifContestation.set('ERREUR_CALCUL_REMUNERATION_REFERENCE');
    component.dateNotificationDecision.set(futureIso);
    component.dateRecoursHierarchiquePropose.set('2026-03-25');
    component.montantContesteEur.set(1000);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // POST analyze
  // ============================================================

  it('analyze() POST → résultat + snackbar succès + body conforme', () => {
    component.ngOnInit();
    flush404();
    component.typeDecisionContestee.set('REFUS_OUVERTURE_DROITS');
    component.motifContestation.set('ERREUR_CALCUL_REMUNERATION_REFERENCE');
    component.dateNotificationDecision.set('2026-03-10');
    component.dateRecoursHierarchiquePropose.set('2026-03-25');
    component.preuvesProduites.set(['BULLETIN_PAIE']);
    component.montantContesteEur.set(1500);
    component.demandeurDejaSaisiTribunal.set(false);
    component.delaiContestationRespecte.set(true);
    component.analyze();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      typeDecisionContestee: 'REFUS_OUVERTURE_DROITS',
      motifContestation: 'ERREUR_CALCUL_REMUNERATION_REFERENCE',
      dateNotificationDecision: '2026-03-10',
      dateRecoursHierarchiquePropose: '2026-03-25',
      preuvesProduites: ['BULLETIN_PAIE'],
      montantContesteEur: 1500,
      demandeurDejaSaisiTribunal: false,
      delaiContestationRespecte: true,
    });
    req.flush(frResponse());
    expect(component.result()!.verdictRecommandation).toBe('RECOURS_HIERARCHIQUE_PRIORITAIRE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Contestation ARE analysée', 'OK', jasmine.any(Object));
  });

  it('analyze() erreur backend → snackbar erreur + analyzing reset', () => {
    component.ngOnInit();
    flush404();
    component.typeDecisionContestee.set('REFUS_OUVERTURE_DROITS');
    component.motifContestation.set('ERREUR_CALCUL_REMUNERATION_REFERENCE');
    component.dateNotificationDecision.set('2026-03-10');
    component.dateRecoursHierarchiquePropose.set('2026-03-25');
    component.montantContesteEur.set(1500);
    component.analyze();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.analyzing()).toBe(false);
  });

  it('analyze() ignoré si form invalide', () => {
    component.ngOnInit();
    flush404();
    component.typeDecisionContestee.set(null);
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ============================================================
  // Verdict / scores
  // ============================================================

  it('verdictBannerClass : INSUFFISAMMENT_FONDE → danger, autres → primary/accent/info', () => {
    expect(component.verdictBannerClass('INSUFFISAMMENT_FONDE')).toContain('ca-banner--danger');
    expect(component.verdictBannerClass('RECOURS_HIERARCHIQUE_PRIORITAIRE')).toContain('ca-banner--primary');
    expect(component.verdictBannerClass('CONTENTIEUX_TA_DIRECT_POSSIBLE')).toContain('ca-banner--accent');
    expect(component.verdictBannerClass('AUTRE_VOIE')).toContain('ca-banner--info');
  });

  it('verdictLabel : retourne un libellé humain pour chaque verdict', () => {
    expect(component.verdictLabel('RECOURS_HIERARCHIQUE_PRIORITAIRE')).toContain('Recours hiérarchique');
    expect(component.verdictLabel('CONTENTIEUX_TA_DIRECT_POSSIBLE')).toContain('Contentieux');
    expect(component.verdictLabel('INSUFFISAMMENT_FONDE')).toContain('insuffisamment');
    expect(component.verdictLabel('AUTRE_VOIE')).toContain('Autre');
  });

  it('scoreClass : >=70 high, >=40 medium, <40 low', () => {
    expect(component.scoreClass(85)).toBe('ca-score--high');
    expect(component.scoreClass(50)).toBe('ca-score--medium');
    expect(component.scoreClass(20)).toBe('ca-score--low');
  });

  // ============================================================
  // Pré-fill IA
  // ============================================================

  describe('Pré-fill IA depuis TravailExtractedData', () => {
    function ai(overrides: Partial<TravailExtractedData> = {}): Partial<TravailExtractedData> {
      return {
        dateLicenciement: '2026-02-15',
        ...overrides,
      };
    }

    it('aiData avec dateLicenciement → pré-fill dateNotificationDecision + badge IA', () => {
      component.aiData = ai();
      component.ngOnInit();
      flush404();
      expect(component.dateNotificationDecision()).toBe('2026-02-15');
      expect(component.provenanceDateNotification()).toBe('IA');
    });

    it('aiData sans dateLicenciement → pas de pré-fill', () => {
      component.aiData = {} as Partial<TravailExtractedData>;
      component.ngOnInit();
      flush404();
      expect(component.dateNotificationDecision()).toBeNull();
      expect(component.provenanceDateNotification()).toBeNull();
    });

    it('handler onDateNotificationChange efface la provenance IA', () => {
      component.aiData = ai();
      component.ngOnInit();
      flush404();
      expect(component.provenanceDateNotification()).toBe('IA');
      component.onDateNotificationChange('2026-03-01');
      expect(component.dateNotificationDecision()).toBe('2026-03-01');
      expect(component.provenanceDateNotification()).toBeNull();
    });

    it('aiData arrive après mount via ngOnChanges → re-prefill (gracieux)', () => {
      component.ngOnInit();
      flush404();
      expect(component.dateNotificationDecision()).toBeNull();

      component.aiData = ai();
      component.ngOnChanges({
        aiData: new SimpleChange(null, component.aiData, false),
      });
      expect(component.dateNotificationDecision()).toBe('2026-02-15');
      expect(component.provenanceDateNotification()).toBe('IA');
    });

    it('GET 200 → pas de pré-fill IA (le backend a déjà persisté la valeur)', () => {
      component.aiData = ai();
      component.ngOnInit();
      const req = httpMock.expectOne(BASE_URL);
      req.flush(frResponse({ dateNotificationDecision: '2026-03-10' }));
      // dateNotificationDecision vient du backend (résultat persisté)
      expect(component.dateNotificationDecision()).toBe('2026-03-10');
      expect(component.provenanceDateNotification()).toBeNull();
    });
  });

  // ============================================================
  // F-IA-03 — alertes de cohérence
  // ============================================================

  describe('F-IA-03 — coherenceAlerts', () => {
    it('divergence date saisie vs aiData.dateLicenciement → alerte WARNING DATE_NOTIFICATION', () => {
      component.aiData = { dateLicenciement: '2026-02-15' };
      component.ngOnInit();
      flush404();
      // pré-fill = '2026-02-15' ; on simule une saisie divergente
      component.onDateNotificationChange('2026-03-20');
      const alerts = component.coherenceAlerts();
      expect(alerts.DATE_NOTIFICATION).toBeTruthy();
      expect(alerts.DATE_NOTIFICATION!.severity).toBe('WARNING');
    });

    it('date saisie identique à aiData.dateLicenciement → pas d\'alerte', () => {
      component.aiData = { dateLicenciement: '2026-02-15' };
      component.ngOnInit();
      flush404();
      const alerts = component.coherenceAlerts();
      expect(alerts.DATE_NOTIFICATION).toBeUndefined();
    });

    it('coherenceAlerts vide en mode résultat (showForm false)', () => {
      component.aiData = { dateLicenciement: '2026-02-15' };
      component.ngOnInit();
      const req = httpMock.expectOne(BASE_URL);
      req.flush(frResponse({ dateNotificationDecision: '2026-03-20' }));
      expect(component.showForm()).toBe(false);
      expect(component.coherenceAlerts()).toEqual({});
    });

    it('coherenceAlerts vide si workspaceCountry !== FRANCE', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = { dateLicenciement: '2026-02-15' };
      component.dateNotificationDecision.set('2026-03-20');
      const alerts = component.coherenceAlerts();
      expect(alerts).toEqual({});
    });
  });

  // ============================================================
  // Toggle / collapse
  // ============================================================

  it('toggleCollapse alterne l\'état collapsed', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('editMode revient en mode formulaire', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });
});
