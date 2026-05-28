import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  EcoChequesChequesRepasBeSectionComponent,
} from './eco-cheques-cheques-repas-be-section.component';
import {
  EcoChequesChequesRepasBeResponse,
} from '../../core/models/eco-cheques-cheques-repas-be.model';

describe('EcoChequesChequesRepasBeSectionComponent', () => {
  let component: EcoChequesChequesRepasBeSectionComponent;
  let fixture: ComponentFixture<EcoChequesChequesRepasBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/eco-cheques-cheques-repas-be';

  /**
   * Construit une réponse backend par défaut — verdict
   * CONFORME_EXONERATION_INTEGRALE (chèques-repas, 220 jours prestés,
   * intervention employeur 6,50 EUR/chèque, contribution travailleur
   * 1,10 EUR, paiement électronique, CCT sectorielle, pas de cumul,
   * pas de substitution).
   */
  function response(
    overrides: Partial<EcoChequesChequesRepasBeResponse> = {},
  ): EcoChequesChequesRepasBeResponse {
    return {
      caseFileId: 'case-1',
      typeAvantage: 'CHEQUES_REPAS',
      montantAnnuelEur: 1430,
      valeurFacialeUnitaireEur: 7.60,
      contributionTravailleurUnitaireEur: 1.10,
      joursEffectivementPrestes: 220,
      cctSectorielleOuEntrepriseExiste: true,
      conventionIndividuelleEcrite: false,
      paiementElectronique: true,
      cumulFraisBouche: false,
      substitutionRemuneration: false,
      dateAttribution: '2026-01-15',
      plafondLegalApplicableEur: 1520.20,
      montantExonereEur: 1430,
      montantRequalifieEnRemunerationEur: 0,
      cotisationsOnssEstimeesEur: 0,
      verdict: 'CONFORME_EXONERATION_INTEGRALE',
      raison: 'CONFORME_INTEGRALE',
      synthese: 'Avantage extra-légal intégralement conforme — toutes conditions cumulatives remplies. Exonération ONSS et impôt.',
      baseJuridique: 'CCT n°98 du CNT du 20/02/2009 + Loi du 25/04/2014 + AR du 03/02/2010 art. 19bis AR 28/11/1969.',
      avertissement: 'Le taux de 25 % de cotisations ONSS employeur est indicatif (taux global moyen) ; à confirmer secteur par secteur.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        EcoChequesChequesRepasBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(EcoChequesChequesRepasBeSectionComponent);
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
    expect(banner.textContent).toContain('titres-restaurant');
  });

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 → hydrate result + mode résultat + rehydrate form pour édition', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('CONFORME_EXONERATION_INTEGRALE');
    expect(component.showForm()).toBe(false);
    expect(component.typeAvantage()).toBe('CHEQUES_REPAS');
    expect(component.montantAnnuelEur()).toBe(1430);
    expect(component.valeurFacialeUnitaireEur()).toBe(7.60);
    expect(component.contributionTravailleurUnitaireEur()).toBe(1.10);
    expect(component.joursEffectivementPrestes()).toBe(220);
    expect(component.cctSectorielleOuEntrepriseExiste()).toBe(true);
    expect(component.conventionIndividuelleEcrite()).toBe(false);
    expect(component.paiementElectronique()).toBe(true);
    expect(component.cumulFraisBouche()).toBe(false);
    expect(component.substitutionRemuneration()).toBe(false);
    expect(component.dateAttribution()).toBe('2026-01-15');
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

  function fillForm() {
    component.typeAvantage.set('CHEQUES_REPAS');
    component.montantAnnuelEur.set(1430);
    component.valeurFacialeUnitaireEur.set(7.60);
    component.contributionTravailleurUnitaireEur.set(1.10);
    component.joursEffectivementPrestes.set(220);
    component.cctSectorielleOuEntrepriseExiste.set(true);
    component.conventionIndividuelleEcrite.set(false);
    component.paiementElectronique.set(true);
    component.cumulFraisBouche.set(false);
    component.substitutionRemuneration.set(false);
  }

  it('formValid() : true avec les champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans typeAvantage', () => {
    fillForm();
    component.typeAvantage.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec montant négatif', () => {
    fillForm();
    component.montantAnnuelEur.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec montant > 100k', () => {
    fillForm();
    component.montantAnnuelEur.set(100001);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec valeur faciale négative', () => {
    fillForm();
    component.valeurFacialeUnitaireEur.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec valeur faciale > 1000', () => {
    fillForm();
    component.valeurFacialeUnitaireEur.set(1001);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec contribution négative', () => {
    fillForm();
    component.contributionTravailleurUnitaireEur.set(-0.5);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec contribution > 1000', () => {
    fillForm();
    component.contributionTravailleurUnitaireEur.set(1001);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec jours prestés négatifs', () => {
    fillForm();
    component.joursEffectivementPrestes.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans cctSectorielleOuEntrepriseExiste', () => {
    fillForm();
    component.cctSectorielleOuEntrepriseExiste.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans conventionIndividuelleEcrite', () => {
    fillForm();
    component.conventionIndividuelleEcrite.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans paiementElectronique', () => {
    fillForm();
    component.paiementElectronique.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans cumulFraisBouche', () => {
    fillForm();
    component.cumulFraisBouche.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans substitutionRemuneration', () => {
    fillForm();
    component.substitutionRemuneration.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true sans dateAttribution (facultatif)', () => {
    fillForm();
    component.dateAttribution.set(null);
    expect(component.formValid()).toBe(true);
  });

  it('isChequesRepas() true uniquement si CHEQUES_REPAS', () => {
    expect(component.isChequesRepas()).toBe(false);
    component.typeAvantage.set('CHEQUES_REPAS');
    expect(component.isChequesRepas()).toBe(true);
    component.typeAvantage.set('ECO_CHEQUES');
    expect(component.isChequesRepas()).toBe(false);
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  it('calculate() → POST + snack succès', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.typeAvantage).toBe('CHEQUES_REPAS');
    expect(post.request.body.montantAnnuelEur).toBe(1430);
    expect(post.request.body.valeurFacialeUnitaireEur).toBe(7.60);
    expect(post.request.body.joursEffectivementPrestes).toBe(220);
    expect(post.request.body.paiementElectronique).toBe(true);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Conformité éco-chèques / chèques-repas calculée', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'valeurFacialeUnitaireEur plafonnée à 1000' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'valeurFacialeUnitaireEur plafonnée à 1000', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : erreur 500 → snack générique', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
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
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({}, { status: 404, statusText: 'Not Found' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Dossier introuvable', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : FRANCE → snack indispo + pas de POST', () => {
    component.workspaceCountry = 'FRANCE';
    fillForm();
    component.calculate();
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Outil indisponible pour ce workspace', 'Fermer', expect.anything(),
    );
    httpMock.expectNone((r) => r.url === BASE_URL && r.method === 'POST');
  });

  // ============================================================
  // Verdict — label / classe / icône (6 cas)
  // ============================================================

  it('verdict CONFORME_EXONERATION_INTEGRALE → label, verdict-ok, verified', () => {
    component.result.set(response());
    expect(component.verdictLabel()).toContain('intégralement conforme');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict CONFORME_PARTIELLEMENT_EXONERE → label, verdict-warn, rule', () => {
    component.result.set(response({
      verdict: 'CONFORME_PARTIELLEMENT_EXONERE',
    }));
    expect(component.verdictLabel()).toContain('partiellement');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('rule');
  });

  it('verdict NON_CONFORME_DEPASSEMENT_PLAFOND → label, verdict-warn, price_change', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_DEPASSEMENT_PLAFOND',
    }));
    expect(component.verdictLabel()).toContain('plafond');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('price_change');
  });

  it('verdict NON_CONFORME_SUBSTITUTION_REMUNERATION → label, verdict-critical, swap_horiz', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_SUBSTITUTION_REMUNERATION',
      substitutionRemuneration: true,
    }));
    expect(component.verdictLabel()).toContain('Substitution');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('swap_horiz');
  });

  it('verdict NON_CONFORME_CONDITION_MANQUANTE → label, verdict-critical, report_problem', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_CONDITION_MANQUANTE',
      paiementElectronique: false,
    }));
    expect(component.verdictLabel()).toContain('Condition');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('report_problem');
  });

  it('verdict A_ANALYSER → label, verdict-neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_ANALYSER',
      typeAvantage: 'INDETERMINE',
    }));
    expect(component.verdictLabel()).toContain('indéterminé');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('redeem');
  });

  // ============================================================
  // Badge dans header (6 cas)
  // ============================================================

  it('badge CONFORME_EXONERATION_INTEGRALE = vert', () => {
    expect(component.verdictBadge('CONFORME_EXONERATION_INTEGRALE')).toBe('CONFORME');
    expect(component.verdictBadgeClass('CONFORME_EXONERATION_INTEGRALE')).toBe('badge-ok');
  });

  it('badge CONFORME_PARTIELLEMENT_EXONERE = ambre', () => {
    expect(component.verdictBadge('CONFORME_PARTIELLEMENT_EXONERE')).toBe('PARTIEL');
    expect(component.verdictBadgeClass('CONFORME_PARTIELLEMENT_EXONERE')).toBe('badge-warn');
  });

  it('badge NON_CONFORME_DEPASSEMENT_PLAFOND = ambre', () => {
    expect(component.verdictBadge('NON_CONFORME_DEPASSEMENT_PLAFOND')).toBe('PLAFOND');
    expect(component.verdictBadgeClass('NON_CONFORME_DEPASSEMENT_PLAFOND')).toBe('badge-warn');
  });

  it('badge NON_CONFORME_SUBSTITUTION_REMUNERATION = rouge', () => {
    expect(component.verdictBadge('NON_CONFORME_SUBSTITUTION_REMUNERATION')).toBe('SUBSTITUTION');
    expect(component.verdictBadgeClass('NON_CONFORME_SUBSTITUTION_REMUNERATION')).toBe('badge-critical');
  });

  it('badge NON_CONFORME_CONDITION_MANQUANTE = rouge', () => {
    expect(component.verdictBadge('NON_CONFORME_CONDITION_MANQUANTE')).toBe('CONDITION');
    expect(component.verdictBadgeClass('NON_CONFORME_CONDITION_MANQUANTE')).toBe('badge-critical');
  });

  it('badge A_ANALYSER = neutre', () => {
    expect(component.verdictBadge('A_ANALYSER')).toBe('À ANALYSER');
    expect(component.verdictBadgeClass('A_ANALYSER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — verdict CONFORME rendu (badge + montants)
  // ============================================================

  it('CONFORME → badge vert dans le DOM + 5 lignes montants affichées', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('CONFORME');
    const montants = fixture.nativeElement.querySelectorAll('.montant-row');
    // 4 montants détaillés + 1 cotisations ONSS = 5 lignes.
    expect(montants.length).toBe(5);
  });

  it('SUBSTITUTION → badge rouge + bannière critique', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'NON_CONFORME_SUBSTITUTION_REMUNERATION',
      substitutionRemuneration: true,
      montantExonereEur: 0,
      montantRequalifieEnRemunerationEur: 1430,
      cotisationsOnssEstimeesEur: 357.50,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('SUBSTITUTION');
    const banner = fixture.nativeElement.querySelector('.result-banner.verdict-critical');
    expect(banner).not.toBeNull();
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellé typeAvantage : 3 cas + null', () => {
    expect(component.typeAvantageLabel('ECO_CHEQUES')).toContain('Éco-chèques');
    expect(component.typeAvantageLabel('CHEQUES_REPAS')).toContain('Chèques-repas');
    expect(component.typeAvantageLabel('INDETERMINE')).toContain('indéterminé');
    expect(component.typeAvantageLabel(null)).toBe('—');
  });

  // ============================================================
  // Formats
  // ============================================================

  it('formatMontant arrondit à 2 décimales avec € + locale FR', () => {
    expect(component.formatMontant(1430)).toContain('1');
    expect(component.formatMontant(1430)).toContain('430,00');
    expect(component.formatMontant(1430)).toContain('€');
    expect(component.formatMontant(null)).toBe('—');
    expect(component.formatMontant(undefined)).toBe('—');
  });

  it('formatBoolean — Oui / Non / —', () => {
    expect(component.formatBoolean(true)).toBe('Oui');
    expect(component.formatBoolean(false)).toBe('Non');
    expect(component.formatBoolean(null)).toBe('—');
    expect(component.formatBoolean(undefined)).toBe('—');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non vide → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      avertissement: 'Le taux ONSS 25 % est indicatif — à confirmer secteur par secteur.',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('indicatif');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Avantage extra-légal intégralement conforme — toutes conditions cumulatives remplies.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('intégralement conforme');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(EcoChequesChequesRepasBeSectionComponent.getPrefillCount({})).toBe(0);
    expect(EcoChequesChequesRepasBeSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateNaissanceSalarie: '1980-04-15',
        dateRuptureContrat: '2026-04-10',
        motifRupture: 'autre',
        anneesCarriereSalarie: 12,
        salaireBrutMensuel: 2800,
        salaireBrutAnnuel: 33600,
      },
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

  it('toolId pour jurisprudence = eco-cheques-cheques-repas-be', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('eco-cheques-cheques-repas-be');
  });

  // ============================================================
  // Toggle collapse + forceExpanded
  // ============================================================

  it('toggleCollapse() flips collapsed signal', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('forceExpanded → collapsed=false au ngOnInit', () => {
    component.forceExpanded = true;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    expect(component.collapsed()).toBe(false);
  });

  // ============================================================
  // Handlers signals
  // ============================================================

  it('handlers mettent à jour les signals correctement', () => {
    component.onTypeAvantageChange('ECO_CHEQUES');
    expect(component.typeAvantage()).toBe('ECO_CHEQUES');

    component.onMontantAnnuelEurChange('250');
    expect(component.montantAnnuelEur()).toBe(250);

    component.onValeurFacialeUnitaireEurChange(8);
    expect(component.valeurFacialeUnitaireEur()).toBe(8);

    component.onContributionTravailleurUnitaireEurChange('1.09');
    expect(component.contributionTravailleurUnitaireEur()).toBe(1.09);

    component.onJoursEffectivementPrestesChange('200');
    expect(component.joursEffectivementPrestes()).toBe(200);

    component.onCctSectorielleOuEntrepriseExisteChange(true);
    expect(component.cctSectorielleOuEntrepriseExiste()).toBe(true);

    component.onConventionIndividuelleEcriteChange(false);
    expect(component.conventionIndividuelleEcrite()).toBe(false);

    component.onPaiementElectroniqueChange(true);
    expect(component.paiementElectronique()).toBe(true);

    component.onCumulFraisBoucheChange(false);
    expect(component.cumulFraisBouche()).toBe(false);

    component.onSubstitutionRemunerationChange(false);
    expect(component.substitutionRemuneration()).toBe(false);

    component.onDateAttributionChange('2026-01-15');
    expect(component.dateAttribution()).toBe('2026-01-15');

    component.onJoursEffectivementPrestesChange(null);
    expect(component.joursEffectivementPrestes()).toBeNull();

    component.onMontantAnnuelEurChange('');
    expect(component.montantAnnuelEur()).toBeNull();
  });
});
