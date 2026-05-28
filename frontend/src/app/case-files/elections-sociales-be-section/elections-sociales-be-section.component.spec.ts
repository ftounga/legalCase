import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  ElectionsSocialesBeSectionComponent,
} from './elections-sociales-be-section.component';
import {
  ElectionsSocialesBeResponse,
} from '../../core/models/elections-sociales-be.model';

describe('ElectionsSocialesBeSectionComponent', () => {
  let component: ElectionsSocialesBeSectionComponent;
  let fixture: ComponentFixture<ElectionsSocialesBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/elections-sociales-be';

  /**
   * Construit une réponse backend par défaut — verdict OBLIGATION_CE_ET_CPPT
   * (cycle 2024, 120 ETP, UTE confirmée, calendrier rebours complet).
   */
  function response(
    overrides: Partial<ElectionsSocialesBeResponse> = {},
  ): ElectionsSocialesBeResponse {
    return {
      caseFileId: 'case-1',
      cycleElectoral: 'CYCLE_2024',
      dateJourY: '2024-05-15',
      effectifMoyenEtp: 120,
      uniteTechniqueExploitationConfirmee: true,
      verdict: 'OBLIGATION_CE_ET_CPPT',
      conseilEntrepriseObligatoire: true,
      comitePreventionProtectionObligatoire: true,
      seuilCeApplique: 100,
      seuilCpptApplique: 50,
      jourX: '2024-02-15',
      dateDebutProcedureUte: '2023-12-17',
      dateTransmissionListesElecteurs: '2024-01-11',
      dateDepotListesCandidats: '2024-03-21',
      dateAffichageCandidats: '2024-03-26',
      dateProclamationResultats: '2024-05-21',
      datePremiereReunionInstance: '2024-06-29',
      dateDebutPeriodeProtegeeCandidats: '2024-02-25',
      dateFinPeriodeProtegeeNonElus: '2026-03-26',
      raison: null,
      synthese: 'CE + CPPT obligatoires — effectif 120 ETP au-delà du seuil 100.',
      baseJuridique: 'Loi 04/12/2007 + AR 25/05/2012 + Loi 04/08/1996 art. 49 + Loi 19/03/1991',
      avertissement: '',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        ElectionsSocialesBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ElectionsSocialesBeSectionComponent);
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
    expect(component.result()!.verdict).toBe('OBLIGATION_CE_ET_CPPT');
    expect(component.showForm()).toBe(false);
    expect(component.cycleElectoral()).toBe('CYCLE_2024');
    expect(component.dateJourY()).toBe('2024-05-15');
    expect(component.effectifMoyenEtp()).toBe(120);
    expect(component.uniteTechniqueExploitationConfirmee()).toBe(true);
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

  it('formValid() : true avec 4 champs requis remplis', () => {
    component.cycleElectoral.set('CYCLE_2024');
    component.dateJourY.set('2024-05-15');
    component.effectifMoyenEtp.set(120);
    component.uniteTechniqueExploitationConfirmee.set(true);
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans cycleElectoral', () => {
    component.dateJourY.set('2024-05-15');
    component.effectifMoyenEtp.set(120);
    component.uniteTechniqueExploitationConfirmee.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dateJourY', () => {
    component.cycleElectoral.set('CYCLE_2024');
    component.effectifMoyenEtp.set(120);
    component.uniteTechniqueExploitationConfirmee.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans effectif', () => {
    component.cycleElectoral.set('CYCLE_2024');
    component.dateJourY.set('2024-05-15');
    component.uniteTechniqueExploitationConfirmee.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec effectif négatif', () => {
    component.cycleElectoral.set('CYCLE_2024');
    component.dateJourY.set('2024-05-15');
    component.effectifMoyenEtp.set(-1);
    component.uniteTechniqueExploitationConfirmee.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans UTE confirmée précisée', () => {
    component.cycleElectoral.set('CYCLE_2024');
    component.dateJourY.set('2024-05-15');
    component.effectifMoyenEtp.set(120);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  function fillForm() {
    component.cycleElectoral.set('CYCLE_2024');
    component.dateJourY.set('2024-05-15');
    component.effectifMoyenEtp.set(120);
    component.uniteTechniqueExploitationConfirmee.set(true);
  }

  it('calculate() → POST + snack succès', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.cycleElectoral).toBe('CYCLE_2024');
    expect(post.request.body.dateJourY).toBe('2024-05-15');
    expect(post.request.body.effectifMoyenEtp).toBe(120);
    expect(post.request.body.uniteTechniqueExploitationConfirmee).toBe(true);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Élections sociales analysées', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'dateJourY hors fenêtre AR' }, { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'dateJourY hors fenêtre AR', 'Fermer', expect.anything(),
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

  it('verdict OBLIGATION_CE_ET_CPPT → label, verdict-ok, verified', () => {
    component.result.set(response({ verdict: 'OBLIGATION_CE_ET_CPPT' }));
    expect(component.verdictLabel()).toContain('Conseil d\'entreprise');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict OBLIGATION_CPPT_SEUL → label, verdict-ok, verified', () => {
    component.result.set(response({
      verdict: 'OBLIGATION_CPPT_SEUL',
      effectifMoyenEtp: 70,
      conseilEntrepriseObligatoire: false,
      comitePreventionProtectionObligatoire: true,
    }));
    expect(component.verdictLabel()).toContain('CPPT seul');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict NON_APPLICABLE_SEUIL → label, verdict-neutral, info', () => {
    component.result.set(response({
      verdict: 'NON_APPLICABLE_SEUIL',
      effectifMoyenEtp: 30,
      conseilEntrepriseObligatoire: false,
      comitePreventionProtectionObligatoire: false,
      dateDebutPeriodeProtegeeCandidats: null,
      dateFinPeriodeProtegeeNonElus: null,
    }));
    expect(component.verdictLabel()).toContain('Pas d\'élections');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('info');
  });

  it('verdict EFFECTIF_A_RECALCULER → label, verdict-warn, warning_amber', () => {
    component.result.set(response({
      verdict: 'EFFECTIF_A_RECALCULER',
      effectifMoyenEtp: 100,
    }));
    expect(component.verdictLabel()).toContain('recalcul');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('warning_amber');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('how_to_vote');
  });

  // ============================================================
  // Badge dans header (4 cas)
  // ============================================================

  it('badge OBLIGATION_CE_ET_CPPT = vert "CE + CPPT"', () => {
    expect(component.verdictBadge('OBLIGATION_CE_ET_CPPT')).toBe('CE + CPPT');
    expect(component.verdictBadgeClass('OBLIGATION_CE_ET_CPPT')).toBe('badge-ok');
  });

  it('badge OBLIGATION_CPPT_SEUL = vert "CPPT SEUL"', () => {
    expect(component.verdictBadge('OBLIGATION_CPPT_SEUL')).toBe('CPPT SEUL');
    expect(component.verdictBadgeClass('OBLIGATION_CPPT_SEUL')).toBe('badge-ok');
  });

  it('badge NON_APPLICABLE_SEUIL = neutre "NON APPLICABLE"', () => {
    expect(component.verdictBadge('NON_APPLICABLE_SEUIL')).toBe('NON APPLICABLE');
    expect(component.verdictBadgeClass('NON_APPLICABLE_SEUIL')).toBe('badge-neutral');
  });

  it('badge EFFECTIF_A_RECALCULER = ambre "À RECALCULER"', () => {
    expect(component.verdictBadge('EFFECTIF_A_RECALCULER')).toBe('À RECALCULER');
    expect(component.verdictBadgeClass('EFFECTIF_A_RECALCULER')).toBe('badge-warn');
  });

  it('OBLIGATION_CE_ET_CPPT → badge vert dans le DOM + calendrier + fenêtre protection', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('CE + CPPT');
    const calendar = fixture.nativeElement.querySelector('.calendar-list');
    expect(calendar).not.toBeNull();
    expect(calendar.textContent).toContain('2024-02-15'); // jourX
    expect(calendar.textContent).toContain('2024-05-15'); // jourY
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('Fenêtre de protection candidats');
    expect(text).toContain('2024-02-25');
  });

  it('NON_APPLICABLE_SEUIL → badge gris dans le DOM + fenêtre protection absente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'NON_APPLICABLE_SEUIL',
      effectifMoyenEtp: 30,
      conseilEntrepriseObligatoire: false,
      comitePreventionProtectionObligatoire: false,
      dateDebutPeriodeProtegeeCandidats: null,
      dateFinPeriodeProtegeeNonElus: null,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-neutral');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('NON APPLICABLE');
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).not.toContain('Fenêtre de protection candidats');
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellé cycleElectoral : 3 cas + null', () => {
    expect(component.cycleLabel('CYCLE_2024')).toContain('2024');
    expect(component.cycleLabel('CYCLE_2028')).toContain('2028');
    expect(component.cycleLabel('CYCLE_2032')).toContain('2032');
    expect(component.cycleLabel(null)).toBe('—');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non vide → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'EFFECTIF_A_RECALCULER',
      effectifMoyenEtp: 100,
      avertissement: 'Effectif limite — recalcul ETP requis sur 4 trimestres précis',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('recalcul');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Effectif 120 ETP — CE + CPPT obligatoires cycle 2024.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('CE + CPPT obligatoires');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(ElectionsSocialesBeSectionComponent.getPrefillCount({})).toBe(0);
    expect(ElectionsSocialesBeSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateNaissanceSalarie: '1976-04-10',
        dateRuptureContrat: '2026-04-10',
        motifRupture: 'autre',
        anneesCarriereSalarie: 20,
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

  it('toolId pour jurisprudence = elections-sociales-be', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('elections-sociales-be');
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
    component.onCycleElectoralChange('CYCLE_2028');
    expect(component.cycleElectoral()).toBe('CYCLE_2028');

    component.onDateJourYChange('2028-05-15');
    expect(component.dateJourY()).toBe('2028-05-15');

    component.onEffectifMoyenEtpChange(80);
    expect(component.effectifMoyenEtp()).toBe(80);

    component.onEffectifMoyenEtpChange('150');
    expect(component.effectifMoyenEtp()).toBe(150);

    component.onEffectifMoyenEtpChange(null);
    expect(component.effectifMoyenEtp()).toBeNull();

    component.onEffectifMoyenEtpChange('');
    expect(component.effectifMoyenEtp()).toBeNull();

    component.onEffectifMoyenEtpChange('abc');
    expect(component.effectifMoyenEtp()).toBeNull();

    component.onUniteTechniqueExploitationConfirmeeChange(true);
    expect(component.uniteTechniqueExploitationConfirmee()).toBe(true);

    component.onUniteTechniqueExploitationConfirmeeChange(false);
    expect(component.uniteTechniqueExploitationConfirmee()).toBe(false);
  });

  // ============================================================
  // Static metadata F-177
  // ============================================================

  it('TOOL_LABEL et TOOL_ICON statiques exposés', () => {
    expect(ElectionsSocialesBeSectionComponent.TOOL_LABEL).toBe('ÉLECTIONS SOCIALES (BE)');
    expect(ElectionsSocialesBeSectionComponent.TOOL_ICON).toBe('how_to_vote');
  });
});
