import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  EgaliteFemmesHommesBeSectionComponent,
} from './egalite-femmes-hommes-be-section.component';
import {
  EgaliteFemmesHommesBeResponse,
} from '../../core/models/egalite-femmes-hommes-be.model';

describe('EgaliteFemmesHommesBeSectionComponent', () => {
  let component: EgaliteFemmesHommesBeSectionComponent;
  let fixture: ComponentFixture<EgaliteFemmesHommesBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/egalite-femmes-hommes-be';

  /**
   * Construit une réponse backend par défaut — verdict
   * CONFORME_RAPPORT_PLAN_COMPLETS (entreprise 120 ETP, rapport
   * déposé complet, ventilation 5/5, aucun écart).
   */
  function response(
    overrides: Partial<EgaliteFemmesHommesBeResponse> = {},
  ): EgaliteFemmesHommesBeResponse {
    return {
      caseFileId: 'case-1',
      effectifEntreprise: 120,
      statutRapport: 'DEPOSE_FORMULAIRE_COMPLET',
      dateDepotRapport: '2026-03-15',
      ventilationNiveauFonctionFournie: true,
      ventilationAncienneteFournie: true,
      ventilationQualificationFournie: true,
      ventilationRegimeTravailFournie: true,
      ventilationComposantsRemunerationFournie: true,
      ecartSalarialNonJustifieConstate: false,
      pourcentageEcartConstate: null,
      planActionEtabli: false,
      mediateurDesigne: true,
      plainteIefhOuInspectionEnCours: false,
      verdict: 'CONFORME_RAPPORT_PLAN_COMPLETS',
      seuilAtteint: true,
      rapportDepose: true,
      ventilationComplete: true,
      planActionRequis: false,
      planActionConforme: true,
      typeFormulaireRequis: 'ANNEXE_I_DETAILLE',
      raison: 'CONFORME_RAPPORT_PLAN_COMPLETS',
      synthese: 'L\'entreprise occupe en moyenne 120 travailleurs et a déposé un rapport d\'analyse complet sur le formulaire ANNEXE_I_DETAILLE.',
      baseJuridique: 'Loi du 22/04/2012 + AR du 17/08/2013 + AR du 25/04/2014 + CCT n° 25 + C. pén. social art. 195/1.',
      avertissement: 'L\'avocat doit confirmer le décompte exact de l\'effectif moyen ETP au sens de la Loi 04/12/2007 art. 7.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        EgaliteFemmesHommesBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(EgaliteFemmesHommesBeSectionComponent);
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
    expect(banner.textContent).toContain('Index');
  });

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 → hydrate result + mode résultat + rehydrate form pour édition', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('CONFORME_RAPPORT_PLAN_COMPLETS');
    expect(component.showForm()).toBe(false);
    expect(component.effectifEntreprise()).toBe(120);
    expect(component.statutRapport()).toBe('DEPOSE_FORMULAIRE_COMPLET');
    expect(component.dateDepotRapport()).toBe('2026-03-15');
    expect(component.ventilationNiveauFonctionFournie()).toBe(true);
    expect(component.ventilationAncienneteFournie()).toBe(true);
    expect(component.ventilationQualificationFournie()).toBe(true);
    expect(component.ventilationRegimeTravailFournie()).toBe(true);
    expect(component.ventilationComposantsRemunerationFournie()).toBe(true);
    expect(component.ecartSalarialNonJustifieConstate()).toBe(false);
    expect(component.planActionEtabli()).toBe(false);
    expect(component.mediateurDesigne()).toBe(true);
    expect(component.plainteIefhOuInspectionEnCours()).toBe(false);
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
    component.effectifEntreprise.set(120);
    component.statutRapport.set('DEPOSE_FORMULAIRE_COMPLET');
    component.ventilationNiveauFonctionFournie.set(true);
    component.ventilationAncienneteFournie.set(true);
    component.ventilationQualificationFournie.set(true);
    component.ventilationRegimeTravailFournie.set(true);
    component.ventilationComposantsRemunerationFournie.set(true);
    component.ecartSalarialNonJustifieConstate.set(false);
    component.planActionEtabli.set(false);
    component.mediateurDesigne.set(true);
    component.plainteIefhOuInspectionEnCours.set(false);
  }

  it('formValid() : true avec les champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans effectifEntreprise', () => {
    fillForm();
    component.effectifEntreprise.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec effectif négatif', () => {
    fillForm();
    component.effectifEntreprise.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec effectif > 1M', () => {
    fillForm();
    component.effectifEntreprise.set(1000001);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans statutRapport', () => {
    fillForm();
    component.statutRapport.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans ventilation niveau fonction', () => {
    fillForm();
    component.ventilationNiveauFonctionFournie.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans ventilation ancienneté', () => {
    fillForm();
    component.ventilationAncienneteFournie.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans ventilation qualification', () => {
    fillForm();
    component.ventilationQualificationFournie.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans ventilation régime', () => {
    fillForm();
    component.ventilationRegimeTravailFournie.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans ventilation composants', () => {
    fillForm();
    component.ventilationComposantsRemunerationFournie.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans ecartSalarialNonJustifieConstate', () => {
    fillForm();
    component.ecartSalarialNonJustifieConstate.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans planActionEtabli', () => {
    fillForm();
    component.planActionEtabli.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans mediateurDesigne', () => {
    fillForm();
    component.mediateurDesigne.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans plainteIefhOuInspectionEnCours', () => {
    fillForm();
    component.plainteIefhOuInspectionEnCours.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true avec pourcentageEcartConstate valide', () => {
    fillForm();
    component.pourcentageEcartConstate.set(8.5);
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false avec pourcentage négatif', () => {
    fillForm();
    component.pourcentageEcartConstate.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec pourcentage > 100', () => {
    fillForm();
    component.pourcentageEcartConstate.set(101);
    expect(component.formValid()).toBe(false);
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
    expect(post.request.body.effectifEntreprise).toBe(120);
    expect(post.request.body.statutRapport).toBe('DEPOSE_FORMULAIRE_COMPLET');
    expect(post.request.body.ventilationNiveauFonctionFournie).toBe(true);
    expect(post.request.body.ecartSalarialNonJustifieConstate).toBe(false);
    expect(post.request.body.mediateurDesigne).toBe(true);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse égalité H/F calculée', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'effectifEntreprise plafonné à 1 000 000' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'effectifEntreprise plafonné à 1 000 000', 'Fermer', expect.anything(),
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

  it('verdict CONFORME_RAPPORT_PLAN_COMPLETS → label, verdict-ok, verified', () => {
    component.result.set(response());
    expect(component.verdictLabel()).toContain('Conformité');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict HORS_CHAMP_SEUIL_NON_ATTEINT → label, verdict-neutral, do_not_disturb_on', () => {
    component.result.set(response({
      verdict: 'HORS_CHAMP_SEUIL_NON_ATTEINT',
      effectifEntreprise: 30,
      seuilAtteint: false,
      typeFormulaireRequis: 'NON_APPLICABLE',
    }));
    expect(component.verdictLabel()).toContain('Hors champ');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('do_not_disturb_on');
  });

  it('verdict MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE → label, verdict-critical, report', () => {
    component.result.set(response({
      verdict: 'MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE',
      statutRapport: 'NON_DEPOSE_DELAI_DEPASSE',
      rapportDepose: false,
      ventilationComplete: false,
    }));
    expect(component.verdictLabel()).toContain('Manquement');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('report');
  });

  it('verdict NON_CONFORME_RAPPORT_INCOMPLET → label, verdict-critical, rule_folder', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_RAPPORT_INCOMPLET',
      statutRapport: 'DEPOSE_FORMULAIRE_INCOMPLET',
      ventilationComplete: false,
      ventilationAncienneteFournie: false,
    }));
    expect(component.verdictLabel()).toContain('Non-conformité matérielle');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('rule_folder');
  });

  it('verdict NON_CONFORME_PLAN_ACTION_MANQUANT → label, verdict-critical, pending_actions', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_PLAN_ACTION_MANQUANT',
      ecartSalarialNonJustifieConstate: true,
      pourcentageEcartConstate: 8.5,
      planActionRequis: true,
      planActionConforme: false,
    }));
    expect(component.verdictLabel()).toContain('plan d\'action');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('pending_actions');
  });

  it('verdict A_ANALYSER → label, verdict-neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_ANALYSER',
      statutRapport: 'INDETERMINE',
    }));
    expect(component.verdictLabel()).toContain('Configuration');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('balance');
  });

  // ============================================================
  // Badge dans header (6 cas)
  // ============================================================

  it('badge CONFORME_RAPPORT_PLAN_COMPLETS = vert', () => {
    expect(component.verdictBadge('CONFORME_RAPPORT_PLAN_COMPLETS')).toBe('CONFORME');
    expect(component.verdictBadgeClass('CONFORME_RAPPORT_PLAN_COMPLETS')).toBe('badge-ok');
  });

  it('badge HORS_CHAMP_SEUIL_NON_ATTEINT = neutre', () => {
    expect(component.verdictBadge('HORS_CHAMP_SEUIL_NON_ATTEINT')).toBe('HORS CHAMP');
    expect(component.verdictBadgeClass('HORS_CHAMP_SEUIL_NON_ATTEINT')).toBe('badge-neutral');
  });

  it('badge MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE = rouge', () => {
    expect(component.verdictBadge('MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE')).toBe('NON DÉPOSÉ');
    expect(component.verdictBadgeClass('MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE')).toBe('badge-critical');
  });

  it('badge NON_CONFORME_RAPPORT_INCOMPLET = rouge', () => {
    expect(component.verdictBadge('NON_CONFORME_RAPPORT_INCOMPLET')).toBe('INCOMPLET');
    expect(component.verdictBadgeClass('NON_CONFORME_RAPPORT_INCOMPLET')).toBe('badge-critical');
  });

  it('badge NON_CONFORME_PLAN_ACTION_MANQUANT = rouge', () => {
    expect(component.verdictBadge('NON_CONFORME_PLAN_ACTION_MANQUANT')).toBe('PLAN MANQUANT');
    expect(component.verdictBadgeClass('NON_CONFORME_PLAN_ACTION_MANQUANT')).toBe('badge-critical');
  });

  it('badge A_ANALYSER = neutre', () => {
    expect(component.verdictBadge('A_ANALYSER')).toBe('À ANALYSER');
    expect(component.verdictBadgeClass('A_ANALYSER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — verdict CONFORME rendu (badge + ventilations)
  // ============================================================

  it('CONFORME → badge vert dans le DOM + 5 lignes ventilations affichées', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('CONFORME');
    const ventilations = fixture.nativeElement.querySelectorAll('.ventilation-row');
    // 5 ventilations toujours visibles (pourcentageEcartConstate null donc 6ᵉ ligne masquée).
    expect(ventilations.length).toBe(5);
  });

  it('MANQUEMENT_GRAVE → badge rouge + bannière critique', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE',
      statutRapport: 'NON_DEPOSE_DELAI_DEPASSE',
      rapportDepose: false,
      ventilationComplete: false,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('NON DÉPOSÉ');
    const banner = fixture.nativeElement.querySelector('.result-banner.verdict-critical');
    expect(banner).not.toBeNull();
  });

  it('result avec pourcentageEcartConstate → 6 lignes ventilations affichées', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'NON_CONFORME_PLAN_ACTION_MANQUANT',
      ecartSalarialNonJustifieConstate: true,
      pourcentageEcartConstate: 7.2,
      planActionRequis: true,
      planActionConforme: false,
    }));
    fixture.detectChanges();
    const ventilations = fixture.nativeElement.querySelectorAll('.ventilation-row');
    expect(ventilations.length).toBe(6);
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellé statutRapport : 5 cas + null', () => {
    expect(component.statutRapportLabel('DEPOSE_FORMULAIRE_COMPLET')).toContain('complet');
    expect(component.statutRapportLabel('DEPOSE_FORMULAIRE_INCOMPLET')).toContain('incomplet');
    expect(component.statutRapportLabel('EN_PREPARATION')).toContain('cours');
    expect(component.statutRapportLabel('NON_DEPOSE_DELAI_DEPASSE')).toContain('Délai');
    expect(component.statutRapportLabel('INDETERMINE')).toContain('indéterminé');
    expect(component.statutRapportLabel(null)).toBe('—');
  });

  it('libellé formulaire : 3 cas + null', () => {
    expect(component.formulaireLabel('ANNEXE_I_DETAILLE')).toContain('Annexe I');
    expect(component.formulaireLabel('ANNEXE_II_SIMPLIFIE')).toContain('Annexe II');
    expect(component.formulaireLabel('NON_APPLICABLE')).toContain('Non applicable');
    expect(component.formulaireLabel(null)).toBe('—');
    expect(component.formulaireLabel(undefined)).toBe('—');
  });

  // ============================================================
  // Formats
  // ============================================================

  it('formatPourcentage : 2 décimales + %', () => {
    expect(component.formatPourcentage(8.5)).toContain('8,50');
    expect(component.formatPourcentage(8.5)).toContain('%');
    expect(component.formatPourcentage(null)).toBe('—');
    expect(component.formatPourcentage(undefined)).toBe('—');
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
      avertissement: 'Effectif moyen ETP à valider sur la période biennale (Loi 04/12/2007 art. 7).',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('ETP');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Rapport biennal conforme déposé.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('Rapport');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(EgaliteFemmesHommesBeSectionComponent.getPrefillCount({})).toBe(0);
    expect(EgaliteFemmesHommesBeSectionComponent.getPrefillCount({
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

  it('toolId pour jurisprudence = egalite-femmes-hommes-be', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('egalite-femmes-hommes-be');
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
    component.onEffectifEntrepriseChange('120');
    expect(component.effectifEntreprise()).toBe(120);

    component.onStatutRapportChange('DEPOSE_FORMULAIRE_COMPLET');
    expect(component.statutRapport()).toBe('DEPOSE_FORMULAIRE_COMPLET');

    component.onDateDepotRapportChange('2026-03-15');
    expect(component.dateDepotRapport()).toBe('2026-03-15');

    component.onVentilationNiveauFonctionFournieChange(true);
    expect(component.ventilationNiveauFonctionFournie()).toBe(true);

    component.onVentilationAncienneteFournieChange(true);
    expect(component.ventilationAncienneteFournie()).toBe(true);

    component.onVentilationQualificationFournieChange(false);
    expect(component.ventilationQualificationFournie()).toBe(false);

    component.onVentilationRegimeTravailFournieChange(true);
    expect(component.ventilationRegimeTravailFournie()).toBe(true);

    component.onVentilationComposantsRemunerationFournieChange(false);
    expect(component.ventilationComposantsRemunerationFournie()).toBe(false);

    component.onEcartSalarialNonJustifieConstateChange(true);
    expect(component.ecartSalarialNonJustifieConstate()).toBe(true);

    component.onPourcentageEcartConstateChange('8.5');
    expect(component.pourcentageEcartConstate()).toBe(8.5);

    component.onPlanActionEtabliChange(false);
    expect(component.planActionEtabli()).toBe(false);

    component.onMediateurDesigneChange(true);
    expect(component.mediateurDesigne()).toBe(true);

    component.onPlainteIefhOuInspectionEnCoursChange(false);
    expect(component.plainteIefhOuInspectionEnCours()).toBe(false);

    component.onEffectifEntrepriseChange('');
    expect(component.effectifEntreprise()).toBeNull();

    component.onPourcentageEcartConstateChange(null);
    expect(component.pourcentageEcartConstate()).toBeNull();
  });
});
