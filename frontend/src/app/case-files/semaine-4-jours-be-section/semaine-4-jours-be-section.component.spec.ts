import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  Semaine4JoursBeSectionComponent,
} from './semaine-4-jours-be-section.component';
import {
  Semaine4JoursBeResponse,
} from '../../core/models/semaine-4-jours-be.model';

describe('Semaine4JoursBeSectionComponent', () => {
  let component: Semaine4JoursBeSectionComponent;
  let fixture: ComponentFixture<Semaine4JoursBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/semaine-4-jours-be';

  /**
   * Construit une réponse backend par défaut — verdict
   * CONFORME_REGIME_4_JOURS_VALIDE (temps plein, demande écrite, journée
   * 9 h pile, avenant 5 mois signé + règlement de travail modifié).
   */
  function response(
    overrides: Partial<Semaine4JoursBeResponse> = {},
  ): Semaine4JoursBeResponse {
    return {
      caseFileId: 'case-1',
      statutDemande: 'ACCORDE_AVENANT_SIGNE',
      travailleurTempsPlein: true,
      demandeEcriteTravailleur: true,
      dateDemande: '2025-01-15',
      dureeHebdomadaireHeures: 38,
      journeeMaximaleHeures: 9.5,
      cctAutorise10h: false,
      avenantEcritSigne: true,
      reglementTravailModifie: true,
      dureeAvenantMois: 5,
      avenantRenouvele: false,
      refusMotiveParEcrit: false,
      dateLicenciement: null,
      motifLicenciementObjectifEtabli: false,
      verdict: 'CONFORME_REGIME_4_JOURS_VALIDE',
      eligibiliteRespectee: true,
      demandeRespectee: true,
      journeeRespectee: true,
      formalisationRespectee: true,
      dureeRespectee: true,
      journeeMaximaleAutoriseeHeures: 9.5,
      raison: 'CONFORME_REGIME_4_JOURS_VALIDE',
      synthese: 'Semaine de 4 jours valablement mise en place (art. 5 Loi 03/10/2022).',
      baseJuridique: 'Loi du 03/10/2022 « Deal pour l\'emploi » M.B. 10/11/2022, art. 5 + art. 6 ; Loi du 16/03/1971 art. 19 ; Loi du 03/07/1978 art. 25 ; Loi du 08/04/1965 art. 12.',
      avertissement: 'Le « Deal pour l\'emploi » (Loi 03/10/2022) est récent et la jurisprudence post-2023 reste limitée.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        Semaine4JoursBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(Semaine4JoursBeSectionComponent);
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
    expect(banner.textContent).toContain('Aubry');
  });

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 → hydrate result + mode résultat + rehydrate form pour édition', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('CONFORME_REGIME_4_JOURS_VALIDE');
    expect(component.showForm()).toBe(false);
    expect(component.statutDemande()).toBe('ACCORDE_AVENANT_SIGNE');
    expect(component.travailleurTempsPlein()).toBe(true);
    expect(component.demandeEcriteTravailleur()).toBe(true);
    expect(component.dateDemande()).toBe('2025-01-15');
    expect(component.dureeHebdomadaireHeures()).toBe(38);
    expect(component.journeeMaximaleHeures()).toBe(9.5);
    expect(component.cctAutorise10h()).toBe(false);
    expect(component.avenantEcritSigne()).toBe(true);
    expect(component.reglementTravailModifie()).toBe(true);
    expect(component.dureeAvenantMois()).toBe(5);
    expect(component.avenantRenouvele()).toBe(false);
    expect(component.refusMotiveParEcrit()).toBe(false);
    expect(component.motifLicenciementObjectifEtabli()).toBe(false);
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
    component.statutDemande.set('ACCORDE_AVENANT_SIGNE');
    component.travailleurTempsPlein.set(true);
    component.demandeEcriteTravailleur.set(true);
    component.dateDemande.set('2025-01-15');
    component.dureeHebdomadaireHeures.set(38);
    component.journeeMaximaleHeures.set(9.5);
    component.cctAutorise10h.set(false);
    component.avenantEcritSigne.set(true);
    component.reglementTravailModifie.set(true);
    component.dureeAvenantMois.set(5);
    component.avenantRenouvele.set(false);
    component.refusMotiveParEcrit.set(false);
    component.motifLicenciementObjectifEtabli.set(false);
  }

  it('formValid() : true avec les champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans statutDemande', () => {
    fillForm();
    component.statutDemande.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans travailleurTempsPlein précisé', () => {
    fillForm();
    component.travailleurTempsPlein.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans demandeEcriteTravailleur précisé', () => {
    fillForm();
    component.demandeEcriteTravailleur.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dateDemande', () => {
    fillForm();
    component.dateDemande.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec dureeHebdomadaire <= 0', () => {
    fillForm();
    component.dureeHebdomadaireHeures.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec dureeHebdomadaire > 60', () => {
    fillForm();
    component.dureeHebdomadaireHeures.set(61);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec journeeMaximale <= 0', () => {
    fillForm();
    component.journeeMaximaleHeures.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec journeeMaximale > 16', () => {
    fillForm();
    component.journeeMaximaleHeures.set(17);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans cctAutorise10h précisé', () => {
    fillForm();
    component.cctAutorise10h.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans avenantEcritSigne précisé', () => {
    fillForm();
    component.avenantEcritSigne.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans reglementTravailModifie précisé', () => {
    fillForm();
    component.reglementTravailModifie.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec dureeAvenantMois < 0', () => {
    fillForm();
    component.dureeAvenantMois.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec dureeAvenantMois > 120', () => {
    fillForm();
    component.dureeAvenantMois.set(121);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans avenantRenouvele précisé', () => {
    fillForm();
    component.avenantRenouvele.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans refusMotiveParEcrit précisé', () => {
    fillForm();
    component.refusMotiveParEcrit.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans motifLicenciementObjectifEtabli précisé', () => {
    fillForm();
    component.motifLicenciementObjectifEtabli.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : LICENCIE_APRES_DEMANDE sans dateLicenciement → false', () => {
    fillForm();
    component.statutDemande.set('LICENCIE_APRES_DEMANDE');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : LICENCIE_APRES_DEMANDE avec dateLicenciement → true', () => {
    fillForm();
    component.statutDemande.set('LICENCIE_APRES_DEMANDE');
    component.dateLicenciement.set('2025-02-10');
    expect(component.formValid()).toBe(true);
  });

  it('isLicencieScenario() true uniquement si LICENCIE_APRES_DEMANDE', () => {
    expect(component.isLicencieScenario()).toBe(false);
    component.statutDemande.set('LICENCIE_APRES_DEMANDE');
    expect(component.isLicencieScenario()).toBe(true);
    component.statutDemande.set('ACCORDE_AVENANT_SIGNE');
    expect(component.isLicencieScenario()).toBe(false);
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
    expect(post.request.body.statutDemande).toBe('ACCORDE_AVENANT_SIGNE');
    expect(post.request.body.travailleurTempsPlein).toBe(true);
    expect(post.request.body.dureeHebdomadaireHeures).toBe(38);
    expect(post.request.body.journeeMaximaleHeures).toBe(9.5);
    expect(post.request.body.dureeAvenantMois).toBe(5);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Conformité semaine de 4 jours analysée', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'journeeMaximaleHeures plafonnée à 16h' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'journeeMaximaleHeures plafonnée à 16h', 'Fermer', expect.anything(),
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
  // Verdict — label / classe / icône (9 cas)
  // ============================================================

  it('verdict CONFORME_REGIME_4_JOURS_VALIDE → label, verdict-ok, check_circle', () => {
    component.result.set(response());
    expect(component.verdictLabel()).toContain('valablement');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('check_circle');
  });

  it('verdict LICENCIEMENT_REPRESAILLES_PRESUME → label, verdict-critical, gavel', () => {
    component.result.set(response({
      verdict: 'LICENCIEMENT_REPRESAILLES_PRESUME',
      statutDemande: 'LICENCIE_APRES_DEMANDE',
      dateLicenciement: '2025-02-10',
    }));
    expect(component.verdictLabel()).toContain('représailles');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('gavel');
  });

  it('verdict REFUS_EMPLOYEUR_NON_MOTIVE → label, verdict-critical, block', () => {
    component.result.set(response({
      verdict: 'REFUS_EMPLOYEUR_NON_MOTIVE',
      statutDemande: 'REFUSE_SANS_MOTIVATION_ECRITE',
    }));
    expect(component.verdictLabel()).toContain('abus de droit');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('block');
  });

  it('verdict NON_ELIGIBLE_TEMPS_PARTIEL → label, verdict-neutral, do_not_disturb_on', () => {
    component.result.set(response({
      verdict: 'NON_ELIGIBLE_TEMPS_PARTIEL',
      travailleurTempsPlein: false,
      eligibiliteRespectee: false,
    }));
    expect(component.verdictLabel()).toContain('temps partiel');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('do_not_disturb_on');
  });

  it('verdict NON_CONFORME_DEMANDE_ECRITE_MANQUANTE → label, verdict-warn, description', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_DEMANDE_ECRITE_MANQUANTE',
      demandeEcriteTravailleur: false,
      demandeRespectee: false,
    }));
    expect(component.verdictLabel()).toContain('Demande écrite');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('description');
  });

  it('verdict NON_CONFORME_JOURNEE_DEPASSE_9H30 → label, verdict-warn, schedule', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_JOURNEE_DEPASSE_9H30',
      journeeMaximaleHeures: 10.5,
      journeeRespectee: false,
    }));
    expect(component.verdictLabel()).toContain('plafond');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('schedule');
  });

  it('verdict NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT → label, verdict-warn, edit_document', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT',
      avenantEcritSigne: false,
      formalisationRespectee: false,
    }));
    expect(component.verdictLabel()).toContain('Avenant');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('edit_document');
  });

  it('verdict NON_CONFORME_DUREE_DEPASSE_6_MOIS → label, verdict-warn, hourglass_top', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_DUREE_DEPASSE_6_MOIS',
      dureeAvenantMois: 9,
      dureeRespectee: false,
    }));
    expect(component.verdictLabel()).toContain('6 mois');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('hourglass_top');
  });

  it('verdict A_ANALYSER → label, verdict-neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_ANALYSER',
      statutDemande: 'INDETERMINE',
    }));
    expect(component.verdictLabel()).toContain('Configuration');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('view_week');
  });

  // ============================================================
  // Badge dans header (9 cas)
  // ============================================================

  it('badge CONFORME = vert', () => {
    expect(component.verdictBadge('CONFORME_REGIME_4_JOURS_VALIDE')).toBe('CONFORME');
    expect(component.verdictBadgeClass('CONFORME_REGIME_4_JOURS_VALIDE')).toBe('badge-ok');
  });

  it('badge LICENCIEMENT_REPRESAILLES_PRESUME = rouge', () => {
    expect(component.verdictBadge('LICENCIEMENT_REPRESAILLES_PRESUME')).toBe('REPRÉSAILLES');
    expect(component.verdictBadgeClass('LICENCIEMENT_REPRESAILLES_PRESUME')).toBe('badge-critical');
  });

  it('badge REFUS_EMPLOYEUR_NON_MOTIVE = rouge', () => {
    expect(component.verdictBadge('REFUS_EMPLOYEUR_NON_MOTIVE')).toBe('REFUS NON MOTIVÉ');
    expect(component.verdictBadgeClass('REFUS_EMPLOYEUR_NON_MOTIVE')).toBe('badge-critical');
  });

  it('badge NON_ELIGIBLE_TEMPS_PARTIEL = neutre', () => {
    expect(component.verdictBadge('NON_ELIGIBLE_TEMPS_PARTIEL')).toBe('NON ÉLIGIBLE');
    expect(component.verdictBadgeClass('NON_ELIGIBLE_TEMPS_PARTIEL')).toBe('badge-neutral');
  });

  it('badge NON_CONFORME_DEMANDE_ECRITE_MANQUANTE = ambre', () => {
    expect(component.verdictBadge('NON_CONFORME_DEMANDE_ECRITE_MANQUANTE')).toBe('DEMANDE MANQUANTE');
    expect(component.verdictBadgeClass('NON_CONFORME_DEMANDE_ECRITE_MANQUANTE')).toBe('badge-warn');
  });

  it('badge NON_CONFORME_JOURNEE_DEPASSE_9H30 = ambre', () => {
    expect(component.verdictBadge('NON_CONFORME_JOURNEE_DEPASSE_9H30')).toBe('JOURNÉE > PLAFOND');
    expect(component.verdictBadgeClass('NON_CONFORME_JOURNEE_DEPASSE_9H30')).toBe('badge-warn');
  });

  it('badge NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT = ambre', () => {
    expect(component.verdictBadge('NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT')).toBe('FORMALISATION');
    expect(component.verdictBadgeClass('NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT')).toBe('badge-warn');
  });

  it('badge NON_CONFORME_DUREE_DEPASSE_6_MOIS = ambre', () => {
    expect(component.verdictBadge('NON_CONFORME_DUREE_DEPASSE_6_MOIS')).toBe('DURÉE > 6 MOIS');
    expect(component.verdictBadgeClass('NON_CONFORME_DUREE_DEPASSE_6_MOIS')).toBe('badge-warn');
  });

  it('badge A_ANALYSER = neutre', () => {
    expect(component.verdictBadge('A_ANALYSER')).toBe('À ANALYSER');
    expect(component.verdictBadgeClass('A_ANALYSER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — verdict CONFORME rendu (badge + 5 conformités)
  // ============================================================

  it('CONFORME → badge vert dans le DOM + 5 conformités affichées', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('CONFORME');
    const conformites = fixture.nativeElement.querySelectorAll('.conformite-row');
    expect(conformites.length).toBe(5);
  });

  it('LICENCIEMENT_REPRESAILLES → badge rouge + bannière critique', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'LICENCIEMENT_REPRESAILLES_PRESUME',
      statutDemande: 'LICENCIE_APRES_DEMANDE',
      dateLicenciement: '2025-02-10',
      motifLicenciementObjectifEtabli: false,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('REPRÉSAILLES');
    const banner = fixture.nativeElement.querySelector('.result-banner.verdict-critical');
    expect(banner).not.toBeNull();
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellé statutDemande : 6 cas + null', () => {
    expect(component.statutDemandeLabel('ACCORDE_AVENANT_SIGNE')).toContain('Accord');
    expect(component.statutDemandeLabel('REFUSE_MOTIVE_PAR_ECRIT')).toContain('motivé');
    expect(component.statutDemandeLabel('REFUSE_SANS_MOTIVATION_ECRITE')).toContain('sans motivation');
    expect(component.statutDemandeLabel('EN_ATTENTE_REPONSE_EMPLOYEUR')).toContain('attente');
    expect(component.statutDemandeLabel('LICENCIE_APRES_DEMANDE')).toContain('Licenciement');
    expect(component.statutDemandeLabel('INDETERMINE')).toContain('indéterminé');
    expect(component.statutDemandeLabel(null)).toBe('—');
  });

  // ============================================================
  // Formats
  // ============================================================

  it('formatHeures arrondit à 2 décimales avec h + locale FR', () => {
    expect(component.formatHeures(9.5)).toContain('9');
    expect(component.formatHeures(9.5)).toContain('50');
    expect(component.formatHeures(9.5)).toContain('h');
    expect(component.formatHeures(null)).toBe('—');
    expect(component.formatHeures(undefined)).toBe('—');
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
      avertissement: 'Loi 03/10/2022 récente — jurisprudence post-2023 limitée, CCT 10 h à vérifier au cas par cas.',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('jurisprudence');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Régime semaine de 4 jours valablement mis en place — toutes conditions cumulatives art. 5.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('cumulatives');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(Semaine4JoursBeSectionComponent.getPrefillCount({})).toBe(0);
    expect(Semaine4JoursBeSectionComponent.getPrefillCount({
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

  it('toolId pour jurisprudence = semaine-4-jours-be', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('semaine-4-jours-be');
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
    component.onStatutDemandeChange('REFUSE_MOTIVE_PAR_ECRIT');
    expect(component.statutDemande()).toBe('REFUSE_MOTIVE_PAR_ECRIT');

    component.onTravailleurTempsPleinChange(true);
    expect(component.travailleurTempsPlein()).toBe(true);

    component.onDemandeEcriteTravailleurChange(true);
    expect(component.demandeEcriteTravailleur()).toBe(true);

    component.onDateDemandeChange('2025-06-01');
    expect(component.dateDemande()).toBe('2025-06-01');

    component.onDureeHebdomadaireHeuresChange('40');
    expect(component.dureeHebdomadaireHeures()).toBe(40);

    component.onJourneeMaximaleHeuresChange(10);
    expect(component.journeeMaximaleHeures()).toBe(10);

    component.onCctAutorise10hChange(true);
    expect(component.cctAutorise10h()).toBe(true);

    component.onAvenantEcritSigneChange(true);
    expect(component.avenantEcritSigne()).toBe(true);

    component.onReglementTravailModifieChange(true);
    expect(component.reglementTravailModifie()).toBe(true);

    component.onDureeAvenantMoisChange('6');
    expect(component.dureeAvenantMois()).toBe(6);

    component.onAvenantRenouveleChange(true);
    expect(component.avenantRenouvele()).toBe(true);

    component.onRefusMotiveParEcritChange(false);
    expect(component.refusMotiveParEcrit()).toBe(false);

    component.onDateLicenciementChange('2025-08-15');
    expect(component.dateLicenciement()).toBe('2025-08-15');

    component.onMotifLicenciementObjectifEtabliChange(false);
    expect(component.motifLicenciementObjectifEtabli()).toBe(false);

    component.onDureeHebdomadaireHeuresChange(null);
    expect(component.dureeHebdomadaireHeures()).toBeNull();

    component.onDureeAvenantMoisChange('');
    expect(component.dureeAvenantMois()).toBeNull();
  });
});
