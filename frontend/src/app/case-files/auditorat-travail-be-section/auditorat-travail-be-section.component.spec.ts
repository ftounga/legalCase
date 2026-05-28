import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  AuditoratTravailBeSectionComponent,
} from './auditorat-travail-be-section.component';
import {
  AuditoratTravailBeResponse,
} from '../../core/models/auditorat-travail-be.model';

describe('AuditoratTravailBeSectionComponent', () => {
  let component: AuditoratTravailBeSectionComponent;
  let fixture: ComponentFixture<AuditoratTravailBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/auditorat-travail-be';

  /**
   * Construit une réponse backend par défaut — verdict
   * SAISINE_AUDITORAT_RECOMMANDEE (infraction pénale sociale,
   * plainte directe, urgence sécurité).
   */
  function response(
    overrides: Partial<AuditoratTravailBeResponse> = {},
  ): AuditoratTravailBeResponse {
    return {
      caseFileId: 'case-1',
      natureFait: 'INFRACTION_PENALE_SOCIALE',
      modeSaisineEnvisage: 'PLAINTE_DIRECTE',
      dateFaits: '2026-03-15',
      faitsPrescrits: false,
      inspectionDejaSaisie: true,
      plainteCivileDeposee: false,
      urgenceSecuritePersonnes: true,
      recoursPenalEnvisage: true,
      employeurPersonneMorale: true,
      verdict: 'SAISINE_AUDITORAT_RECOMMANDEE',
      modeSaisineRecommande: 'PLAINTE_DIRECTE',
      constitutionPartieCivileRecommandee: true,
      inspectionPrealableRequise: false,
      voieCivileConcurrente: true,
      prescriptionBloquante: false,
      urgenceSignalee: true,
      unaViaApplicable: true,
      avisAuditeurObligatoireCivil: false,
      raison: 'INFRACTION_PENALE_SOCIALE_CARACTERISEE',
      synthese: 'Infraction pénale sociale caractérisée — saisine directe auditorat recommandée.',
      baseJuridique: 'Code judiciaire art. 138bis + Code d\'instruction criminelle art. 24 + Loi 06/06/2010 art. 73-78.',
      avertissement: 'Règle « Una Via » art. 73 C. pén. soc. — l\'auditeur choisit voie pénale OU administrative.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        AuditoratTravailBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AuditoratTravailBeSectionComponent);
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
    expect(banner.textContent).toContain('auditorat');
  });

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 → hydrate result + mode résultat + rehydrate form pour édition', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('SAISINE_AUDITORAT_RECOMMANDEE');
    expect(component.showForm()).toBe(false);
    expect(component.natureFait()).toBe('INFRACTION_PENALE_SOCIALE');
    expect(component.modeSaisineEnvisage()).toBe('PLAINTE_DIRECTE');
    expect(component.faitsPrescrits()).toBe(false);
    expect(component.inspectionDejaSaisie()).toBe(true);
    expect(component.plainteCivileDeposee()).toBe(false);
    expect(component.urgenceSecuritePersonnes()).toBe(true);
    expect(component.recoursPenalEnvisage()).toBe(true);
    expect(component.employeurPersonneMorale()).toBe(true);
    expect(component.dateFaits()).toBe('2026-03-15');
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
    component.natureFait.set('INFRACTION_PENALE_SOCIALE');
    component.modeSaisineEnvisage.set('PLAINTE_DIRECTE');
    component.faitsPrescrits.set(false);
    component.inspectionDejaSaisie.set(true);
    component.plainteCivileDeposee.set(false);
    component.urgenceSecuritePersonnes.set(true);
    component.recoursPenalEnvisage.set(true);
    component.employeurPersonneMorale.set(true);
  }

  it('formValid() : true avec les champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans natureFait', () => {
    fillForm();
    component.natureFait.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans modeSaisineEnvisage', () => {
    fillForm();
    component.modeSaisineEnvisage.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans faitsPrescrits', () => {
    fillForm();
    component.faitsPrescrits.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans inspectionDejaSaisie', () => {
    fillForm();
    component.inspectionDejaSaisie.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans plainteCivileDeposee', () => {
    fillForm();
    component.plainteCivileDeposee.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans urgenceSecuritePersonnes', () => {
    fillForm();
    component.urgenceSecuritePersonnes.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans recoursPenalEnvisage', () => {
    fillForm();
    component.recoursPenalEnvisage.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans employeurPersonneMorale', () => {
    fillForm();
    component.employeurPersonneMorale.set(null);
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
    expect(post.request.body.natureFait).toBe('INFRACTION_PENALE_SOCIALE');
    expect(post.request.body.modeSaisineEnvisage).toBe('PLAINTE_DIRECTE');
    expect(post.request.body.urgenceSecuritePersonnes).toBe(true);
    expect(post.request.body.employeurPersonneMorale).toBe(true);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse auditorat du travail calculée', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'natureFait est requis' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'natureFait est requis', 'Fermer', expect.anything(),
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

  it('verdict SAISINE_AUDITORAT_RECOMMANDEE → label, verdict-critical, gavel', () => {
    component.result.set(response());
    expect(component.verdictLabel()).toContain('Saisine de l\'auditorat');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('gavel');
  });

  it('verdict DENONCIATION_INSPECTION_PREALABLE → verdict-warn, fact_check', () => {
    component.result.set(response({
      verdict: 'DENONCIATION_INSPECTION_PREALABLE',
      natureFait: 'TRAVAIL_NON_DECLARE_SUSPECTE',
      modeSaisineRecommande: 'SIGNALEMENT_INSPECTION_SOCIALE',
      inspectionPrealableRequise: true,
      urgenceSignalee: false,
    }));
    expect(component.verdictLabel()).toContain('inspection sociale');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('fact_check');
  });

  it('verdict SAISINE_NON_PERTINENTE → verdict-info, info', () => {
    component.result.set(response({
      verdict: 'SAISINE_NON_PERTINENTE',
      natureFait: 'LITIGE_CIVIL_PUR',
      modeSaisineRecommande: 'VOIE_CIVILE_PARALLELE',
      constitutionPartieCivileRecommandee: false,
      unaViaApplicable: false,
    }));
    expect(component.verdictLabel()).toContain('Saisine non pertinente');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('info');
  });

  it('verdict A_QUALIFIER → verdict-neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_QUALIFIER',
      natureFait: 'AUTRE',
      modeSaisineRecommande: 'INDETERMINE',
      constitutionPartieCivileRecommandee: false,
      urgenceSignalee: false,
      unaViaApplicable: false,
    }));
    expect(component.verdictLabel()).toContain('Qualification ouverte');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('account_balance');
  });

  // ============================================================
  // Badge dans header (4 cas)
  // ============================================================

  it('badge SAISINE_AUDITORAT_RECOMMANDEE = critical', () => {
    expect(component.verdictBadge('SAISINE_AUDITORAT_RECOMMANDEE')).toBe('SAISIR AUDITORAT');
    expect(component.verdictBadgeClass('SAISINE_AUDITORAT_RECOMMANDEE')).toBe('badge-critical');
  });

  it('badge DENONCIATION_INSPECTION_PREALABLE = ambre', () => {
    expect(component.verdictBadge('DENONCIATION_INSPECTION_PREALABLE')).toBe('INSPECTION PRÉALABLE');
    expect(component.verdictBadgeClass('DENONCIATION_INSPECTION_PREALABLE')).toBe('badge-warn');
  });

  it('badge SAISINE_NON_PERTINENTE = info', () => {
    expect(component.verdictBadge('SAISINE_NON_PERTINENTE')).toBe('NON PERTINENT');
    expect(component.verdictBadgeClass('SAISINE_NON_PERTINENTE')).toBe('badge-info');
  });

  it('badge A_QUALIFIER = neutre', () => {
    expect(component.verdictBadge('A_QUALIFIER')).toBe('À QUALIFIER');
    expect(component.verdictBadgeClass('A_QUALIFIER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — verdict rendu (badge + ventilations)
  // ============================================================

  it('SAISINE_AUDITORAT_RECOMMANDEE → badge critique + ventilations rendues', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('SAISIR AUDITORAT');
    const ventilations = fixture.nativeElement.querySelectorAll('.ventilation-row');
    // 8 rangs : mode reco + 7 drapeaux (constitution PC, inspection préalable,
    // voie civile concurrente, prescription bloquante, urgence signalée,
    // Una Via, avis obligatoire civil).
    expect(ventilations.length).toBe(8);
  });

  it('DENONCIATION_INSPECTION_PREALABLE → badge warn + bannière warn', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'DENONCIATION_INSPECTION_PREALABLE',
      natureFait: 'TRAVAIL_NON_DECLARE_SUSPECTE',
      modeSaisineRecommande: 'SIGNALEMENT_INSPECTION_SOCIALE',
      inspectionPrealableRequise: true,
      urgenceSignalee: false,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-warn');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('INSPECTION PRÉALABLE');
    const banner = fixture.nativeElement.querySelector('.result-banner.verdict-warn');
    expect(banner).not.toBeNull();
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellé natureFait : 8 cas + null', () => {
    expect(component.natureFaitLabel('INFRACTION_PENALE_SOCIALE')).toContain('Infraction');
    expect(component.natureFaitLabel('ACCIDENT_TRAVAIL_GRAVE')).toContain('Accident');
    expect(component.natureFaitLabel('TRAVAIL_NON_DECLARE_SUSPECTE')).toContain('Travail non');
    expect(component.natureFaitLabel('DISCRIMINATION_PENALE')).toContain('Discrimination');
    expect(component.natureFaitLabel('HARCELEMENT_PENAL')).toContain('Harc');
    expect(component.natureFaitLabel('ENTRAVE_INSPECTION')).toContain('Entrave');
    expect(component.natureFaitLabel('LITIGE_CIVIL_PUR')).toContain('Litige');
    expect(component.natureFaitLabel('AUTRE')).toContain('Autre');
    expect(component.natureFaitLabel(null)).toBe('—');
  });

  it('libellé modeSaisine : 5 cas + null', () => {
    expect(component.modeSaisineLabel('PLAINTE_DIRECTE')).toContain('Plainte');
    expect(component.modeSaisineLabel('DENONCIATION_SIMPLE')).toContain('nonciation');
    expect(component.modeSaisineLabel('SIGNALEMENT_INSPECTION_SOCIALE')).toContain('Signalement');
    expect(component.modeSaisineLabel('VOIE_CIVILE_PARALLELE')).toContain('civile');
    expect(component.modeSaisineLabel('INDETERMINE')).toContain('non encore');
    expect(component.modeSaisineLabel(null)).toBe('—');
  });

  // ============================================================
  // Formats
  // ============================================================

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
      avertissement: 'Règle « Una Via » art. 73 C. pén. soc.',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('Una Via');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Infraction pénale sociale caractérisée.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('Infraction');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(AuditoratTravailBeSectionComponent.getPrefillCount({})).toBe(0);
    expect(AuditoratTravailBeSectionComponent.getPrefillCount({
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

  it('toolId pour jurisprudence = auditorat-travail-be', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('auditorat-travail-be');
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
    component.onNatureFaitChange('ACCIDENT_TRAVAIL_GRAVE');
    expect(component.natureFait()).toBe('ACCIDENT_TRAVAIL_GRAVE');

    component.onModeSaisineEnvisageChange('DENONCIATION_SIMPLE');
    expect(component.modeSaisineEnvisage()).toBe('DENONCIATION_SIMPLE');

    component.onDateFaitsChange('2026-03-15');
    expect(component.dateFaits()).toBe('2026-03-15');

    component.onFaitsPrescritsChange(false);
    expect(component.faitsPrescrits()).toBe(false);

    component.onInspectionDejaSaisieChange(true);
    expect(component.inspectionDejaSaisie()).toBe(true);

    component.onPlainteCivileDeposeeChange(false);
    expect(component.plainteCivileDeposee()).toBe(false);

    component.onUrgenceSecuritePersonnesChange(true);
    expect(component.urgenceSecuritePersonnes()).toBe(true);

    component.onRecoursPenalEnvisageChange(true);
    expect(component.recoursPenalEnvisage()).toBe(true);

    component.onEmployeurPersonneMoraleChange(false);
    expect(component.employeurPersonneMorale()).toBe(false);
  });
});
