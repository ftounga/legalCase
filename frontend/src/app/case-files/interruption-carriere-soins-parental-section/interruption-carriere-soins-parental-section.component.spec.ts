import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  InterruptionCarriereSoinsParentalSectionComponent,
} from './interruption-carriere-soins-parental-section.component';
import {
  InterruptionCarriereSoinsParentalResponse,
} from '../../core/models/interruption-carriere-soins-parental.model';

describe('InterruptionCarriereSoinsParentalSectionComponent', () => {
  let component: InterruptionCarriereSoinsParentalSectionComponent;
  let fixture: ComponentFixture<InterruptionCarriereSoinsParentalSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/interruption-carriere-soins-parental';

  /**
   * Reponse backend par defaut — verdict ELIGIBLE_COMPLET
   * (cas favorable typique : temps plein 4 mois, anciennete 18 mois,
   * enfant 2 ans, solde 4 mois, recommandee, accord employeur).
   */
  function response(
    overrides: Partial<InterruptionCarriereSoinsParentalResponse> = {},
  ): InterruptionCarriereSoinsParentalResponse {
    return {
      caseFileId: 'case-1',
      forme: 'TEMPS_PLEIN',
      ancienneteMois: 18,
      ageEnfantMois: 24,
      enfantHandicap: false,
      soldeRestantMoisEtp: 4,
      modeNotification: 'LETTRE_RECOMMANDEE',
      employeurAccepte: true,
      employeurAdiffere: false,
      cumulAllocationOnemDemande: true,
      dateDebutInterruption: '2026-09-01',
      dateNotification: '2026-06-15',
      remunerationMensuelleBrute: 3200,
      verdict: 'ELIGIBLE_COMPLET',
      ancienneteOk: true,
      ageEnfantOk: true,
      formalismeOk: true,
      dureeInterruptionMois: 4,
      soldeRestantApresImputationMoisEtp: 0,
      allocationOnemMensuelle: 870,
      allocationOnemTotale: 3480,
      dateFinInterruption: '2026-12-31',
      finProtectionLicenciement: '2027-03-31',
      protectionLicenciementActive: true,
      indemniteProtectionEnCasLicenciement: 19200,
      raison: 'ELIGIBILITE_COMPLETE',
      synthese: 'Eligibilite complete au conge parental temps plein 4 mois.',
      baseJuridique: 'Loi 22/01/1985 art. 99 + AR 29/10/1997 + CCT 64.',
      avertissement: 'Suivre le depot du formulaire ONEM C61 / C62.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        InterruptionCarriereSoinsParentalSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(InterruptionCarriereSoinsParentalSectionComponent);
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
    expect(banner.textContent).toContain('CAF');
  });

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 => hydrate result + mode resultat + rehydrate form', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('ELIGIBLE_COMPLET');
    expect(component.showForm()).toBe(false);
    expect(component.forme()).toBe('TEMPS_PLEIN');
    expect(component.ancienneteMois()).toBe(18);
    expect(component.ageEnfantMois()).toBe(24);
    expect(component.enfantHandicap()).toBe(false);
    expect(component.soldeRestantMoisEtp()).toBe(4);
    expect(component.modeNotification()).toBe('LETTRE_RECOMMANDEE');
    expect(component.employeurAccepte()).toBe(true);
    expect(component.employeurAdiffere()).toBe(false);
    expect(component.cumulAllocationOnemDemande()).toBe(true);
    expect(component.dateDebutInterruption()).toBe('2026-09-01');
    expect(component.dateNotification()).toBe('2026-06-15');
    expect(component.remunerationMensuelleBrute()).toBe(3200);
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
    component.forme.set('TEMPS_PLEIN');
    component.ancienneteMois.set(18);
    component.ageEnfantMois.set(24);
    component.enfantHandicap.set(false);
    component.soldeRestantMoisEtp.set(4);
    component.modeNotification.set('LETTRE_RECOMMANDEE');
    component.employeurAccepte.set(true);
    component.employeurAdiffere.set(false);
    component.cumulAllocationOnemDemande.set(true);
    component.dateDebutInterruption.set('2026-09-01');
    component.dateNotification.set('2026-06-15');
    component.remunerationMensuelleBrute.set(3200);
  }

  it('formValid() : true avec champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans forme', () => {
    fillForm();
    component.forme.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans mode notification', () => {
    fillForm();
    component.modeNotification.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans anciennete', () => {
    fillForm();
    component.ancienneteMois.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec anciennete negative', () => {
    fillForm();
    component.ancienneteMois.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans age enfant', () => {
    fillForm();
    component.ageEnfantMois.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec age enfant negatif', () => {
    fillForm();
    component.ageEnfantMois.set(-2);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans solde restant', () => {
    fillForm();
    component.soldeRestantMoisEtp.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec solde negatif', () => {
    fillForm();
    component.soldeRestantMoisEtp.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec remuneration negative', () => {
    fillForm();
    component.remunerationMensuelleBrute.set(-100);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true avec remuneration null (optionnelle)', () => {
    fillForm();
    component.remunerationMensuelleBrute.set(null);
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false si notification posterieure au debut', () => {
    fillForm();
    component.dateNotification.set('2026-12-01');
    component.dateDebutInterruption.set('2026-09-01');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true sans dates (optionnelles)', () => {
    fillForm();
    component.dateNotification.set(null);
    component.dateDebutInterruption.set(null);
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
    expect(post.request.body.forme).toBe('TEMPS_PLEIN');
    expect(post.request.body.ancienneteMois).toBe(18);
    expect(post.request.body.ageEnfantMois).toBe(24);
    expect(post.request.body.soldeRestantMoisEtp).toBe(4);
    expect(post.request.body.modeNotification).toBe('LETTRE_RECOMMANDEE');
    expect(post.request.body.employeurAccepte).toBe(true);
    expect(post.request.body.cumulAllocationOnemDemande).toBe(true);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse conge parental calculee', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 => snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'ancienneteMois doit etre >= 0' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'ancienneteMois doit etre >= 0', 'Fermer', expect.anything(),
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

  it('verdict ELIGIBLE_COMPLET => info, check_circle', () => {
    component.result.set(response());
    expect(component.verdictLabel()).toContain('Eligibilite complete');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('check_circle');
  });

  it('verdict ELIGIBLE_AVEC_RESERVES => neutral, verified', () => {
    component.result.set(response({ verdict: 'ELIGIBLE_AVEC_RESERVES' }));
    expect(component.verdictLabel()).toContain('points de vigilance');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict INELIGIBLE_ANCIENNETE => critical, work_history', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_ANCIENNETE',
      ancienneteOk: false,
    }));
    expect(component.verdictLabel()).toContain('anciennete insuffisante');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('work_history');
  });

  it('verdict INELIGIBLE_AGE_ENFANT => critical, child_care', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_AGE_ENFANT',
      ageEnfantOk: false,
    }));
    expect(component.verdictLabel()).toContain('hors plafond');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('child_care');
  });

  it('verdict INELIGIBLE_SOLDE_INSUFFISANT => critical, production_quantity_limits', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_SOLDE_INSUFFISANT',
    }));
    expect(component.verdictLabel()).toContain('solde individuel insuffisant');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('production_quantity_limits');
  });

  it('verdict INELIGIBLE_FORMALISME => warn, report', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_FORMALISME',
      formalismeOk: false,
    }));
    expect(component.verdictLabel()).toContain('vice de formalisme');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('report');
  });

  it('verdict DIFFERE_EMPLOYEUR => warn, schedule', () => {
    component.result.set(response({
      verdict: 'DIFFERE_EMPLOYEUR',
      employeurAdiffere: true,
    }));
    expect(component.verdictLabel()).toContain('differe motive');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('schedule');
  });

  it('verdict A_QUALIFIER => neutral, help_outline', () => {
    component.result.set(response({ verdict: 'A_QUALIFIER' }));
    expect(component.verdictLabel()).toContain('Qualification ouverte');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null => labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('family_restroom');
  });

  // ============================================================
  // Badge dans header (8 cas)
  // ============================================================

  it('badge ELIGIBLE_COMPLET = info', () => {
    expect(component.verdictBadge('ELIGIBLE_COMPLET')).toBe('ELIGIBLE');
    expect(component.verdictBadgeClass('ELIGIBLE_COMPLET')).toBe('badge-info');
  });

  it('badge ELIGIBLE_AVEC_RESERVES = neutre', () => {
    expect(component.verdictBadge('ELIGIBLE_AVEC_RESERVES')).toContain('RESERVES');
    expect(component.verdictBadgeClass('ELIGIBLE_AVEC_RESERVES')).toBe('badge-neutral');
  });

  it('badge INELIGIBLE_ANCIENNETE = critical', () => {
    expect(component.verdictBadge('INELIGIBLE_ANCIENNETE')).toBe('ANCIENNETE KO');
    expect(component.verdictBadgeClass('INELIGIBLE_ANCIENNETE')).toBe('badge-critical');
  });

  it('badge INELIGIBLE_AGE_ENFANT = critical', () => {
    expect(component.verdictBadge('INELIGIBLE_AGE_ENFANT')).toBe('AGE ENFANT KO');
    expect(component.verdictBadgeClass('INELIGIBLE_AGE_ENFANT')).toBe('badge-critical');
  });

  it('badge INELIGIBLE_SOLDE_INSUFFISANT = critical', () => {
    expect(component.verdictBadge('INELIGIBLE_SOLDE_INSUFFISANT')).toBe('SOLDE KO');
    expect(component.verdictBadgeClass('INELIGIBLE_SOLDE_INSUFFISANT')).toBe('badge-critical');
  });

  it('badge INELIGIBLE_FORMALISME = warn', () => {
    expect(component.verdictBadge('INELIGIBLE_FORMALISME')).toBe('FORMALISME KO');
    expect(component.verdictBadgeClass('INELIGIBLE_FORMALISME')).toBe('badge-warn');
  });

  it('badge DIFFERE_EMPLOYEUR = warn', () => {
    expect(component.verdictBadge('DIFFERE_EMPLOYEUR')).toContain('DIFFERE');
    expect(component.verdictBadgeClass('DIFFERE_EMPLOYEUR')).toBe('badge-warn');
  });

  it('badge A_QUALIFIER = neutre', () => {
    expect(component.verdictBadge('A_QUALIFIER')).toBe('A QUALIFIER');
    expect(component.verdictBadgeClass('A_QUALIFIER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — rendu verdict
  // ============================================================

  it('ELIGIBLE_COMPLET => badge info rendu', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-info');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('ELIGIBLE');
  });

  // ============================================================
  // Libelles
  // ============================================================

  it('libelle forme : 3 cas + null', () => {
    expect(component.formeLabel('TEMPS_PLEIN')).toContain('Temps plein');
    expect(component.formeLabel('MI_TEMPS')).toContain('Mi-temps');
    expect(component.formeLabel('CINQUIEME_TEMPS')).toContain('Cinquieme');
    expect(component.formeLabel(null)).toBe('—');
  });

  it('libelle mode notification : 3 cas + null', () => {
    expect(component.modeNotificationLabel('LETTRE_RECOMMANDEE')).toContain('recommandee');
    expect(component.modeNotificationLabel('REMISE_EN_MAIN')).toContain('main');
    expect(component.modeNotificationLabel('AUTRE')).toContain('non conforme');
    expect(component.modeNotificationLabel(null)).toBe('—');
  });

  // ============================================================
  // Formats
  // ============================================================

  it('formatDate : ISO -> jj/mm/aaaa', () => {
    expect(component.formatDate('2026-09-01')).toBe('01/09/2026');
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

  it('formatMontant : EUR fr-BE', () => {
    const v = component.formatMontant(3200);
    expect(v).toContain('3');
    expect(v).toContain('200');
    expect(v).toContain('€');
    expect(component.formatMontant(null)).toBe('—');
    expect(component.formatMontant(undefined)).toBe('—');
  });

  it('formatMois : suffixe mois', () => {
    expect(component.formatMois(4)).toContain('4');
    expect(component.formatMois(4)).toContain('mois');
    expect(component.formatMois(null)).toBe('—');
    expect(component.formatMois(undefined)).toBe('—');
  });

  // ============================================================
  // Avertissement / synthese
  // ============================================================

  it('avertissement non vide => banniere ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      avertissement: 'Verifier le depot ONEM dans les delais utiles.',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('ONEM');
  });

  it('synthese affichee si presente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Conge parental temps plein 4 mois eligible.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('temps plein');
  });

  // ============================================================
  // F-177 SF-177-12 — parite getPrefillCount static
  // ============================================================

  it('static getPrefillCount => 0 (V1)', () => {
    expect(InterruptionCarriereSoinsParentalSectionComponent.getPrefillCount({})).toBe(0);
    expect(InterruptionCarriereSoinsParentalSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateNaissanceSalarie: '1985-06-20',
        dateRuptureContrat: '2026-09-30',
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

  it('toolId pour jurisprudence = interruption-carriere-soins-parental', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('interruption-carriere-soins-parental');
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
    component.onFormeChange('TEMPS_PLEIN');
    expect(component.forme()).toBe('TEMPS_PLEIN');

    component.onAncienneteMoisChange(18);
    expect(component.ancienneteMois()).toBe(18);

    component.onAncienneteMoisChange('24');
    expect(component.ancienneteMois()).toBe(24);

    component.onAncienneteMoisChange(null);
    expect(component.ancienneteMois()).toBeNull();

    component.onAncienneteMoisChange('');
    expect(component.ancienneteMois()).toBeNull();

    component.onAgeEnfantMoisChange(24);
    expect(component.ageEnfantMois()).toBe(24);

    component.onEnfantHandicapChange(true);
    expect(component.enfantHandicap()).toBe(true);

    component.onSoldeRestantMoisEtpChange(4);
    expect(component.soldeRestantMoisEtp()).toBe(4);

    component.onModeNotificationChange('LETTRE_RECOMMANDEE');
    expect(component.modeNotification()).toBe('LETTRE_RECOMMANDEE');

    component.onEmployeurAccepteChange(true);
    expect(component.employeurAccepte()).toBe(true);

    component.onEmployeurAdiffereChange(true);
    expect(component.employeurAdiffere()).toBe(true);

    component.onCumulAllocationOnemDemandeChange(true);
    expect(component.cumulAllocationOnemDemande()).toBe(true);

    component.onDateDebutInterruptionChange('2026-09-01');
    expect(component.dateDebutInterruption()).toBe('2026-09-01');

    component.onDateDebutInterruptionChange('');
    expect(component.dateDebutInterruption()).toBeNull();

    component.onDateNotificationChange('2026-06-15');
    expect(component.dateNotification()).toBe('2026-06-15');

    component.onDateNotificationChange(null);
    expect(component.dateNotification()).toBeNull();

    component.onRemunerationMensuelleBruteChange(3200);
    expect(component.remunerationMensuelleBrute()).toBe(3200);

    component.onRemunerationMensuelleBruteChange(null);
    expect(component.remunerationMensuelleBrute()).toBeNull();
  });
});
