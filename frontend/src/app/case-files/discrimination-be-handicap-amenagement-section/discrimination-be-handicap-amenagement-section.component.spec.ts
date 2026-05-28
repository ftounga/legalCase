import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  DiscriminationBeHandicapAmenagementSectionComponent,
} from './discrimination-be-handicap-amenagement-section.component';
import {
  DiscriminationBeHandicapAmenagementResponse,
} from '../../core/models/discrimination-be-handicap-amenagement.model';

describe('DiscriminationBeHandicapAmenagementSectionComponent', () => {
  let component: DiscriminationBeHandicapAmenagementSectionComponent;
  let fixture: ComponentFixture<DiscriminationBeHandicapAmenagementSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/discrimination-be-handicap-amenagement';

  /**
   * Construit une réponse backend par défaut — verdict
   * CONFORME_AMENAGEMENT_ACCORDE (statut handicap reconnu officiel,
   * réponse ACCORDE, aucune sanction).
   */
  function response(
    overrides: Partial<DiscriminationBeHandicapAmenagementResponse> = {},
  ): DiscriminationBeHandicapAmenagementResponse {
    return {
      caseFileId: 'case-1',
      statutHandicap: 'RECONNU_OFFICIEL',
      dateDemandeAmenagement: '2026-01-15',
      typeAmenagementDemande: 'Télétravail 2 jours par semaine',
      coutEstimeAmenagement: 1200,
      subsidesDemandes: true,
      effectifEntreprise: 80,
      chiffreAffairesAnnuel: 5000000,
      reponseEmployeur: 'ACCORDE',
      motivationDetailleeFournie: true,
      avisSeppFavorable: true,
      chargeDisproportionneeInvoquee: false,
      devisExterneFourni: false,
      mesuresAlternativesProposees: false,
      sanctionSubie: 'AUCUNE',
      dateSanction: null,
      salaireMensuelBrut: 3200,
      procedureUniaSaisie: false,
      verdict: 'CONFORME_AMENAGEMENT_ACCORDE',
      handicapQualifie: true,
      demandeFormalisee: true,
      refusCaracterise: false,
      chargeDisproportionneeDemontree: false,
      representaillesPresumees: false,
      indemniteForfaitaire6Mois: null,
      raison: 'CONFORME_AMENAGEMENT_ACCORDE',
      synthese: 'Aménagement raisonnable accordé conformément à l\'art. 14 Loi 10/05/2007.',
      baseJuridique: 'Loi du 10/05/2007 + CCT n° 95 + Directive 2000/78/CE art. 5.',
      avertissement: 'L\'avocat doit confirmer le suivi de la mise en oeuvre effective de l\'aménagement.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        DiscriminationBeHandicapAmenagementSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(DiscriminationBeHandicapAmenagementSectionComponent);
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
    expect(banner.textContent).toContain('OETH');
  });

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 → hydrate result + mode résultat + rehydrate form pour édition', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('CONFORME_AMENAGEMENT_ACCORDE');
    expect(component.showForm()).toBe(false);
    expect(component.statutHandicap()).toBe('RECONNU_OFFICIEL');
    expect(component.dateDemandeAmenagement()).toBe('2026-01-15');
    expect(component.typeAmenagementDemande()).toBe('Télétravail 2 jours par semaine');
    expect(component.coutEstimeAmenagement()).toBe(1200);
    expect(component.subsidesDemandes()).toBe(true);
    expect(component.effectifEntreprise()).toBe(80);
    expect(component.chiffreAffairesAnnuel()).toBe(5000000);
    expect(component.reponseEmployeur()).toBe('ACCORDE');
    expect(component.motivationDetailleeFournie()).toBe(true);
    expect(component.avisSeppFavorable()).toBe(true);
    expect(component.chargeDisproportionneeInvoquee()).toBe(false);
    expect(component.devisExterneFourni()).toBe(false);
    expect(component.mesuresAlternativesProposees()).toBe(false);
    expect(component.sanctionSubie()).toBe('AUCUNE');
    expect(component.salaireMensuelBrut()).toBe(3200);
    expect(component.procedureUniaSaisie()).toBe(false);
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
    component.statutHandicap.set('RECONNU_OFFICIEL');
    component.subsidesDemandes.set(true);
    component.effectifEntreprise.set(80);
    component.reponseEmployeur.set('ACCORDE');
    component.motivationDetailleeFournie.set(true);
    component.avisSeppFavorable.set(true);
    component.chargeDisproportionneeInvoquee.set(false);
    component.devisExterneFourni.set(false);
    component.mesuresAlternativesProposees.set(false);
    component.sanctionSubie.set('AUCUNE');
    component.procedureUniaSaisie.set(false);
  }

  it('formValid() : true avec les champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans statutHandicap', () => {
    fillForm();
    component.statutHandicap.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans subsidesDemandes', () => {
    fillForm();
    component.subsidesDemandes.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans effectifEntreprise', () => {
    fillForm();
    component.effectifEntreprise.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec effectif négatif', () => {
    fillForm();
    component.effectifEntreprise.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec effectif > 1M', () => {
    fillForm();
    component.effectifEntreprise.set(1000001);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans reponseEmployeur', () => {
    fillForm();
    component.reponseEmployeur.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans motivationDetailleeFournie', () => {
    fillForm();
    component.motivationDetailleeFournie.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans avisSeppFavorable', () => {
    fillForm();
    component.avisSeppFavorable.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans chargeDisproportionneeInvoquee', () => {
    fillForm();
    component.chargeDisproportionneeInvoquee.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans devisExterneFourni', () => {
    fillForm();
    component.devisExterneFourni.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans mesuresAlternativesProposees', () => {
    fillForm();
    component.mesuresAlternativesProposees.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans sanctionSubie', () => {
    fillForm();
    component.sanctionSubie.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans procedureUniaSaisie', () => {
    fillForm();
    component.procedureUniaSaisie.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true avec montants optionnels valides', () => {
    fillForm();
    component.coutEstimeAmenagement.set(1200);
    component.chiffreAffairesAnnuel.set(5000000);
    component.salaireMensuelBrut.set(3200);
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false avec coût négatif', () => {
    fillForm();
    component.coutEstimeAmenagement.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec CA négatif', () => {
    fillForm();
    component.chiffreAffairesAnnuel.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec salaire négatif', () => {
    fillForm();
    component.salaireMensuelBrut.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec typeAmenagement > 500 caractères', () => {
    fillForm();
    component.typeAmenagementDemande.set('a'.repeat(501));
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
    expect(post.request.body.statutHandicap).toBe('RECONNU_OFFICIEL');
    expect(post.request.body.reponseEmployeur).toBe('ACCORDE');
    expect(post.request.body.effectifEntreprise).toBe(80);
    expect(post.request.body.sanctionSubie).toBe('AUCUNE');
    expect(post.request.body.subsidesDemandes).toBe(true);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse aménagements raisonnables calculée', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'effectifEntreprise plafonné à 1 000 000' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'effectifEntreprise plafonné à 1 000 000', 'Fermer', expect.anything(),
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

  it('verdict CONFORME_AMENAGEMENT_ACCORDE → label, verdict-ok, verified', () => {
    component.result.set(response());
    expect(component.verdictLabel()).toContain('aménagement raisonnable accordé');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE → verdict-ok, check_circle', () => {
    component.result.set(response({
      verdict: 'CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE',
      reponseEmployeur: 'REFUSE_MOTIVE_DETAILLE',
      chargeDisproportionneeInvoquee: true,
      devisExterneFourni: true,
      chargeDisproportionneeDemontree: true,
      refusCaracterise: true,
    }));
    expect(component.verdictLabel()).toContain('charge disproportionnée');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('check_circle');
  });

  it('verdict DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE → verdict-critical, rule_folder', () => {
    component.result.set(response({
      verdict: 'DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE',
      reponseEmployeur: 'REFUSE_MOTIVE_DETAILLE',
      subsidesDemandes: false,
      refusCaracterise: true,
      chargeDisproportionneeDemontree: false,
    }));
    expect(component.verdictLabel()).toContain('Discrimination indirecte');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('rule_folder');
  });

  it('verdict DISCRIMINATION_PRESUMEE_NON_MOTIVATION → verdict-critical, report', () => {
    component.result.set(response({
      verdict: 'DISCRIMINATION_PRESUMEE_NON_MOTIVATION',
      reponseEmployeur: 'REFUSE_NON_MOTIVE',
      motivationDetailleeFournie: false,
      refusCaracterise: true,
    }));
    expect(component.verdictLabel()).toContain('Présomption');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('report');
  });

  it('verdict REPRESAILLES_PRESUMEES_LICENCIEMENT → verdict-critical, gavel', () => {
    component.result.set(response({
      verdict: 'REPRESAILLES_PRESUMEES_LICENCIEMENT',
      sanctionSubie: 'LICENCIEMENT',
      representaillesPresumees: true,
      indemniteForfaitaire6Mois: 19200,
    }));
    expect(component.verdictLabel()).toContain('Représailles');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('gavel');
  });

  it('verdict FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE → verdict-warn, warning', () => {
    component.result.set(response({
      verdict: 'FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE',
      statutHandicap: 'CONTESTE',
      handicapQualifie: false,
    }));
    expect(component.verdictLabel()).toContain('Qualification handicap incertaine');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('warning');
  });

  it('verdict A_ANALYSER → verdict-neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_ANALYSER',
      reponseEmployeur: 'EN_ATTENTE',
    }));
    expect(component.verdictLabel()).toContain('Configuration');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('accessible');
  });

  // ============================================================
  // Badge dans header (7 cas)
  // ============================================================

  it('badge CONFORME_AMENAGEMENT_ACCORDE = vert', () => {
    expect(component.verdictBadge('CONFORME_AMENAGEMENT_ACCORDE')).toBe('ACCORDÉ');
    expect(component.verdictBadgeClass('CONFORME_AMENAGEMENT_ACCORDE')).toBe('badge-ok');
  });

  it('badge CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE = vert', () => {
    expect(component.verdictBadge('CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE')).toBe('CONFORME');
    expect(component.verdictBadgeClass('CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE')).toBe('badge-ok');
  });

  it('badge DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE = rouge', () => {
    expect(component.verdictBadge('DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE')).toBe('REFUS INJUSTIFIÉ');
    expect(component.verdictBadgeClass('DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE')).toBe('badge-critical');
  });

  it('badge DISCRIMINATION_PRESUMEE_NON_MOTIVATION = rouge', () => {
    expect(component.verdictBadge('DISCRIMINATION_PRESUMEE_NON_MOTIVATION')).toBe('NON MOTIVÉ');
    expect(component.verdictBadgeClass('DISCRIMINATION_PRESUMEE_NON_MOTIVATION')).toBe('badge-critical');
  });

  it('badge REPRESAILLES_PRESUMEES_LICENCIEMENT = rouge', () => {
    expect(component.verdictBadge('REPRESAILLES_PRESUMEES_LICENCIEMENT')).toBe('REPRÉSAILLES');
    expect(component.verdictBadgeClass('REPRESAILLES_PRESUMEES_LICENCIEMENT')).toBe('badge-critical');
  });

  it('badge FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE = ambre', () => {
    expect(component.verdictBadge('FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE')).toBe('À QUALIFIER');
    expect(component.verdictBadgeClass('FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE')).toBe('badge-warn');
  });

  it('badge A_ANALYSER = neutre', () => {
    expect(component.verdictBadge('A_ANALYSER')).toBe('À ANALYSER');
    expect(component.verdictBadgeClass('A_ANALYSER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — verdict rendu (badge + ventilations)
  // ============================================================

  it('CONFORME_AMENAGEMENT_ACCORDE → badge vert + 5 lignes ventilations (sans indemnité)', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('ACCORDÉ');
    const ventilations = fixture.nativeElement.querySelectorAll('.ventilation-row');
    // 5 ventilations cumulatives + 0 ligne indemnité (null) = 5.
    expect(ventilations.length).toBe(5);
  });

  it('REPRESAILLES_PRESUMEES → badge rouge + 6 lignes ventilations (avec indemnité)', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'REPRESAILLES_PRESUMEES_LICENCIEMENT',
      sanctionSubie: 'LICENCIEMENT',
      representaillesPresumees: true,
      indemniteForfaitaire6Mois: 19200,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('REPRÉSAILLES');
    const banner = fixture.nativeElement.querySelector('.result-banner.verdict-critical');
    expect(banner).not.toBeNull();
    const ventilations = fixture.nativeElement.querySelectorAll('.ventilation-row');
    expect(ventilations.length).toBe(6);
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellé statutHandicap : 6 cas + null', () => {
    expect(component.statutHandicapLabel('RECONNU_OFFICIEL')).toContain('Reconnaissance');
    expect(component.statutHandicapLabel('MEDECIN_TRAVAIL_AVIS')).toContain('SEPP');
    expect(component.statutHandicapLabel('INVALIDITE_MUTUELLE')).toContain('mutualité');
    expect(component.statutHandicapLabel('DURABLE_FACTUEL')).toContain('factuelle');
    expect(component.statutHandicapLabel('CONTESTE')).toContain('contestée');
    expect(component.statutHandicapLabel('INDETERMINE')).toContain('non encore');
    expect(component.statutHandicapLabel(null)).toBe('—');
  });

  it('libellé reponseEmployeur : 7 cas + null', () => {
    expect(component.reponseEmployeurLabel('ACCORDE')).toContain('accordé');
    expect(component.reponseEmployeurLabel('CONTRE_PROPOSITION')).toContain('Contre-proposition');
    expect(component.reponseEmployeurLabel('REFUSE_MOTIVE_DETAILLE')).toContain('motivation');
    expect(component.reponseEmployeurLabel('REFUSE_NON_MOTIVE')).toContain('sans motivation');
    expect(component.reponseEmployeurLabel('NON_REPONDU_RAISONNABLE')).toContain('Silence');
    expect(component.reponseEmployeurLabel('EN_ATTENTE')).toContain('cours');
    expect(component.reponseEmployeurLabel('INDETERMINE')).toContain('indéterminé');
    expect(component.reponseEmployeurLabel(null)).toBe('—');
  });

  it('libellé sanctionSubie : 4 cas + null', () => {
    expect(component.sanctionSubieLabel('LICENCIEMENT')).toContain('Licenciement');
    expect(component.sanctionSubieLabel('REGRESSION_CONDITIONS_TRAVAIL')).toContain('Régression');
    expect(component.sanctionSubieLabel('HARCELEMENT_REPRESAILLE')).toContain('Harcèlement');
    expect(component.sanctionSubieLabel('AUCUNE')).toContain('Aucune');
    expect(component.sanctionSubieLabel(null)).toBe('—');
  });

  // ============================================================
  // Formats
  // ============================================================

  it('formatEuros : 2 décimales + €', () => {
    expect(component.formatEuros(19200)).toContain('19');
    expect(component.formatEuros(19200)).toContain('€');
    expect(component.formatEuros(null)).toBe('—');
    expect(component.formatEuros(undefined)).toBe('—');
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
      avertissement: 'L\'avocat doit confirmer le suivi de la mise en oeuvre effective de l\'aménagement.',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('aménagement');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Aménagement raisonnable accordé conformément.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('Aménagement');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(DiscriminationBeHandicapAmenagementSectionComponent.getPrefillCount({})).toBe(0);
    expect(DiscriminationBeHandicapAmenagementSectionComponent.getPrefillCount({
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

  it('toolId pour jurisprudence = discrimination-be-handicap-amenagement', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('discrimination-be-handicap-amenagement');
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
    component.onStatutHandicapChange('RECONNU_OFFICIEL');
    expect(component.statutHandicap()).toBe('RECONNU_OFFICIEL');

    component.onDateDemandeAmenagementChange('2026-01-15');
    expect(component.dateDemandeAmenagement()).toBe('2026-01-15');

    component.onTypeAmenagementDemandeChange('Télétravail');
    expect(component.typeAmenagementDemande()).toBe('Télétravail');

    component.onCoutEstimeAmenagementChange('1200');
    expect(component.coutEstimeAmenagement()).toBe(1200);

    component.onSubsidesDemandesChange(true);
    expect(component.subsidesDemandes()).toBe(true);

    component.onEffectifEntrepriseChange('80');
    expect(component.effectifEntreprise()).toBe(80);

    component.onChiffreAffairesAnnuelChange('5000000');
    expect(component.chiffreAffairesAnnuel()).toBe(5000000);

    component.onReponseEmployeurChange('ACCORDE');
    expect(component.reponseEmployeur()).toBe('ACCORDE');

    component.onMotivationDetailleeFournieChange(true);
    expect(component.motivationDetailleeFournie()).toBe(true);

    component.onAvisSeppFavorableChange(true);
    expect(component.avisSeppFavorable()).toBe(true);

    component.onChargeDisproportionneeInvoqueeChange(false);
    expect(component.chargeDisproportionneeInvoquee()).toBe(false);

    component.onDevisExterneFourniChange(false);
    expect(component.devisExterneFourni()).toBe(false);

    component.onMesuresAlternativesProposeesChange(false);
    expect(component.mesuresAlternativesProposees()).toBe(false);

    component.onSanctionSubieChange('AUCUNE');
    expect(component.sanctionSubie()).toBe('AUCUNE');

    component.onDateSanctionChange('2026-02-01');
    expect(component.dateSanction()).toBe('2026-02-01');

    component.onSalaireMensuelBrutChange('3200');
    expect(component.salaireMensuelBrut()).toBe(3200);

    component.onProcedureUniaSaisieChange(false);
    expect(component.procedureUniaSaisie()).toBe(false);

    component.onEffectifEntrepriseChange('');
    expect(component.effectifEntreprise()).toBeNull();

    component.onCoutEstimeAmenagementChange(null);
    expect(component.coutEstimeAmenagement()).toBeNull();
  });
});
