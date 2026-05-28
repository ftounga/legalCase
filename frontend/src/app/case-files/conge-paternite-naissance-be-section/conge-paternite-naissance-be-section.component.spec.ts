import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  CongePaterniteNaissanceBeSectionComponent,
} from './conge-paternite-naissance-be-section.component';
import {
  CongePaterniteNaissanceBeResponse,
} from '../../core/models/conge-paternite-naissance-be.model';

describe('CongePaterniteNaissanceBeSectionComponent', () => {
  let component: CongePaterniteNaissanceBeSectionComponent;
  let fixture: ComponentFixture<CongePaterniteNaissanceBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/conge-paternite-naissance-be';

  /**
   * Reponse backend par defaut — verdict ELIGIBLE_CONGE_OUVERT (cas
   * nominal : salarie, pere biologique, naissance survenue, droit
   * ouvert avec 20 jours ouvrables applicables).
   */
  function response(
    overrides: Partial<CongePaterniteNaissanceBeResponse> = {},
  ): CongePaterniteNaissanceBeResponse {
    return {
      caseFileId: 'case-1',
      statutTravailleur: 'SALARIE',
      lienFiliation: 'PERE_BIOLOGIQUE',
      etapeProcedure: 'NAISSANCE_SURVENUE',
      filiationEtablie: true,
      contratTravailEnCours: true,
      notificationEmployeurFaite: false,
      dateNaissance: '2026-03-12',
      dateNotificationEmployeur: null,
      joursDejaPrisOuvrables: 0,
      dateFinPriseEffective: null,
      verdict: 'ELIGIBLE_CONGE_OUVERT',
      dureeApplicableJoursOuvrables: 20,
      joursRestantsAPrendre: 20,
      echeanceQuatreMoisPostNaissance: '2026-07-12',
      finProtectionLicenciement: null,
      protectionLicenciementActive: false,
      indemniteEmployeurJoursCent: 3,
      indemniteMutuelleJoursQuatreVingtDeux: 17,
      raison: 'ELIGIBLE',
      synthese: 'Droit ouvert — 20 jours ouvrables applicables (naissance >= 01/01/2023).',
      baseJuridique: 'Loi du 03/07/1978 art. 30 paragr. 2 + Loi du 07/04/2023.',
      avertissement: 'Notifier l\'employeur avec justificatif (acte de naissance).',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        CongePaterniteNaissanceBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(CongePaterniteNaissanceBeSectionComponent);
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

  it('BELGIQUE => isAvailable() true, GET au ngOnInit', () => {
    expect(component.isAvailable()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('FRANCE => isAvailable() false, aucun GET au ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isAvailable()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
  });

  it('FRANCE => banniere reserve droit BE + masquage form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.empty-result');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('belge');
    expect(banner.textContent).toContain('CPAM');
  });

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 => hydrate result + mode resultat + rehydrate form', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('ELIGIBLE_CONGE_OUVERT');
    expect(component.showForm()).toBe(false);
    expect(component.statutTravailleur()).toBe('SALARIE');
    expect(component.lienFiliation()).toBe('PERE_BIOLOGIQUE');
    expect(component.etapeProcedure()).toBe('NAISSANCE_SURVENUE');
    expect(component.filiationEtablie()).toBe(true);
    expect(component.contratTravailEnCours()).toBe(true);
    expect(component.dateNaissance()).toBe('2026-03-12');
  });

  it('GET 404 => reste en mode formulaire vierge', () => {
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
    component.statutTravailleur.set('SALARIE');
    component.lienFiliation.set('PERE_BIOLOGIQUE');
    component.etapeProcedure.set('NAISSANCE_SURVENUE');
    component.filiationEtablie.set(true);
    component.contratTravailEnCours.set(true);
    component.notificationEmployeurFaite.set(false);
    component.dateNaissance.set('2026-03-12');
    component.dateNotificationEmployeur.set(null);
    component.joursDejaPrisOuvrables.set(0);
    component.dateFinPriseEffective.set(null);
  }

  it('formValid() : true avec les 3 champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans statut travailleur', () => {
    fillForm();
    component.statutTravailleur.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans lien filiation', () => {
    fillForm();
    component.lienFiliation.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans etape procedure', () => {
    fillForm();
    component.etapeProcedure.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec joursDejaPrisOuvrables negatif', () => {
    fillForm();
    component.joursDejaPrisOuvrables.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true en AVANT_NAISSANCE sans date naissance', () => {
    fillForm();
    component.etapeProcedure.set('AVANT_NAISSANCE');
    component.dateNaissance.set(null);
    expect(component.formValid()).toBe(true);
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  it('calculate() => POST + snack succes', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.statutTravailleur).toBe('SALARIE');
    expect(post.request.body.lienFiliation).toBe('PERE_BIOLOGIQUE');
    expect(post.request.body.etapeProcedure).toBe('NAISSANCE_SURVENUE');
    expect(post.request.body.dateNaissance).toBe('2026-03-12');
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse conge paternite / naissance calculee', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 => snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'joursDejaPrisOuvrables negatif' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'joursDejaPrisOuvrables negatif', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : erreur 500 => snack generique', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({}, { status: 500, statusText: 'Server Error' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Une erreur est survenue. Veuillez reessayer.', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : erreur 404 => snack "Dossier introuvable"', () => {
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

  it('calculate() : FRANCE => snack indispo + pas de POST', () => {
    component.workspaceCountry = 'FRANCE';
    fillForm();
    component.calculate();
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Outil indisponible pour ce workspace', 'Fermer', expect.anything(),
    );
    httpMock.expectNone((r) => r.url === BASE_URL && r.method === 'POST');
  });

  // ============================================================
  // Verdict — label / classe / icone (8 cas)
  // ============================================================

  it('verdict ELIGIBLE_CONGE_OUVERT => info, check_circle', () => {
    component.result.set(response());
    expect(component.verdictLabel()).toContain('OUVERT');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('check_circle');
  });

  it('verdict CONGE_EN_COURS_PROTECTION_ACTIVE => info, shield', () => {
    component.result.set(response({
      verdict: 'CONGE_EN_COURS_PROTECTION_ACTIVE',
      etapeProcedure: 'CONGE_EN_COURS_DE_PRISE',
      protectionLicenciementActive: true,
    }));
    expect(component.verdictLabel()).toContain('en cours de prise');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('shield');
  });

  it('verdict CONGE_PRIS_PROTECTION_RESIDUELLE => info, verified', () => {
    component.result.set(response({
      verdict: 'CONGE_PRIS_PROTECTION_RESIDUELLE',
      etapeProcedure: 'CONGE_PRIS_ENTIEREMENT',
    }));
    expect(component.verdictLabel()).toContain('entierement pris');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict INELIGIBLE_STATUT_NON_COUVERT => critical, block', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_STATUT_NON_COUVERT',
      statutTravailleur: 'INDEPENDANT',
    }));
    expect(component.verdictLabel()).toContain('hors champ');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('block');
  });

  it('verdict INELIGIBLE_FILIATION_NON_ETABLIE => critical, family_restroom', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_FILIATION_NON_ETABLIE',
      filiationEtablie: false,
    }));
    expect(component.verdictLabel()).toContain('parental non etabli');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('family_restroom');
  });

  it('verdict DROIT_PERDU_DELAI_DEPASSE => critical, event_busy', () => {
    component.result.set(response({
      verdict: 'DROIT_PERDU_DELAI_DEPASSE',
      etapeProcedure: 'DELAI_QUATRE_MOIS_DEPASSE',
    }));
    expect(component.verdictLabel()).toContain('DEPASSE');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('event_busy');
  });

  it('verdict INELIGIBLE_NAISSANCE_FUTURE => warn, hourglass_empty', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_NAISSANCE_FUTURE',
      etapeProcedure: 'AVANT_NAISSANCE',
    }));
    expect(component.verdictLabel()).toContain('Naissance future');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('hourglass_empty');
  });

  it('verdict A_QUALIFIER => neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_QUALIFIER',
    }));
    expect(component.verdictLabel()).toContain('Inputs ambigus');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null => labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('child_care');
  });

  // ============================================================
  // Badge dans header (8 cas)
  // ============================================================

  it('badge ELIGIBLE_CONGE_OUVERT = info', () => {
    expect(component.verdictBadge('ELIGIBLE_CONGE_OUVERT')).toBe('CONGE OUVERT');
    expect(component.verdictBadgeClass('ELIGIBLE_CONGE_OUVERT')).toBe('badge-info');
  });

  it('badge CONGE_EN_COURS_PROTECTION_ACTIVE = info', () => {
    expect(component.verdictBadge('CONGE_EN_COURS_PROTECTION_ACTIVE')).toBe('PRISE EN COURS');
    expect(component.verdictBadgeClass('CONGE_EN_COURS_PROTECTION_ACTIVE')).toBe('badge-info');
  });

  it('badge CONGE_PRIS_PROTECTION_RESIDUELLE = info', () => {
    expect(component.verdictBadge('CONGE_PRIS_PROTECTION_RESIDUELLE')).toBe('CONGE PRIS');
    expect(component.verdictBadgeClass('CONGE_PRIS_PROTECTION_RESIDUELLE')).toBe('badge-info');
  });

  it('badge INELIGIBLE_STATUT_NON_COUVERT = critical', () => {
    expect(component.verdictBadge('INELIGIBLE_STATUT_NON_COUVERT')).toBe('STATUT HORS CHAMP');
    expect(component.verdictBadgeClass('INELIGIBLE_STATUT_NON_COUVERT')).toBe('badge-critical');
  });

  it('badge INELIGIBLE_FILIATION_NON_ETABLIE = critical', () => {
    expect(component.verdictBadge('INELIGIBLE_FILIATION_NON_ETABLIE')).toBe('FILIATION ABSENTE');
    expect(component.verdictBadgeClass('INELIGIBLE_FILIATION_NON_ETABLIE')).toBe('badge-critical');
  });

  it('badge DROIT_PERDU_DELAI_DEPASSE = critical', () => {
    expect(component.verdictBadge('DROIT_PERDU_DELAI_DEPASSE')).toBe('DELAI DEPASSE');
    expect(component.verdictBadgeClass('DROIT_PERDU_DELAI_DEPASSE')).toBe('badge-critical');
  });

  it('badge INELIGIBLE_NAISSANCE_FUTURE = warn', () => {
    expect(component.verdictBadge('INELIGIBLE_NAISSANCE_FUTURE')).toBe('NAISSANCE FUTURE');
    expect(component.verdictBadgeClass('INELIGIBLE_NAISSANCE_FUTURE')).toBe('badge-warn');
  });

  it('badge A_QUALIFIER = neutre', () => {
    expect(component.verdictBadge('A_QUALIFIER')).toBe('A QUALIFIER');
    expect(component.verdictBadgeClass('A_QUALIFIER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — rendu verdict
  // ============================================================

  it('ELIGIBLE_CONGE_OUVERT => badge info rendu', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-info');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('CONGE OUVERT');
  });

  // ============================================================
  // Libelles
  // ============================================================

  it('libelle statut travailleur : 4 cas + null', () => {
    expect(component.statutTravailleurLabel('SALARIE')).toContain('Salarie');
    expect(component.statutTravailleurLabel('INDEPENDANT')).toContain('Independant');
    expect(component.statutTravailleurLabel('AGENT_PUBLIC_STATUTAIRE')).toContain('Agent public');
    expect(component.statutTravailleurLabel('AUTRE')).toContain('Autre');
    expect(component.statutTravailleurLabel(null)).toBe('—');
  });

  it('libelle lien filiation : 5 cas + null', () => {
    expect(component.lienFiliationLabel('PERE_BIOLOGIQUE')).toContain('Pere biologique');
    expect(component.lienFiliationLabel('COPARENT_COMATERNITE')).toContain('comaternite');
    expect(component.lienFiliationLabel('COPARENT_RECONNAISSANCE')).toContain('reconnaissance');
    expect(component.lienFiliationLabel('COHABITANT_LEGAL_OU_MARIE')).toContain('Cohabitant');
    expect(component.lienFiliationLabel('AUTRE')).toContain('Autre');
    expect(component.lienFiliationLabel(null)).toBe('—');
  });

  it('libelle etape procedure : 6 cas + null', () => {
    expect(component.etapeProcedureLabel('AVANT_NAISSANCE')).toContain('Avant');
    expect(component.etapeProcedureLabel('NAISSANCE_SURVENUE')).toContain('Naissance');
    expect(component.etapeProcedureLabel('NOTIFICATION_EMPLOYEUR_FAITE')).toContain('Notification');
    expect(component.etapeProcedureLabel('CONGE_EN_COURS_DE_PRISE')).toContain('en cours');
    expect(component.etapeProcedureLabel('CONGE_PRIS_ENTIEREMENT')).toContain('totalement');
    expect(component.etapeProcedureLabel('DELAI_QUATRE_MOIS_DEPASSE')).toContain('Delai');
    expect(component.etapeProcedureLabel(null)).toBe('—');
  });

  // ============================================================
  // Formats
  // ============================================================

  it('formatDate : ISO -> jj/mm/aaaa', () => {
    expect(component.formatDate('2026-03-12')).toBe('12/03/2026');
    expect(component.formatDate(null)).toBe('—');
    expect(component.formatDate(undefined)).toBe('—');
    expect(component.formatDate('')).toBe('—');
  });

  it('formatBoolean : Oui / Non / —', () => {
    expect(component.formatBoolean(true)).toBe('Oui');
    expect(component.formatBoolean(false)).toBe('Non');
    expect(component.formatBoolean(null)).toBe('—');
    expect(component.formatBoolean(undefined)).toBe('—');
  });

  it('formatJours : singulier / pluriel / null', () => {
    expect(component.formatJours(1)).toBe('1 jour ouvrable');
    expect(component.formatJours(20)).toBe('20 jours ouvrables');
    expect(component.formatJours(0)).toBe('0 jour ouvrable');
    expect(component.formatJours(null)).toBe('—');
  });

  // ============================================================
  // Avertissement / synthese
  // ============================================================

  it('avertissement non vide => banniere ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      avertissement: 'Verifier le delai 4 mois post-naissance.',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('4 mois');
  });

  it('synthese affichee si presente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Droit ouvert — 20 jours ouvrables applicables.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('20 jours');
  });

  // ============================================================
  // F-177 SF-177-12 — parite getPrefillCount static
  // ============================================================

  it('static getPrefillCount => 0 (V1)', () => {
    expect(CongePaterniteNaissanceBeSectionComponent.getPrefillCount({})).toBe(0);
    expect(CongePaterniteNaissanceBeSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateNaissanceSalarie: '1985-04-12',
        dateRuptureContrat: '2026-03-12',
        motifRupture: 'autre',
        anneesCarriereSalarie: 10,
        salaireBrutMensuel: 2900,
        salaireBrutAnnuel: 34800,
      },
    })).toBe(0);
  });

  // ============================================================
  // editMode => re-affiche le form
  // ============================================================

  it('editMode() => showForm true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ============================================================
  // F-JU-03 — citation jurisprudentielle bien cablee
  // ============================================================

  it('toolId pour jurisprudence = conge-paternite-naissance-be', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('conge-paternite-naissance-be');
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

  it('forceExpanded => collapsed=false au ngOnInit', () => {
    component.forceExpanded = true;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    expect(component.collapsed()).toBe(false);
  });

  // ============================================================
  // Handlers signals
  // ============================================================

  it('handlers mettent a jour les signals correctement', () => {
    component.onStatutTravailleurChange('SALARIE');
    expect(component.statutTravailleur()).toBe('SALARIE');

    component.onLienFiliationChange('PERE_BIOLOGIQUE');
    expect(component.lienFiliation()).toBe('PERE_BIOLOGIQUE');

    component.onEtapeProcedureChange('NAISSANCE_SURVENUE');
    expect(component.etapeProcedure()).toBe('NAISSANCE_SURVENUE');

    component.onFiliationEtablieChange(true);
    expect(component.filiationEtablie()).toBe(true);

    component.onContratTravailEnCoursChange(true);
    expect(component.contratTravailEnCours()).toBe(true);

    component.onNotificationEmployeurFaiteChange(true);
    expect(component.notificationEmployeurFaite()).toBe(true);

    component.onDateNaissanceChange('2026-03-12');
    expect(component.dateNaissance()).toBe('2026-03-12');

    component.onDateNaissanceChange('');
    expect(component.dateNaissance()).toBeNull();

    component.onDateNotificationEmployeurChange('2026-03-15');
    expect(component.dateNotificationEmployeur()).toBe('2026-03-15');

    component.onJoursDejaPrisOuvrablesChange(5);
    expect(component.joursDejaPrisOuvrables()).toBe(5);

    component.onJoursDejaPrisOuvrablesChange('10');
    expect(component.joursDejaPrisOuvrables()).toBe(10);

    component.onJoursDejaPrisOuvrablesChange(null);
    expect(component.joursDejaPrisOuvrables()).toBeNull();

    component.onJoursDejaPrisOuvrablesChange('');
    expect(component.joursDejaPrisOuvrables()).toBeNull();

    component.onDateFinPriseEffectiveChange('2026-04-10');
    expect(component.dateFinPriseEffective()).toBe('2026-04-10');
  });
});
