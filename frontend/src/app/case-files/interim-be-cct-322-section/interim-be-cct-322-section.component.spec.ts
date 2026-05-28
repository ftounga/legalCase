import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  InterimBeCct322SectionComponent,
} from './interim-be-cct-322-section.component';
import {
  InterimBeCct322Response,
} from '../../core/models/interim-be-cct-322.model';

describe('InterimBeCct322SectionComponent', () => {
  let component: InterimBeCct322SectionComponent;
  let fixture: ComponentFixture<InterimBeCct322SectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/interim-be-cct-322';

  /**
   * Construit une réponse backend par défaut — verdict
   * ELIGIBLE_MISSION_REGULIERE (motif remplacement, 5j cumulés ≤ 7j max,
   * parité OK, contrat + Dimona OK).
   */
  function response(
    overrides: Partial<InterimBeCct322Response> = {},
  ): InterimBeCct322Response {
    return {
      caseFileId: 'case-1',
      dateDebutMission: '2024-05-01',
      motifMission: 'REMPLACEMENT_TRAVAILLEUR_PERMANENT',
      remplacementGreveOuLockout: false,
      dureeTotaleMissionJours: 5,
      dureeMaxLegaleJours: 7,
      salaireHoraireIntimaireBrut: 15.5,
      salaireHoraireReferenceBrut: 15.5,
      contratEcritSigne: true,
      dimonaDeclareeParEti: true,
      verdict: 'ELIGIBLE_MISSION_REGULIERE',
      motifAutorise: true,
      dureeRespectee: true,
      pariteRespectee: true,
      formalismeRespecte: true,
      joursExcedentaires: 0,
      ecartPariteSalariale: 0,
      raison: 'MISSION_REGULIERE',
      synthese: 'Mission intérimaire pleinement régulière au sens de la Loi du 24/07/1987.',
      baseJuridique: 'Loi du 24/07/1987 + CCT n° 322 du 14/06/2010 + CCT n° 108 du 16/07/2013',
      avertissement: '',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        InterimBeCct322SectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(InterimBeCct322SectionComponent);
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
    expect(component.result()!.verdict).toBe('ELIGIBLE_MISSION_REGULIERE');
    expect(component.showForm()).toBe(false);
    expect(component.dateDebutMission()).toBe('2024-05-01');
    expect(component.motifMission()).toBe('REMPLACEMENT_TRAVAILLEUR_PERMANENT');
    expect(component.remplacementGreveOuLockout()).toBe(false);
    expect(component.dureeTotaleMissionJours()).toBe(5);
    expect(component.dureeMaxLegaleJours()).toBe(7);
    expect(component.salaireHoraireIntimaireBrut()).toBe(15.5);
    expect(component.salaireHoraireReferenceBrut()).toBe(15.5);
    expect(component.contratEcritSigne()).toBe(true);
    expect(component.dimonaDeclareeParEti()).toBe(true);
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
    component.dateDebutMission.set('2024-05-01');
    component.motifMission.set('REMPLACEMENT_TRAVAILLEUR_PERMANENT');
    component.remplacementGreveOuLockout.set(false);
    component.dureeTotaleMissionJours.set(5);
    component.dureeMaxLegaleJours.set(7);
    component.salaireHoraireIntimaireBrut.set(15.5);
    component.salaireHoraireReferenceBrut.set(15.5);
    component.contratEcritSigne.set(true);
    component.dimonaDeclareeParEti.set(true);
  }

  it('formValid() : true avec 9 champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans dateDebutMission', () => {
    fillForm();
    component.dateDebutMission.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans motifMission', () => {
    fillForm();
    component.motifMission.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans remplacementGreveOuLockout précisé', () => {
    fillForm();
    component.remplacementGreveOuLockout.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec dureeTotale négative', () => {
    fillForm();
    component.dureeTotaleMissionJours.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec dureeMaxLegale <= 0', () => {
    fillForm();
    component.dureeMaxLegaleJours.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec salaire intérimaire négatif', () => {
    fillForm();
    component.salaireHoraireIntimaireBrut.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec salaire référence <= 0', () => {
    fillForm();
    component.salaireHoraireReferenceBrut.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans contratEcritSigne précisé', () => {
    fillForm();
    component.contratEcritSigne.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dimonaDeclareeParEti précisé', () => {
    fillForm();
    component.dimonaDeclareeParEti.set(null);
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
    expect(post.request.body.motifMission).toBe('REMPLACEMENT_TRAVAILLEUR_PERMANENT');
    expect(post.request.body.dureeTotaleMissionJours).toBe(5);
    expect(post.request.body.salaireHoraireIntimaireBrut).toBe(15.5);
    expect(post.request.body.contratEcritSigne).toBe(true);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Statut intérim analysé', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'dureeMaxLegaleJours doit être strictement positive' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'dureeMaxLegaleJours doit être strictement positive', 'Fermer', expect.anything(),
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
  // Verdict — label / classe / icône (7 cas)
  // ============================================================

  it('verdict ELIGIBLE_MISSION_REGULIERE → label, verdict-ok, verified', () => {
    component.result.set(response({ verdict: 'ELIGIBLE_MISSION_REGULIERE' }));
    expect(component.verdictLabel()).toContain('régulière');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict FRAGILE_CONTRAT_OU_DIMONA_MANQUANT → label, verdict-warn, warning_amber', () => {
    component.result.set(response({
      verdict: 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT',
      contratEcritSigne: false,
      formalismeRespecte: false,
    }));
    expect(component.verdictLabel()).toContain('Formalisme');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('warning_amber');
  });

  it('verdict INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT → label, verdict-critical, cancel', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT',
      remplacementGreveOuLockout: true,
    }));
    expect(component.verdictLabel()).toContain('grève');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('cancel');
  });

  it('verdict INELIGIBLE_MOTIF_NON_AUTORISE → label, verdict-critical, cancel', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_MOTIF_NON_AUTORISE',
      motifMission: 'AUTRE_NON_AUTORISE',
      motifAutorise: false,
    }));
    expect(component.verdictLabel()).toContain('Motif non autorisé');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('cancel');
  });

  it('verdict INELIGIBLE_DUREE_MAX_DEPASSEE → label, verdict-critical, cancel', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_DUREE_MAX_DEPASSEE',
      dureeTotaleMissionJours: 400,
      dureeMaxLegaleJours: 365,
      dureeRespectee: false,
      joursExcedentaires: 35,
    }));
    expect(component.verdictLabel()).toContain('Durée');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('cancel');
  });

  it('verdict INELIGIBLE_PARITE_SALARIALE_VIOLEE → label, verdict-critical, cancel', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_PARITE_SALARIALE_VIOLEE',
      salaireHoraireIntimaireBrut: 12,
      salaireHoraireReferenceBrut: 15.5,
      pariteRespectee: false,
      ecartPariteSalariale: 3.5,
    }));
    expect(component.verdictLabel()).toContain('Parité');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('cancel');
  });

  it('verdict A_ANALYSER → label, verdict-neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_ANALYSER',
      motifMission: 'MOTIF_ARTISTIQUE',
    }));
    expect(component.verdictLabel()).toContain('Zone grise');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('engineering');
  });

  // ============================================================
  // Badge dans header (7 cas)
  // ============================================================

  it('badge ELIGIBLE_MISSION_REGULIERE = vert', () => {
    expect(component.verdictBadge('ELIGIBLE_MISSION_REGULIERE')).toBe('ÉLIGIBLE');
    expect(component.verdictBadgeClass('ELIGIBLE_MISSION_REGULIERE')).toBe('badge-ok');
  });

  it('badge FRAGILE_CONTRAT_OU_DIMONA_MANQUANT = ambre', () => {
    expect(component.verdictBadge('FRAGILE_CONTRAT_OU_DIMONA_MANQUANT')).toBe('FORMALISME FRAGILE');
    expect(component.verdictBadgeClass('FRAGILE_CONTRAT_OU_DIMONA_MANQUANT')).toBe('badge-warn');
  });

  it('badge INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT = rouge', () => {
    expect(component.verdictBadge('INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT')).toBe('GRÈVE / LOCK-OUT');
    expect(component.verdictBadgeClass('INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT')).toBe('badge-critical');
  });

  it('badge INELIGIBLE_MOTIF_NON_AUTORISE = rouge', () => {
    expect(component.verdictBadge('INELIGIBLE_MOTIF_NON_AUTORISE')).toBe('MOTIF INTERDIT');
    expect(component.verdictBadgeClass('INELIGIBLE_MOTIF_NON_AUTORISE')).toBe('badge-critical');
  });

  it('badge INELIGIBLE_DUREE_MAX_DEPASSEE = rouge', () => {
    expect(component.verdictBadge('INELIGIBLE_DUREE_MAX_DEPASSEE')).toBe('DURÉE DÉPASSÉE');
    expect(component.verdictBadgeClass('INELIGIBLE_DUREE_MAX_DEPASSEE')).toBe('badge-critical');
  });

  it('badge INELIGIBLE_PARITE_SALARIALE_VIOLEE = rouge', () => {
    expect(component.verdictBadge('INELIGIBLE_PARITE_SALARIALE_VIOLEE')).toBe('PARITÉ VIOLÉE');
    expect(component.verdictBadgeClass('INELIGIBLE_PARITE_SALARIALE_VIOLEE')).toBe('badge-critical');
  });

  it('badge A_ANALYSER = neutre', () => {
    expect(component.verdictBadge('A_ANALYSER')).toBe('À ANALYSER');
    expect(component.verdictBadgeClass('A_ANALYSER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — verdict ELIGIBLE rendu (badge + 4 dimensions OK)
  // ============================================================

  it('ELIGIBLE → badge vert dans le DOM + 4 dimensions cumulatives toutes OK', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('ÉLIGIBLE');
    const okRows = fixture.nativeElement.querySelectorAll('.dimension-row.dim-ok');
    expect(okRows.length).toBe(4);
    const koRows = fixture.nativeElement.querySelectorAll('.dimension-row.dim-ko');
    expect(koRows.length).toBe(0);
  });

  it('INELIGIBLE_DUREE_MAX_DEPASSEE → badge rouge + jours excédentaires affichés', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INELIGIBLE_DUREE_MAX_DEPASSEE',
      dureeTotaleMissionJours: 400,
      dureeMaxLegaleJours: 365,
      dureeRespectee: false,
      joursExcedentaires: 35,
      avertissement: 'Requalification immédiate en CDI utilisateur',
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('DURÉE');
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('35');
    expect(text).toContain('excédentaires');
  });

  it('INELIGIBLE_PARITE_SALARIALE_VIOLEE → badge rouge + écart parité affiché', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INELIGIBLE_PARITE_SALARIALE_VIOLEE',
      salaireHoraireIntimaireBrut: 12,
      salaireHoraireReferenceBrut: 15.5,
      pariteRespectee: false,
      ecartPariteSalariale: 3.5,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('PARITÉ');
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('3.5');
    expect(text).toContain('Écart');
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellé motifMission : 7 cas + null', () => {
    expect(component.motifMissionLabel('REMPLACEMENT_TRAVAILLEUR_PERMANENT')).toContain('Remplacement');
    expect(component.motifMissionLabel('SURCROIT_TEMPORAIRE_DE_TRAVAIL')).toContain('Surcroît');
    expect(component.motifMissionLabel('EXECUTION_TRAVAIL_EXCEPTIONNEL')).toContain('exceptionnel');
    expect(component.motifMissionLabel('INSERTION_EMBAUCHE_DURABLE')).toContain('Insertion');
    expect(component.motifMissionLabel('MOTIF_ARTISTIQUE')).toContain('artistique');
    expect(component.motifMissionLabel('FLUX_TRAVAUX_EXCEPTIONNEL_TRAJET_SCOLAIRE')).toContain('Flux');
    expect(component.motifMissionLabel('AUTRE_NON_AUTORISE')).toContain('non listé');
    expect(component.motifMissionLabel(null)).toBe('—');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non vide → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT',
      contratEcritSigne: false,
      formalismeRespecte: false,
      avertissement: 'Contrat écrit absent — requalification automatique',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('requalification');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Mission intérimaire pleinement régulière au sens de la CCT 322.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('régulière');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(InterimBeCct322SectionComponent.getPrefillCount({})).toBe(0);
    expect(InterimBeCct322SectionComponent.getPrefillCount({
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

  it('toolId pour jurisprudence = interim-be-cct-322', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('interim-be-cct-322');
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
    component.onDateDebutMissionChange('2024-06-01');
    expect(component.dateDebutMission()).toBe('2024-06-01');

    component.onMotifMissionChange('SURCROIT_TEMPORAIRE_DE_TRAVAIL');
    expect(component.motifMission()).toBe('SURCROIT_TEMPORAIRE_DE_TRAVAIL');

    component.onRemplacementGreveOuLockoutChange(true);
    expect(component.remplacementGreveOuLockout()).toBe(true);

    component.onDureeTotaleMissionJoursChange(180);
    expect(component.dureeTotaleMissionJours()).toBe(180);

    component.onDureeTotaleMissionJoursChange('200');
    expect(component.dureeTotaleMissionJours()).toBe(200);

    component.onDureeTotaleMissionJoursChange(null);
    expect(component.dureeTotaleMissionJours()).toBeNull();

    component.onDureeTotaleMissionJoursChange('');
    expect(component.dureeTotaleMissionJours()).toBeNull();

    component.onDureeTotaleMissionJoursChange('abc');
    expect(component.dureeTotaleMissionJours()).toBeNull();

    component.onDureeMaxLegaleJoursChange(365);
    expect(component.dureeMaxLegaleJours()).toBe(365);

    component.onSalaireHoraireIntimaireBrutChange(14.25);
    expect(component.salaireHoraireIntimaireBrut()).toBe(14.25);

    component.onSalaireHoraireReferenceBrutChange(15.5);
    expect(component.salaireHoraireReferenceBrut()).toBe(15.5);

    component.onContratEcritSigneChange(false);
    expect(component.contratEcritSigne()).toBe(false);

    component.onDimonaDeclareeParEtiChange(true);
    expect(component.dimonaDeclareeParEti()).toBe(true);
  });

  // ============================================================
  // Static metadata F-177
  // ============================================================

  it('TOOL_LABEL et TOOL_ICON statiques exposés', () => {
    expect(InterimBeCct322SectionComponent.TOOL_LABEL).toBe('INTÉRIM (BE — CCT 322)');
    expect(InterimBeCct322SectionComponent.TOOL_ICON).toBe('engineering');
  });
});
