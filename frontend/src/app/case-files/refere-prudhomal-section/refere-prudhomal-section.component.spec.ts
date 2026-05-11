import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReferePrudhomalService } from '../../core/services/refere-prudhomal.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange, SimpleChanges } from '@angular/core';
import { ReferePrudhomalSectionComponent } from './refere-prudhomal-section.component';
import { ReferePrudhomalResponse } from '../../core/models/refere-prudhomal.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('ReferePrudhomalSectionComponent', () => {
  let component: ReferePrudhomalSectionComponent;
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<ReferePrudhomalSectionComponent>;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/refere-prudhomal';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function frResponse(): ReferePrudhomalResponse {
    return {
      caseFileId: 'case-1',
      typeRefere: 'PROVISION_SALAIRES',
      natureCreance: 'SALAIRES_NON_VERSES',
      montantProvisionDemandeeEur: 5000,
      absenceContestationSerieuse: true,
      preuvesUrgenceProduites: ['BULLETIN_PAIE', 'MISE_EN_DEMEURE'],
      dommageImmediatCarac: true,
      trésorerieEmployeurDouteuse: false,
      dateMiseEnDemeure: '2026-04-01',
      ancienneteContratMois: 24,
      scoreSuccess: 78,
      verdictRecommandation: 'PROVISION_PROBABLE',
      delaiAudienceJoursPrevisionnel: 30,
      delaiOrdonnanceJoursPrevisionnel: 45,
      montantProvisionRecommandeEur: 5000,
      baseJuridique: 'Art. R.1454-1 al. 1 Code du travail',
      formule: '5 000,00 € de provision pour salaires non versés (24 mois ancienneté)',
      messages: ['Absence de contestation sérieuse → R.1454-1 al. 1 applicable.'],
      country: 'FRANCE',
    };
  }

  function expectSourceExplanationCall(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        ReferePrudhomalSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReferePrudhomalSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Mount + GET / POST nominal
  // ---------------------------------------------------------------------------

  it('mount + workspaceCountry FRANCE par défaut', () => {
    expect(component).toBeTruthy();
    expect(component.workspaceCountry).toBe('FRANCE');
    expect(component.collapsed()).toBe(true);
  });

  it('charge l\'analyse existante si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(frResponse());
    expectSourceExplanationCall();
    expect(component.result()!.scoreSuccess).toBe(78);
    expect(component.showForm()).toBe(false);
    expect(component.typeRefere()).toBe('PROVISION_SALAIRES');
    expect(component.natureCreance()).toBe('SALAIRES_NON_VERSES');
    expect(component.dateMiseEnDemeure()).toBe('2026-04-01');
    expect(component.ancienneteContratMois()).toBe(24);
    expect(component.preuvesUrgenceProduites().length).toBe(2);
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  it('formValid : true quand tous les champs requis renseignés', () => {
    component.typeRefere.set('PROVISION_SALAIRES');
    component.natureCreance.set('SALAIRES_NON_VERSES');
    component.dateMiseEnDemeure.set('2026-04-01');
    component.montantProvisionDemandeeEur.set(5000);
    component.ancienneteContratMois.set(24);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : false si typeRefere absent', () => {
    component.natureCreance.set('SALAIRES_NON_VERSES');
    component.dateMiseEnDemeure.set('2026-04-01');
    component.montantProvisionDemandeeEur.set(5000);
    component.ancienneteContratMois.set(24);
    expect(component.formValid()).toBe(false);
  });

  it('formValid : false si dateMiseEnDemeure absente', () => {
    component.typeRefere.set('PROVISION_SALAIRES');
    component.natureCreance.set('SALAIRES_NON_VERSES');
    component.montantProvisionDemandeeEur.set(5000);
    component.ancienneteContratMois.set(24);
    expect(component.formValid()).toBe(false);
  });

  it('formValid : false si dateMiseEnDemeure dans le futur', () => {
    component.typeRefere.set('PROVISION_SALAIRES');
    component.natureCreance.set('SALAIRES_NON_VERSES');
    component.dateMiseEnDemeure.set('2099-01-01');
    component.montantProvisionDemandeeEur.set(5000);
    component.ancienneteContratMois.set(24);
    expect(component.formValid()).toBe(false);
  });

  it('formValid : false si montant < 0 ou ancienneté < 0', () => {
    component.typeRefere.set('PROVISION_SALAIRES');
    component.natureCreance.set('SALAIRES_NON_VERSES');
    component.dateMiseEnDemeure.set('2026-04-01');
    component.montantProvisionDemandeeEur.set(-1);
    component.ancienneteContratMois.set(24);
    expect(component.formValid()).toBe(false);

    component.montantProvisionDemandeeEur.set(5000);
    component.ancienneteContratMois.set(-1);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // POST + erreurs
  // ---------------------------------------------------------------------------

  it('analyze() POST + affiche résultat + snackbar succès + triggerRefresh', () => {
    component.typeRefere.set('PROVISION_SALAIRES');
    component.natureCreance.set('SALAIRES_NON_VERSES');
    component.dateMiseEnDemeure.set('2026-04-01');
    component.montantProvisionDemandeeEur.set(5000);
    component.ancienneteContratMois.set(24);
    component.preuvesUrgenceProduites.set(['BULLETIN_PAIE', 'MISE_EN_DEMEURE']);
    component.absenceContestationSerieuse.set(true);
    component.dommageImmediatCarac.set(true);
    component.tresorerieEmployeurDouteuse.set(false);
    component.analyze();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      typeRefere: 'PROVISION_SALAIRES',
      natureCreance: 'SALAIRES_NON_VERSES',
      montantProvisionDemandeeEur: 5000,
      absenceContestationSerieuse: true,
      preuvesUrgenceProduites: ['BULLETIN_PAIE', 'MISE_EN_DEMEURE'],
      dommageImmediatCarac: true,
      trésorerieEmployeurDouteuse: false,
      dateMiseEnDemeure: '2026-04-01',
      ancienneteContratMois: 24,
    });
    req.flush(frResponse());

    expect(component.result()!.scoreSuccess).toBe(78);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Référé prud\'homal analysé', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('analyze() erreur backend → snackbar rouge', () => {
    component.typeRefere.set('PROVISION_SALAIRES');
    component.natureCreance.set('SALAIRES_NON_VERSES');
    component.dateMiseEnDemeure.set('2026-04-01');
    component.montantProvisionDemandeeEur.set(5000);
    component.ancienneteContratMois.set(24);
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

  it('analyze() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.typeRefere.set(null);
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Gate BELGIQUE
  // ---------------------------------------------------------------------------

  it('BELGIQUE : pas de GET, pas de form, bannière info', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    httpMock.expectNone(SOURCE_EXPL_URL);
    expect(component.result()).toBeNull();
    expect(component.isFrance()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA
  // ---------------------------------------------------------------------------

  it('SF-DT-34-02 : prefill IA dateMiseEnDemeure depuis dateLicenciement (404 → form)', () => {
    component.aiData = {
      dateLicenciement: '2026-04-01',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.dateMiseEnDemeure()).toBe('2026-04-01');
    expect(component.provenanceDateMiseEnDemeure()).toBe('IA');
  });

  it('SF-DT-34-02 : prefill IA ancienneté calculée depuis dateEntree → dateLicenciement', () => {
    component.aiData = {
      dateEntree: '2024-04-01',
      dateLicenciement: '2026-04-01',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // 24 mois entre 2024-04-01 et 2026-04-01.
    expect(component.ancienneteContratMois()).toBe(24);
    expect(component.provenanceAnciennete()).toBe('IA');
  });

  it('SF-DT-34-02 : prefill IA natureCreance HEURES_SUPPLEMENTAIRES si heures sup détectées', () => {
    component.aiData = {
      heuresSupMentionneesDansDossier: { totalDeclarees25pct: 35 },
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.natureCreance()).toBe('HEURES_SUPPLEMENTAIRES');
    expect(component.provenanceNatureCreance()).toBe('IA');
  });

  it('SF-DT-34-02 : prefill sans aiData → pas de pré-remplissage', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.dateMiseEnDemeure()).toBeNull();
    expect(component.ancienneteContratMois()).toBeNull();
    expect(component.natureCreance()).toBeNull();
    expect(component.provenanceDateMiseEnDemeure()).toBeNull();
    expect(component.provenanceAnciennete()).toBeNull();
    expect(component.provenanceNatureCreance()).toBeNull();
  });

  it('SF-DT-34-02 : onDateMiseEnDemeureChange efface le badge IA date', () => {
    component.aiData = { dateLicenciement: '2026-04-01' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceDateMiseEnDemeure()).toBe('IA');
    component.onDateMiseEnDemeureChange('2026-03-15');
    expect(component.dateMiseEnDemeure()).toBe('2026-03-15');
    expect(component.provenanceDateMiseEnDemeure()).toBeNull();
  });

  it('SF-DT-34-02 : onAncienneteChange efface le badge IA ancienneté', () => {
    component.aiData = {
      dateEntree: '2024-04-01',
      dateLicenciement: '2026-04-01',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceAnciennete()).toBe('IA');
    component.onAncienneteChange(36);
    expect(component.ancienneteContratMois()).toBe(36);
    expect(component.provenanceAnciennete()).toBeNull();
  });

  it('SF-DT-34-02 : onNatureCreanceChange efface le badge IA nature', () => {
    component.aiData = {
      heuresSupMentionneesDansDossier: { totalDeclarees25pct: 35 },
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceNatureCreance()).toBe('IA');
    component.onNatureCreanceChange('PRIMES');
    expect(component.natureCreance()).toBe('PRIMES');
    expect(component.provenanceNatureCreance()).toBeNull();
  });

  it('SF-DT-34-02 : loadExisting (GET 200) → pas de badge IA (valeurs persistées prioritaires)', () => {
    component.aiData = {
      dateLicenciement: '2025-01-01',
      dateEntree: '2020-01-01',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());
    expectSourceExplanationCall();

    expect(component.dateMiseEnDemeure()).toBe('2026-04-01');
    expect(component.ancienneteContratMois()).toBe(24);
    expect(component.provenanceDateMiseEnDemeure()).toBeNull();
    expect(component.provenanceAnciennete()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // F-IA-03 : alertes de cohérence
  // ---------------------------------------------------------------------------

  it('SF-DT-34-02 : coherenceAlerts.DATE_MISE_EN_DEMEURE présent si dates IA et user diffèrent', () => {
    component.aiData = { dateLicenciement: '2026-04-01' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onDateMiseEnDemeureChange('2026-01-15');

    const alerts = component.coherenceAlerts();
    expect(alerts.DATE_MISE_EN_DEMEURE).toBeDefined();
    expect(alerts.DATE_MISE_EN_DEMEURE!.field).toBe('DATE_MISE_EN_DEMEURE');
    expect(alerts.DATE_MISE_EN_DEMEURE!.source).toBe('IA');
    expect(alerts.DATE_MISE_EN_DEMEURE!.expectedDisplay).toBe('2026-04-01');
  });

  it('SF-DT-34-02 : coherenceAlerts.ANCIENNETE présent si écart > 2 mois', () => {
    component.aiData = {
      dateEntree: '2024-04-01',
      dateLicenciement: '2026-04-01',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // IA calcul ~24 mois ; user saisit 36 → écart 12 mois > 2.
    component.onAncienneteChange(36);

    const alerts = component.coherenceAlerts();
    expect(alerts.ANCIENNETE).toBeDefined();
    expect(alerts.ANCIENNETE!.field).toBe('ANCIENNETE');
    expect(alerts.ANCIENNETE!.source).toBe('IA');
    expect(alerts.ANCIENNETE!.expectedDisplay).toContain('mois');
  });

  it('SF-DT-34-02 : coherenceAlerts.ANCIENNETE absent si écart ≤ 2 mois', () => {
    component.aiData = {
      dateEntree: '2024-04-01',
      dateLicenciement: '2026-04-01',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onAncienneteChange(25);

    expect(component.coherenceAlerts().ANCIENNETE).toBeUndefined();
  });

  it('SF-DT-34-02 : alertes masquées après résultat affiché (showForm=false)', () => {
    component.aiData = { dateLicenciement: '2026-04-01' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onDateMiseEnDemeureChange('2026-01-01');
    expect(component.coherenceAlerts().DATE_MISE_EN_DEMEURE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().DATE_MISE_EN_DEMEURE).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // ngOnChanges + handlers + helpers
  // ---------------------------------------------------------------------------

  it('SF-DT-34-02 : ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newAi = {
      dateLicenciement: '2026-04-01',
      dateEntree: '2024-04-01',
    } as TravailExtractedData;
    component.aiData = newAi;
    const changes: SimpleChanges = {
      aiData: new SimpleChange(null, newAi, false),
    };
    component.ngOnChanges(changes);

    expect(component.dateMiseEnDemeure()).toBe('2026-04-01');
    expect(component.ancienneteContratMois()).toBe(24);
    expect(component.provenanceDateMiseEnDemeure()).toBe('IA');
    expect(component.provenanceAnciennete()).toBe('IA');
  });

  it('SF-DT-34-02 : ngOnChanges(aiData) après saisie manuelle n\'écrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onDateMiseEnDemeureChange('2026-02-15');
    component.onAncienneteChange(48);
    expect(component.provenanceDateMiseEnDemeure()).toBeNull();
    expect(component.provenanceAnciennete()).toBeNull();

    const newAi = {
      dateLicenciement: '2026-04-01',
      dateEntree: '2024-04-01',
    } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.dateMiseEnDemeure()).toBe('2026-02-15');
    expect(component.ancienneteContratMois()).toBe(48);
    expect(component.provenanceDateMiseEnDemeure()).toBeNull();
    expect(component.provenanceAnciennete()).toBeNull();
  });

  it('SF-DT-34-02 : toggleCollapse fonctionne', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('SF-DT-34-02 : editMode ré-affiche le form', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Helpers d'affichage
  // ---------------------------------------------------------------------------

  it('SF-DT-34-02 : verdictBannerClass retourne classe selon verdict', () => {
    expect(component.verdictBannerClass('PROVISION_PROBABLE')).toContain('rp-banner--success');
    expect(component.verdictBannerClass('EXPERTISE_RECOMMANDEE')).toContain('rp-banner--accent');
    expect(component.verdictBannerClass('INSUFFISAMMENT_FONDE')).toContain('rp-banner--info');
    expect(component.verdictBannerClass('AUTRE_VOIE_RECOMMANDEE')).toContain('rp-banner--info');
    expect(component.verdictBannerClass(null)).toBe('rp-banner');
  });

  it('SF-DT-34-02 : scoreClass retourne high/medium/low', () => {
    expect(component.scoreClass(85)).toBe('rp-score--high');
    expect(component.scoreClass(55)).toBe('rp-score--medium');
    expect(component.scoreClass(20)).toBe('rp-score--low');
  });

  it('SF-DT-34-02 : verdictLabel renvoie le libellé canonique', () => {
    expect(component.verdictLabel('PROVISION_PROBABLE')).toContain('Provision probable');
    expect(component.verdictLabel(null)).toBe('');
  });

  it('SF-DT-34-02 : alertBadgeLabel + alertTooltip avec source IA', () => {
    component.aiData = { dateLicenciement: '2026-04-01' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onDateMiseEnDemeureChange('2026-01-15');
    const alert = component.coherenceAlerts().DATE_MISE_EN_DEMEURE!;
    expect(component.alertBadgeLabel(alert)).toContain('Incohérence');
    expect(component.alertTooltip(alert)).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02b — mode standalone (CA-08, CA-09, CA-10).
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02b — mode standalone', () => {
    const STANDALONE_URL = '/api/v1/simulators/F-DT-34-refere-prudhomal/calculate';

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
      expect(ReferePrudhomalService.STANDALONE_TOOL_ID).toBe('F-DT-34-refere-prudhomal');
      expect(STANDALONE_URL).toContain(ReferePrudhomalService.STANDALONE_TOOL_ID);
    });
  });

});
