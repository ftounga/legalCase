import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  InterimBeIndemniteFinMissionSectionComponent,
} from './interim-be-indemnite-fin-mission-section.component';
import {
  InterimBeIndemniteFinMissionResponse,
} from '../../core/models/interim-be-indemnite-fin-mission.model';

describe('InterimBeIndemniteFinMissionSectionComponent', () => {
  let component: InterimBeIndemniteFinMissionSectionComponent;
  let fixture: ComponentFixture<InterimBeIndemniteFinMissionSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/interim-be-indemnite-fin-mission';

  /**
   * Construit une réponse backend par défaut — verdict INDEMNITES_DUES
   * (CP 200 employés, prime 1/12, mission au terme, FSI non versé,
   * 152 h de prestation à 15 €/h).
   */
  function response(
    overrides: Partial<InterimBeIndemniteFinMissionResponse> = {},
  ): InterimBeIndemniteFinMissionResponse {
    return {
      caseFileId: 'case-1',
      dateDebutMission: '2025-01-01',
      dateFinPrevue: '2025-02-01',
      dateFinReelle: '2025-02-01',
      dureeReellePrestationJours: 22,
      dureePrevueJours: 22,
      salaireHoraireBrut: 15,
      heuresPrestees: 152,
      heuresSupplementairesSemaine: 0,
      heuresSupplementairesDimancheFerie: 0,
      peculeDejaVerseParFsi: false,
      ruptureAnticipeeParEtiSansMotifGrave: false,
      commissionParitaireUtilisateur: 'CP_200_AUXILIAIRE_EMPLOYES',
      ancienneteSectorielleJours: 200,
      verdict: 'INDEMNITES_DUES',
      salaireBrutTotalPrestations: 2280,
      peculeVacancesInterim: 350.66,
      primeFinAnneeSectorielle: 190,
      indemniteRuptureAnticipee: 0,
      sursalaireHeuresSupplementaires: 0,
      totalIndemnitesBrutes: 540.66,
      primePrecaritePresumee: 228,
      tauxPrimeFinAnneeApplique: 0.0833,
      joursRestantsACourirJusquAuTerme: 0,
      ancienneteSectorielleSuffisante: true,
      raison: 'COMPOSANTES_CALCULEES',
      synthese: 'Pécule 15,38 % + prime CP 200 (1/12) — total 540,66 €.',
      baseJuridique: 'Loi 24/07/1987 + CCT n° 322 + CCT n° 322bis + AR 30/03/1967 art. 19 + Cass. BE 03/05/2010',
      avertissement: 'Pas de prime de précarité 10 % style FR (art. L. 1251-32) en droit belge.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        InterimBeIndemniteFinMissionSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(InterimBeIndemniteFinMissionSectionComponent);
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
    expect(banner.textContent).toContain('IFM');
  });

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 → hydrate result + mode résultat + rehydrate form pour édition', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('INDEMNITES_DUES');
    expect(component.showForm()).toBe(false);
    expect(component.dateDebutMission()).toBe('2025-01-01');
    expect(component.dateFinPrevue()).toBe('2025-02-01');
    expect(component.dateFinReelle()).toBe('2025-02-01');
    expect(component.dureeReellePrestationJours()).toBe(22);
    expect(component.salaireHoraireBrut()).toBe(15);
    expect(component.heuresPrestees()).toBe(152);
    expect(component.peculeDejaVerseParFsi()).toBe(false);
    expect(component.ruptureAnticipeeParEtiSansMotifGrave()).toBe(false);
    expect(component.commissionParitaireUtilisateur()).toBe('CP_200_AUXILIAIRE_EMPLOYES');
    expect(component.ancienneteSectorielleJours()).toBe(200);
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
    component.dateDebutMission.set('2025-01-01');
    component.dateFinPrevue.set('2025-02-01');
    component.dateFinReelle.set('2025-02-01');
    component.dureeReellePrestationJours.set(22);
    component.dureePrevueJours.set(22);
    component.salaireHoraireBrut.set(15);
    component.heuresPrestees.set(152);
    component.heuresSupplementairesSemaine.set(0);
    component.heuresSupplementairesDimancheFerie.set(0);
    component.peculeDejaVerseParFsi.set(false);
    component.ruptureAnticipeeParEtiSansMotifGrave.set(false);
    component.commissionParitaireUtilisateur.set('CP_200_AUXILIAIRE_EMPLOYES');
    component.ancienneteSectorielleJours.set(200);
  }

  it('formValid() : true avec 13 champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans dateDebutMission', () => {
    fillForm();
    component.dateDebutMission.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dateFinPrevue', () => {
    fillForm();
    component.dateFinPrevue.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dateFinReelle', () => {
    fillForm();
    component.dateFinReelle.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec dureeReelle négative', () => {
    fillForm();
    component.dureeReellePrestationJours.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec dureePrevue <= 0', () => {
    fillForm();
    component.dureePrevueJours.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec salaireHoraire <= 0', () => {
    fillForm();
    component.salaireHoraireBrut.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec heuresPrestees négatives', () => {
    fillForm();
    component.heuresPrestees.set(-5);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec heuresSupSemaine négatives', () => {
    fillForm();
    component.heuresSupplementairesSemaine.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec heuresSupDimancheFerie négatives', () => {
    fillForm();
    component.heuresSupplementairesDimancheFerie.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans peculeDejaVerseParFsi précisé', () => {
    fillForm();
    component.peculeDejaVerseParFsi.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans rupture précisée', () => {
    fillForm();
    component.ruptureAnticipeeParEtiSansMotifGrave.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans CP utilisateur', () => {
    fillForm();
    component.commissionParitaireUtilisateur.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec ancienneté négative', () => {
    fillForm();
    component.ancienneteSectorielleJours.set(-1);
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
    expect(post.request.body.commissionParitaireUtilisateur).toBe('CP_200_AUXILIAIRE_EMPLOYES');
    expect(post.request.body.salaireHoraireBrut).toBe(15);
    expect(post.request.body.heuresPrestees).toBe(152);
    expect(post.request.body.peculeDejaVerseParFsi).toBe(false);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Indemnité fin de mission calculée', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'salaireHoraireBrut doit être strictement positif' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'salaireHoraireBrut doit être strictement positif', 'Fermer', expect.anything(),
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
  // Verdict — label / classe / icône (4 cas)
  // ============================================================

  it('verdict INDEMNITES_DUES → label, verdict-ok, paid', () => {
    component.result.set(response({ verdict: 'INDEMNITES_DUES' }));
    expect(component.verdictLabel()).toContain('Indemnités dues');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('paid');
  });

  it('verdict RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR → label, verdict-critical, gavel', () => {
    component.result.set(response({
      verdict: 'RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR',
      ruptureAnticipeeParEtiSansMotifGrave: true,
      indemniteRuptureAnticipee: 1200,
      joursRestantsACourirJusquAuTerme: 10,
    }));
    expect(component.verdictLabel()).toContain('Rupture anticipée');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('gavel');
  });

  it('verdict AUCUNE_INDEMNITE_DUE → label, verdict-neutral, do_not_disturb_on', () => {
    component.result.set(response({
      verdict: 'AUCUNE_INDEMNITE_DUE',
      peculeDejaVerseParFsi: true,
      peculeVacancesInterim: 0,
      primeFinAnneeSectorielle: 0,
      indemniteRuptureAnticipee: 0,
      sursalaireHeuresSupplementaires: 0,
      totalIndemnitesBrutes: 0,
    }));
    expect(component.verdictLabel()).toContain('Aucune indemnité');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('do_not_disturb_on');
  });

  it('verdict A_ANALYSER_SECTEUR_NON_RECONNU → label, verdict-neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_ANALYSER_SECTEUR_NON_RECONNU',
      commissionParitaireUtilisateur: 'AUTRE_NON_RECENSE',
      primeFinAnneeSectorielle: 0,
    }));
    expect(component.verdictLabel()).toContain('non recensée');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('request_quote');
  });

  // ============================================================
  // Badge dans header (4 cas)
  // ============================================================

  it('badge INDEMNITES_DUES = vert', () => {
    expect(component.verdictBadge('INDEMNITES_DUES')).toBe('INDEMNITÉS DUES');
    expect(component.verdictBadgeClass('INDEMNITES_DUES')).toBe('badge-ok');
  });

  it('badge RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR = rouge', () => {
    expect(component.verdictBadge('RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR')).toBe('RUPTURE ANTICIPÉE');
    expect(component.verdictBadgeClass('RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR')).toBe('badge-critical');
  });

  it('badge AUCUNE_INDEMNITE_DUE = neutre', () => {
    expect(component.verdictBadge('AUCUNE_INDEMNITE_DUE')).toBe('AUCUNE INDEMNITÉ');
    expect(component.verdictBadgeClass('AUCUNE_INDEMNITE_DUE')).toBe('badge-neutral');
  });

  it('badge A_ANALYSER_SECTEUR_NON_RECONNU = neutre', () => {
    expect(component.verdictBadge('A_ANALYSER_SECTEUR_NON_RECONNU')).toBe('À ANALYSER');
    expect(component.verdictBadgeClass('A_ANALYSER_SECTEUR_NON_RECONNU')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — verdict INDEMNITES_DUES rendu (badge + composantes)
  // ============================================================

  it('INDEMNITES_DUES → badge vert dans le DOM + 4 composantes affichées', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('INDEMNITÉS');
    const composantes = fixture.nativeElement.querySelectorAll('.composante-row');
    expect(composantes.length).toBe(4);
  });

  it('RUPTURE_ANTICIPEE → badge rouge + composante rupture > 0 affichée + jours restants', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR',
      ruptureAnticipeeParEtiSansMotifGrave: true,
      dateFinReelle: '2025-01-22',
      dureeReellePrestationJours: 15,
      indemniteRuptureAnticipee: 1050,
      joursRestantsACourirJusquAuTerme: 10,
      totalIndemnitesBrutes: 1400,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('RUPTURE');
    const text = fixture.nativeElement.textContent;
    // 1050 en fr-FR → "1 050,00" avec narrow no-break space U+202F (pas regular space)
    expect(text).toContain('050,00');
    expect(text).toContain('jour');
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellé commissionParitaire : 7 cas + null', () => {
    expect(component.commissionParitaireLabel('CP_124_CONSTRUCTION')).toContain('Construction');
    expect(component.commissionParitaireLabel('CP_200_AUXILIAIRE_EMPLOYES')).toContain('Auxiliaire');
    expect(component.commissionParitaireLabel('CP_218_NON_MARCHAND')).toContain('non-marchand');
    expect(component.commissionParitaireLabel('CP_322_INTERIM')).toContain('intérimaire');
    expect(component.commissionParitaireLabel('CP_140_TRANSPORT_LOGISTIQUE')).toContain('Transport');
    expect(component.commissionParitaireLabel('CP_209_METAUX_FABRICATIONS_METALLIQUES')).toContain('Métaux');
    expect(component.commissionParitaireLabel('AUTRE_NON_RECENSE')).toContain('non recensée');
    expect(component.commissionParitaireLabel(null)).toBe('—');
  });

  // ============================================================
  // Formats
  // ============================================================

  it('formatEuro arrondit à 2 décimales avec € + locale FR', () => {
    expect(component.formatEuro(1234.5)).toContain('1');
    expect(component.formatEuro(1234.5)).toContain('234');
    expect(component.formatEuro(1234.5)).toContain('50');
    expect(component.formatEuro(1234.5)).toContain('€');
    expect(component.formatEuro(null)).toBe('—');
    expect(component.formatEuro(undefined)).toBe('—');
  });

  it('formatPourcentage convertit décimal → % avec locale FR', () => {
    expect(component.formatPourcentage(0.0833)).toContain('8');
    expect(component.formatPourcentage(0.0833)).toContain('33');
    expect(component.formatPourcentage(0.0833)).toContain('%');
    expect(component.formatPourcentage(null)).toBe('—');
    expect(component.formatPourcentage(undefined)).toBe('—');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non vide → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      avertissement: 'Pas de prime de précarité 10 % style FR (Cass. BE 03/05/2010).',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('précarité');
  });

  // ============================================================
  // Contraste FR — prime de précarité présumée
  // ============================================================

  it('bloc contraste FR affiche prime de précarité présumée', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({ primePrecaritePresumee: 228 }));
    fixture.detectChanges();
    const contrast = fixture.nativeElement.querySelector('.result-contrast');
    expect(contrast).not.toBeNull();
    expect(contrast.textContent).toContain('précarité');
    expect(contrast.textContent).toContain('228,00');
    expect(contrast.textContent).toContain('Non due en droit belge');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Pécule 15,38 % + prime CP 200 — total dû 540,66 €.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('540,66');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(InterimBeIndemniteFinMissionSectionComponent.getPrefillCount({})).toBe(0);
    expect(InterimBeIndemniteFinMissionSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateNaissanceSalarie: '1980-04-15',
        dateRuptureContrat: '2026-04-10',
        motifRupture: 'autre',
        anneesCarriereSalarie: 12,
        salaireBrutMensuel: 2800,
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

  it('toolId pour jurisprudence = interim-be-indemnite-fin-mission', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('interim-be-indemnite-fin-mission');
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
    component.onDateDebutMissionChange('2025-06-01');
    expect(component.dateDebutMission()).toBe('2025-06-01');

    component.onDateFinPrevueChange('2025-07-01');
    expect(component.dateFinPrevue()).toBe('2025-07-01');

    component.onDateFinReelleChange('2025-06-25');
    expect(component.dateFinReelle()).toBe('2025-06-25');

    component.onDureeReellePrestationJoursChange(20);
    expect(component.dureeReellePrestationJours()).toBe(20);

    component.onDureePrevueJoursChange('22');
    expect(component.dureePrevueJours()).toBe(22);

    component.onSalaireHoraireBrutChange('18.5');
    expect(component.salaireHoraireBrut()).toBe(18.5);

    component.onHeuresPresteesChange(160);
    expect(component.heuresPrestees()).toBe(160);

    component.onHeuresSupplementairesSemaineChange(4);
    expect(component.heuresSupplementairesSemaine()).toBe(4);

    component.onHeuresSupplementairesDimancheFerieChange(2);
    expect(component.heuresSupplementairesDimancheFerie()).toBe(2);

    component.onPeculeDejaVerseParFsiChange(true);
    expect(component.peculeDejaVerseParFsi()).toBe(true);

    component.onRuptureAnticipeeParEtiSansMotifGraveChange(true);
    expect(component.ruptureAnticipeeParEtiSansMotifGrave()).toBe(true);

    component.onCommissionParitaireUtilisateurChange('CP_124_CONSTRUCTION');
    expect(component.commissionParitaireUtilisateur()).toBe('CP_124_CONSTRUCTION');

    component.onAncienneteSectorielleJoursChange(120);
    expect(component.ancienneteSectorielleJours()).toBe(120);

    component.onDureeReellePrestationJoursChange(null);
    expect(component.dureeReellePrestationJours()).toBeNull();

    component.onSalaireHoraireBrutChange('');
    expect(component.salaireHoraireBrut()).toBeNull();
  });
});
