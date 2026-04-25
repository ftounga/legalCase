import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ReferesAdminSectionComponent } from './referes-admin-section.component';
import { ReferesAdminResponse } from '../../core/models/referes-admin.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('ReferesAdminSectionComponent', () => {
  let component: ReferesAdminSectionComponent;
  let fixture: ComponentFixture<ReferesAdminSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/referes-admin';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function flushSourceExplanations(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  function frResponse(overrides: Partial<ReferesAdminResponse> = {}): ReferesAdminResponse {
    return {
      caseFileId: 'case-1',
      typeRefere: 'SUSPENSION',
      decisionContestee: 'OQTF',
      dateNotificationDecision: '2026-04-10',
      urgenceCaracterisee: true,
      atteinteLiberteFondamentale: false,
      doutesSerieuxLegalite: true,
      preuvesUrgence: ['MENACE_VIE_PRIVEE'],
      demandeurDejaPrived: false,
      scoreSuccessProbabiliteSuspension: 65,
      scoreSuccessProbabiliteLiberte: 25,
      verdictRecommandation: 'REFERE_SUSPENSION_PRIORITAIRE',
      delaiJugeTaJoursL521_1: 30,
      delaiJugeTaHeuresL521_2: 48,
      conditionsCumulativesL521_1Ok: true,
      conditionsCumulativesL521_2Ok: false,
      baseJuridique: 'Art. L.521-1, L.521-2, R.522-1 CJA',
      formule: 'Urgence + doutes sérieux légalité (L.521-1) → 65/100',
      messages: ['Référé suspension recevable'],
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
        ReferesAdminSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ReferesAdminSectionComponent);
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

  it('FRANCE — 3 types référé / 5 décisions / 6 preuves urgence', () => {
    expect(component.typesRefere.length).toBe(3);
    expect(component.decisionsContestees.length).toBe(5);
    expect(component.preuvesUrgenceOptions.length).toBe(6);
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
    expect(component.result()!.verdictRecommandation).toBe('REFERE_SUSPENSION_PRIORITAIRE');
    expect(component.showForm()).toBe(false);
    expect(component.typeRefere()).toBe('SUSPENSION');
    expect(component.decisionContestee()).toBe('OQTF');
    expect(component.dateNotificationDecision()).toBe('2026-04-10');
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid : exige typeRefere + decisionContestee + dateNotification', () => {
    component.typeRefere.set('SUSPENSION');
    component.decisionContestee.set('OQTF');
    component.dateNotificationDecision.set(null);
    expect(component.formValid()).toBe(false);

    component.dateNotificationDecision.set('2026-04-10');
    expect(component.formValid()).toBe(true);

    component.typeRefere.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si dateNotification dans le futur', () => {
    const future = new Date();
    future.setDate(future.getDate() + 5);
    const futureIso = future.toISOString().slice(0, 10);
    component.typeRefere.set('LIBERTE');
    component.decisionContestee.set('OQTF');
    component.dateNotificationDecision.set(futureIso);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // POST analyze
  // ============================================================

  it('analyze() POST → résultat + snackbar succès + body conforme', () => {
    component.ngOnInit();
    flush404();
    component.typeRefere.set('SUSPENSION');
    component.decisionContestee.set('OQTF');
    component.dateNotificationDecision.set('2026-04-10');
    component.urgenceCaracterisee.set(true);
    component.atteinteLiberteFondamentale.set(false);
    component.doutesSerieuxLegalite.set(true);
    component.preuvesUrgence.set(['MENACE_VIE_PRIVEE']);
    component.demandeurDejaPrived.set(false);
    component.analyze();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      typeRefere: 'SUSPENSION',
      decisionContestee: 'OQTF',
      dateNotificationDecision: '2026-04-10',
      urgenceCaracterisee: true,
      atteinteLiberteFondamentale: false,
      doutesSerieuxLegalite: true,
      preuvesUrgence: ['MENACE_VIE_PRIVEE'],
      demandeurDejaPrived: false,
    });
    req.flush(frResponse());
    expect(component.result()!.verdictRecommandation).toBe('REFERE_SUSPENSION_PRIORITAIRE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Référés administratifs analysés', 'OK', jasmine.any(Object));
  });

  it('analyze() erreur backend → snackbar erreur + analyzing reset', () => {
    component.ngOnInit();
    flush404();
    component.typeRefere.set('LIBERTE');
    component.decisionContestee.set('OQTF');
    component.dateNotificationDecision.set('2026-04-10');
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
    component.typeRefere.set(null);
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ============================================================
  // Verdict / scores
  // ============================================================

  it('verdictBannerClass : LIBERTE_PRIORITAIRE / LES_DEUX → danger-strong, SUSPENSION_PRIORITAIRE → danger-medium, AUCUN → info', () => {
    expect(component.verdictBannerClass('REFERE_LIBERTE_PRIORITAIRE')).toContain('ra-banner--danger-strong');
    expect(component.verdictBannerClass('LES_DEUX_CUMULES')).toContain('ra-banner--danger-strong');
    expect(component.verdictBannerClass('REFERE_SUSPENSION_PRIORITAIRE')).toContain('ra-banner--danger-medium');
    expect(component.verdictBannerClass('AUCUN_PROBABLE')).toContain('ra-banner--info');
  });

  it('verdictLabel : retourne un libellé humain pour chaque verdict', () => {
    expect(component.verdictLabel('REFERE_SUSPENSION_PRIORITAIRE')).toContain('L.521-1');
    expect(component.verdictLabel('REFERE_LIBERTE_PRIORITAIRE')).toContain('L.521-2');
    expect(component.verdictLabel('LES_DEUX_CUMULES')).toContain('Cumul');
    expect(component.verdictLabel('AUCUN_PROBABLE')).toContain('Aucun');
  });

  it('scoreClass : >=70 high, >=40 medium, <40 low', () => {
    expect(component.scoreClass(85)).toBe('ra-score--high');
    expect(component.scoreClass(50)).toBe('ra-score--medium');
    expect(component.scoreClass(20)).toBe('ra-score--low');
  });

  // ============================================================
  // Pré-fill IA
  // ============================================================

  describe('Pré-fill IA depuis ImmigrationExtractedData', () => {
    function ai(overrides: Partial<ImmigrationExtractedData> = {}): ImmigrationExtractedData {
      return {
        dateNotificationDecisionContestee: '2026-04-10',
        typeRecoursCode: 'OQTF',
        transfertImminentDetected: true,
        ...overrides,
      };
    }

    it('aiData complet → pré-fill 3 champs + badges provenance IA', () => {
      component.aiData = ai();
      component.ngOnInit();
      flush404();
      expect(component.dateNotificationDecision()).toBe('2026-04-10');
      expect(component.decisionContestee()).toBe('OQTF');
      expect(component.preuvesUrgence()).toEqual(['TRANSFERT_IMMINENT']);
      expect(component.provenanceDateNotification()).toBe('IA');
      expect(component.provenanceDecisionContestee()).toBe('IA');
      expect(component.provenancePreuvesUrgence()).toBe('IA');
    });

    it('typeRecoursCode hors mapping → decisionContestee reste null', () => {
      component.aiData = ai({ typeRecoursCode: 'XYZ_INCONNU' });
      component.ngOnInit();
      flush404();
      expect(component.decisionContestee()).toBeNull();
      expect(component.provenanceDecisionContestee()).toBeNull();
    });

    it('handler onChange efface la provenance IA', () => {
      component.aiData = ai();
      component.ngOnInit();
      flush404();
      expect(component.provenanceDecisionContestee()).toBe('IA');
      component.onDecisionContesteeChange('REFUS_TITRE');
      expect(component.decisionContestee()).toBe('REFUS_TITRE');
      expect(component.provenanceDecisionContestee()).toBeNull();
    });

    it('transfertImminentDetected=false → preuvesUrgence non pré-rempli', () => {
      component.aiData = ai({ transfertImminentDetected: false });
      component.ngOnInit();
      flush404();
      expect(component.preuvesUrgence()).toEqual([]);
      expect(component.provenancePreuvesUrgence()).toBeNull();
    });
  });

  // ============================================================
  // F-IA-03 — alertes de cohérence
  // ============================================================

  describe('F-IA-03 — coherenceAlerts', () => {
    it('divergence date saisie vs IA → alerte WARNING DATE_NOTIFICATION', () => {
      component.aiDataSignal_test_only_seed({ dateNotificationDecisionContestee: '2026-04-01' });
      component.dateNotificationDecision.set('2026-04-15');
      const alerts = component.coherenceAlerts();
      expect(alerts.DATE_NOTIFICATION).toBeTruthy();
      expect(alerts.DATE_NOTIFICATION!.severity).toBe('WARNING');
    });

    it('LIBERTE + atteinteLiberte=false → alerte CRITICAL ATTEINTE_LIBERTE', () => {
      component.typeRefere.set('LIBERTE');
      component.atteinteLiberteFondamentale.set(false);
      const alerts = component.coherenceAlerts();
      expect(alerts.ATTEINTE_LIBERTE).toBeTruthy();
      expect(alerts.ATTEINTE_LIBERTE!.severity).toBe('CRITICAL');
    });

    it('LIBERTE + atteinteLiberte=true → pas d\'alerte ATTEINTE_LIBERTE', () => {
      component.typeRefere.set('LIBERTE');
      component.atteinteLiberteFondamentale.set(true);
      const alerts = component.coherenceAlerts();
      expect(alerts.ATTEINTE_LIBERTE).toBeUndefined();
    });

    it('SUSPENSION + atteinteLiberte=false → pas d\'alerte ATTEINTE_LIBERTE (non requise pour L.521-1)', () => {
      component.typeRefere.set('SUSPENSION');
      component.atteinteLiberteFondamentale.set(false);
      const alerts = component.coherenceAlerts();
      expect(alerts.ATTEINTE_LIBERTE).toBeUndefined();
    });

    it('alertes vides quand showForm=false (gate strict SF-IA-03-12)', () => {
      component.typeRefere.set('LIBERTE');
      component.atteinteLiberteFondamentale.set(false);
      component.showForm.set(false);
      expect(Object.keys(component.coherenceAlerts()).length).toBe(0);
    });

    it('alertes vides quand BELGIQUE', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.typeRefere.set('LIBERTE');
      component.atteinteLiberteFondamentale.set(false);
      expect(Object.keys(component.coherenceAlerts()).length).toBe(0);
    });

    it('alertsSummary compte total + blockers (CRITICAL)', () => {
      component.typeRefere.set('LIBERTE');
      component.atteinteLiberteFondamentale.set(false);
      const summary = component.alertsSummary();
      expect(summary.total).toBe(1);
      expect(summary.blockers).toBe(1);
    });
  });

  // ============================================================
  // Misc
  // ============================================================

  it('toggleCollapse() inverse l\'état collapsed', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
  });

  it('editMode() → showForm true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });
});

// ----- Helper d'accès au signal interne pour tests F-IA-03 -----
declare module './referes-admin-section.component' {
  interface ReferesAdminSectionComponent {
    aiDataSignal_test_only_seed(data: ImmigrationExtractedData): void;
  }
}

(ReferesAdminSectionComponent.prototype as any).aiDataSignal_test_only_seed =
  function (data: ImmigrationExtractedData) {
    // Hydrate le signal miroir interne pour tester coherenceAlerts sans
    // passer par ngOnChanges (cas des tests purs sans cycle de vie).
    (this as any).aiDataSignal.set(data);
    (this as any).aiData = data;
  };
