import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  PeculeVacancesBeSectionComponent,
} from './pecule-vacances-be-section.component';
import {
  PeculeVacancesBeResponse,
} from '../../core/models/pecule-vacances-be.model';

describe('PeculeVacancesBeSectionComponent', () => {
  let component: PeculeVacancesBeSectionComponent;
  let fixture: ComponentFixture<PeculeVacancesBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/pecule-vacances-be';

  /**
   * Construit une réponse backend par défaut — verdict
   * DU_PECULE_SIMPLE_CALCULE (employé, 12 jours pris, base 30 000 €
   * annuel / 2 500 € mensuel).
   */
  function response(
    overrides: Partial<PeculeVacancesBeResponse> = {},
  ): PeculeVacancesBeResponse {
    return {
      caseFileId: 'case-1',
      statut: 'EMPLOYE',
      typeCalcul: 'PECULE_SIMPLE',
      remunerationBruteAnnuelleExerciceEur: 30000,
      remunerationMensuelleBruteEur: 2500,
      joursCongesPris: 12,
      peculeDejaPaye: false,
      remunerationBruteAnnuelleExercicePrecedentEur: 28000,
      dateSortie: null,
      dateFinContrat: null,
      dateReclamation: '2026-04-15',
      montantPeculeSimpleEur: 1250,
      montantDoublePeculeEur: 2125,
      montantPeculeDepartEur: 8895.20,
      montantTotalDuEur: 1250,
      fractionLegaleAppliquee: 0.5,
      prescrit: false,
      joursDepuisFaitGenerateur: 0,
      verdict: 'DU_PECULE_SIMPLE_CALCULE',
      raison: 'PECULE_SIMPLE_CALCULE',
      synthese: 'Pécule simple employé (art. 38 AR 30/03/1967) — rémunération maintenue pendant les 12 jours pris.',
      baseJuridique: 'Lois coordonnées du 28/06/1971 + AR du 30/03/1967, art. 19, art. 38, art. 46 + Loi du 03/07/1978 art. 15.',
      avertissement: 'Les Lois coordonnées 28/06/1971 et l\'AR 30/03/1967 sont d\'application complexe ; l\'avocat doit confirmer le statut effectif (employé / ouvrier), la base de calcul, l\'application du coefficient 108 % ouvrier, le régime 6 j vs 5 j, et la prescription applicable.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        PeculeVacancesBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(PeculeVacancesBeSectionComponent);
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
    expect(banner.textContent).toContain('ICCP');
  });

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 → hydrate result + mode résultat + rehydrate form pour édition', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('DU_PECULE_SIMPLE_CALCULE');
    expect(component.showForm()).toBe(false);
    expect(component.statut()).toBe('EMPLOYE');
    expect(component.typeCalcul()).toBe('PECULE_SIMPLE');
    expect(component.remunerationBruteAnnuelleExerciceEur()).toBe(30000);
    expect(component.remunerationMensuelleBruteEur()).toBe(2500);
    expect(component.joursCongesPris()).toBe(12);
    expect(component.peculeDejaPaye()).toBe(false);
    expect(component.remunerationBruteAnnuelleExercicePrecedentEur()).toBe(28000);
    expect(component.dateReclamation()).toBe('2026-04-15');
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
    component.statut.set('EMPLOYE');
    component.typeCalcul.set('PECULE_SIMPLE');
    component.remunerationBruteAnnuelleExerciceEur.set(30000);
    component.remunerationMensuelleBruteEur.set(2500);
    component.joursCongesPris.set(12);
    component.peculeDejaPaye.set(false);
    component.remunerationBruteAnnuelleExercicePrecedentEur.set(28000);
    component.dateReclamation.set('2026-04-15');
  }

  it('formValid() : true avec les champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans statut', () => {
    fillForm();
    component.statut.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans typeCalcul', () => {
    fillForm();
    component.typeCalcul.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec rémunération exercice négative', () => {
    fillForm();
    component.remunerationBruteAnnuelleExerciceEur.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec rémunération exercice > 10M', () => {
    fillForm();
    component.remunerationBruteAnnuelleExerciceEur.set(10000001);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec rémunération mensuelle négative', () => {
    fillForm();
    component.remunerationMensuelleBruteEur.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec rémunération mensuelle > 1M', () => {
    fillForm();
    component.remunerationMensuelleBruteEur.set(1000001);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec joursCongesPris négatif', () => {
    fillForm();
    component.joursCongesPris.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec joursCongesPris > 30', () => {
    fillForm();
    component.joursCongesPris.set(31);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans peculeDejaPaye précisé', () => {
    fillForm();
    component.peculeDejaPaye.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec rémunération exercice précédent négative', () => {
    fillForm();
    component.remunerationBruteAnnuelleExercicePrecedentEur.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dateReclamation', () => {
    fillForm();
    component.dateReclamation.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : PECULE_DEPART sans dateSortie → false', () => {
    fillForm();
    component.typeCalcul.set('PECULE_DEPART');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : PECULE_DEPART avec dateSortie → true', () => {
    fillForm();
    component.typeCalcul.set('PECULE_DEPART');
    component.dateSortie.set('2026-06-30');
    expect(component.formValid()).toBe(true);
  });

  it('isDepartScenario() true uniquement si PECULE_DEPART', () => {
    expect(component.isDepartScenario()).toBe(false);
    component.typeCalcul.set('PECULE_DEPART');
    expect(component.isDepartScenario()).toBe(true);
    component.typeCalcul.set('DOUBLE_PECULE');
    expect(component.isDepartScenario()).toBe(false);
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
    expect(post.request.body.statut).toBe('EMPLOYE');
    expect(post.request.body.typeCalcul).toBe('PECULE_SIMPLE');
    expect(post.request.body.remunerationBruteAnnuelleExerciceEur).toBe(30000);
    expect(post.request.body.remunerationMensuelleBruteEur).toBe(2500);
    expect(post.request.body.joursCongesPris).toBe(12);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Pécule de vacances calculé', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'joursCongesPris plafonné à 30' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'joursCongesPris plafonné à 30', 'Fermer', expect.anything(),
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
  // Verdict — label / classe / icône (8 cas)
  // ============================================================

  it('verdict DU_PECULE_SIMPLE_CALCULE → label, verdict-ok, paid', () => {
    component.result.set(response());
    expect(component.verdictLabel()).toContain('simple');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('paid');
  });

  it('verdict DU_DOUBLE_PECULE_CALCULE → label, verdict-ok, savings', () => {
    component.result.set(response({
      verdict: 'DU_DOUBLE_PECULE_CALCULE',
      typeCalcul: 'DOUBLE_PECULE',
    }));
    expect(component.verdictLabel()).toContain('Double');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('savings');
  });

  it('verdict DU_PECULE_DEPART_CALCULE → label, verdict-ok, logout', () => {
    component.result.set(response({
      verdict: 'DU_PECULE_DEPART_CALCULE',
      typeCalcul: 'PECULE_DEPART',
      dateSortie: '2026-06-30',
    }));
    expect(component.verdictLabel()).toContain('départ');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('logout');
  });

  it('verdict NON_DU_OUVRIER_VIA_ONVA → label, verdict-neutral, account_balance', () => {
    component.result.set(response({
      verdict: 'NON_DU_OUVRIER_VIA_ONVA',
      statut: 'OUVRIER',
    }));
    expect(component.verdictLabel()).toContain('ONVA');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('account_balance');
  });

  it('verdict NON_DU_DEJA_PAYE → label, verdict-neutral, check_circle_outline', () => {
    component.result.set(response({
      verdict: 'NON_DU_DEJA_PAYE',
      peculeDejaPaye: true,
    }));
    expect(component.verdictLabel()).toContain('déjà liquidé');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('check_circle_outline');
  });

  it('verdict NON_DU_JOURS_INSUFFISANTS → label, verdict-neutral, do_not_disturb_on', () => {
    component.result.set(response({
      verdict: 'NON_DU_JOURS_INSUFFISANTS',
      statut: 'JEUNE_TRAVAILLEUR',
      joursCongesPris: 0,
    }));
    expect(component.verdictLabel()).toContain('insuffisante');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('do_not_disturb_on');
  });

  it('verdict PRESCRIT → label, verdict-critical, gavel', () => {
    component.result.set(response({
      verdict: 'PRESCRIT',
      prescrit: true,
      joursDepuisFaitGenerateur: 500,
    }));
    expect(component.verdictLabel()).toContain('prescrite');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('gavel');
  });

  it('verdict A_ANALYSER → label, verdict-neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_ANALYSER',
      statut: 'INDETERMINE',
    }));
    expect(component.verdictLabel()).toContain('Configuration');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('beach_access');
  });

  // ============================================================
  // Badge dans header (8 cas)
  // ============================================================

  it('badge DU_PECULE_SIMPLE_CALCULE = vert', () => {
    expect(component.verdictBadge('DU_PECULE_SIMPLE_CALCULE')).toBe('SIMPLE DÛ');
    expect(component.verdictBadgeClass('DU_PECULE_SIMPLE_CALCULE')).toBe('badge-ok');
  });

  it('badge DU_DOUBLE_PECULE_CALCULE = vert', () => {
    expect(component.verdictBadge('DU_DOUBLE_PECULE_CALCULE')).toBe('DOUBLE DÛ');
    expect(component.verdictBadgeClass('DU_DOUBLE_PECULE_CALCULE')).toBe('badge-ok');
  });

  it('badge DU_PECULE_DEPART_CALCULE = vert', () => {
    expect(component.verdictBadge('DU_PECULE_DEPART_CALCULE')).toBe('DÉPART DÛ');
    expect(component.verdictBadgeClass('DU_PECULE_DEPART_CALCULE')).toBe('badge-ok');
  });

  it('badge NON_DU_OUVRIER_VIA_ONVA = neutre', () => {
    expect(component.verdictBadge('NON_DU_OUVRIER_VIA_ONVA')).toBe('VOIR ONVA');
    expect(component.verdictBadgeClass('NON_DU_OUVRIER_VIA_ONVA')).toBe('badge-neutral');
  });

  it('badge NON_DU_DEJA_PAYE = neutre', () => {
    expect(component.verdictBadge('NON_DU_DEJA_PAYE')).toBe('DÉJÀ PAYÉ');
    expect(component.verdictBadgeClass('NON_DU_DEJA_PAYE')).toBe('badge-neutral');
  });

  it('badge NON_DU_JOURS_INSUFFISANTS = neutre', () => {
    expect(component.verdictBadge('NON_DU_JOURS_INSUFFISANTS')).toBe('JOURS INSUFFISANTS');
    expect(component.verdictBadgeClass('NON_DU_JOURS_INSUFFISANTS')).toBe('badge-neutral');
  });

  it('badge PRESCRIT = rouge', () => {
    expect(component.verdictBadge('PRESCRIT')).toBe('PRESCRIT');
    expect(component.verdictBadgeClass('PRESCRIT')).toBe('badge-critical');
  });

  it('badge A_ANALYSER = neutre', () => {
    expect(component.verdictBadge('A_ANALYSER')).toBe('À ANALYSER');
    expect(component.verdictBadgeClass('A_ANALYSER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — verdict DU_PECULE_SIMPLE rendu (badge + montants)
  // ============================================================

  it('DU_PECULE_SIMPLE → badge vert dans le DOM + 5 lignes montants affichées', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('SIMPLE');
    const montants = fixture.nativeElement.querySelectorAll('.montant-row');
    // 4 montants détaillés + 1 fraction = 5 lignes (montant total inclus).
    expect(montants.length).toBe(5);
  });

  it('PRESCRIT → badge rouge + bannière critique + prescription-ko', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'PRESCRIT',
      prescrit: true,
      joursDepuisFaitGenerateur: 500,
      dateFinContrat: '2024-01-15',
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('PRESCRIT');
    const banner = fixture.nativeElement.querySelector('.result-banner.verdict-critical');
    expect(banner).not.toBeNull();
    const prescription = fixture.nativeElement.querySelector('.prescription-row.prescription-ko');
    expect(prescription).not.toBeNull();
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellé statut : 4 cas + null', () => {
    expect(component.statutLabel('EMPLOYE')).toContain('Employé');
    expect(component.statutLabel('OUVRIER')).toContain('Ouvrier');
    expect(component.statutLabel('JEUNE_TRAVAILLEUR')).toContain('Jeune');
    expect(component.statutLabel('INDETERMINE')).toContain('indéterminé');
    expect(component.statutLabel(null)).toBe('—');
  });

  it('libellé typeCalcul : 3 cas + null', () => {
    expect(component.typeCalculLabel('PECULE_SIMPLE')).toContain('simple');
    expect(component.typeCalculLabel('DOUBLE_PECULE')).toContain('Double');
    expect(component.typeCalculLabel('PECULE_DEPART')).toContain('départ');
    expect(component.typeCalculLabel(null)).toBe('—');
  });

  // ============================================================
  // Formats
  // ============================================================

  it('formatMontant arrondit à 2 décimales avec € + locale FR', () => {
    expect(component.formatMontant(1250)).toContain('1');
    expect(component.formatMontant(1250)).toContain('250,00');
    expect(component.formatMontant(1250)).toContain('€');
    expect(component.formatMontant(null)).toBe('—');
    expect(component.formatMontant(undefined)).toBe('—');
  });

  it('formatPourcentage convertit fraction → %, 2 décimales', () => {
    const txt = component.formatPourcentage(0.1534);
    expect(txt).toContain('15,34');
    expect(txt).toContain('%');
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
      avertissement: 'Coefficient 108 % ouvrier à vérifier + régime 6 j vs 5 j + actes interruptifs prescription.',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('108');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Pécule simple employé liquidé pour 12 jours pris (art. 38 AR 30/03/1967).',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('Pécule simple');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(PeculeVacancesBeSectionComponent.getPrefillCount({})).toBe(0);
    expect(PeculeVacancesBeSectionComponent.getPrefillCount({
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

  it('toolId pour jurisprudence = pecule-vacances-be', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('pecule-vacances-be');
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
    component.onStatutChange('OUVRIER');
    expect(component.statut()).toBe('OUVRIER');

    component.onTypeCalculChange('DOUBLE_PECULE');
    expect(component.typeCalcul()).toBe('DOUBLE_PECULE');

    component.onRemunerationBruteAnnuelleExerciceEurChange('45000');
    expect(component.remunerationBruteAnnuelleExerciceEur()).toBe(45000);

    component.onRemunerationMensuelleBruteEurChange(3500);
    expect(component.remunerationMensuelleBruteEur()).toBe(3500);

    component.onJoursCongesPrisChange('20');
    expect(component.joursCongesPris()).toBe(20);

    component.onPeculeDejaPayeChange(true);
    expect(component.peculeDejaPaye()).toBe(true);

    component.onRemunerationBruteAnnuelleExercicePrecedentEurChange(42000);
    expect(component.remunerationBruteAnnuelleExercicePrecedentEur()).toBe(42000);

    component.onDateSortieChange('2026-09-30');
    expect(component.dateSortie()).toBe('2026-09-30');

    component.onDateFinContratChange('2026-09-30');
    expect(component.dateFinContrat()).toBe('2026-09-30');

    component.onDateReclamationChange('2026-10-15');
    expect(component.dateReclamation()).toBe('2026-10-15');

    component.onJoursCongesPrisChange(null);
    expect(component.joursCongesPris()).toBeNull();

    component.onRemunerationBruteAnnuelleExerciceEurChange('');
    expect(component.remunerationBruteAnnuelleExerciceEur()).toBeNull();
  });
});
