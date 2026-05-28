import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  ClauseEcolageBeSectionComponent,
} from './clause-ecolage-be-section.component';
import {
  ClauseEcolageBeResponse,
} from '../../core/models/clause-ecolage-be.model';

describe('ClauseEcolageBeSectionComponent', () => {
  let component: ClauseEcolageBeSectionComponent;
  let fixture: ComponentFixture<ClauseEcolageBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/clause-ecolage-be';

  /**
   * Construit une réponse backend par défaut — verdict
   * VALIDE_REMBOURSEMENT_DEGRESSIF (clause valable, départ en 2e tiers,
   * remboursement plafonné à 80 %).
   */
  function response(
    overrides: Partial<ClauseEcolageBeResponse> = {},
  ): ClauseEcolageBeResponse {
    return {
      caseFileId: 'case-1',
      typeFormation: 'SPECIFIQUE',
      clauseEcriteAvantEntreeFormation: true,
      coutReelFormationEuros: 5000,
      rmmmgMensuelEuros: 2029.88,
      dureeEfficaciteMois: 36,
      dateFinFormation: '2024-06-30',
      dateDepartTravailleur: '2025-09-30',
      motifDepart: 'DEMISSION_TRAVAILLEUR',
      verdict: 'VALIDE_REMBOURSEMENT_DEGRESSIF',
      coutMinLegalEuros: 1014.94,
      moisEcoulesDepuisFinFormation: 15,
      tierDureeAuDepart: 2,
      quotiteDueRatio: 0.66,
      montantBrutDueEuros: 3300,
      plafond80Euros: 4000,
      montantDuFinalEuros: 3300,
      raison: 'VALIDE_DEGRESSIF',
      synthese: 'Clause valable — 2e tiers — 3 300 € dus (sous plafond 4 000 €).',
      baseJuridique: 'Art. 22bis Loi 03/07/1978 + CCT n° 43 + CCT n° 13',
      avertissement: '',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        ClauseEcolageBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ClauseEcolageBeSectionComponent);
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
    expect(component.result()!.verdict).toBe('VALIDE_REMBOURSEMENT_DEGRESSIF');
    expect(component.showForm()).toBe(false);
    expect(component.typeFormation()).toBe('SPECIFIQUE');
    expect(component.clauseEcriteAvantEntreeFormation()).toBe(true);
    expect(component.coutReelFormationEuros()).toBe(5000);
    expect(component.rmmmgMensuelEuros()).toBe(2029.88);
    expect(component.dureeEfficaciteMois()).toBe(36);
    expect(component.dateFinFormation()).toBe('2024-06-30');
    expect(component.dateDepartTravailleur()).toBe('2025-09-30');
    expect(component.motifDepart()).toBe('DEMISSION_TRAVAILLEUR');
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
    component.typeFormation.set('SPECIFIQUE');
    component.clauseEcriteAvantEntreeFormation.set(true);
    component.coutReelFormationEuros.set(5000);
    component.rmmmgMensuelEuros.set(2029.88);
    component.dureeEfficaciteMois.set(36);
    component.dateFinFormation.set('2024-06-30');
    component.dateDepartTravailleur.set('2025-09-30');
    component.motifDepart.set('DEMISSION_TRAVAILLEUR');
  }

  it('formValid() : true avec 8 champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans typeFormation', () => {
    fillForm();
    component.typeFormation.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans clauseEcriteAvantEntreeFormation précisée', () => {
    fillForm();
    component.clauseEcriteAvantEntreeFormation.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec coût réel négatif', () => {
    fillForm();
    component.coutReelFormationEuros.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec RMMMG <= 0', () => {
    fillForm();
    component.rmmmgMensuelEuros.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec durée efficacité < 1', () => {
    fillForm();
    component.dureeEfficaciteMois.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dateFinFormation', () => {
    fillForm();
    component.dateFinFormation.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dateDepartTravailleur', () => {
    fillForm();
    component.dateDepartTravailleur.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans motifDepart', () => {
    fillForm();
    component.motifDepart.set(null);
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
    expect(post.request.body.typeFormation).toBe('SPECIFIQUE');
    expect(post.request.body.clauseEcriteAvantEntreeFormation).toBe(true);
    expect(post.request.body.coutReelFormationEuros).toBe(5000);
    expect(post.request.body.dureeEfficaciteMois).toBe(36);
    expect(post.request.body.motifDepart).toBe('DEMISSION_TRAVAILLEUR');
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Validité clause d\'écolage analysée', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'dureeEfficaciteMois doit être ≥ 1' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'dureeEfficaciteMois doit être ≥ 1', 'Fermer', expect.anything(),
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

  it('verdict VALIDE_REMBOURSEMENT_DEGRESSIF → label, verdict-ok, verified', () => {
    component.result.set(response({ verdict: 'VALIDE_REMBOURSEMENT_DEGRESSIF' }));
    expect(component.verdictLabel()).toContain('valable');
    expect(component.verdictLabel()).toContain('dégressif');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict VALIDE_DUREE_EXPIREE → label, verdict-ok, verified', () => {
    component.result.set(response({
      verdict: 'VALIDE_DUREE_EXPIREE',
      tierDureeAuDepart: 0,
      quotiteDueRatio: 0,
      montantBrutDueEuros: 0,
      montantDuFinalEuros: 0,
    }));
    expect(component.verdictLabel()).toContain('durée d\'efficacité expirée');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict INOPPOSABLE_MOTIF_DEPART → label, verdict-critical, cancel', () => {
    component.result.set(response({
      verdict: 'INOPPOSABLE_MOTIF_DEPART',
      motifDepart: 'LICENCIEMENT_EMPLOYEUR_SANS_MOTIF_GRAVE',
    }));
    expect(component.verdictLabel()).toContain('inopposable');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('cancel');
  });

  it('verdict NULLE_FORME_ECRITE_MANQUANTE → label, verdict-critical, cancel', () => {
    component.result.set(response({
      verdict: 'NULLE_FORME_ECRITE_MANQUANTE',
      clauseEcriteAvantEntreeFormation: false,
    }));
    expect(component.verdictLabel()).toContain('pas d\'écrit');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('cancel');
  });

  it('verdict NULLE_FORMATION_OBLIGATOIRE → label, verdict-critical, cancel', () => {
    component.result.set(response({
      verdict: 'NULLE_FORMATION_OBLIGATOIRE',
      typeFormation: 'OBLIGATOIRE_LEGALE',
    }));
    expect(component.verdictLabel()).toContain('imposée');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('cancel');
  });

  it('verdict NULLE_COUT_INSUFFISANT → label, verdict-critical, cancel', () => {
    component.result.set(response({
      verdict: 'NULLE_COUT_INSUFFISANT',
      coutReelFormationEuros: 500,
    }));
    expect(component.verdictLabel()).toContain('coût réel');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('cancel');
  });

  it('verdict NULLE_DUREE_EXCESSIVE → label, verdict-critical, cancel', () => {
    component.result.set(response({
      verdict: 'NULLE_DUREE_EXCESSIVE',
      dureeEfficaciteMois: 48,
    }));
    expect(component.verdictLabel()).toContain('durée d\'efficacité');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('cancel');
  });

  it('verdict A_ANALYSER → label, verdict-neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_ANALYSER',
      typeFormation: 'INDETERMINE',
    }));
    expect(component.verdictLabel()).toContain('indéterminé');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('school');
  });

  // ============================================================
  // Badge dans header (8 cas)
  // ============================================================

  it('badge VALIDE_REMBOURSEMENT_DEGRESSIF = vert', () => {
    expect(component.verdictBadge('VALIDE_REMBOURSEMENT_DEGRESSIF')).toBe('VALABLE — REMBOURSEMENT DÛ');
    expect(component.verdictBadgeClass('VALIDE_REMBOURSEMENT_DEGRESSIF')).toBe('badge-ok');
  });

  it('badge VALIDE_DUREE_EXPIREE = vert', () => {
    expect(component.verdictBadge('VALIDE_DUREE_EXPIREE')).toBe('VALABLE — DURÉE EXPIRÉE');
    expect(component.verdictBadgeClass('VALIDE_DUREE_EXPIREE')).toBe('badge-ok');
  });

  it('badge INOPPOSABLE_MOTIF_DEPART = rouge', () => {
    expect(component.verdictBadge('INOPPOSABLE_MOTIF_DEPART')).toBe('INOPPOSABLE');
    expect(component.verdictBadgeClass('INOPPOSABLE_MOTIF_DEPART')).toBe('badge-critical');
  });

  it('badge NULLE_FORME_ECRITE_MANQUANTE = rouge', () => {
    expect(component.verdictBadge('NULLE_FORME_ECRITE_MANQUANTE')).toBe('NULLE — ÉCRIT MANQUANT');
    expect(component.verdictBadgeClass('NULLE_FORME_ECRITE_MANQUANTE')).toBe('badge-critical');
  });

  it('badge NULLE_FORMATION_OBLIGATOIRE = rouge', () => {
    expect(component.verdictBadge('NULLE_FORMATION_OBLIGATOIRE')).toBe('NULLE — FORMATION OBLIGATOIRE');
    expect(component.verdictBadgeClass('NULLE_FORMATION_OBLIGATOIRE')).toBe('badge-critical');
  });

  it('badge NULLE_COUT_INSUFFISANT = rouge', () => {
    expect(component.verdictBadge('NULLE_COUT_INSUFFISANT')).toBe('NULLE — COÛT INSUFFISANT');
    expect(component.verdictBadgeClass('NULLE_COUT_INSUFFISANT')).toBe('badge-critical');
  });

  it('badge NULLE_DUREE_EXCESSIVE = rouge', () => {
    expect(component.verdictBadge('NULLE_DUREE_EXCESSIVE')).toBe('NULLE — DURÉE EXCESSIVE');
    expect(component.verdictBadgeClass('NULLE_DUREE_EXCESSIVE')).toBe('badge-critical');
  });

  it('badge A_ANALYSER = neutre', () => {
    expect(component.verdictBadge('A_ANALYSER')).toBe('À ANALYSER');
    expect(component.verdictBadgeClass('A_ANALYSER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — verdict VALIDE rendu (badge + calculs dégressifs)
  // ============================================================

  it('VALIDE_REMBOURSEMENT_DEGRESSIF → badge vert dans le DOM + 5 lignes calcul affichées', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('VALABLE');
    const rows = fixture.nativeElement.querySelectorAll('.dimension-row');
    expect(rows.length).toBe(5);
    const finalRow = fixture.nativeElement.querySelector('.dimension-row.dim-final');
    expect(finalRow).not.toBeNull();
    expect(finalRow.textContent).toContain('3300');
  });

  it('NULLE_COUT_INSUFFISANT → badge rouge + bloc seuil légal affiché', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'NULLE_COUT_INSUFFISANT',
      coutReelFormationEuros: 800,
      coutMinLegalEuros: 1014.94,
      avertissement: 'Coût insuffisant — clause nulle de plein droit',
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('COÛT');
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('1014.94');
  });

  it('INOPPOSABLE_MOTIF_DEPART → badge rouge + pas de bloc calcul', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INOPPOSABLE_MOTIF_DEPART',
      motifDepart: 'FIN_CDD_NORMALE',
      tierDureeAuDepart: 0,
      quotiteDueRatio: 0,
      montantBrutDueEuros: 0,
      montantDuFinalEuros: 0,
      coutMinLegalEuros: 1014.94,
    }));
    fixture.detectChanges();
    const dimList = fixture.nativeElement.querySelector('.dimensions-list');
    expect(dimList).toBeNull();
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellé typeFormation : 3 cas + null', () => {
    expect(component.typeFormationLabel('SPECIFIQUE')).toContain('Spécifique');
    expect(component.typeFormationLabel('OBLIGATOIRE_LEGALE')).toContain('Obligatoire');
    expect(component.typeFormationLabel('INDETERMINE')).toContain('Indéterminé');
    expect(component.typeFormationLabel(null)).toBe('—');
  });

  it('libellé motifDepart : 5 cas + null', () => {
    expect(component.motifDepartLabel('DEMISSION_TRAVAILLEUR')).toContain('Démission');
    expect(component.motifDepartLabel('LICENCIEMENT_EMPLOYEUR_SANS_MOTIF_GRAVE')).toContain('inopposable');
    expect(component.motifDepartLabel('LICENCIEMENT_MOTIF_GRAVE_TRAVAILLEUR')).toContain('actionnable');
    expect(component.motifDepartLabel('RUPTURE_MOTIF_GRAVE_EMPLOYEUR')).toContain('inopposable');
    expect(component.motifDepartLabel('FIN_CDD_NORMALE')).toContain('CDD');
    expect(component.motifDepartLabel(null)).toBe('—');
  });

  it('libellé tierDureeLabel : 4 valeurs nominales + autre', () => {
    expect(component.tierDureeLabel(1)).toContain('100');
    expect(component.tierDureeLabel(2)).toContain('66');
    expect(component.tierDureeLabel(3)).toContain('33');
    expect(component.tierDureeLabel(0)).toContain('au-delà');
    expect(component.tierDureeLabel(99)).toBe('—');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non vide → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'NULLE_DUREE_EXCESSIVE',
      dureeEfficaciteMois: 48,
      avertissement: 'Durée d\'efficacité > 36 mois — plafond légal dépassé',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('36 mois');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Clause valable — départ en 2e tiers — 3 300 € dus.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('valable');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(ClauseEcolageBeSectionComponent.getPrefillCount({})).toBe(0);
    expect(ClauseEcolageBeSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateNaissanceSalarie: '1985-03-12',
        dateRuptureContrat: '2026-09-30',
        motifRupture: 'demission',
        anneesCarriereSalarie: 8,
        salaireBrutMensuel: 3200,
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

  it('toolId pour jurisprudence = clause-ecolage-be', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('clause-ecolage-be');
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
    component.onTypeFormationChange('OBLIGATOIRE_LEGALE');
    expect(component.typeFormation()).toBe('OBLIGATOIRE_LEGALE');

    component.onClauseEcriteAvantEntreeFormationChange(false);
    expect(component.clauseEcriteAvantEntreeFormation()).toBe(false);

    component.onCoutReelFormationEurosChange(3000);
    expect(component.coutReelFormationEuros()).toBe(3000);

    component.onCoutReelFormationEurosChange('2500');
    expect(component.coutReelFormationEuros()).toBe(2500);

    component.onCoutReelFormationEurosChange(null);
    expect(component.coutReelFormationEuros()).toBeNull();

    component.onCoutReelFormationEurosChange('');
    expect(component.coutReelFormationEuros()).toBeNull();

    component.onCoutReelFormationEurosChange('abc');
    expect(component.coutReelFormationEuros()).toBeNull();

    component.onRmmmgMensuelEurosChange(1994.18);
    expect(component.rmmmgMensuelEuros()).toBe(1994.18);

    component.onDureeEfficaciteMoisChange(24);
    expect(component.dureeEfficaciteMois()).toBe(24);

    component.onDateFinFormationChange('2024-12-31');
    expect(component.dateFinFormation()).toBe('2024-12-31');

    component.onDateDepartTravailleurChange('2025-06-30');
    expect(component.dateDepartTravailleur()).toBe('2025-06-30');

    component.onMotifDepartChange('LICENCIEMENT_MOTIF_GRAVE_TRAVAILLEUR');
    expect(component.motifDepart()).toBe('LICENCIEMENT_MOTIF_GRAVE_TRAVAILLEUR');
  });

  // ============================================================
  // Static metadata F-177
  // ============================================================

  it('TOOL_LABEL et TOOL_ICON statiques exposés', () => {
    expect(ClauseEcolageBeSectionComponent.TOOL_LABEL).toBe('CLAUSE D\'ÉCOLAGE (BE — art. 22bis)');
    expect(ClauseEcolageBeSectionComponent.TOOL_ICON).toBe('school');
  });
});
