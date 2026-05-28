import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  EtudiantJobisteBeSectionComponent,
} from './etudiant-jobiste-be-section.component';
import {
  EtudiantJobisteBeResponse,
} from '../../core/models/etudiant-jobiste-be.model';

describe('EtudiantJobisteBeSectionComponent', () => {
  let component: EtudiantJobisteBeSectionComponent;
  let fixture: ComponentFixture<EtudiantJobisteBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/etudiant-jobiste-be';

  /**
   * Construit une réponse backend par défaut — verdict ELIGIBLE
   * (étudiant principal, 100h déjà / 200h contrat, quota 600h, formalisme + cotisations OK).
   */
  function response(
    overrides: Partial<EtudiantJobisteBeResponse> = {},
  ): EtudiantJobisteBeResponse {
    return {
      caseFileId: 'case-1',
      dateDebutOccupation: '2026-07-01',
      statutEtudiant: 'ETUDIANT_PRINCIPAL',
      heuresDejaPresteesDansAnnee: 100,
      heuresContratEnCours: 200,
      quotaAnnuelHeures: 600,
      contratEcritSigne: true,
      dimonaStuDeclaree: true,
      cotisationsReduitesAppliquees: true,
      remunerationBruteHoraire: 13.5,
      verdict: 'ELIGIBLE',
      statutEligible: true,
      quotaRespecte: true,
      formalismeRespecte: true,
      cotisationsConformes: true,
      heuresRestantesAvantContrat: 500,
      heuresHorsQuota: 0,
      montantRedressementEstime: 0,
      raison: 'ETUDIANT_JOBISTE_REGULIER',
      synthese: 'Occupation étudiant pleinement régulière au sens de la Loi du 03/07/1978.',
      baseJuridique: 'Loi du 03/07/1978 Titre VII + AR 14/07/1995 + Loi-prog. 22/12/2023',
      avertissement: '',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        EtudiantJobisteBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(EtudiantJobisteBeSectionComponent);
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
    expect(component.result()!.verdict).toBe('ELIGIBLE');
    expect(component.showForm()).toBe(false);
    expect(component.dateDebutOccupation()).toBe('2026-07-01');
    expect(component.statutEtudiant()).toBe('ETUDIANT_PRINCIPAL');
    expect(component.heuresDejaPresteesDansAnnee()).toBe(100);
    expect(component.heuresContratEnCours()).toBe(200);
    expect(component.quotaAnnuelHeures()).toBe(600);
    expect(component.contratEcritSigne()).toBe(true);
    expect(component.dimonaStuDeclaree()).toBe(true);
    expect(component.cotisationsReduitesAppliquees()).toBe(true);
    expect(component.remunerationBruteHoraire()).toBe(13.5);
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
    component.quotaAnnuelHeures.set(null);
    expect(component.formValid()).toBe(false);
  });

  function fillForm() {
    component.dateDebutOccupation.set('2026-07-01');
    component.statutEtudiant.set('ETUDIANT_PRINCIPAL');
    component.heuresDejaPresteesDansAnnee.set(100);
    component.heuresContratEnCours.set(200);
    component.quotaAnnuelHeures.set(600);
    component.contratEcritSigne.set(true);
    component.dimonaStuDeclaree.set(true);
    component.cotisationsReduitesAppliquees.set(true);
    component.remunerationBruteHoraire.set(13.5);
  }

  it('formValid() : true avec 9 champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans date début', () => {
    fillForm();
    component.dateDebutOccupation.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans statutEtudiant', () => {
    fillForm();
    component.statutEtudiant.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec heures déjà prestées négatives', () => {
    fillForm();
    component.heuresDejaPresteesDansAnnee.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec heures contrat <= 0', () => {
    fillForm();
    component.heuresContratEnCours.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec quota <= 0', () => {
    fillForm();
    component.quotaAnnuelHeures.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans contratEcritSigne précisé', () => {
    fillForm();
    component.contratEcritSigne.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dimonaStuDeclaree précisé', () => {
    fillForm();
    component.dimonaStuDeclaree.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans cotisationsReduitesAppliquees précisé', () => {
    fillForm();
    component.cotisationsReduitesAppliquees.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec rémunération horaire négative', () => {
    fillForm();
    component.remunerationBruteHoraire.set(-1);
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
    expect(post.request.body.statutEtudiant).toBe('ETUDIANT_PRINCIPAL');
    expect(post.request.body.quotaAnnuelHeures).toBe(600);
    expect(post.request.body.heuresContratEnCours).toBe(200);
    expect(post.request.body.remunerationBruteHoraire).toBe(13.5);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Occupation étudiant analysée', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'quotaAnnuelHeures doit être strictement positif' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'quotaAnnuelHeures doit être strictement positif', 'Fermer', expect.anything(),
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

  it('verdict ELIGIBLE → label, verdict-ok, verified', () => {
    component.result.set(response({ verdict: 'ELIGIBLE' }));
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

  it('verdict FRAGILE_COTISATIONS_NON_REDUITES → label, verdict-warn, warning_amber', () => {
    component.result.set(response({
      verdict: 'FRAGILE_COTISATIONS_NON_REDUITES',
      cotisationsReduitesAppliquees: false,
      cotisationsConformes: false,
    }));
    expect(component.verdictLabel()).toContain('Cotisations');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('warning_amber');
  });

  it('verdict INELIGIBLE_STATUT_NON_ETUDIANT → label, verdict-critical, cancel', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_STATUT_NON_ETUDIANT',
      statutEtudiant: 'NON_ETUDIANT',
      statutEligible: false,
    }));
    expect(component.verdictLabel()).toContain('Statut');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('cancel');
  });

  it('verdict INELIGIBLE_QUOTA_DEPASSE → label, verdict-critical, cancel', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_QUOTA_DEPASSE',
      heuresDejaPresteesDansAnnee: 550,
      heuresContratEnCours: 200,
      quotaRespecte: false,
      heuresHorsQuota: 150,
      montantRedressementEstime: 847.62,
    }));
    expect(component.verdictLabel()).toContain('Quota');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('cancel');
  });

  it('verdict A_ANALYSER → label, verdict-neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_ANALYSER',
      statutEtudiant: 'VIDE_PEDAGOGIQUE_PROLONGE',
    }));
    expect(component.verdictLabel()).toContain('Zone grise');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('school');
  });

  // ============================================================
  // Badge dans header (6 cas)
  // ============================================================

  it('badge ELIGIBLE = vert "ÉLIGIBLE"', () => {
    expect(component.verdictBadge('ELIGIBLE')).toBe('ÉLIGIBLE');
    expect(component.verdictBadgeClass('ELIGIBLE')).toBe('badge-ok');
  });

  it('badge FRAGILE_CONTRAT_OU_DIMONA_MANQUANT = ambre', () => {
    expect(component.verdictBadge('FRAGILE_CONTRAT_OU_DIMONA_MANQUANT')).toBe('FORMALISME FRAGILE');
    expect(component.verdictBadgeClass('FRAGILE_CONTRAT_OU_DIMONA_MANQUANT')).toBe('badge-warn');
  });

  it('badge FRAGILE_COTISATIONS_NON_REDUITES = ambre', () => {
    expect(component.verdictBadge('FRAGILE_COTISATIONS_NON_REDUITES')).toBe('COTISATIONS HORS BARÈME');
    expect(component.verdictBadgeClass('FRAGILE_COTISATIONS_NON_REDUITES')).toBe('badge-warn');
  });

  it('badge INELIGIBLE_STATUT_NON_ETUDIANT = rouge', () => {
    expect(component.verdictBadge('INELIGIBLE_STATUT_NON_ETUDIANT')).toBe('INÉLIGIBLE (STATUT)');
    expect(component.verdictBadgeClass('INELIGIBLE_STATUT_NON_ETUDIANT')).toBe('badge-critical');
  });

  it('badge INELIGIBLE_QUOTA_DEPASSE = rouge', () => {
    expect(component.verdictBadge('INELIGIBLE_QUOTA_DEPASSE')).toBe('QUOTA DÉPASSÉ');
    expect(component.verdictBadgeClass('INELIGIBLE_QUOTA_DEPASSE')).toBe('badge-critical');
  });

  it('badge A_ANALYSER = neutre', () => {
    expect(component.verdictBadge('A_ANALYSER')).toBe('À ANALYSER');
    expect(component.verdictBadgeClass('A_ANALYSER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — verdict ELIGIBLE rendu (badge + 4 dimensions OK + heures restantes)
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
    // Heures restantes affichées
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('500');
  });

  it('INELIGIBLE_QUOTA_DEPASSE → badge rouge + heures hors quota + redressement affichés', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INELIGIBLE_QUOTA_DEPASSE',
      heuresDejaPresteesDansAnnee: 550,
      heuresContratEnCours: 200,
      quotaRespecte: false,
      heuresHorsQuota: 150,
      heuresRestantesAvantContrat: 50,
      montantRedressementEstime: 847.62,
      avertissement: 'Heures hors quota basculent au régime ordinaire',
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('QUOTA');
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('150');
    expect(text).toContain('847.62');
    expect(text).toContain('hors quota');
  });

  it('INELIGIBLE_STATUT_NON_ETUDIANT → badge rouge + dimension statut KO', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INELIGIBLE_STATUT_NON_ETUDIANT',
      statutEtudiant: 'NON_ETUDIANT',
      statutEligible: false,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('STATUT');
    const koRows = fixture.nativeElement.querySelectorAll('.dimension-row.dim-ko');
    expect(koRows.length).toBeGreaterThanOrEqual(1);
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellé statutEtudiant : 4 cas + null', () => {
    expect(component.statutEtudiantLabel('ETUDIANT_PRINCIPAL')).toContain('principal');
    expect(component.statutEtudiantLabel('INTERRUPTION_COURTE_MOINS_12_MOIS')).toContain('Interruption');
    expect(component.statutEtudiantLabel('VIDE_PEDAGOGIQUE_PROLONGE')).toContain('Vide');
    expect(component.statutEtudiantLabel('NON_ETUDIANT')).toContain('Non');
    expect(component.statutEtudiantLabel(null)).toBe('—');
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
      avertissement: 'Contrat écrit absent — risque requalification ONSS',
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
      synthese: 'Toutes conditions cumulatives réunies — étudiant jobiste éligible.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('étudiant jobiste éligible');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(EtudiantJobisteBeSectionComponent.getPrefillCount({})).toBe(0);
    expect(EtudiantJobisteBeSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateNaissanceSalarie: '2004-03-15',
        dateRuptureContrat: '2026-04-10',
        motifRupture: 'autre',
        anneesCarriereSalarie: 0,
        salaireBrutMensuel: 1200,
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

  it('toolId pour jurisprudence = etudiant-jobiste-be', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('etudiant-jobiste-be');
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
    component.onDateDebutOccupationChange('2026-07-01');
    expect(component.dateDebutOccupation()).toBe('2026-07-01');

    component.onStatutEtudiantChange('ETUDIANT_PRINCIPAL');
    expect(component.statutEtudiant()).toBe('ETUDIANT_PRINCIPAL');

    component.onHeuresDejaPresteesDansAnneeChange(100);
    expect(component.heuresDejaPresteesDansAnnee()).toBe(100);

    component.onHeuresDejaPresteesDansAnneeChange('150');
    expect(component.heuresDejaPresteesDansAnnee()).toBe(150);

    component.onHeuresDejaPresteesDansAnneeChange(null);
    expect(component.heuresDejaPresteesDansAnnee()).toBeNull();

    component.onHeuresDejaPresteesDansAnneeChange('');
    expect(component.heuresDejaPresteesDansAnnee()).toBeNull();

    component.onHeuresDejaPresteesDansAnneeChange('abc');
    expect(component.heuresDejaPresteesDansAnnee()).toBeNull();

    component.onHeuresContratEnCoursChange(200);
    expect(component.heuresContratEnCours()).toBe(200);

    component.onQuotaAnnuelHeuresChange(475);
    expect(component.quotaAnnuelHeures()).toBe(475);

    component.onContratEcritSigneChange(true);
    expect(component.contratEcritSigne()).toBe(true);

    component.onDimonaStuDeclareeChange(false);
    expect(component.dimonaStuDeclaree()).toBe(false);

    component.onCotisationsReduitesAppliqueesChange(true);
    expect(component.cotisationsReduitesAppliquees()).toBe(true);

    component.onRemunerationBruteHoraireChange(14.5);
    expect(component.remunerationBruteHoraire()).toBe(14.5);
  });

  // ============================================================
  // Static metadata F-177
  // ============================================================

  it('TOOL_LABEL et TOOL_ICON statiques exposés', () => {
    expect(EtudiantJobisteBeSectionComponent.TOOL_LABEL).toBe('ÉTUDIANT JOBISTE (BE)');
    expect(EtudiantJobisteBeSectionComponent.TOOL_ICON).toBe('school');
  });
});
