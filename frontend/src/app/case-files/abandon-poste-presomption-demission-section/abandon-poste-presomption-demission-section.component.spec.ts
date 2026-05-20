import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AbandonPostePresomptionDemissionSectionComponent } from './abandon-poste-presomption-demission-section.component';
import { AbandonPostePresomptionDemissionResponse } from '../../core/models/abandon-poste-presomption-demission.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('AbandonPostePresomptionDemissionSectionComponent', () => {
  let component: AbandonPostePresomptionDemissionSectionComponent;
  let fixture: ComponentFixture<AbandonPostePresomptionDemissionSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/abandon-poste-presomption-demission';

  /** SF-206-02 : fixture IA complète — les 8 champs pré-remplissables F-DT-42. */
  const FULL_AI_DATA: TravailExtractedData = {
    abandonPosteDateMiseEnDemeure: '2026-03-01',
    abandonPosteModeNotification: 'LRAR',
    abandonPosteDelaiAccordeJours: 14,
    abandonPosteMotifAbsence: 'MEDICAL',
    abandonPosteDateReprise: '2026-03-10',
    abandonPosteMedMentionneDelai: true,
    abandonPosteMedMentionneConsequences: false,
    abandonPosteRepriseDansDelai: true,
  };

  function response(verdict: AbandonPostePresomptionDemissionResponse['verdict'])
      : AbandonPostePresomptionDemissionResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs ré-exposé par la réponse (ré-édition du formulaire).
      dateMiseEnDemeurePresentee: '2026-03-01',
      modeNotificationMiseEnDemeure: 'LRAR',
      delaiAccordeJours: 14,
      miseEnDemeureMentionneDelai: true,
      miseEnDemeureMentionneConsequences: false,
      motifAbsenceInvoque: 'MEDICAL',
      motifAbsenceCommentaire: 'Arrêt maladie en cours',
      repriseOuJustificationDansDelai: false,
      dateRepriseOuJustification: null,
      // Champs calculés.
      verdict,
      scoreContestation: verdict === 'CONTESTATION_SOLIDE' ? 90
                       : verdict === 'CONTESTATION_INCERTAINE' ? 50 : 0,
      motifsContestation: verdict === 'CONTESTATION_DIFFICILE' ? [] : [
        {
          code: 'DT42_DELAI_INSUFFISANT',
          libelle: 'Délai accordé inférieur au minimum légal',
          fondement: 'D.1237-2-1 C. trav.',
          poids: 20,
          explication: 'Le délai accordé (14 j) est inférieur au minimum de 15 jours calendaires.',
        },
      ],
      dateExpirationDelai: '2026-03-15',
      basesJuridiques: ['Art. L.1237-1-1 C. trav.', 'D.1237-2-1 C. trav.'],
      messages: ['Vérifier la disponibilité de la mise en demeure au dossier.'],
      country: 'FRANCE',
      calculatedAt: '2026-05-20T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        AbandonPostePresomptionDemissionSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AbandonPostePresomptionDemissionSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Chargement (GET)
  // ---------------------------------------------------------------------------

  it('mount FRANCE → GET initial déclenché', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
  });

  it('mount BELGIQUE → pas d\'appel HTTP', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('GET 200 → result rechargé + showForm=false', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response('CONTESTATION_INCERTAINE'));
    expect(component.result()!.verdict).toBe('CONTESTATION_INCERTAINE');
    expect(component.showForm()).toBe(false);
  });

  it('GET 200 → formulaire ré-hydraté depuis le snapshot d\'inputs (ré-édition)', () => {
    component.ngOnInit();
    const r = response('CONTESTATION_INCERTAINE');
    r.delaiAccordeJours = 10;
    r.modeNotificationMiseEnDemeure = 'AUTRE';
    r.repriseOuJustificationDansDelai = true;
    r.dateRepriseOuJustification = '2026-03-08';
    httpMock.expectOne(BASE_URL).flush(r);

    expect(component.delaiAccordeJours()).toBe(10);
    expect(component.modeNotificationMiseEnDemeure()).toBe('AUTRE');
    expect(component.repriseOuJustificationDansDelai()).toBe(true);
    expect(component.dateRepriseOuJustification()).toBe('2026-03-08');

    component.editMode();
    expect(component.showForm()).toBe(true);
    expect(component.delaiAccordeJours()).toBe(10);
  });

  it('GET 404 → reste en mode formulaire, pas de result', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST)
  // ---------------------------------------------------------------------------

  it('calculate() POST → payload conforme au contrat + result + snackbar + triggerRefresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.dateMiseEnDemeurePresentee.set('2026-03-01');
    component.modeNotificationMiseEnDemeure.set('LRAR');
    component.delaiAccordeJours.set(14);
    component.miseEnDemeureMentionneDelai.set(true);
    component.miseEnDemeureMentionneConsequences.set(false);
    component.motifAbsenceInvoque.set('MEDICAL');
    component.motifAbsenceCommentaire.set('Arrêt maladie');
    component.repriseOuJustificationDansDelai.set(true);
    component.dateRepriseOuJustification.set('2026-03-10');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateMiseEnDemeurePresentee: '2026-03-01',
      modeNotificationMiseEnDemeure: 'LRAR',
      delaiAccordeJours: 14,
      miseEnDemeureMentionneDelai: true,
      miseEnDemeureMentionneConsequences: false,
      motifAbsenceInvoque: 'MEDICAL',
      motifAbsenceCommentaire: 'Arrêt maladie',
      repriseOuJustificationDansDelai: true,
      dateRepriseOuJustification: '2026-03-10',
    });
    req.flush(response('CONTESTATION_SOLIDE'));

    expect(component.result()!.verdict).toBe('CONTESTATION_SOLIDE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse de la contestation calculée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() — dateRepriseOuJustification = null si toggle reprise off', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.repriseOuJustificationDansDelai.set(false);
    component.dateRepriseOuJustification.set('2026-03-10'); // valeur résiduelle
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.repriseOuJustificationDansDelai).toBe(false);
    expect(req.request.body.dateRepriseOuJustification).toBeNull();
    req.flush(response('CONTESTATION_INCERTAINE'));
  });

  it('calculate() erreur backend → snackbar rouge, formulaire conservé', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Verdicts — 3 niveaux (rouge réservé à CONTESTATION_DIFFICILE)
  // ---------------------------------------------------------------------------

  it('verdict CONTESTATION_SOLIDE → banner navy --available', () => {
    expect(component.verdictBannerClass('CONTESTATION_SOLIDE')).toContain('--available');
    expect(component.verdictBannerClass('CONTESTATION_SOLIDE')).not.toContain('--danger');
    expect(component.verdictBannerLabel('CONTESTATION_SOLIDE')).toContain('solide');
    expect(component.verdictBannerIcon('CONTESTATION_SOLIDE')).toBe('check_circle');
  });

  it('verdict CONTESTATION_INCERTAINE → banner or --medium', () => {
    expect(component.verdictBannerClass('CONTESTATION_INCERTAINE')).toContain('--medium');
    expect(component.verdictBannerClass('CONTESTATION_INCERTAINE')).not.toContain('--danger');
    expect(component.verdictBannerLabel('CONTESTATION_INCERTAINE')).toContain('incertaine');
    expect(component.verdictBannerIcon('CONTESTATION_INCERTAINE')).toBe('warning');
  });

  it('verdict CONTESTATION_DIFFICILE → banner rouge --danger', () => {
    expect(component.verdictBannerClass('CONTESTATION_DIFFICILE')).toContain('--danger');
    expect(component.verdictBannerLabel('CONTESTATION_DIFFICILE')).toContain('difficile');
    expect(component.verdictBannerIcon('CONTESTATION_DIFFICILE')).toBe('error');
  });

  it('GET 200 CONTESTATION_SOLIDE → motif détecté rendu', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response('CONTESTATION_SOLIDE'));
    expect(component.result()!.verdict).toBe('CONTESTATION_SOLIDE');
    expect(component.result()!.motifsContestation[0].code).toBe('DT42_DELAI_INSUFFISANT');
  });

  // ---------------------------------------------------------------------------
  // Bannière BELGIQUE
  // ---------------------------------------------------------------------------

  it('bannière info affichée si workspace BELGIQUE', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="apd-country-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('France uniquement');
  });

  it('pas de bannière BE si workspace FRANCE', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="apd-country-banner"]');
    expect(banner).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // SF-206-02 — pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('getPrefillCount() cas 0 — input vide retourne 0', () => {
    expect(AbandonPostePresomptionDemissionSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount() cas partiel — 3 champs renseignés retourne 3', () => {
    expect(AbandonPostePresomptionDemissionSectionComponent.getPrefillCount({
      aiData: {
        abandonPosteDateMiseEnDemeure: '2026-03-01',
        abandonPosteDelaiAccordeJours: 15,
        abandonPosteMotifAbsence: 'MEDICAL',
      },
      workspaceCountry: 'FRANCE',
    })).toBe(3);
  });

  it('getPrefillCount() cas nominal — 8 champs retourne 8', () => {
    expect(AbandonPostePresomptionDemissionSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(8);
  });

  it('getPrefillCount() BELGIQUE retourne 0', () => {
    expect(AbandonPostePresomptionDemissionSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('prefillFromAi : 8 champs renseignés + badges de provenance présents', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateMiseEnDemeurePresentee()).toBe('2026-03-01');
    expect(component.modeNotificationMiseEnDemeure()).toBe('LRAR');
    expect(component.delaiAccordeJours()).toBe(14);
    expect(component.motifAbsenceInvoque()).toBe('MEDICAL');
    expect(component.dateRepriseOuJustification()).toBe('2026-03-10');
    expect(component.miseEnDemeureMentionneDelai()).toBe(true);
    expect(component.miseEnDemeureMentionneConsequences()).toBe(false);
    expect(component.repriseOuJustificationDansDelai()).toBe(true);
    // Badges de provenance des 8 champs instrumentés.
    expect(component.provenanceDateMiseEnDemeure()).toBe('IA');
    expect(component.provenanceModeNotification()).toBe('IA');
    expect(component.provenanceDelaiAccordeJours()).toBe('IA');
    expect(component.provenanceMotifAbsence()).toBe('IA');
    expect(component.provenanceDateReprise()).toBe('IA');
    expect(component.provenanceMedMentionneDelai()).toBe('IA');
    expect(component.provenanceMedMentionneConsequences()).toBe('IA');
    expect(component.provenanceRepriseDansDelai()).toBe('IA');
  });

  it('prefillFromAi : parité stricte getPrefillCount ↔ badges IA présents', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const count = AbandonPostePresomptionDemissionSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    });
    // Tous les 8 champs sont instrumentés avec un badge IA.
    const badged = [
      component.provenanceDateMiseEnDemeure(),
      component.provenanceModeNotification(),
      component.provenanceDelaiAccordeJours(),
      component.provenanceMotifAbsence(),
      component.provenanceDateReprise(),
      component.provenanceMedMentionneDelai(),
      component.provenanceMedMentionneConsequences(),
      component.provenanceRepriseDansDelai(),
    ].filter((p) => p === 'IA').length;
    expect(badged).toBe(count);
  });

  it('prefillFromAi : no-op gracieux si aiData absent', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.provenanceDateMiseEnDemeure()).toBeNull();
    expect(component.provenanceMotifAbsence()).toBeNull();
  });

  it('prefillFromAi : dossier BELGIQUE → aucun champ pré-rempli', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    // Pas de GET en BE (outil FR-only).
    expect(component.provenanceDateMiseEnDemeure()).toBeNull();
    expect(component.provenanceMotifAbsence()).toBeNull();
    expect(component.dateMiseEnDemeurePresentee()).toBeNull();
  });

  it('onDateMedChange efface le badge IA', () => {
    component.provenanceDateMiseEnDemeure.set('IA');
    component.onDateMedChange('2026-04-01');
    expect(component.dateMiseEnDemeurePresentee()).toBe('2026-04-01');
    expect(component.provenanceDateMiseEnDemeure()).toBeNull();
  });

  it('onMotifAbsenceChange efface le badge IA + reset commentaire si AUCUN', () => {
    component.provenanceMotifAbsence.set('IA');
    component.motifAbsenceCommentaire.set('comment');
    component.onMotifAbsenceChange('AUCUN');
    expect(component.motifAbsenceInvoque()).toBe('AUCUN');
    expect(component.provenanceMotifAbsence()).toBeNull();
    expect(component.motifAbsenceCommentaire()).toBeNull();
  });

  it('onRepriseDansDelaiChange(false) reset date reprise', () => {
    component.repriseOuJustificationDansDelai.set(true);
    component.dateRepriseOuJustification.set('2026-03-10');
    component.provenanceDateReprise.set('IA');
    component.onRepriseDansDelaiChange(false);
    expect(component.repriseOuJustificationDansDelai()).toBe(false);
    expect(component.dateRepriseOuJustification()).toBeNull();
    expect(component.provenanceDateReprise()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Métadonnées statiques (TOOL_LABEL / TOOL_ICON)
  // ---------------------------------------------------------------------------

  it('expose TOOL_LABEL et TOOL_ICON statiques', () => {
    expect(AbandonPostePresomptionDemissionSectionComponent.TOOL_LABEL).toContain('ABANDON');
    expect(AbandonPostePresomptionDemissionSectionComponent.TOOL_ICON).toBe('no_accounts');
  });

  // ---------------------------------------------------------------------------
  // Rendu du résultat — échéance inter-onglets vers Suivi
  // ---------------------------------------------------------------------------

  it('rendu résultat : mention « onglet Suivi » présente avec date d\'expiration', () => {
    component.collapsed.set(false);
    fixture.detectChanges(); // déclenche ngOnInit() → GET
    httpMock.expectOne(BASE_URL).flush(response('CONTESTATION_INCERTAINE'));
    fixture.detectChanges();
    const node = fixture.nativeElement.querySelector('[data-testid="apd-deadline-suivi-note"]');
    expect(node).not.toBeNull();
    expect(node.textContent).toContain('Suivi');
    expect(node.textContent).toContain('2026-03-15');
  });

  // ---------------------------------------------------------------------------
  // Validité du formulaire (gate isFrance + délai)
  // ---------------------------------------------------------------------------

  it('formValid() false en BELGIQUE', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.formValid()).toBe(false);
  });

  it('formValid() true en FRANCE avec délai par défaut', () => {
    expect(component.formValid()).toBe(true);
  });

  it('toggleCollapse fonctionne', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
  });

  it('editMode ré-affiche le form après calcul', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // coherenceAlerts F-IA-03 (DELAI_ACCORDE : auto-alerte IA si délai < 15 j)
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.DELAI_ACCORDE lève une alerte IA si délai < 15 j', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.delaiAccordeJours.set(10);
    const alert = component.coherenceAlerts().DELAI_ACCORDE;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('IA');
  });

  it('coherenceAlerts.DELAI_ACCORDE absente si délai ≥ 15 j', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.delaiAccordeJours.set(20);
    expect(component.coherenceAlerts().DELAI_ACCORDE).toBeUndefined();
  });

  it('coherenceAlerts vide en standaloneMode', () => {
    component.standaloneMode = true;
    component.ngOnInit();
    // Le GET initial est tjrs émis en FRANCE même standaloneMode (cohérence avec
    // la même structure que F-DT-36). On le purge pour ne pas pollueur httpMock.
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.delaiAccordeJours.set(5);
    expect(Object.keys(component.coherenceAlerts())).toHaveLength(0);
  });

  it('coherenceAlerts masquées après calcul (showForm=false)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.delaiAccordeJours.set(5);
    expect(component.coherenceAlerts().DELAI_ACCORDE).toBeDefined();
    component.showForm.set(false);
    expect(component.coherenceAlerts().DELAI_ACCORDE).toBeUndefined();
  });
});
