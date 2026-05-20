import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PriseActeRuptureSectionComponent } from './prise-acte-rupture-section.component';
import {
  PriseActeRuptureResponse,
  PriseActeRuptureVerdict,
  PriseActeRuptureEffetProbable,
} from '../../core/models/prise-acte-rupture.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('PriseActeRuptureSectionComponent', () => {
  let component: PriseActeRuptureSectionComponent;
  let fixture: ComponentFixture<PriseActeRuptureSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/prise-acte-rupture';

  /** SF-206-06 : fixture IA complète — les 11 champs priseActe*. */
  const FULL_AI_DATA: TravailExtractedData = {
    priseActeDefautPaiementSalaire: true,
    priseActeMontantImpayes: 3200,
    priseActeHarcelement: false,
    priseActeManquementSecurite: true,
    priseActeModificationContrat: false,
    priseActeDeclassement: false,
    priseActeDiscrimination: false,
    priseActeHeuresSupNonPayees: true,
    priseActeNonRespectRepos: false,
    priseActeGriefsPersistants: true,
    priseActeGriefImpossiblePoursuite: true,
  };

  function response(
    verdict: PriseActeRuptureVerdict,
    effetProbable: PriseActeRuptureEffetProbable = 'LICENCIEMENT_SANS_CAUSE',
  ): PriseActeRuptureResponse {
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
      griefsActuelsEtPersistants: true,
      griefRendImpossiblePoursuite: true,
      griefsCommentaire: null,
      // Champs calculés.
      verdict,
      scoreSolidite: verdict === 'PRISE_ACTE_FAVORABLE' ? 75
                   : verdict === 'PRISE_ACTE_RISQUEE' ? 35
                   : 10,
      griefsRetenus: [
        {
          code: 'DT39_DEFAUT_PAIEMENT_SALAIRE',
          libelle: 'Défaut ou retard de paiement du salaire',
          fondement: 'Cass. soc. 20/03/2013 n°11-26.770',
          poids: 30,
          explication: 'Défaut de paiement significatif et persistant.',
        },
      ],
      effetProbable,
      basesJuridiques: [
        'Cass. soc. 25/06/2003 n°01-42.679',
        'Cass. soc. 26/03/2014 n°12-21.372',
      ],
      messages: ['Prise d\'acte favorable : effets licenciement probables.'],
      country: 'FRANCE',
      calculatedAt: '2026-05-20T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        PriseActeRuptureSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PriseActeRuptureSectionComponent);
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
    httpMock.expectOne(BASE_URL).flush(response('PRISE_ACTE_FAVORABLE'));
    expect(component.result()!.verdict).toBe('PRISE_ACTE_FAVORABLE');
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
    const r = response('PRISE_ACTE_FAVORABLE');
    r.harcelement = true;
    r.discrimination = false;
    r.griefsCommentaire = 'Précisions';
    httpMock.expectOne(BASE_URL).flush(r);

    expect(component.defautPaiementSalaire()).toBe(true);
    expect(component.harcelement()).toBe(true);
    expect(component.discrimination()).toBe(false);
    expect(component.griefsCommentaire()).toBe('Précisions');

    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST)
  // ---------------------------------------------------------------------------

  it('calculate() POST → payload 12 champs conforme au contrat + snackbar + refresh', () => {
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
    component.griefsActuelsEtPersistants.set(true);
    component.griefRendImpossiblePoursuite.set(true);
    component.griefsCommentaire.set('');
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
      griefsActuelsEtPersistants: true,
      griefRendImpossiblePoursuite: true,
      griefsCommentaire: null,
    });
    req.flush(response('PRISE_ACTE_FAVORABLE'));

    expect(component.result()!.verdict).toBe('PRISE_ACTE_FAVORABLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse de prise d\'acte calculée', 'OK', jasmine.any(Object));
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
    req.flush(response('PRISE_ACTE_DEFAVORABLE', 'DEMISSION'));
  });

  it('calculate() — griefsCommentaire trimmed avant POST', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.harcelement.set(true);
    component.griefsCommentaire.set('   Note   ');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.griefsCommentaire).toBe('Note');
    req.flush(response('PRISE_ACTE_FAVORABLE', 'LICENCIEMENT_NUL'));
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

  it('verdict PRISE_ACTE_FAVORABLE → banner --favorable', () => {
    expect(component.verdictBannerClass('PRISE_ACTE_FAVORABLE')).toContain('--favorable');
    expect(component.verdictBannerLabel('PRISE_ACTE_FAVORABLE')).toContain('favorable');
    expect(component.verdictBannerIcon('PRISE_ACTE_FAVORABLE')).toBe('check_circle');
  });

  it('verdict PRISE_ACTE_RISQUEE → banner --risquee', () => {
    expect(component.verdictBannerClass('PRISE_ACTE_RISQUEE')).toContain('--risquee');
    expect(component.verdictBannerLabel('PRISE_ACTE_RISQUEE')).toContain('risquée');
    expect(component.verdictBannerIcon('PRISE_ACTE_RISQUEE')).toBe('warning');
  });

  it('verdict PRISE_ACTE_DEFAVORABLE → banner --defavorable', () => {
    expect(component.verdictBannerClass('PRISE_ACTE_DEFAVORABLE')).toContain('--defavorable');
    expect(component.verdictBannerLabel('PRISE_ACTE_DEFAVORABLE')).toContain('défavorable');
    expect(component.verdictBannerIcon('PRISE_ACTE_DEFAVORABLE')).toBe('error');
  });

  // ---------------------------------------------------------------------------
  // Effets probables — 3 niveaux (avec bascule LICENCIEMENT_NUL explicite)
  // ---------------------------------------------------------------------------

  it('effetProbable LICENCIEMENT_SANS_CAUSE → libellé "sans cause" + pill navy', () => {
    expect(component.effetProbableLabel('LICENCIEMENT_SANS_CAUSE')).toContain('sans cause');
    expect(component.effetProbableClass('LICENCIEMENT_SANS_CAUSE')).toContain('licenciement-sans-cause');
  });

  it('effetProbable LICENCIEMENT_NUL → libellé "NUL" + pill spécifique', () => {
    expect(component.effetProbableLabel('LICENCIEMENT_NUL')).toContain('NUL');
    expect(component.effetProbableClass('LICENCIEMENT_NUL')).toContain('licenciement-nul');
  });

  it('effetProbable DEMISSION → libellé "Démission" + pill rouge', () => {
    expect(component.effetProbableLabel('DEMISSION')).toContain('Démission');
    expect(component.effetProbableClass('DEMISSION')).toContain('demission');
  });

  it('rendu résultat — bascule harcèlement → LICENCIEMENT_NUL affichée', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(
      response('PRISE_ACTE_FAVORABLE', 'LICENCIEMENT_NUL'));
    fixture.detectChanges();
    const node = fixture.nativeElement.querySelector('[data-testid="par-effet-probable"]');
    expect(node).not.toBeNull();
    expect(node.textContent).toContain('NUL');
  });

  it('rendu résultat — bascule discrimination → LICENCIEMENT_NUL affichée', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(
      response('PRISE_ACTE_FAVORABLE', 'LICENCIEMENT_NUL'));
    fixture.detectChanges();
    const node = fixture.nativeElement.querySelector('[data-testid="par-effet-probable"]');
    expect(node?.textContent).toContain('NUL');
  });

  it('rendu résultat — verdict défavorable + effet DEMISSION', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(
      response('PRISE_ACTE_DEFAVORABLE', 'DEMISSION'));
    fixture.detectChanges();
    const node = fixture.nativeElement.querySelector('[data-testid="par-effet-probable"]');
    expect(node?.textContent).toContain('Démission');
  });

  it('rendu résultat — liste des griefs retenus affichée', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(response('PRISE_ACTE_FAVORABLE'));
    fixture.detectChanges();
    const items = fixture.nativeElement.querySelectorAll('[data-testid="par-grief-item"]');
    expect(items.length).toBeGreaterThanOrEqual(1);
    expect(items[0].textContent).toContain('Défaut');
    expect(items[0].textContent).toContain('+30');
  });

  // ---------------------------------------------------------------------------
  // Avertissement persistant — rupture immédiate
  // ---------------------------------------------------------------------------

  it('avertissement persistant visible en mode formulaire (FRANCE)', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="par-warning-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('rupture immédiate');
    expect(banner.textContent).toContain('résiliation judiciaire');
  });

  it('avertissement persistant visible aussi en mode résultat', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(response('PRISE_ACTE_DEFAVORABLE', 'DEMISSION'));
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="par-warning-banner"]');
    expect(banner).not.toBeNull();
  });

  it('pas d\'avertissement si workspace BELGIQUE (form non rendu)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="par-warning-banner"]');
    expect(banner).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Bannière BELGIQUE
  // ---------------------------------------------------------------------------

  it('bannière info affichée si workspace BELGIQUE', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="par-country-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('France uniquement');
  });

  it('pas de bannière BE si workspace FRANCE', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="par-country-banner"]');
    expect(banner).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount (parité stricte)
  // ---------------------------------------------------------------------------

  it('getPrefillCount() cas 0 — input vide retourne 0', () => {
    expect(PriseActeRuptureSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount() cas partiel — 3 champs renseignés retourne 3', () => {
    expect(PriseActeRuptureSectionComponent.getPrefillCount({
      aiData: {
        priseActeDefautPaiementSalaire: true,
        priseActeMontantImpayes: 1200,
        priseActeHarcelement: true,
      },
      workspaceCountry: 'FRANCE',
    })).toBe(3);
  });

  it('getPrefillCount() cas nominal — 11 champs retourne 11', () => {
    expect(PriseActeRuptureSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(11);
  });

  it('getPrefillCount() BELGIQUE retourne 0', () => {
    expect(PriseActeRuptureSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('prefillFromAi : 11 champs renseignés + badges de provenance présents', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.defautPaiementSalaire()).toBe(true);
    expect(component.montantImpayesEur()).toBe(3200);
    expect(component.harcelement()).toBe(false);
    expect(component.manquementSecurite()).toBe(true);
    expect(component.heuresSupNonPayees()).toBe(true);
    expect(component.griefsActuelsEtPersistants()).toBe(true);
    expect(component.griefRendImpossiblePoursuite()).toBe(true);

    // Badges IA des 11 champs.
    expect(component.provenanceDefautPaiement()).toBe('IA');
    expect(component.provenanceMontantImpayes()).toBe('IA');
    expect(component.provenanceHarcelement()).toBe('IA');
    expect(component.provenanceManquementSecurite()).toBe('IA');
    expect(component.provenanceModificationContrat()).toBe('IA');
    expect(component.provenanceDeclassement()).toBe('IA');
    expect(component.provenanceDiscrimination()).toBe('IA');
    expect(component.provenanceHeuresSupNonPayees()).toBe('IA');
    expect(component.provenanceNonRespectRepos()).toBe('IA');
    expect(component.provenanceGriefsPersistants()).toBe('IA');
    expect(component.provenanceGriefImpossiblePoursuite()).toBe('IA');
  });

  it('prefillFromAi : parité stricte getPrefillCount ↔ badges IA présents', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const count = PriseActeRuptureSectionComponent.getPrefillCount({
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
      component.provenanceGriefsPersistants(),
      component.provenanceGriefImpossiblePoursuite(),
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
    const provenanceNotes = fixture.nativeElement.querySelectorAll('.par-provenance-note');
    expect(provenanceNotes.length).toBeGreaterThanOrEqual(11);
    const icon = fixture.nativeElement.querySelector('.par-provenance-note mat-icon');
    expect(icon?.textContent?.trim()).toBe('auto_awesome');
  });

  // ---------------------------------------------------------------------------
  // Métadonnées statiques (TOOL_LABEL / TOOL_ICON)
  // ---------------------------------------------------------------------------

  it('expose TOOL_LABEL et TOOL_ICON statiques', () => {
    expect(PriseActeRuptureSectionComponent.TOOL_LABEL).toContain('PRISE D\'ACTE');
    expect(PriseActeRuptureSectionComponent.TOOL_ICON).toBe('gavel');
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
