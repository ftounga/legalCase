import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ResiliationJudiciaireCphSectionComponent } from './resiliation-judiciaire-cph-section.component';
import {
  ResiliationJudiciaireCphResponse,
  ResiliationJudiciaireCphVerdict,
  ResiliationJudiciaireCphDateEffetProbable,
} from '../../core/models/resiliation-judiciaire-cph.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('ResiliationJudiciaireCphSectionComponent', () => {
  let component: ResiliationJudiciaireCphSectionComponent;
  let fixture: ComponentFixture<ResiliationJudiciaireCphSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/resiliation-judiciaire-cph';

  /** SF-206-08 : fixture IA complète — les 12 champs resiliationJud*. */
  const FULL_AI_DATA: TravailExtractedData = {
    resiliationJudDefautPaiementSalaire: true,
    resiliationJudMontantImpayes: 3200,
    resiliationJudHarcelement: false,
    resiliationJudManquementSecurite: true,
    resiliationJudModificationContrat: false,
    resiliationJudDeclassement: false,
    resiliationJudDiscrimination: false,
    resiliationJudHeuresSupNonPayees: true,
    resiliationJudNonRespectRepos: false,
    resiliationJudManquementsPersistants: true,
    resiliationJudSalarieEnPoste: true,
    resiliationJudLicenciementEnCours: false,
  };

  function response(
    verdict: ResiliationJudiciaireCphVerdict,
    dateEffetProbable: ResiliationJudiciaireCphDateEffetProbable = 'DATE_DECISION',
  ): ResiliationJudiciaireCphResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs ré-exposé par la réponse (ré-édition du formulaire).
      defautPaiementSalaire: true,
      montantImpayesEur: 3200,
      harcelement: false,
      manquementSecurite: true,
      modificationUnilateraleContrat: false,
      declassement: false,
      discrimination: false,
      heuresSupNonPayees: true,
      nonRespectDureesRepos: false,
      manquementsPersistantsAuJourDemande: true,
      salarieToujoursEnPoste: true,
      licenciementIntervenuEnCoursInstance: dateEffetProbable === 'DATE_LICENCIEMENT',
      manquementsCommentaire: null,
      // Champs calculés.
      verdict,
      scoreSolidite: verdict === 'RESILIATION_FAVORABLE' ? 75
                   : verdict === 'RESILIATION_INCERTAINE' ? 35
                   : 10,
      manquementsRetenus: [
        {
          code: 'DT40_DEFAUT_PAIEMENT_SALAIRE',
          libelle: 'Défaut ou retard de paiement du salaire',
          fondement: 'Cass. soc. 20/03/2013 n°11-26.770',
          poids: 30,
          explication: 'Défaut de paiement significatif et persistant.',
        },
      ],
      dateEffetProbable,
      basesJuridiques: [
        'Cass. soc. 16/03/1989',
        'Cass. soc. 20/01/1998',
        'art. L.1411-1 CT',
      ],
      messages: [
        'Voie sans risque de rupture : un rejet de la demande ne rompt pas le contrat.',
      ],
      country: 'FRANCE',
      calculatedAt: '2026-05-20T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        ResiliationJudiciaireCphSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ResiliationJudiciaireCphSectionComponent);
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
    httpMock.expectOne(BASE_URL).flush(response('RESILIATION_FAVORABLE'));
    expect(component.result()!.verdict).toBe('RESILIATION_FAVORABLE');
    expect(component.showForm()).toBe(false);
  });

  it('GET 404 → reste en mode formulaire, pas de result', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('GET 200 → form ré-hydraté depuis le snapshot (ré-édition)', () => {
    component.ngOnInit();
    const r = response('RESILIATION_FAVORABLE');
    r.harcelement = true;
    r.discrimination = false;
    r.manquementsCommentaire = 'Précisions';
    httpMock.expectOne(BASE_URL).flush(r);

    expect(component.defautPaiementSalaire()).toBe(true);
    expect(component.harcelement()).toBe(true);
    expect(component.discrimination()).toBe(false);
    expect(component.manquementsCommentaire()).toBe('Précisions');

    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST)
  // ---------------------------------------------------------------------------

  it('calculate() POST → payload 13 champs conforme au contrat + snackbar + refresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.defautPaiementSalaire.set(true);
    component.montantImpayesEur.set(3200);
    component.harcelement.set(false);
    component.manquementSecurite.set(true);
    component.modificationUnilateraleContrat.set(false);
    component.declassement.set(false);
    component.discrimination.set(false);
    component.heuresSupNonPayees.set(true);
    component.nonRespectDureesRepos.set(false);
    component.manquementsPersistantsAuJourDemande.set(true);
    component.salarieToujoursEnPoste.set(true);
    component.licenciementIntervenuEnCoursInstance.set(false);
    component.manquementsCommentaire.set('');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      defautPaiementSalaire: true,
      montantImpayesEur: 3200,
      harcelement: false,
      manquementSecurite: true,
      modificationUnilateraleContrat: false,
      declassement: false,
      discrimination: false,
      heuresSupNonPayees: true,
      nonRespectDureesRepos: false,
      manquementsPersistantsAuJourDemande: true,
      salarieToujoursEnPoste: true,
      licenciementIntervenuEnCoursInstance: false,
      manquementsCommentaire: null,
    });
    req.flush(response('RESILIATION_FAVORABLE'));

    expect(component.result()!.verdict).toBe('RESILIATION_FAVORABLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse de résiliation judiciaire calculée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() — montantImpayesEur null forcé si defautPaiementSalaire=false', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.defautPaiementSalaire.set(false);
    component.montantImpayesEur.set(1500); // valeur résiduelle ignorée
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.defautPaiementSalaire).toBe(false);
    expect(req.request.body.montantImpayesEur).toBeNull();
    req.flush(response('RESILIATION_DEFAVORABLE'));
  });

  it('calculate() — manquementsCommentaire trimmed avant POST', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.harcelement.set(true);
    component.manquementsCommentaire.set('   Note   ');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.manquementsCommentaire).toBe('Note');
    req.flush(response('RESILIATION_FAVORABLE'));
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
  // Verdicts — 3 niveaux
  // ---------------------------------------------------------------------------

  it('verdict RESILIATION_FAVORABLE → banner --favorable', () => {
    expect(component.verdictBannerClass('RESILIATION_FAVORABLE')).toContain('--favorable');
    expect(component.verdictBannerLabel('RESILIATION_FAVORABLE')).toContain('favorable');
    expect(component.verdictBannerIcon('RESILIATION_FAVORABLE')).toBe('check_circle');
  });

  it('verdict RESILIATION_INCERTAINE → banner --incertaine', () => {
    expect(component.verdictBannerClass('RESILIATION_INCERTAINE')).toContain('--incertaine');
    expect(component.verdictBannerLabel('RESILIATION_INCERTAINE')).toContain('incertaine');
    expect(component.verdictBannerIcon('RESILIATION_INCERTAINE')).toBe('warning');
  });

  it('verdict RESILIATION_DEFAVORABLE → banner --defavorable (navy, NON rouge)', () => {
    expect(component.verdictBannerClass('RESILIATION_DEFAVORABLE')).toContain('--defavorable');
    expect(component.verdictBannerLabel('RESILIATION_DEFAVORABLE')).toContain('défavorable');
    // Icône info (NON error) — voie sans risque, le rejet ne rompt pas le contrat.
    expect(component.verdictBannerIcon('RESILIATION_DEFAVORABLE')).toBe('info');
  });

  // ---------------------------------------------------------------------------
  // Dates d'effet probable — 2 niveaux
  // ---------------------------------------------------------------------------

  it('dateEffetProbable DATE_DECISION → libellé + pill navy', () => {
    expect(component.dateEffetProbableLabel('DATE_DECISION')).toContain('date du jugement');
    expect(component.dateEffetProbableClass('DATE_DECISION')).toContain('date-decision');
  });

  it('dateEffetProbable DATE_LICENCIEMENT → libellé + pill indigo', () => {
    expect(component.dateEffetProbableLabel('DATE_LICENCIEMENT')).toContain('licenciement intervenu');
    expect(component.dateEffetProbableClass('DATE_LICENCIEMENT')).toContain('date-licenciement');
  });

  it('rendu résultat — date d\'effet DATE_DECISION affichée', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(
      response('RESILIATION_FAVORABLE', 'DATE_DECISION'));
    fixture.detectChanges();
    const node = fixture.nativeElement.querySelector('[data-testid="rjc-date-effet-probable"]');
    expect(node).not.toBeNull();
    expect(node.textContent).toContain('date du jugement');
  });

  it('rendu résultat — date d\'effet DATE_LICENCIEMENT affichée', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(
      response('RESILIATION_FAVORABLE', 'DATE_LICENCIEMENT'));
    fixture.detectChanges();
    const node = fixture.nativeElement.querySelector('[data-testid="rjc-date-effet-probable"]');
    expect(node?.textContent).toContain('licenciement intervenu');
  });

  it('rendu résultat — verdict défavorable affiché (voie sans risque)', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(
      response('RESILIATION_DEFAVORABLE', 'DATE_DECISION'));
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.rjc-verdict-banner--defavorable');
    expect(banner).not.toBeNull();
  });

  it('rendu résultat — liste des manquements retenus affichée', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(response('RESILIATION_FAVORABLE'));
    fixture.detectChanges();
    const items = fixture.nativeElement.querySelectorAll('[data-testid="rjc-manquement-item"]');
    expect(items.length).toBeGreaterThanOrEqual(1);
    expect(items[0].textContent).toContain('Défaut');
    expect(items[0].textContent).toContain('+30');
  });

  // ---------------------------------------------------------------------------
  // Message persistant — voie moins risquée vs prise d'acte
  // ---------------------------------------------------------------------------

  it('message persistant visible en mode formulaire (FRANCE)', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="rjc-safety-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('moins risquée');
    expect(banner.textContent).toContain('prise d\'acte');
  });

  it('message persistant visible aussi en mode résultat', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(response('RESILIATION_DEFAVORABLE', 'DATE_DECISION'));
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="rjc-safety-banner"]');
    expect(banner).not.toBeNull();
  });

  it('pas de message persistant si workspace BELGIQUE (form non rendu)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="rjc-safety-banner"]');
    expect(banner).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Bannière BELGIQUE
  // ---------------------------------------------------------------------------

  it('bannière info affichée si workspace BELGIQUE', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="rjc-country-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('France uniquement');
  });

  it('pas de bannière BE si workspace FRANCE', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="rjc-country-banner"]');
    expect(banner).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount (parité stricte)
  // ---------------------------------------------------------------------------

  it('getPrefillCount() cas 0 — input vide retourne 0', () => {
    expect(ResiliationJudiciaireCphSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount() cas partiel — 3 champs renseignés retourne 3', () => {
    expect(ResiliationJudiciaireCphSectionComponent.getPrefillCount({
      aiData: {
        resiliationJudDefautPaiementSalaire: true,
        resiliationJudMontantImpayes: 1200,
        resiliationJudHarcelement: true,
      },
      workspaceCountry: 'FRANCE',
    })).toBe(3);
  });

  it('getPrefillCount() cas nominal — 12 champs retourne 12', () => {
    expect(ResiliationJudiciaireCphSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(12);
  });

  it('getPrefillCount() BELGIQUE retourne 0', () => {
    expect(ResiliationJudiciaireCphSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('prefillFromAi : 12 champs renseignés + badges de provenance présents', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.defautPaiementSalaire()).toBe(true);
    expect(component.montantImpayesEur()).toBe(3200);
    expect(component.harcelement()).toBe(false);
    expect(component.manquementSecurite()).toBe(true);
    expect(component.heuresSupNonPayees()).toBe(true);
    expect(component.manquementsPersistantsAuJourDemande()).toBe(true);
    expect(component.salarieToujoursEnPoste()).toBe(true);
    expect(component.licenciementIntervenuEnCoursInstance()).toBe(false);

    // Badges IA des 12 champs.
    expect(component.provenanceDefautPaiement()).toBe('IA');
    expect(component.provenanceMontantImpayes()).toBe('IA');
    expect(component.provenanceHarcelement()).toBe('IA');
    expect(component.provenanceManquementSecurite()).toBe('IA');
    expect(component.provenanceModificationContrat()).toBe('IA');
    expect(component.provenanceDeclassement()).toBe('IA');
    expect(component.provenanceDiscrimination()).toBe('IA');
    expect(component.provenanceHeuresSupNonPayees()).toBe('IA');
    expect(component.provenanceNonRespectRepos()).toBe('IA');
    expect(component.provenanceManquementsPersistants()).toBe('IA');
    expect(component.provenanceSalarieEnPoste()).toBe('IA');
    expect(component.provenanceLicenciementEnCours()).toBe('IA');
  });

  it('prefillFromAi : parité stricte getPrefillCount ↔ badges IA présents', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const count = ResiliationJudiciaireCphSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    });
    const badged = [
      component.provenanceDefautPaiement(),
      component.provenanceMontantImpayes(),
      component.provenanceHarcelement(),
      component.provenanceManquementSecurite(),
      component.provenanceModificationContrat(),
      component.provenanceDeclassement(),
      component.provenanceDiscrimination(),
      component.provenanceHeuresSupNonPayees(),
      component.provenanceNonRespectRepos(),
      component.provenanceManquementsPersistants(),
      component.provenanceSalarieEnPoste(),
      component.provenanceLicenciementEnCours(),
    ].filter((p) => p === 'IA').length;
    expect(badged).toBe(count);
  });

  it('prefillFromAi : no-op gracieux si aiData absent', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.provenanceDefautPaiement()).toBeNull();
    expect(component.provenanceHarcelement()).toBeNull();
  });

  it('prefillFromAi : dossier BELGIQUE → aucun champ pré-rempli', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    expect(component.provenanceDefautPaiement()).toBeNull();
    expect(component.provenanceHarcelement()).toBeNull();
  });

  it('onDefautPaiementChange efface le badge IA + reset montant si false', () => {
    component.provenanceDefautPaiement.set('IA');
    component.montantImpayesEur.set(1500);
    component.provenanceMontantImpayes.set('IA');
    component.onDefautPaiementChange(false);
    expect(component.defautPaiementSalaire()).toBe(false);
    expect(component.provenanceDefautPaiement()).toBeNull();
    expect(component.montantImpayesEur()).toBeNull();
    expect(component.provenanceMontantImpayes()).toBeNull();
  });

  it('onHarcelementChange efface le badge IA', () => {
    component.provenanceHarcelement.set('IA');
    component.onHarcelementChange(true);
    expect(component.harcelement()).toBe(true);
    expect(component.provenanceHarcelement()).toBeNull();
  });

  it('onManquementsPersistantsChange efface le badge IA', () => {
    component.provenanceManquementsPersistants.set('IA');
    component.onManquementsPersistantsChange(false);
    expect(component.manquementsPersistantsAuJourDemande()).toBe(false);
    expect(component.provenanceManquementsPersistants()).toBeNull();
  });

  it('onLicenciementEnCoursChange efface le badge IA', () => {
    component.provenanceLicenciementEnCours.set('IA');
    component.onLicenciementEnCoursChange(true);
    expect(component.licenciementIntervenuEnCoursInstance()).toBe(true);
    expect(component.provenanceLicenciementEnCours()).toBeNull();
  });

  it('onMontantImpayesChange efface le badge IA + clamp ≥ 0', () => {
    component.provenanceMontantImpayes.set('IA');
    component.onMontantImpayesChange(-100);
    expect(component.montantImpayesEur()).toBe(0);
    expect(component.provenanceMontantImpayes()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Champ conditionnel montantImpayesEur (rendu)
  // ---------------------------------------------------------------------------

  it('champ montantImpayesEur masqué si defautPaiementSalaire=false', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.defautPaiementSalaire.set(false);
    fixture.detectChanges();
    const input = fixture.nativeElement.querySelector('input[name="montantImpayesEur"]');
    expect(input).toBeNull();
  });

  it('champ montantImpayesEur affiché si defautPaiementSalaire=true', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.defautPaiementSalaire.set(true);
    fixture.detectChanges();
    const input = fixture.nativeElement.querySelector('input[name="montantImpayesEur"]');
    expect(input).not.toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Badges auto_awesome dans le DOM
  // ---------------------------------------------------------------------------

  it('rendu : badges auto_awesome visibles pour champs pré-remplis', () => {
    component.aiData = FULL_AI_DATA;
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    const provenanceNotes = fixture.nativeElement.querySelectorAll('.rjc-provenance-note');
    expect(provenanceNotes.length).toBeGreaterThanOrEqual(12);
    const icon = fixture.nativeElement.querySelector('.rjc-provenance-note mat-icon');
    expect(icon?.textContent?.trim()).toBe('auto_awesome');
  });

  // ---------------------------------------------------------------------------
  // Métadonnées statiques (TOOL_LABEL / TOOL_ICON)
  // ---------------------------------------------------------------------------

  it('expose TOOL_LABEL et TOOL_ICON statiques', () => {
    expect(ResiliationJudiciaireCphSectionComponent.TOOL_LABEL).toContain('RÉSILIATION');
    expect(ResiliationJudiciaireCphSectionComponent.TOOL_ICON).toBe('balance');
  });

  // ---------------------------------------------------------------------------
  // Validité du formulaire (gate isFrance + montantImpayesEur ≥ 0)
  // ---------------------------------------------------------------------------

  it('formValid() false en BELGIQUE', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.formValid()).toBe(false);
  });

  it('formValid() true en FRANCE avec valeurs par défaut', () => {
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
  // coherenceAlerts F-IA-03 — masquage standalone / après calcul
  // ---------------------------------------------------------------------------

  it('coherenceAlerts vide en standaloneMode', () => {
    component.standaloneMode = true;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(Object.keys(component.coherenceAlerts())).toHaveLength(0);
  });

  it('coherenceAlerts masquées après calcul (showForm=false)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.showForm.set(false);
    expect(Object.keys(component.coherenceAlerts())).toHaveLength(0);
  });
});
