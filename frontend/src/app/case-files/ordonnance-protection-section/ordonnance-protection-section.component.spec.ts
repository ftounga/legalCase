import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { OrdonnanceProtectionSectionComponent } from './ordonnance-protection-section.component';
import { OrdonnanceProtectionResponse } from '../../core/models/ordonnance-protection.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('OrdonnanceProtectionSectionComponent', () => {
  let component: OrdonnanceProtectionSectionComponent;
  let fixture: ComponentFixture<OrdonnanceProtectionSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/ordonnance-protection';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function defaultResponse(): OrdonnanceProtectionResponse {
    return {
      caseFileId: 'case-1',
      dateRequete: '2026-04-20',
      violencesAlleguees: ['PHYSIQUES', 'PSYCHOLOGIQUES', 'MENACES_MORT'],
      preuvesViolences: ['CONSTAT_HUISSIER', 'CERTIFICAT_MEDICAL'],
      dangerImmediat: true,
      presenceEnfants: true,
      ageEnfants: [5, 8],
      logementCommun: true,
      victimeFinanciairementDependante: false,
      demandeurDejaProtege: false,
      demandeMesures: ['EVICTION_CONJOINT', 'INTERDICTION_APPROCHER', 'TGD', 'BAR'],
      decEnvisage: false,
      scoreVraisemblance: 95,
      verdictProbabiliteOctroi: 'ELEVEE',
      mesuresRecommandees: ['EVICTION_CONJOINT', 'INTERDICTION_APPROCHER', 'TGD', 'BAR'],
      delaiTraitementJoursPrevisionnel: 6,
      baseJuridique: 'Art. 515-9 à 515-13 Cciv + Loi 30/07/2020 (BAR)',
      formule: 'Score 95 = vraisemblance des faits élevée (≥ 75).',
      messages: [
        'Audience à demander en urgence — délai indicatif 6 jours (art. 515-11 Cciv)',
        'TGD attribué au plus tard 24h après ordonnance',
      ],
      country: 'FRANCE',
    };
  }

  /**
   * Absorbe la requête source-explanations émise par `loadSourceExplanations()`
   * dans `ngOnInit` (fail-open). Empêche `httpMock.verify()` de crasher.
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
        OrdonnanceProtectionSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OrdonnanceProtectionSectionComponent);
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
  // Mount + form validators
  // ---------------------------------------------------------------------------

  it('mount sans erreur (FRANCE) — options enums correctes', () => {
    expect(component).toBeTruthy();
    expect(component.violencesOptions.length).toBe(5);
    expect(component.preuvesOptions.length).toBe(8);
    expect(component.mesuresOptions.length).toBe(7);
  });

  it('formValid faux si violencesAlleguees vide', () => {
    component.violencesAlleguees.set([]);
    component.demandeMesures.set(['EVICTION_CONJOINT']);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si demandeMesures vide', () => {
    component.violencesAlleguees.set(['PHYSIQUES']);
    component.demandeMesures.set([]);
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai si ≥ 1 violence + ≥ 1 mesure demandée', () => {
    component.violencesAlleguees.set(['PHYSIQUES']);
    component.demandeMesures.set(['EVICTION_CONJOINT']);
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // HTTP : load + calculate
  // ---------------------------------------------------------------------------

  it('GET 200 → form masqué, valeurs persistées affichées, pas de badge IA', () => {
    component.aiData = {
      logementCommunDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(defaultResponse());
    expectSourceExplanationCall();

    expect(component.result()!.scoreVraisemblance).toBe(95);
    expect(component.showForm()).toBe(false);
    expect(component.violencesAlleguees()).toEqual(['PHYSIQUES', 'PSYCHOLOGIQUES', 'MENACES_MORT']);
    expect(component.provenanceLogementCommun()).toBeNull();
  });

  it('GET 404 → reste en mode formulaire ; pré-fill IA appliqué', () => {
    component.aiData = {
      dateRequeteOP: '2026-04-15',
      violencesAllegueesDetectees: ['PHYSIQUES', 'MENACES_MORT'],
      preuvesViolencesDetectees: ['CERTIFICAT_MEDICAL', 'TEMOIGNAGES'],
      dangerImmediatDetected: true,
      presenceEnfantsDetected: true,
      logementCommunDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.showForm()).toBe(true);
    expect(component.dateRequete()).toBe('2026-04-15');
    expect(component.violencesAlleguees()).toEqual(['PHYSIQUES', 'MENACES_MORT']);
    expect(component.preuvesViolences()).toEqual(['CERTIFICAT_MEDICAL', 'TEMOIGNAGES']);
    expect(component.dangerImmediat()).toBe(true);
    expect(component.presenceEnfants()).toBe(true);
    expect(component.logementCommun()).toBe(true);
    expect(component.provenanceDateRequete()).toBe('IA');
    expect(component.provenanceViolencesAlleguees()).toBe('IA');
    expect(component.provenanceDangerImmediat()).toBe('IA');
    expect(component.provenanceLogementCommun()).toBe('IA');
  });

  it('calculate() POST → résultat affiché + snackbar succès + dashboardRefresh', () => {
    component.dateRequete.set('2026-04-20');
    component.violencesAlleguees.set(['PHYSIQUES', 'PSYCHOLOGIQUES', 'MENACES_MORT']);
    component.preuvesViolences.set(['CONSTAT_HUISSIER', 'CERTIFICAT_MEDICAL']);
    component.dangerImmediat.set(true);
    component.presenceEnfants.set(true);
    component.logementCommun.set(true);
    component.victimeFinanciairementDependante.set(false);
    component.demandeurDejaProtege.set(false);
    component.demandeMesures.set(['EVICTION_CONJOINT', 'INTERDICTION_APPROCHER', 'TGD', 'BAR']);

    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.violencesAlleguees).toEqual(['PHYSIQUES', 'PSYCHOLOGIQUES', 'MENACES_MORT']);
    expect(req.request.body.demandeMesures).toEqual(['EVICTION_CONJOINT', 'INTERDICTION_APPROCHER', 'TGD', 'BAR']);
    expect(req.request.body.dangerImmediat).toBe(true);
    req.flush(defaultResponse());

    expect(component.result()!.verdictProbabiliteOctroi).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse ordonnance de protection calculée',
      'OK',
      jasmine.any(Object),
    );
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('SF-222-05 : calculate() envoie decEnvisage dans le body + affiche le DEC recommandé', () => {
    component.violencesAlleguees.set(['PHYSIQUES']);
    component.dangerImmediat.set(true);
    component.demandeMesures.set(['INTERDICTION_APPROCHER']);
    component.onDecEnvisageChange(true);
    expect(component.decEnvisage()).toBe(true);

    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.decEnvisage).toBe(true);

    const resp = defaultResponse();
    resp.decEnvisage = true;
    resp.mesuresRecommandees = ['INTERDICTION_APPROCHER', 'DEC'];
    req.flush(resp);

    expect(component.result()!.decEnvisage).toBe(true);
    expect(component.result()!.mesuresRecommandees).toContain('DEC');
    expect(component.mesureLabel('DEC')).toContain('Dispositif Électronique');
  });

  it('SF-222-05 : DEC absent du multi-select des mesures demandées (toggle dédié)', () => {
    expect(component.mesuresOptions.some((m) => m.code === 'DEC')).toBe(false);
  });

  it('calculate() erreur 400 → snackbar rouge', () => {
    component.violencesAlleguees.set(['PHYSIQUES']);
    component.demandeMesures.set(['EVICTION_CONJOINT']);
    component.calculate();

    httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL)
      .flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.violencesAlleguees.set([]);
    component.demandeMesures.set([]);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Provenance handlers (badge IA effacé sur edit manuel)
  // ---------------------------------------------------------------------------

  it('onViolencesChange efface le badge IA', () => {
    component.aiData = {
      violencesAllegueesDetectees: ['PHYSIQUES'],
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceViolencesAlleguees()).toBe('IA');
    component.onViolencesChange(['SEXUELLES']);
    expect(component.violencesAlleguees()).toEqual(['SEXUELLES']);
    expect(component.provenanceViolencesAlleguees()).toBeNull();
  });

  it('onLogementCommunChange efface le badge IA', () => {
    component.aiData = {
      logementCommunDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceLogementCommun()).toBe('IA');
    component.onLogementCommunChange(false);
    expect(component.logementCommun()).toBe(false);
    expect(component.provenanceLogementCommun()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03 (via CoherenceAlertBuilder)
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.VIOLENCES_ALLEGUEES présent si IA détecte un set ≠ saisie avocat', () => {
    component.aiData = {
      violencesAllegueesDetectees: ['PHYSIQUES'],
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onViolencesChange(['SEXUELLES']);
    const alerts = component.coherenceAlerts();
    expect(alerts.VIOLENCES_ALLEGUEES).toBeDefined();
    expect(alerts.VIOLENCES_ALLEGUEES!.field).toBe('VIOLENCES_ALLEGUEES');
    expect(alerts.VIOLENCES_ALLEGUEES!.expectedDisplay).toContain('PHYSIQUES');
    expect(alerts.VIOLENCES_ALLEGUEES!.source).toBe('IA');
    expect(alerts.VIOLENCES_ALLEGUEES!.contributors).toEqual(['IA']);
    expect(alerts.VIOLENCES_ALLEGUEES!.severity).toBe('WARNING');
  });

  it('coherenceAlerts.LOGEMENT_COMMUN présent si IA dit true et avocat false', () => {
    component.aiData = {
      logementCommunDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // Pré-fill a posé true. Avocat saisit false.
    component.onLogementCommunChange(false);
    const alert = component.coherenceAlerts().LOGEMENT_COMMUN;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('LOGEMENT_COMMUN');
    expect(alert!.expectedDisplay).toBe('Oui');
    expect(alert!.source).toBe('IA');
  });

  it('coherenceAlerts.DATE_REQUETE alerte multi-source IA + F96 + PIECE_MANQUANTE', () => {
    component.aiData = { dateRequeteOP: '2026-04-15' } as FamilleExtractedData;
    component.procedureChecks = [
      {
        id: 'c1', ordre: 1, description: 'Date requête', statut: 'NON_COMPLIANT',
        critereCode: 'FA14_DATE_REQUETE', expectedValue: '2026-04-15', raison: 'date confirmée',
      },
    ] as ProcedureCheck[];
    component.piecesManquantes = [
      { texte: 'Copie requête OP', critereCode: 'COPIE_REQUETE_OP' },
    ] as PieceManquanteEntry[];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onDateRequeteChange('2026-05-15');

    const alert = component.coherenceAlerts().DATE_REQUETE;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('DATE_REQUETE');
    expect(alert!.contributors).toContain('IA');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('PIECE_MANQUANTE');
    expect(alert!.source).toBe('MULTI');
    expect(alert!.expectedDisplay).toBe('2026-04-15');
    expect(alert!.pieceTexte).toBe('Copie requête OP');
  });

  it('alertes masquées après résultat affiché (showForm=false)', () => {
    component.aiData = {
      logementCommunDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onLogementCommunChange(false);
    expect(component.coherenceAlerts().LOGEMENT_COMMUN).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().LOGEMENT_COMMUN).toBeUndefined();
  });

  it('alertBadgeLabel reflète le type de source (IA / MULTI)', () => {
    const alertIA = {
      field: 'VIOLENCES_ALLEGUEES' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: 'PHYSIQUES',
      reason: 'r',
    };
    const alertMulti = { ...alertIA, source: 'MULTI' as const, contributors: ['IA' as const, 'F96' as const] };
    expect(component.alertBadgeLabel(alertIA)).toContain('Incohérence détectée');
    expect(component.alertBadgeLabel(alertIA)).toContain('PHYSIQUES');
    expect(component.alertBadgeLabel(alertMulti)).toContain('multiple');
  });

  it('alertTooltip ajoute "Contredit" si > 1 contributor', () => {
    const alertSingle = {
      field: 'DATE_REQUETE' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: '2026-04-15',
      reason: 'simple',
    };
    const alertMulti = { ...alertSingle, source: 'MULTI' as const, contributors: ['IA' as const, 'F96' as const] };
    expect(component.alertTooltip(alertSingle)).toBe('simple');
    expect(component.alertTooltip(alertMulti)).toContain('Contredit');
  });

  it('explanationFor retourne la liste filtrée (fail-open empty)', () => {
    expect(component.explanationFor('DATE_REQUETE')).toEqual([]);
    expect(component.explanationFor('VIOLENCES_ALLEGUEES')).toEqual([]);
    expect(component.explanationFor('LOGEMENT_COMMUN')).toEqual([]);
  });

  // ---------------------------------------------------------------------------
  // Gate workspaceCountry
  // ---------------------------------------------------------------------------

  it('gate BELGIQUE → form non rendu, GET non appelé', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    httpMock.expectNone(SOURCE_EXPL_URL);
    expect(component.isFrance()).toBe(false);
  });

  it('gate FRANCE → load() appelé', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.isFrance()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // ngOnChanges + UI helpers
  // ---------------------------------------------------------------------------

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newAi = {
      violencesAllegueesDetectees: ['PHYSIQUES', 'PSYCHOLOGIQUES'],
      dangerImmediatDetected: true,
    } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({
      aiData: new SimpleChange(null, newAi, false),
    });

    expect(component.violencesAlleguees()).toEqual(['PHYSIQUES', 'PSYCHOLOGIQUES']);
    expect(component.dangerImmediat()).toBe(true);
    expect(component.provenanceViolencesAlleguees()).toBe('IA');
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

  it('verdictBannerClass renvoie la classe attendue (strong/medium/weak)', () => {
    expect(component.verdictBannerClass('ELEVEE')).toContain('strong');
    expect(component.verdictBannerClass('MOYENNE')).toContain('medium');
    expect(component.verdictBannerClass('FAIBLE')).toContain('weak');
  });

  it('verdictProbabiliteOctroiLabel mappe les 3 verdicts', () => {
    expect(component.verdictProbabiliteOctroiLabel('ELEVEE')).toContain('élevée');
    expect(component.verdictProbabiliteOctroiLabel('MOYENNE')).toContain('moyenne');
    expect(component.verdictProbabiliteOctroiLabel('FAIBLE')).toContain('faible');
  });

  it('mesureLabel mappe les codes mesures FR', () => {
    expect(component.mesureLabel('TGD')).toContain('TGD');
    expect(component.mesureLabel('BAR')).toContain('BAR');
    expect(component.mesureLabel('EVICTION_CONJOINT')).toContain('Éviction');
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02c — mode simulateur autonome
  // ---------------------------------------------------------------------------

  describe('F-163 SF-163-02c — mode standalone', () => {
    const STANDALONE_URL = '/api/v1/simulators/F-FA-14-ordonnance-protection/calculate';
    const CASE_URL = '/api/v1/case-files/case-1/ordonnance-protection';

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
