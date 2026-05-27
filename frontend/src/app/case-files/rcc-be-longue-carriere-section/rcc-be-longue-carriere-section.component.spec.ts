import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  RccBeLongueCarriereSectionComponent,
} from './rcc-be-longue-carriere-section.component';
import {
  RccBeLongueCarriereResponse,
} from '../../core/models/rcc-be-longue-carriere.model';

describe('RccBeLongueCarriereSectionComponent', () => {
  let component: RccBeLongueCarriereSectionComponent;
  let fixture: ComponentFixture<RccBeLongueCarriereSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/rcc-be-longue-carriere';

  /**
   * Construit une réponse backend par défaut (éligible 60+/41 + indemnité).
   */
  function response(
    overrides: Partial<RccBeLongueCarriereResponse> = {},
  ): RccBeLongueCarriereResponse {
    return {
      caseFileId: 'case-1',
      ageFinContrat: 60,
      anneesCarriereTotale: 41,
      dateFinContrat: '2026-09-30',
      licenciementEffectif: true,
      remunerationNetteMensuelleReference: 2800,
      allocationChomageMensuelleEstimee: 1400,
      verdict: 'ELIGIBLE_RCC_LONGUE_CARRIERE',
      eligible: true,
      conditionAgeRemplie: true,
      conditionCarriereRemplie: true,
      conditionLicenciementRemplie: true,
      indemniteComplementaireMensuelle: 700.00,
      synthese: 'Conditions cumulatives remplies (âge ≥ 59 ans, '
        + 'carrière ≥ 40 ans, licenciement effectif) — RCC longue '
        + 'carrière ouvert (AR 03/05/2007 art. 3).',
      baseJuridique: 'Loi du 26/12/2013 ; CCT n° 17 du 19/12/1974 ; '
        + 'AR du 03/05/2007 art. 3 (régime longue carrière 59+/40)',
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
        RccBeLongueCarriereSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RccBeLongueCarriereSectionComponent);
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
    expect(component.result()!.verdict).toBe('ELIGIBLE_RCC_LONGUE_CARRIERE');
    expect(component.showForm()).toBe(false);
    // Rehydratation form.
    expect(component.ageFinContrat()).toBe(60);
    expect(component.anneesCarriereTotale()).toBe(41);
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

  it('formValid() : false sans champs', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true avec champs requis (sans couple financier)', () => {
    component.ageFinContrat.set(60);
    component.anneesCarriereTotale.set(41);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : true avec couple financier complet', () => {
    component.ageFinContrat.set(60);
    component.anneesCarriereTotale.set(41);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    component.remunerationNetteMensuelleReference.set(2800);
    component.allocationChomageMensuelleEstimee.set(1400);
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false si âge négatif', () => {
    component.ageFinContrat.set(-1);
    component.anneesCarriereTotale.set(41);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false si carrière négative', () => {
    component.ageFinContrat.set(60);
    component.anneesCarriereTotale.set(-5);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dateFinContrat', () => {
    component.ageFinContrat.set(60);
    component.anneesCarriereTotale.set(41);
    component.licenciementEffectif.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false si couple financier dépareillé (réf seule)', () => {
    component.ageFinContrat.set(60);
    component.anneesCarriereTotale.set(41);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    component.remunerationNetteMensuelleReference.set(2800);
    component.allocationChomageMensuelleEstimee.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false si couple financier dépareillé (alloc seule)', () => {
    component.ageFinContrat.set(60);
    component.anneesCarriereTotale.set(41);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    component.remunerationNetteMensuelleReference.set(null);
    component.allocationChomageMensuelleEstimee.set(1400);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false si rémunération nette négative', () => {
    component.ageFinContrat.set(60);
    component.anneesCarriereTotale.set(41);
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
    component.ageFinContrat.set(60);
    component.anneesCarriereTotale.set(41);
    component.dateFinContrat.set('2026-09-30');
    component.licenciementEffectif.set(true);
    component.remunerationNetteMensuelleReference.set(2800);
    component.allocationChomageMensuelleEstimee.set(1400);
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.ageFinContrat).toBe(60);
    expect(post.request.body.anneesCarriereTotale).toBe(41);
    expect(post.request.body.dateFinContrat).toBe('2026-09-30');
    expect(post.request.body.licenciementEffectif).toBe(true);
    expect(post.request.body.remunerationNetteMensuelleReference).toBe(2800);
    expect(post.request.body.allocationChomageMensuelleEstimee).toBe(1400);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'RCC longue carrière analysé', 'OK', expect.anything(),
    );
  });

  it('calculate() : POST sans couple financier → body null/null', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.ageFinContrat.set(60);
    component.anneesCarriereTotale.set(41);
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
    component.ageFinContrat.set(60);
    component.anneesCarriereTotale.set(41);
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
    component.ageFinContrat.set(60);
    component.anneesCarriereTotale.set(41);
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
    component.ageFinContrat.set(60);
    component.anneesCarriereTotale.set(41);
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
  // Verdict — label / classe / icône / badge (4 états)
  // ============================================================

  it('verdict ELIGIBLE → label, verdict-ok, verified, badge ÉLIGIBLE', () => {
    component.result.set(response({ verdict: 'ELIGIBLE_RCC_LONGUE_CARRIERE' }));
    expect(component.verdictLabel()).toContain('Éligible');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
    expect(component.verdictBadgeClass()).toBe('badge-ok');
    expect(component.verdictBadgeLabel()).toBe('ÉLIGIBLE');
  });

  it('verdict DEMISSION → label, verdict-critical, gpp_bad, badge INÉLIGIBLE', () => {
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

  it('verdict AGE_INSUFFISANT → label, verdict-critical', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_AGE_INSUFFISANT',
      eligible: false,
      conditionAgeRemplie: false,
      ageFinContrat: 55,
      indemniteComplementaireMensuelle: null,
    }));
    expect(component.verdictLabel()).toContain('59 ans');
    expect(component.verdictClass()).toBe('verdict-critical');
  });

  it('verdict CARRIERE_INSUFFISANTE → label, verdict-critical', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_CARRIERE_INSUFFISANTE',
      eligible: false,
      conditionCarriereRemplie: false,
      anneesCarriereTotale: 30,
      indemniteComplementaireMensuelle: null,
    }));
    expect(component.verdictLabel()).toContain('40 ans');
    expect(component.verdictClass()).toBe('verdict-critical');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('elderly');
    expect(component.verdictBadgeClass()).toBe('');
    expect(component.verdictBadgeLabel()).toBe('');
  });

  // ============================================================
  // Badge dans header (4 verdicts)
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

  it('conditions block visible avec 3 conditions', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('.conditions-block');
    expect(block).not.toBeNull();
    const items = block.querySelectorAll('.conditions-list li');
    expect(items.length).toBe(3);
  });

  it('toutes conditions OK → 3 condition-ok', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const items = fixture.nativeElement.querySelectorAll('.conditions-list li.condition-ok');
    expect(items.length).toBe(3);
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

  it('conditionIcon / conditionClass helpers', () => {
    expect(component.conditionIcon(true)).toBe('check_circle');
    expect(component.conditionIcon(false)).toBe('cancel');
    expect(component.conditionClass(true)).toBe('condition-ok');
    expect(component.conditionClass(false)).toBe('condition-ko');
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
      ageFinContrat: 55,
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
    expect(RccBeLongueCarriereSectionComponent.getPrefillCount({})).toBe(0);
    expect(RccBeLongueCarriereSectionComponent.getPrefillCount({
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

  it('toolId pour jurisprudence = rcc-be-longue-carriere', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('rcc-be-longue-carriere');
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
    expect(RccBeLongueCarriereSectionComponent.TOOL_LABEL)
      .toBe('RCC BE — LONGUE CARRIÈRE');
    expect(RccBeLongueCarriereSectionComponent.TOOL_ICON).toBe('elderly');
  });

  // ============================================================
  // verdictOf accessor
  // ============================================================

  it('verdictOf retourne le verdict du response', () => {
    const r = response({ verdict: 'INELIGIBLE_DEMISSION' });
    expect(component.verdictOf(r)).toBe('INELIGIBLE_DEMISSION');
  });
});
