import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  RccBeEntrepriseDifficulteSectionComponent,
} from './rcc-be-entreprise-difficulte-section.component';
import {
  RccBeEntrepriseDifficulteResponse,
} from '../../core/models/rcc-be-entreprise-difficulte.model';

describe('RccBeEntrepriseDifficulteSectionComponent', () => {
  let component: RccBeEntrepriseDifficulteSectionComponent;
  let fixture: ComponentFixture<RccBeEntrepriseDifficulteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/rcc-be-entreprise-difficulte';

  /**
   * Construit une réponse backend par défaut (éligible 56+/15/8 + indemnité).
   */
  function response(
    overrides: Partial<RccBeEntrepriseDifficulteResponse> = {},
  ): RccBeEntrepriseDifficulteResponse {
    return {
      caseFileId: 'case-1',
      typeReconnaissance: 'EN_DIFFICULTE',
      ageReduitPlan: 55,
      ageFinContrat: 56,
      anneesCarriereTotale: 15,
      anneesAncienneteSecteur: 8,
      dateFinContrat: '2026-09-30',
      licenciementEffectif: true,
      remunerationNetteMensuelleReference: 2800,
      allocationChomageMensuelleEstimee: 1400,
      verdict: 'ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE',
      eligible: true,
      conditionReconnaissanceRemplie: true,
      conditionAgeRemplie: true,
      conditionCarriereRemplie: true,
      conditionAncienneteRemplie: true,
      conditionLicenciementRemplie: true,
      indemniteComplementaireMensuelle: 700.00,
      synthese: 'Conditions cumulatives remplies (reconnaissance ministérielle, '
        + 'âge ≥ 55, carrière ≥ 10 ans, ancienneté secteur ≥ 5 ans, '
        + 'licenciement effectif) — RCC entreprise difficulté ouvert.',
      baseJuridique: 'Loi du 26/12/2013 ; CCT n° 17 du 19/12/1974 ; '
        + 'AR du 03/05/2007 ; AR de reconnaissance ministérielle',
      avertissement: 'Le calcul de l\'indemnité complémentaire est indicatif — '
        + 'montant définitif à confirmer par calcul ONSS / ONEM en tenant '
        + 'compte des plafonds, du précompte professionnel et de l\'âge légal '
        + 'de la pension.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        RccBeEntrepriseDifficulteSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RccBeEntrepriseDifficulteSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ============================================================
  // Gate pays + init
  // ============================================================

  it('BELGIQUE → isAvailable() true, GET au ngOnInit', () => {
    expect(component.isAvailable()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('FRANCE → isAvailable() false, aucun GET au ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isAvailable()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
  });

  it('FRANCE → bannière « réservé droit BE » + masquage form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.empty-result');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('belge');
  });

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 → hydrate result + mode résultat + rehydrate form pour édition', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE');
    expect(component.showForm()).toBe(false);
    // Rehydratation form.
    expect(component.typeReconnaissance()).toBe('EN_DIFFICULTE');
    expect(component.ageReduitPlan()).toBe(55);
    expect(component.ageFinContrat()).toBe(56);
    expect(component.anneesCarriereTotale()).toBe(15);
    expect(component.anneesAncienneteSecteur()).toBe(8);
    expect(component.dateFinContrat()).toBe('2026-09-30');
    expect(component.licenciementEffectif()).toBe(true);
    expect(component.remunerationNetteMensuelleReference()).toBe(2800);
    expect(component.allocationChomageMensuelleEstimee()).toBe(1400);
  });

  it('GET 404 → reste en mode formulaire vierge', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.result()).toBeNull();
    expect(component.showForm()).toBe(true);
  });

  // ============================================================
  // Form valid
  // ============================================================

  it('formValid() : false sans champs (type non choisi)', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true avec champs requis (sans couple financier)', () => {
    component.typeReconnaissance.set('EN_DIFFICULTE');
    component.ageReduitPlan.set(55);
    component.ageFinContrat.set(56);
    component.anneesCarriereTotale.set(15);
    component.anneesAncienneteSecteur.set(8);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : true avec couple financier complet', () => {
    component.typeReconnaissance.set('EN_DIFFICULTE');
    component.ageReduitPlan.set(55);
    component.ageFinContrat.set(56);
    component.anneesCarriereTotale.set(15);
    component.anneesAncienneteSecteur.set(8);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    component.remunerationNetteMensuelleReference.set(2800);
    component.allocationChomageMensuelleEstimee.set(1400);
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false si âge négatif', () => {
    component.typeReconnaissance.set('EN_DIFFICULTE');
    component.ageReduitPlan.set(55);
    component.ageFinContrat.set(-1);
    component.anneesCarriereTotale.set(15);
    component.anneesAncienneteSecteur.set(8);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false si carrière négative', () => {
    component.typeReconnaissance.set('EN_DIFFICULTE');
    component.ageReduitPlan.set(55);
    component.ageFinContrat.set(56);
    component.anneesCarriereTotale.set(-5);
    component.anneesAncienneteSecteur.set(8);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false si ancienneté négative', () => {
    component.typeReconnaissance.set('EN_DIFFICULTE');
    component.ageReduitPlan.set(55);
    component.ageFinContrat.set(56);
    component.anneesCarriereTotale.set(15);
    component.anneesAncienneteSecteur.set(-1);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dateFinContrat', () => {
    component.typeReconnaissance.set('EN_DIFFICULTE');
    component.ageReduitPlan.set(55);
    component.ageFinContrat.set(56);
    component.anneesCarriereTotale.set(15);
    component.anneesAncienneteSecteur.set(8);
    component.licenciementEffectif.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false si couple financier dépareillé (réf seule)', () => {
    component.typeReconnaissance.set('EN_DIFFICULTE');
    component.ageReduitPlan.set(55);
    component.ageFinContrat.set(56);
    component.anneesCarriereTotale.set(15);
    component.anneesAncienneteSecteur.set(8);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    component.remunerationNetteMensuelleReference.set(2800);
    component.allocationChomageMensuelleEstimee.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false si couple financier dépareillé (alloc seule)', () => {
    component.typeReconnaissance.set('EN_DIFFICULTE');
    component.ageReduitPlan.set(55);
    component.ageFinContrat.set(56);
    component.anneesCarriereTotale.set(15);
    component.anneesAncienneteSecteur.set(8);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    component.remunerationNetteMensuelleReference.set(null);
    component.allocationChomageMensuelleEstimee.set(1400);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false si rémunération nette négative', () => {
    component.typeReconnaissance.set('EN_DIFFICULTE');
    component.ageReduitPlan.set(55);
    component.ageFinContrat.set(56);
    component.anneesCarriereTotale.set(15);
    component.anneesAncienneteSecteur.set(8);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    component.remunerationNetteMensuelleReference.set(-100);
    component.allocationChomageMensuelleEstimee.set(1400);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  it('calculate() → POST + dashboardRefresh + snack', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.typeReconnaissance.set('EN_DIFFICULTE');
    component.ageReduitPlan.set(55);
    component.ageFinContrat.set(56);
    component.anneesCarriereTotale.set(15);
    component.anneesAncienneteSecteur.set(8);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    component.remunerationNetteMensuelleReference.set(2800);
    component.allocationChomageMensuelleEstimee.set(1400);
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.typeReconnaissance).toBe('EN_DIFFICULTE');
    expect(post.request.body.ageReduitPlan).toBe(55);
    expect(post.request.body.ageFinContrat).toBe(56);
    expect(post.request.body.anneesCarriereTotale).toBe(15);
    expect(post.request.body.anneesAncienneteSecteur).toBe(8);
    expect(post.request.body.dateFinContrat).toBe('2026-09-30');
    expect(post.request.body.licenciementEffectif).toBe(true);
    expect(post.request.body.remunerationNetteMensuelleReference).toBe(2800);
    expect(post.request.body.allocationChomageMensuelleEstimee).toBe(1400);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'RCC entreprise difficulté analysé', 'OK', expect.anything(),
    );
  });

  it('calculate() : POST sans couple financier → body null/null', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.typeReconnaissance.set('EN_DIFFICULTE');
    component.ageReduitPlan.set(55);
    component.ageFinContrat.set(56);
    component.anneesCarriereTotale.set(15);
    component.anneesAncienneteSecteur.set(8);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.body.remunerationNetteMensuelleReference).toBeNull();
    expect(post.request.body.allocationChomageMensuelleEstimee).toBeNull();
    post.flush(response({
      remunerationNetteMensuelleReference: null,
      allocationChomageMensuelleEstimee: null,
      indemniteComplementaireMensuelle: null,
      avertissement: null,
    }));
  });

  it('calculate() : erreur 400 → snack "invalides"', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.typeReconnaissance.set('EN_DIFFICULTE');
    component.ageReduitPlan.set(55);
    component.ageFinContrat.set(56);
    component.anneesCarriereTotale.set(15);
    component.anneesAncienneteSecteur.set(8);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'champ invalide' }, { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'champ invalide', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : erreur 500 → snack générique', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.typeReconnaissance.set('EN_DIFFICULTE');
    component.ageReduitPlan.set(55);
    component.ageFinContrat.set(56);
    component.anneesCarriereTotale.set(15);
    component.anneesAncienneteSecteur.set(8);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({}, { status: 500, statusText: 'Server Error' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Une erreur est survenue. Veuillez réessayer.', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : erreur 404 → snack "Dossier introuvable"', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.typeReconnaissance.set('EN_DIFFICULTE');
    component.ageReduitPlan.set(55);
    component.ageFinContrat.set(56);
    component.anneesCarriereTotale.set(15);
    component.anneesAncienneteSecteur.set(8);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'not found' }, { status: 404, statusText: 'Not Found' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Dossier introuvable', 'Fermer', expect.anything(),
    );
  });

  // ============================================================
  // Verdict — label / classe / icône / badge (6 états)
  // ============================================================

  it('verdict ELIGIBLE → label, verdict-ok, verified, badge ÉLIGIBLE', () => {
    component.result.set(response({ verdict: 'ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE' }));
    expect(component.verdictLabel()).toContain('Éligible');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
    expect(component.verdictBadgeClass()).toBe('badge-ok');
    expect(component.verdictBadgeLabel()).toBe('ÉLIGIBLE');
  });

  it('verdict DEMISSION → verdict-critical, gpp_bad, badge INÉLIGIBLE', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_DEMISSION',
      eligible: false,
      conditionLicenciementRemplie: false,
      licenciementEffectif: false,
      indemniteComplementaireMensuelle: null,
    }));
    expect(component.verdictLabel()).toContain('démission');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('gpp_bad');
    expect(component.verdictBadgeClass()).toBe('badge-critical');
    expect(component.verdictBadgeLabel()).toBe('INÉLIGIBLE');
  });

  it('verdict RECONNAISSANCE_ABSENTE → label spécifique, verdict-critical', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_RECONNAISSANCE_ABSENTE',
      eligible: false,
      conditionReconnaissanceRemplie: false,
      typeReconnaissance: 'NON_RECONNUE',
      indemniteComplementaireMensuelle: null,
    }));
    expect(component.verdictLabel()).toContain('non reconnue');
    expect(component.verdictClass()).toBe('verdict-critical');
  });

  it('verdict AGE_INSUFFISANT → label, verdict-critical', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_AGE_INSUFFISANT',
      eligible: false,
      conditionAgeRemplie: false,
      ageFinContrat: 50,
      indemniteComplementaireMensuelle: null,
    }));
    expect(component.verdictLabel()).toContain('âge');
    expect(component.verdictClass()).toBe('verdict-critical');
  });

  it('verdict CARRIERE_INSUFFISANTE → label, verdict-critical', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_CARRIERE_INSUFFISANTE',
      eligible: false,
      conditionCarriereRemplie: false,
      anneesCarriereTotale: 5,
      indemniteComplementaireMensuelle: null,
    }));
    expect(component.verdictLabel()).toContain('10 ans');
    expect(component.verdictClass()).toBe('verdict-critical');
  });

  it('verdict ANCIENNETE_INSUFFISANTE → label, verdict-critical', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_ANCIENNETE_INSUFFISANTE',
      eligible: false,
      conditionAncienneteRemplie: false,
      anneesAncienneteSecteur: 2,
      indemniteComplementaireMensuelle: null,
    }));
    expect(component.verdictLabel()).toContain('ancienneté');
    expect(component.verdictClass()).toBe('verdict-critical');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('business');
    expect(component.verdictBadgeClass()).toBe('');
    expect(component.verdictBadgeLabel()).toBe('');
  });

  // ============================================================
  // Badge dans header (6 verdicts)
  // ============================================================

  it('ELIGIBLE → badge vert ÉLIGIBLE visible', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('ÉLIGIBLE');
  });

  it('DEMISSION → badge rouge INÉLIGIBLE visible', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INELIGIBLE_DEMISSION',
      eligible: false,
      conditionLicenciementRemplie: false,
      licenciementEffectif: false,
      indemniteComplementaireMensuelle: null,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('INÉLIGIBLE');
  });

  // ============================================================
  // Bloc conditions cumulatives (toujours visible)
  // ============================================================

  it('conditions block visible avec 5 conditions', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('.conditions-block');
    expect(block).not.toBeNull();
    const items = block.querySelectorAll('.conditions-list li');
    expect(items.length).toBe(5);
  });

  it('toutes conditions OK → 5 condition-ok', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const items = fixture.nativeElement.querySelectorAll('.conditions-list li.condition-ok');
    expect(items.length).toBe(5);
  });

  it('démission → 1 condition-ko (licenciement)', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INELIGIBLE_DEMISSION',
      eligible: false,
      conditionLicenciementRemplie: false,
      licenciementEffectif: false,
      indemniteComplementaireMensuelle: null,
    }));
    fixture.detectChanges();
    const ko = fixture.nativeElement.querySelectorAll('.conditions-list li.condition-ko');
    expect(ko.length).toBe(1);
  });

  it('reconnaissance absente → 1 condition-ko (reconnaissance)', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INELIGIBLE_RECONNAISSANCE_ABSENTE',
      eligible: false,
      conditionReconnaissanceRemplie: false,
      typeReconnaissance: 'NON_RECONNUE',
      indemniteComplementaireMensuelle: null,
    }));
    fixture.detectChanges();
    const ko = fixture.nativeElement.querySelectorAll('.conditions-list li.condition-ko');
    expect(ko.length).toBe(1);
  });

  it('conditionIcon / conditionClass helpers', () => {
    expect(component.conditionIcon(true)).toBe('check_circle');
    expect(component.conditionIcon(false)).toBe('cancel');
    expect(component.conditionClass(true)).toBe('condition-ok');
    expect(component.conditionClass(false)).toBe('condition-ko');
  });

  // ============================================================
  // typeReconnaissanceLabel helper
  // ============================================================

  it('typeReconnaissanceLabel : 3 cas + null', () => {
    expect(component.typeReconnaissanceLabel('EN_DIFFICULTE')).toContain('difficulté');
    expect(component.typeReconnaissanceLabel('EN_RESTRUCTURATION')).toContain('restructuration');
    expect(component.typeReconnaissanceLabel('NON_RECONNUE')).toContain('Aucune');
    expect(component.typeReconnaissanceLabel(null)).toBe('—');
  });

  // ============================================================
  // Indemnité complémentaire affichée
  // ============================================================

  it('ELIGIBLE + indemnité → bloc indemnité rendu avec montant', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({ indemniteComplementaireMensuelle: 700.00 }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('.indemnite-block');
    expect(block).not.toBeNull();
    const amount = block.querySelector('.indemnite-amount');
    expect(amount.textContent).toContain('700');
    expect(amount.textContent).toContain('€');
  });

  it('ELIGIBLE sans couple financier → pas de bloc indemnité', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      remunerationNetteMensuelleReference: null,
      allocationChomageMensuelleEstimee: null,
      indemniteComplementaireMensuelle: null,
      avertissement: null,
    }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('.indemnite-block');
    expect(block).toBeNull();
  });

  it('INELIGIBLE → pas de bloc indemnité', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INELIGIBLE_AGE_INSUFFISANT',
      eligible: false,
      conditionAgeRemplie: false,
      ageFinContrat: 50,
      indemniteComplementaireMensuelle: null,
    }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('.indemnite-block');
    expect(block).toBeNull();
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non null → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      avertissement: 'Indemnité indicative — à confirmer ONSS/ONEM',
    }));
    fixture.detectChanges();
    const alert = fixture.nativeElement.querySelector('.result-alert');
    expect(alert).not.toBeNull();
    expect(alert.textContent).toContain('indicative');
  });

  it('avertissement null → pas de bannière', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({ avertissement: null }));
    fixture.detectChanges();
    const alert = fixture.nativeElement.querySelector('.result-alert');
    expect(alert).toBeNull();
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(RccBeEntrepriseDifficulteSectionComponent.getPrefillCount({})).toBe(0);
    expect(RccBeEntrepriseDifficulteSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: { dateLicenciement: '2026-05-10', salaireBrutMensuel: 3500 },
    })).toBe(0);
  });

  // ============================================================
  // editMode → re-affiche le form
  // ============================================================

  it('editMode() → showForm true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ============================================================
  // F-JU-03 — citation jurisprudentielle bien câblée
  // ============================================================

  it('toolId pour jurisprudence = rcc-be-entreprise-difficulte', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('rcc-be-entreprise-difficulte');
  });

  // ============================================================
  // Toggle collapse
  // ============================================================

  it('toggleCollapse() flips collapsed signal', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  // ============================================================
  // forceExpanded
  // ============================================================

  it('forceExpanded → collapsed=false au ngOnInit', () => {
    component.forceExpanded = true;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    expect(component.collapsed()).toBe(false);
  });

  // ============================================================
  // TOOL_LABEL / TOOL_ICON (F-177)
  // ============================================================

  it('TOOL_LABEL / TOOL_ICON statiques', () => {
    expect(RccBeEntrepriseDifficulteSectionComponent.TOOL_LABEL)
      .toBe('RCC BE — ENTREPRISE EN DIFFICULTÉ');
    expect(RccBeEntrepriseDifficulteSectionComponent.TOOL_ICON).toBe('business');
  });

  // ============================================================
  // verdictOf accessor
  // ============================================================

  it('verdictOf retourne le verdict du response', () => {
    const r = response({ verdict: 'INELIGIBLE_DEMISSION' });
    expect(component.verdictOf(r)).toBe('INELIGIBLE_DEMISSION');
  });
});
