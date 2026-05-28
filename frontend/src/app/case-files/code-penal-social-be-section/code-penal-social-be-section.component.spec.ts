import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  CodePenalSocialBeSectionComponent,
} from './code-penal-social-be-section.component';
import {
  CodePenalSocialBeResponse,
} from '../../core/models/code-penal-social-be.model';

describe('CodePenalSocialBeSectionComponent', () => {
  let component: CodePenalSocialBeSectionComponent;
  let fixture: ComponentFixture<CodePenalSocialBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/code-penal-social-be';

  /**
   * Construit une réponse backend par défaut — verdict SANCTION_NIVEAU_4
   * (travail non déclaré, personne morale, 3 travailleurs).
   */
  function response(
    overrides: Partial<CodePenalSocialBeResponse> = {},
  ): CodePenalSocialBeResponse {
    return {
      caseFileId: 'case-1',
      typeInfraction: 'TRAVAIL_NON_DECLARE',
      niveauPropose: null,
      dateFaits: '2026-03-15',
      nombreTravailleursConcernes: 3,
      personneMorale: true,
      recidiveDansLAn: false,
      prevenuPreposeOuMandataire: false,
      elementMoralIntentionnel: true,
      verdict: 'SANCTION_NIVEAU_4',
      niveauSanction: 4,
      amendeAdminMin: 300,
      amendeAdminMax: 3000,
      amendePenaleMin: 600,
      amendePenaleMax: 6000,
      emprisonnementApplicable: true,
      emprisonnementMinMois: 6,
      emprisonnementMaxMois: 36,
      multiplicationParTravailleur: true,
      majorationPersonneMorale: true,
      majorationRecidive: false,
      raison: 'TRAVAIL_NON_DECLARE_NIVEAU_4',
      synthese: 'Infraction de niveau 4 — travail non déclaré (art. 181 C. pén. soc.).',
      baseJuridique: 'Loi du 06/06/2010 introduisant le Code pénal social, art. 101-103 + 181.',
      avertissement: 'Indexation +0,90 décimes additionnels depuis le 01/01/2025 (AR 27/12/2024).',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        CodePenalSocialBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(CodePenalSocialBeSectionComponent);
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
    expect(banner.textContent).toContain('Una Via');
  });

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 → hydrate result + mode résultat + rehydrate form pour édition', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('SANCTION_NIVEAU_4');
    expect(component.showForm()).toBe(false);
    expect(component.typeInfraction()).toBe('TRAVAIL_NON_DECLARE');
    expect(component.nombreTravailleursConcernes()).toBe(3);
    expect(component.personneMorale()).toBe(true);
    expect(component.recidiveDansLAn()).toBe(false);
    expect(component.prevenuPreposeOuMandataire()).toBe(false);
    expect(component.elementMoralIntentionnel()).toBe(true);
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
    component.typeInfraction.set('TRAVAIL_NON_DECLARE');
    component.nombreTravailleursConcernes.set(3);
    component.personneMorale.set(true);
    component.recidiveDansLAn.set(false);
    component.prevenuPreposeOuMandataire.set(false);
    component.elementMoralIntentionnel.set(true);
  }

  it('formValid() : true avec les champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans typeInfraction', () => {
    fillForm();
    component.typeInfraction.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans nombreTravailleursConcernes', () => {
    fillForm();
    component.nombreTravailleursConcernes.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec nombreTravailleurs négatif', () => {
    fillForm();
    component.nombreTravailleursConcernes.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec nombreTravailleurs > 1M', () => {
    fillForm();
    component.nombreTravailleursConcernes.set(1000001);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans personneMorale', () => {
    fillForm();
    component.personneMorale.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans recidiveDansLAn', () => {
    fillForm();
    component.recidiveDansLAn.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans prevenuPreposeOuMandataire', () => {
    fillForm();
    component.prevenuPreposeOuMandataire.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans elementMoralIntentionnel', () => {
    fillForm();
    component.elementMoralIntentionnel.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true avec niveauPropose valide 1', () => {
    fillForm();
    component.niveauPropose.set(1);
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : true avec niveauPropose valide 4', () => {
    fillForm();
    component.niveauPropose.set(4);
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false avec niveauPropose = 0', () => {
    fillForm();
    component.niveauPropose.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec niveauPropose = 5', () => {
    fillForm();
    component.niveauPropose.set(5);
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
    expect(post.request.body.typeInfraction).toBe('TRAVAIL_NON_DECLARE');
    expect(post.request.body.nombreTravailleursConcernes).toBe(3);
    expect(post.request.body.personneMorale).toBe(true);
    expect(post.request.body.elementMoralIntentionnel).toBe(true);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse Code pénal social calculée', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'nombreTravailleursConcernes plafonné à 1 000 000' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'nombreTravailleursConcernes plafonné à 1 000 000', 'Fermer', expect.anything(),
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
  // Verdict — label / classe / icône (5 cas)
  // ============================================================

  it('verdict SANCTION_NIVEAU_4 → label, verdict-critical, gavel', () => {
    component.result.set(response());
    expect(component.verdictLabel()).toContain('niveau 4');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('gavel');
  });

  it('verdict SANCTION_NIVEAU_3 → verdict-warn, report', () => {
    component.result.set(response({
      verdict: 'SANCTION_NIVEAU_3',
      typeInfraction: 'DEPASSEMENT_TEMPS_TRAVAIL',
      niveauSanction: 3,
      amendeAdminMin: 100,
      amendeAdminMax: 1000,
      amendePenaleMin: 100,
      amendePenaleMax: 1000,
      emprisonnementApplicable: false,
      emprisonnementMinMois: 0,
      emprisonnementMaxMois: 0,
    }));
    expect(component.verdictLabel()).toContain('niveau 3');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('report');
  });

  it('verdict SANCTION_NIVEAU_2 → verdict-warn, warning', () => {
    component.result.set(response({
      verdict: 'SANCTION_NIVEAU_2',
      typeInfraction: 'NON_PAIEMENT_REMUNERATION',
      niveauSanction: 2,
      amendeAdminMin: 50,
      amendeAdminMax: 500,
      amendePenaleMin: 50,
      amendePenaleMax: 500,
      emprisonnementApplicable: false,
      emprisonnementMinMois: 0,
      emprisonnementMaxMois: 0,
    }));
    expect(component.verdictLabel()).toContain('niveau 2');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('warning');
  });

  it('verdict SANCTION_NIVEAU_1 → verdict-info, info', () => {
    component.result.set(response({
      verdict: 'SANCTION_NIVEAU_1',
      typeInfraction: 'AUTRE_QUALIFICATION',
      niveauPropose: 1,
      niveauSanction: 1,
      amendeAdminMin: 10,
      amendeAdminMax: 100,
      amendePenaleMin: 0,
      amendePenaleMax: 0,
      emprisonnementApplicable: false,
      emprisonnementMinMois: 0,
      emprisonnementMaxMois: 0,
    }));
    expect(component.verdictLabel()).toContain('niveau 1');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('info');
  });

  it('verdict A_QUALIFIER → verdict-neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_QUALIFIER',
      typeInfraction: 'AUTRE_QUALIFICATION',
      niveauPropose: null,
      niveauSanction: 0,
      amendeAdminMin: 0,
      amendeAdminMax: 0,
      amendePenaleMin: 0,
      amendePenaleMax: 0,
      emprisonnementApplicable: false,
      emprisonnementMinMois: 0,
      emprisonnementMaxMois: 0,
      multiplicationParTravailleur: false,
      majorationPersonneMorale: false,
    }));
    expect(component.verdictLabel()).toContain('Qualification ouverte');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('gavel');
  });

  // ============================================================
  // Badge dans header (5 cas)
  // ============================================================

  it('badge SANCTION_NIVEAU_1 = info', () => {
    expect(component.verdictBadge('SANCTION_NIVEAU_1')).toBe('NIVEAU 1');
    expect(component.verdictBadgeClass('SANCTION_NIVEAU_1')).toBe('badge-info');
  });

  it('badge SANCTION_NIVEAU_2 = ambre', () => {
    expect(component.verdictBadge('SANCTION_NIVEAU_2')).toBe('NIVEAU 2');
    expect(component.verdictBadgeClass('SANCTION_NIVEAU_2')).toBe('badge-warn');
  });

  it('badge SANCTION_NIVEAU_3 = ambre', () => {
    expect(component.verdictBadge('SANCTION_NIVEAU_3')).toBe('NIVEAU 3');
    expect(component.verdictBadgeClass('SANCTION_NIVEAU_3')).toBe('badge-warn');
  });

  it('badge SANCTION_NIVEAU_4 = rouge', () => {
    expect(component.verdictBadge('SANCTION_NIVEAU_4')).toBe('NIVEAU 4');
    expect(component.verdictBadgeClass('SANCTION_NIVEAU_4')).toBe('badge-critical');
  });

  it('badge A_QUALIFIER = neutre', () => {
    expect(component.verdictBadge('A_QUALIFIER')).toBe('À QUALIFIER');
    expect(component.verdictBadgeClass('A_QUALIFIER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — verdict rendu (badge + ventilations)
  // ============================================================

  it('SANCTION_NIVEAU_4 → badge rouge + ventilations avec emprisonnement', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('NIVEAU 4');
    const ventilations = fixture.nativeElement.querySelectorAll('.ventilation-row');
    // 3 (niveau, amendeAdmin, amendePenale) + 1 emprisonnement + 3 (×trav, ×PM, récidive) = 7.
    expect(ventilations.length).toBe(7);
  });

  it('SANCTION_NIVEAU_2 → badge warn + 6 ventilations (sans emprisonnement)', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'SANCTION_NIVEAU_2',
      typeInfraction: 'NON_PAIEMENT_REMUNERATION',
      niveauSanction: 2,
      amendeAdminMin: 50,
      amendeAdminMax: 500,
      amendePenaleMin: 50,
      amendePenaleMax: 500,
      emprisonnementApplicable: false,
      emprisonnementMinMois: 0,
      emprisonnementMaxMois: 0,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-warn');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('NIVEAU 2');
    const banner = fixture.nativeElement.querySelector('.result-banner.verdict-warn');
    expect(banner).not.toBeNull();
    const ventilations = fixture.nativeElement.querySelectorAll('.ventilation-row');
    expect(ventilations.length).toBe(6);
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellé typeInfraction : 14 cas + null', () => {
    expect(component.typeInfractionLabel('TRAVAIL_NON_DECLARE')).toContain('Travail non déclaré');
    expect(component.typeInfractionLabel('ABSENCE_DIMONA')).toContain('Dimona');
    expect(component.typeInfractionLabel('ABSENCE_DMFA')).toContain('DmfA');
    expect(component.typeInfractionLabel('NON_PAIEMENT_REMUNERATION')).toContain('Non-paiement');
    expect(component.typeInfractionLabel('DEPASSEMENT_TEMPS_TRAVAIL')).toContain('Dépassement');
    expect(component.typeInfractionLabel('INFRACTION_BIEN_ETRE')).toContain('bien-être');
    expect(component.typeInfractionLabel('ACCIDENT_GRAVE_NON_DECLARE')).toContain('Accident');
    expect(component.typeInfractionLabel('DISCRIMINATION_EMPLOI')).toContain('Discrimination');
    expect(component.typeInfractionLabel('ENTRAVE_INSPECTION_SOCIALE')).toContain('Entrave');
    expect(component.typeInfractionLabel('FAUX_SOCIAL')).toContain('Faux');
    expect(component.typeInfractionLabel('ABSENCE_REGISTRE_PERSONNEL')).toContain('registre');
    expect(component.typeInfractionLabel('INFRACTION_INTERIM')).toContain('intérimaire');
    expect(component.typeInfractionLabel('INFRACTION_DETACHEMENT')).toContain('détachement');
    expect(component.typeInfractionLabel('AUTRE_QUALIFICATION')).toContain('Autre');
    expect(component.typeInfractionLabel(null)).toBe('—');
  });

  // ============================================================
  // Formats
  // ============================================================

  it('formatEuros : entiers + €', () => {
    expect(component.formatEuros(3000)).toContain('3');
    expect(component.formatEuros(3000)).toContain('€');
    expect(component.formatEuros(null)).toBe('—');
    expect(component.formatEuros(undefined)).toBe('—');
  });

  it('formatBoolean — Oui / Non / —', () => {
    expect(component.formatBoolean(true)).toBe('Oui');
    expect(component.formatBoolean(false)).toBe('Non');
    expect(component.formatBoolean(null)).toBe('—');
    expect(component.formatBoolean(undefined)).toBe('—');
  });

  it('formatMoisRange : 0/0 → —, identique → "X mois", range → "X mois - Y mois"', () => {
    expect(component.formatMoisRange(0, 0)).toBe('—');
    expect(component.formatMoisRange(6, 6)).toBe('6 mois');
    expect(component.formatMoisRange(6, 36)).toContain('6 mois');
    expect(component.formatMoisRange(6, 36)).toContain('36 mois');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non vide → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      avertissement: 'Indexation +0,90 décimes additionnels depuis le 01/01/2025.',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('Indexation');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Travail non déclaré niveau 4.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('Travail');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(CodePenalSocialBeSectionComponent.getPrefillCount({})).toBe(0);
    expect(CodePenalSocialBeSectionComponent.getPrefillCount({
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

  it('toolId pour jurisprudence = code-penal-social-be', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('code-penal-social-be');
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
    component.onTypeInfractionChange('TRAVAIL_NON_DECLARE');
    expect(component.typeInfraction()).toBe('TRAVAIL_NON_DECLARE');

    component.onNiveauProposeChange('3');
    expect(component.niveauPropose()).toBe(3);

    component.onDateFaitsChange('2026-03-15');
    expect(component.dateFaits()).toBe('2026-03-15');

    component.onNombreTravailleursConcernesChange('5');
    expect(component.nombreTravailleursConcernes()).toBe(5);

    component.onPersonneMoraleChange(true);
    expect(component.personneMorale()).toBe(true);

    component.onRecidiveDansLAnChange(false);
    expect(component.recidiveDansLAn()).toBe(false);

    component.onPrevenuPreposeOuMandataireChange(true);
    expect(component.prevenuPreposeOuMandataire()).toBe(true);

    component.onElementMoralIntentionnelChange(true);
    expect(component.elementMoralIntentionnel()).toBe(true);

    component.onNombreTravailleursConcernesChange('');
    expect(component.nombreTravailleursConcernes()).toBeNull();

    component.onNiveauProposeChange(null);
    expect(component.niveauPropose()).toBeNull();
  });
});
