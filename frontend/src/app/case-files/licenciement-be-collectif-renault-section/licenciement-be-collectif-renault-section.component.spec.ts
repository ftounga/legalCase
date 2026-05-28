import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  LicenciementBeCollectifRenaultSectionComponent,
} from './licenciement-be-collectif-renault-section.component';
import {
  LicenciementBeCollectifRenaultResponse,
} from '../../core/models/licenciement-be-collectif-renault.model';

describe('LicenciementBeCollectifRenaultSectionComponent', () => {
  let component: LicenciementBeCollectifRenaultSectionComponent;
  let fixture: ComponentFixture<LicenciementBeCollectifRenaultSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/licenciement-be-collectif-renault';

  /**
   * Construit une réponse backend par défaut — verdict CONFORME
   * (PME 50 ETP, 12 lic./60 j > seuil 10, 3 phases respectées,
   * délai 30 j échu, aucun préavis prématuré).
   */
  function response(
    overrides: Partial<LicenciementBeCollectifRenaultResponse> = {},
  ): LicenciementBeCollectifRenaultResponse {
    return {
      caseFileId: 'case-1',
      dateProjet: '2026-01-10',
      tailleEntreprise: 'PME_20_99',
      effectifMoyen: 50,
      nombreLicenciementsEnvisages: 12,
      phaseAtteinte: 'DECISION_ET_NOTIFICATION',
      informationEcriteCePrealable: true,
      documentsLegauxCommuniques: true,
      consultationEffectiveTenue: true,
      reponsesMotiveesEmployeur: true,
      notificationAutoriteRegionaleFaite: true,
      dateNotificationAutorite: '2026-02-15',
      datePremierPreavisNotifie: '2026-03-25',
      verdict: 'CONFORME',
      conforme: true,
      seuilDeclenchementAtteint: true,
      seuilLegalApplicable: 10,
      dateFinDelaiAttente: '2026-03-17',
      delaiAttenteRespecte: true,
      raison: 'CONFORME_3_PHASES_DELAI_RESPECTE',
      synthese: 'Procédure Loi Renault conforme — préavis notifiables sans risque de nullité.',
      baseJuridique: 'Loi du 13/02/1998 + CCT n° 24 + CCT n° 39 + Directive 98/59/CE',
      avertissement: '',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        LicenciementBeCollectifRenaultSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(LicenciementBeCollectifRenaultSectionComponent);
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
    expect(component.result()!.verdict).toBe('CONFORME');
    expect(component.showForm()).toBe(false);
    expect(component.dateProjet()).toBe('2026-01-10');
    expect(component.tailleEntreprise()).toBe('PME_20_99');
    expect(component.effectifMoyen()).toBe(50);
    expect(component.nombreLicenciementsEnvisages()).toBe(12);
    expect(component.phaseAtteinte()).toBe('DECISION_ET_NOTIFICATION');
    expect(component.informationEcriteCePrealable()).toBe(true);
    expect(component.documentsLegauxCommuniques()).toBe(true);
    expect(component.consultationEffectiveTenue()).toBe(true);
    expect(component.reponsesMotiveesEmployeur()).toBe(true);
    expect(component.notificationAutoriteRegionaleFaite()).toBe(true);
    expect(component.dateNotificationAutorite()).toBe('2026-02-15');
    expect(component.datePremierPreavisNotifie()).toBe('2026-03-25');
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

  it('formValid() : true avec 5 champs requis remplis + notification non cochée', () => {
    component.dateProjet.set('2026-01-10');
    component.tailleEntreprise.set('PME_20_99');
    component.effectifMoyen.set(50);
    component.nombreLicenciementsEnvisages.set(12);
    component.phaseAtteinte.set('INFORMATION');
    // notificationAutoriteRegionaleFaite par défaut = false → dateNotif non requise
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false si notification cochée sans dateNotificationAutorite', () => {
    component.dateProjet.set('2026-01-10');
    component.tailleEntreprise.set('PME_20_99');
    component.effectifMoyen.set(50);
    component.nombreLicenciementsEnvisages.set(12);
    component.phaseAtteinte.set('DECISION_ET_NOTIFICATION');
    component.notificationAutoriteRegionaleFaite.set(true);
    // dateNotificationAutorite reste null
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true si notification cochée AVEC dateNotificationAutorite', () => {
    component.dateProjet.set('2026-01-10');
    component.tailleEntreprise.set('PME_20_99');
    component.effectifMoyen.set(50);
    component.nombreLicenciementsEnvisages.set(12);
    component.phaseAtteinte.set('DECISION_ET_NOTIFICATION');
    component.notificationAutoriteRegionaleFaite.set(true);
    component.dateNotificationAutorite.set('2026-02-15');
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans dateProjet', () => {
    component.tailleEntreprise.set('PME_20_99');
    component.effectifMoyen.set(50);
    component.nombreLicenciementsEnvisages.set(12);
    component.phaseAtteinte.set('INFORMATION');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans tailleEntreprise', () => {
    component.dateProjet.set('2026-01-10');
    component.effectifMoyen.set(50);
    component.nombreLicenciementsEnvisages.set(12);
    component.phaseAtteinte.set('INFORMATION');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans phaseAtteinte', () => {
    component.dateProjet.set('2026-01-10');
    component.tailleEntreprise.set('PME_20_99');
    component.effectifMoyen.set(50);
    component.nombreLicenciementsEnvisages.set(12);
    expect(component.formValid()).toBe(false);
  });

  it('handler notification décochée → efface dateNotificationAutorite (cohérence)', () => {
    component.dateNotificationAutorite.set('2026-02-15');
    component.notificationAutoriteRegionaleFaite.set(true);
    component.onNotificationAutoriteRegionaleFaiteChange(false);
    expect(component.notificationAutoriteRegionaleFaite()).toBe(false);
    expect(component.dateNotificationAutorite()).toBeNull();
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  function fillForm() {
    component.dateProjet.set('2026-01-10');
    component.tailleEntreprise.set('PME_20_99');
    component.effectifMoyen.set(50);
    component.nombreLicenciementsEnvisages.set(12);
    component.phaseAtteinte.set('DECISION_ET_NOTIFICATION');
    component.informationEcriteCePrealable.set(true);
    component.documentsLegauxCommuniques.set(true);
    component.consultationEffectiveTenue.set(true);
    component.reponsesMotiveesEmployeur.set(true);
    component.notificationAutoriteRegionaleFaite.set(true);
    component.dateNotificationAutorite.set('2026-02-15');
    component.datePremierPreavisNotifie.set('2026-03-25');
  }

  it('calculate() → POST + snack succès', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.dateProjet).toBe('2026-01-10');
    expect(post.request.body.tailleEntreprise).toBe('PME_20_99');
    expect(post.request.body.effectifMoyen).toBe(50);
    expect(post.request.body.nombreLicenciementsEnvisages).toBe(12);
    expect(post.request.body.phaseAtteinte).toBe('DECISION_ET_NOTIFICATION');
    expect(post.request.body.informationEcriteCePrealable).toBe(true);
    expect(post.request.body.documentsLegauxCommuniques).toBe(true);
    expect(post.request.body.consultationEffectiveTenue).toBe(true);
    expect(post.request.body.reponsesMotiveesEmployeur).toBe(true);
    expect(post.request.body.notificationAutoriteRegionaleFaite).toBe(true);
    expect(post.request.body.dateNotificationAutorite).toBe('2026-02-15');
    expect(post.request.body.datePremierPreavisNotifie).toBe('2026-03-25');
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Procédure Loi Renault analysée', 'OK', expect.anything(),
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

  it('verdict CONFORME → label, verdict-ok, verified', () => {
    component.result.set(response({ verdict: 'CONFORME' }));
    expect(component.verdictLabel()).toContain('Procédure conforme');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict NON_APPLICABLE_SEUIL → label, verdict-info, info', () => {
    component.result.set(response({
      verdict: 'NON_APPLICABLE_SEUIL',
      conforme: false,
      tailleEntreprise: 'PME_20_99',
      nombreLicenciementsEnvisages: 5,
      seuilDeclenchementAtteint: false,
    }));
    expect(component.verdictLabel()).toContain('non applicable');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('info');
  });

  it('verdict NON_CONFORME_PHASE_INFORMATION_INCOMPLETE → label, critical, block', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_PHASE_INFORMATION_INCOMPLETE',
      conforme: false,
      informationEcriteCePrealable: false,
    }));
    expect(component.verdictLabel()).toContain('phase 1 information incomplète');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('block');
  });

  it('verdict NON_CONFORME_CONSULTATION_INSUFFISANTE → label, critical, block', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_CONSULTATION_INSUFFISANTE',
      conforme: false,
      consultationEffectiveTenue: false,
    }));
    expect(component.verdictLabel()).toContain('phase 2 consultation insuffisante');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('block');
  });

  it('verdict NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE → label, critical, block', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE',
      conforme: false,
      notificationAutoriteRegionaleFaite: false,
      dateNotificationAutorite: null,
      dateFinDelaiAttente: null,
    }));
    expect(component.verdictLabel()).toContain('notification autorité manquante');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('block');
  });

  it('verdict NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE → label, critical, block', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE',
      conforme: false,
      delaiAttenteRespecte: false,
      datePremierPreavisNotifie: '2026-02-20',
    }));
    expect(component.verdictLabel()).toContain('délai 30 j non respecté');
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

  it('badge CONFORME = vert "CONFORME"', () => {
    expect(component.verdictBadge('CONFORME')).toBe('CONFORME');
    expect(component.verdictBadgeClass('CONFORME')).toBe('badge-ok');
  });

  it('badge NON_APPLICABLE_SEUIL = info "NON APPLICABLE"', () => {
    expect(component.verdictBadge('NON_APPLICABLE_SEUIL')).toBe('NON APPLICABLE');
    expect(component.verdictBadgeClass('NON_APPLICABLE_SEUIL')).toBe('badge-info');
  });

  it('badge NON_CONFORME_PHASE_INFORMATION_INCOMPLETE = rouge "INFO. INCOMPLÈTE"', () => {
    expect(component.verdictBadge('NON_CONFORME_PHASE_INFORMATION_INCOMPLETE')).toBe('INFO. INCOMPLÈTE');
    expect(component.verdictBadgeClass('NON_CONFORME_PHASE_INFORMATION_INCOMPLETE')).toBe('badge-critical');
  });

  it('badge NON_CONFORME_CONSULTATION_INSUFFISANTE = rouge "CONSULT. INSUFF."', () => {
    expect(component.verdictBadge('NON_CONFORME_CONSULTATION_INSUFFISANTE')).toBe('CONSULT. INSUFF.');
    expect(component.verdictBadgeClass('NON_CONFORME_CONSULTATION_INSUFFISANTE')).toBe('badge-critical');
  });

  it('badge NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE = rouge "NOTIF. MANQUANTE"', () => {
    expect(component.verdictBadge('NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE')).toBe('NOTIF. MANQUANTE');
    expect(component.verdictBadgeClass('NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE')).toBe('badge-critical');
  });

  it('badge NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE = rouge "DÉLAI 30 J VIOLÉ"', () => {
    expect(component.verdictBadge('NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE')).toBe('DÉLAI 30 J VIOLÉ');
    expect(component.verdictBadgeClass('NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE')).toBe('badge-critical');
  });

  // ============================================================
  // DOM integration : badge header + bannière délai
  // ============================================================

  it('CONFORME → badge vert dans le DOM + bannière délai d\'attente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();

    const badge = fixture.nativeElement.querySelector('.section-badge');
    expect(badge).not.toBeNull();
    expect(badge.textContent.trim()).toBe('CONFORME');
    expect(badge.classList.contains('badge-ok')).toBe(true);

    const html = fixture.nativeElement.textContent;
    expect(html).toContain('Fin du délai d\'attente 30 j');
    expect(html).toContain('respecté');
  });

  it('NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE → badge rouge + bannière délai violé', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE',
      conforme: false,
      delaiAttenteRespecte: false,
      datePremierPreavisNotifie: '2026-02-20',
    }));
    fixture.detectChanges();

    const badge = fixture.nativeElement.querySelector('.section-badge');
    expect(badge.classList.contains('badge-critical')).toBe(true);

    const html = fixture.nativeElement.textContent;
    expect(html).toContain('violé');
  });

  // ============================================================
  // editMode
  // ============================================================

  it('editMode() → showForm = true', () => {
    component.result.set(response());
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ============================================================
  // Pré-fill — invariant V1 = 0
  // ============================================================

  it('getPrefillCount(...) → 0 (V1)', () => {
    expect(LicenciementBeCollectifRenaultSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: { dateRuptureContrat: '2026-09-30', motifRupture: 'collectif' },
    })).toBe(0);
  });

  // ============================================================
  // Tool metadata statique (F-177 SF-177-03b)
  // ============================================================

  it('TOOL_LABEL + TOOL_ICON exposés', () => {
    expect(LicenciementBeCollectifRenaultSectionComponent.TOOL_LABEL).toBe('LICENCIEMENT COLLECTIF (BE / RENAULT)');
    expect(LicenciementBeCollectifRenaultSectionComponent.TOOL_ICON).toBe('groups');
  });

  // ============================================================
  // Labels enums lisibles
  // ============================================================

  it('tailleEntrepriseLabel mappe les 4 enums + fallback', () => {
    expect(component.tailleEntrepriseLabel('PETITE_MOINS_20')).toContain('< 20');
    expect(component.tailleEntrepriseLabel('PME_20_99')).toContain('PME');
    expect(component.tailleEntrepriseLabel('GRANDE_100_299')).toContain('Grande');
    expect(component.tailleEntrepriseLabel('TRES_GRANDE_300_PLUS')).toContain('Très grande');
    expect(component.tailleEntrepriseLabel(null)).toBe('—');
  });

  it('phaseAtteinteLabel mappe les 4 enums + fallback', () => {
    expect(component.phaseAtteinteLabel('AUCUNE_PROCEDURE_DEMARREE')).toContain('Aucune procédure');
    expect(component.phaseAtteinteLabel('INFORMATION')).toContain('Phase 1');
    expect(component.phaseAtteinteLabel('CONSULTATION')).toContain('Phase 2');
    expect(component.phaseAtteinteLabel('DECISION_ET_NOTIFICATION')).toContain('Phase 3');
    expect(component.phaseAtteinteLabel(null)).toBe('—');
  });
});
