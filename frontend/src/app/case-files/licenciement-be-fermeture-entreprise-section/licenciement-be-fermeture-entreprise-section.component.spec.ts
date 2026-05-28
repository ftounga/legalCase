import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  LicenciementBeFermetureEntrepriseSectionComponent,
} from './licenciement-be-fermeture-entreprise-section.component';
import {
  LicenciementBeFermetureEntrepriseResponse,
} from '../../core/models/licenciement-be-fermeture-entreprise.model';

describe('LicenciementBeFermetureEntrepriseSectionComponent', () => {
  let component: LicenciementBeFermetureEntrepriseSectionComponent;
  let fixture: ComponentFixture<LicenciementBeFermetureEntrepriseSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/licenciement-be-fermeture-entreprise';

  /**
   * Construit une réponse backend par défaut — verdict ELIGIBLE_INDEMNITE_SEULE
   * (employeur solvable, ancienneté 10 ans, âge 50, indemnité 167.72*10 + supp 5*167.72).
   */
  function response(
    overrides: Partial<LicenciementBeFermetureEntrepriseResponse> = {},
  ): LicenciementBeFermetureEntrepriseResponse {
    return {
      caseFileId: 'case-1',
      dateNaissance: '1976-04-10',
      dateDebutContrat: '2016-04-10',
      dateFermeture: '2026-04-10',
      remunerationMensuelleBrute: 3200,
      typeFermeture: 'CESSATION_DEFINITIVE',
      statutEmployeur: 'SOLVABLE',
      effectifEtp: 35,
      salairesImpayes: 0,
      peculeVacancesImpaye: 0,
      indemniteRuptureImpayee: 0,
      verdict: 'ELIGIBLE_INDEMNITE_SEULE',
      eligible: true,
      ageADateFermeture: 50,
      anneesAnciennete: 10,
      indemniteFermeture: 2516.6,
      montantForfaitaireParAnnee: 167.72,
      supplementAgeMensuel: 167.72,
      montantTotalCreancesFfe: 0,
      raison: 'ELIGIBLE_INDEMNITE_EMPLOYEUR_SOLVABLE',
      synthese: 'Fermeture qualifiante (ancienneté 10 ans, âge 50 ans). Indemnité ≈ 2516.60 €.',
      baseJuridique: 'Loi du 26/06/2002 + AR 23/03/2007 + CCT 9bis',
      avertissement: 'Montants indicatifs — confirmer auprès du FFE.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        LicenciementBeFermetureEntrepriseSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(LicenciementBeFermetureEntrepriseSectionComponent);
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
    expect(component.result()!.verdict).toBe('ELIGIBLE_INDEMNITE_SEULE');
    expect(component.showForm()).toBe(false);
    expect(component.dateNaissance()).toBe('1976-04-10');
    expect(component.dateDebutContrat()).toBe('2016-04-10');
    expect(component.dateFermeture()).toBe('2026-04-10');
    expect(component.remunerationMensuelleBrute()).toBe(3200);
    expect(component.typeFermeture()).toBe('CESSATION_DEFINITIVE');
    expect(component.statutEmployeur()).toBe('SOLVABLE');
    expect(component.effectifEtp()).toBe(35);
    expect(component.salairesImpayes()).toBe(0);
    expect(component.peculeVacancesImpaye()).toBe(0);
    expect(component.indemniteRuptureImpayee()).toBe(0);
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

  it('formValid() : true avec 10 champs requis remplis', () => {
    component.dateNaissance.set('1976-04-10');
    component.dateDebutContrat.set('2016-04-10');
    component.dateFermeture.set('2026-04-10');
    component.remunerationMensuelleBrute.set(3200);
    component.typeFermeture.set('CESSATION_DEFINITIVE');
    component.statutEmployeur.set('SOLVABLE');
    component.effectifEtp.set(35);
    component.salairesImpayes.set(0);
    component.peculeVacancesImpaye.set(0);
    component.indemniteRuptureImpayee.set(0);
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans dateNaissance', () => {
    component.dateDebutContrat.set('2016-04-10');
    component.dateFermeture.set('2026-04-10');
    component.remunerationMensuelleBrute.set(3200);
    component.typeFermeture.set('CESSATION_DEFINITIVE');
    component.statutEmployeur.set('SOLVABLE');
    component.effectifEtp.set(35);
    component.salairesImpayes.set(0);
    component.peculeVacancesImpaye.set(0);
    component.indemniteRuptureImpayee.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans typeFermeture', () => {
    component.dateNaissance.set('1976-04-10');
    component.dateDebutContrat.set('2016-04-10');
    component.dateFermeture.set('2026-04-10');
    component.remunerationMensuelleBrute.set(3200);
    component.statutEmployeur.set('SOLVABLE');
    component.effectifEtp.set(35);
    component.salairesImpayes.set(0);
    component.peculeVacancesImpaye.set(0);
    component.indemniteRuptureImpayee.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans statutEmployeur', () => {
    component.dateNaissance.set('1976-04-10');
    component.dateDebutContrat.set('2016-04-10');
    component.dateFermeture.set('2026-04-10');
    component.remunerationMensuelleBrute.set(3200);
    component.typeFermeture.set('CESSATION_DEFINITIVE');
    component.effectifEtp.set(35);
    component.salairesImpayes.set(0);
    component.peculeVacancesImpaye.set(0);
    component.indemniteRuptureImpayee.set(0);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  function fillForm() {
    component.dateNaissance.set('1976-04-10');
    component.dateDebutContrat.set('2016-04-10');
    component.dateFermeture.set('2026-04-10');
    component.remunerationMensuelleBrute.set(3200);
    component.typeFermeture.set('CESSATION_DEFINITIVE');
    component.statutEmployeur.set('SOLVABLE');
    component.effectifEtp.set(35);
    component.salairesImpayes.set(0);
    component.peculeVacancesImpaye.set(0);
    component.indemniteRuptureImpayee.set(0);
  }

  it('calculate() → POST + snack succès', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.dateNaissance).toBe('1976-04-10');
    expect(post.request.body.dateDebutContrat).toBe('2016-04-10');
    expect(post.request.body.dateFermeture).toBe('2026-04-10');
    expect(post.request.body.remunerationMensuelleBrute).toBe(3200);
    expect(post.request.body.typeFermeture).toBe('CESSATION_DEFINITIVE');
    expect(post.request.body.statutEmployeur).toBe('SOLVABLE');
    expect(post.request.body.effectifEtp).toBe(35);
    expect(post.request.body.salairesImpayes).toBe(0);
    expect(post.request.body.peculeVacancesImpaye).toBe(0);
    expect(post.request.body.indemniteRuptureImpayee).toBe(0);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Fermeture entreprise analysée', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'champ invalide' }, { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'champ invalide', 'Fermer', expect.anything(),
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

  it('verdict ELIGIBLE_FFE_COMPLET → label, verdict-ok, verified', () => {
    component.result.set(response({ verdict: 'ELIGIBLE_FFE_COMPLET' }));
    expect(component.verdictLabel()).toContain('FFE complet');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict ELIGIBLE_FFE_REPRISE_CREANCES → label, verdict-ok, savings', () => {
    component.result.set(response({
      verdict: 'ELIGIBLE_FFE_REPRISE_CREANCES',
      statutEmployeur: 'INSOLVABLE_AVERE',
      salairesImpayes: 4800,
      peculeVacancesImpaye: 1200,
      indemniteRuptureImpayee: 3200,
      montantTotalCreancesFfe: 9200,
    }));
    expect(component.verdictLabel()).toContain('reprise des créances');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('savings');
  });

  it('verdict ELIGIBLE_INDEMNITE_SEULE → label, verdict-ok, paid', () => {
    component.result.set(response({ verdict: 'ELIGIBLE_INDEMNITE_SEULE' }));
    expect(component.verdictLabel()).toContain('Indemnité de fermeture seule');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('paid');
  });

  it('verdict INELIGIBLE_ANCIENNETE_INSUFFISANTE → label, verdict-critical, block', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_ANCIENNETE_INSUFFISANTE',
      eligible: false,
      anneesAnciennete: 0,
      indemniteFermeture: 0,
    }));
    expect(component.verdictLabel()).toContain('ancienneté < 1 an');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('block');
  });

  it('verdict INELIGIBLE_TYPE_FERMETURE → label, verdict-critical, block', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_TYPE_FERMETURE',
      typeFermeture: 'TRANSFERT_CCT_32BIS',
      eligible: false,
      indemniteFermeture: 0,
    }));
    expect(component.verdictLabel()).toContain('type de fermeture non qualifiant');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('block');
  });

  it('verdict INELIGIBLE_SEUIL_EFFECTIF → label, verdict-critical, block', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_SEUIL_EFFECTIF',
      effectifEtp: 12,
      eligible: false,
      indemniteFermeture: 0,
    }));
    expect(component.verdictLabel()).toContain('< 20 ETP');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('block');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('gavel');
  });

  // ============================================================
  // Badge dans header (6 cas)
  // ============================================================

  it('badge ELIGIBLE_FFE_COMPLET = vert "FFE COMPLET"', () => {
    expect(component.verdictBadge('ELIGIBLE_FFE_COMPLET')).toBe('FFE COMPLET');
    expect(component.verdictBadgeClass('ELIGIBLE_FFE_COMPLET')).toBe('badge-ok');
  });

  it('badge ELIGIBLE_FFE_REPRISE_CREANCES = vert "FFE CRÉANCES"', () => {
    expect(component.verdictBadge('ELIGIBLE_FFE_REPRISE_CREANCES')).toBe('FFE CRÉANCES');
    expect(component.verdictBadgeClass('ELIGIBLE_FFE_REPRISE_CREANCES')).toBe('badge-ok');
  });

  it('badge ELIGIBLE_INDEMNITE_SEULE = vert "INDEMNITÉ SEULE"', () => {
    expect(component.verdictBadge('ELIGIBLE_INDEMNITE_SEULE')).toBe('INDEMNITÉ SEULE');
    expect(component.verdictBadgeClass('ELIGIBLE_INDEMNITE_SEULE')).toBe('badge-ok');
  });

  it('badge INELIGIBLE_ANCIENNETE_INSUFFISANTE = rouge "ANCIENNETÉ INSUFF."', () => {
    expect(component.verdictBadge('INELIGIBLE_ANCIENNETE_INSUFFISANTE')).toBe('ANCIENNETÉ INSUFF.');
    expect(component.verdictBadgeClass('INELIGIBLE_ANCIENNETE_INSUFFISANTE')).toBe('badge-critical');
  });

  it('badge INELIGIBLE_TYPE_FERMETURE = rouge "TYPE NON QUALIFIANT"', () => {
    expect(component.verdictBadge('INELIGIBLE_TYPE_FERMETURE')).toBe('TYPE NON QUALIFIANT');
    expect(component.verdictBadgeClass('INELIGIBLE_TYPE_FERMETURE')).toBe('badge-critical');
  });

  it('badge INELIGIBLE_SEUIL_EFFECTIF = rouge "EFFECTIF < 20"', () => {
    expect(component.verdictBadge('INELIGIBLE_SEUIL_EFFECTIF')).toBe('EFFECTIF < 20');
    expect(component.verdictBadgeClass('INELIGIBLE_SEUIL_EFFECTIF')).toBe('badge-critical');
  });

  it('ELIGIBLE_INDEMNITE_SEULE → badge vert dans le DOM + bannière indemnité', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'ELIGIBLE_INDEMNITE_SEULE',
      eligible: true,
      indemniteFermeture: 2516.6,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('INDEMNITÉ SEULE');
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('2516.6');
  });

  it('INELIGIBLE_TYPE_FERMETURE → badge rouge dans le DOM', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INELIGIBLE_TYPE_FERMETURE',
      typeFermeture: 'TRANSFERT_CCT_32BIS',
      eligible: false,
      indemniteFermeture: 0,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('TYPE');
  });

  it('ELIGIBLE_FFE_REPRISE_CREANCES → bannière créances FFE rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'ELIGIBLE_FFE_REPRISE_CREANCES',
      eligible: true,
      statutEmployeur: 'INSOLVABLE_AVERE',
      salairesImpayes: 4800,
      peculeVacancesImpaye: 1200,
      indemniteRuptureImpayee: 3200,
      montantTotalCreancesFfe: 9200,
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('9200');
    expect(text).toContain('Créances reprises');
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellés typeFermeture : 6 cas + null', () => {
    expect(component.typeFermetureLabel('CESSATION_DEFINITIVE')).toContain('Cessation définitive');
    expect(component.typeFermetureLabel('FAILLITE_LIQUIDATION_JUDICIAIRE')).toContain('Faillite');
    expect(component.typeFermetureLabel('CESSATION_PARTIELLE')).toContain('partielle');
    expect(component.typeFermetureLabel('TRANSFERT_CCT_32BIS')).toContain('Transfert');
    expect(component.typeFermetureLabel('FUSION_SANS_SUPPRESSION')).toContain('Fusion');
    expect(component.typeFermetureLabel('FERMETURE_TEMPORAIRE')).toContain('temporaire');
    expect(component.typeFermetureLabel(null)).toBe('—');
  });

  it('libellés statutEmployeur : 3 cas + null', () => {
    expect(component.statutEmployeurLabel('SOLVABLE')).toContain('Solvable');
    expect(component.statutEmployeurLabel('INSOLVABLE_AVERE')).toContain('Insolvable');
    expect(component.statutEmployeurLabel('FAILLITE_DECLAREE')).toContain('Faillite');
    expect(component.statutEmployeurLabel(null)).toBe('—');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non vide → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'ELIGIBLE_INDEMNITE_SEULE',
      avertissement: 'Vérifier le barème AR 23/03/2007 en vigueur',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('AR 23/03/2007');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Fermeture qualifiante — indemnité 2516.60 € due.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('2516.60');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(LicenciementBeFermetureEntrepriseSectionComponent.getPrefillCount({})).toBe(0);
    expect(LicenciementBeFermetureEntrepriseSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateNaissanceSalarie: '1976-04-10',
        dateRuptureContrat: '2026-04-10',
        motifRupture: 'fermeture',
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

  it('toolId pour jurisprudence = licenciement-be-fermeture-entreprise', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('licenciement-be-fermeture-entreprise');
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
    component.onDateNaissanceChange('1976-04-10');
    expect(component.dateNaissance()).toBe('1976-04-10');

    component.onDateDebutContratChange('2016-04-10');
    expect(component.dateDebutContrat()).toBe('2016-04-10');

    component.onDateFermetureChange('2026-04-10');
    expect(component.dateFermeture()).toBe('2026-04-10');

    component.onRemunerationMensuelleBruteChange(3200);
    expect(component.remunerationMensuelleBrute()).toBe(3200);

    component.onTypeFermetureChange('FAILLITE_LIQUIDATION_JUDICIAIRE');
    expect(component.typeFermeture()).toBe('FAILLITE_LIQUIDATION_JUDICIAIRE');

    component.onStatutEmployeurChange('FAILLITE_DECLAREE');
    expect(component.statutEmployeur()).toBe('FAILLITE_DECLAREE');

    component.onEffectifEtpChange(50);
    expect(component.effectifEtp()).toBe(50);

    component.onSalairesImpayesChange(1000);
    expect(component.salairesImpayes()).toBe(1000);

    component.onPeculeVacancesImpayeChange(500);
    expect(component.peculeVacancesImpaye()).toBe(500);

    component.onIndemniteRuptureImpayeeChange(2000);
    expect(component.indemniteRuptureImpayee()).toBe(2000);
  });

  // ============================================================
  // Static metadata F-177
  // ============================================================

  it('TOOL_LABEL et TOOL_ICON statiques exposés', () => {
    expect(LicenciementBeFermetureEntrepriseSectionComponent.TOOL_LABEL).toBe('FERMETURE ENTREPRISE (BE)');
    expect(LicenciementBeFermetureEntrepriseSectionComponent.TOOL_ICON).toBe('business');
  });
});
