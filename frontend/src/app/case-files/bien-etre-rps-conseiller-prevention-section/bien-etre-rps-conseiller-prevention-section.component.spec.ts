import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  BienEtreRpsConseillerPreventionSectionComponent,
} from './bien-etre-rps-conseiller-prevention-section.component';
import {
  BienEtreRpsConseillerPreventionResponse,
} from '../../core/models/bien-etre-rps-conseiller-prevention.model';

describe('BienEtreRpsConseillerPreventionSectionComponent', () => {
  let component: BienEtreRpsConseillerPreventionSectionComponent;
  let fixture: ComponentFixture<BienEtreRpsConseillerPreventionSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/bien-etre-rps-conseiller-prevention';

  /**
   * Reponse backend par defaut — verdict SAISINE_CONFORME
   * (cas favorable typique : toutes formalites remplies pour une
   * demande formelle individuelle).
   */
  function response(
    overrides: Partial<BienEtreRpsConseillerPreventionResponse> = {},
  ): BienEtreRpsConseillerPreventionResponse {
    return {
      caseFileId: 'case-1',
      typeRisque: 'HARCELEMENT_MORAL',
      modeDemande: 'FORMELLE_INDIVIDUELLE',
      etapeProcedure: 'DEMANDE_FORMELLE_DEPOSEE',
      conseillerIdentifie: true,
      entretienPrealableRealise: true,
      demandeEcriteSignee: true,
      accuseReceptionRecu: true,
      notificationEmployeurFaite: true,
      dateDepotDemande: '2026-04-15',
      dateAvisRendu: null,
      joursDepuisDepot: 43,
      verdict: 'SAISINE_CONFORME',
      formalitesPrealablesCompletes: true,
      delaiEnqueteRespecte: true,
      echeanceEnqueteTroisMois: '2026-07-15',
      echeanceMesuresEmployeurDeuxMois: null,
      finProtectionRepresailles: '2027-04-15',
      protectionRepresaillesActive: true,
      raison: 'SAISINE_CONFORME_FORMALITES_REMPLIES',
      synthese: 'Saisine CPAP conforme — protection 12 mois active.',
      baseJuridique: 'Loi du 04/08/1996 art. 32sexies + AR du 10/04/2014 art. 16 a 22.',
      avertissement: 'Suivre le delai 3 mois enquete art. 22 § 1.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        BienEtreRpsConseillerPreventionSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(BienEtreRpsConseillerPreventionSectionComponent);
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

  it('FRANCE => banniere "reserve droit BE" + masquage form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.empty-result');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('belge');
    expect(banner.textContent).toContain('CPAP');
  });

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 => hydrate result + mode resultat + rehydrate form', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('SAISINE_CONFORME');
    expect(component.showForm()).toBe(false);
    expect(component.typeRisque()).toBe('HARCELEMENT_MORAL');
    expect(component.modeDemande()).toBe('FORMELLE_INDIVIDUELLE');
    expect(component.etapeProcedure()).toBe('DEMANDE_FORMELLE_DEPOSEE');
    expect(component.conseillerIdentifie()).toBe(true);
    expect(component.entretienPrealableRealise()).toBe(true);
    expect(component.demandeEcriteSignee()).toBe(true);
    expect(component.accuseReceptionRecu()).toBe(true);
    expect(component.notificationEmployeurFaite()).toBe(true);
    expect(component.dateDepotDemande()).toBe('2026-04-15');
    expect(component.dateAvisRendu()).toBeNull();
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
    component.typeRisque.set('HARCELEMENT_MORAL');
    component.modeDemande.set('FORMELLE_INDIVIDUELLE');
    component.etapeProcedure.set('DEMANDE_FORMELLE_DEPOSEE');
    component.conseillerIdentifie.set(true);
    component.entretienPrealableRealise.set(true);
    component.demandeEcriteSignee.set(true);
    component.accuseReceptionRecu.set(true);
    component.notificationEmployeurFaite.set(true);
    component.dateDepotDemande.set('2026-04-15');
    component.dateAvisRendu.set(null);
  }

  it('formValid() : true avec les champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans typeRisque', () => {
    fillForm();
    component.typeRisque.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans modeDemande', () => {
    fillForm();
    component.modeDemande.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans etapeProcedure', () => {
    fillForm();
    component.etapeProcedure.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false si etape AVIS_RENDU sans dateAvisRendu', () => {
    fillForm();
    component.etapeProcedure.set('AVIS_RENDU');
    component.dateAvisRendu.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true si etape AVIS_RENDU avec dateAvisRendu', () => {
    fillForm();
    component.etapeProcedure.set('AVIS_RENDU');
    component.dateAvisRendu.set('2026-07-10');
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false si etape MESURES_PRISES_PAR_EMPLOYEUR sans dateAvisRendu', () => {
    fillForm();
    component.etapeProcedure.set('MESURES_PRISES_PAR_EMPLOYEUR');
    component.dateAvisRendu.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false si avis avant depot', () => {
    fillForm();
    component.etapeProcedure.set('AVIS_RENDU');
    component.dateDepotDemande.set('2026-04-15');
    component.dateAvisRendu.set('2026-03-01');
    expect(component.formValid()).toBe(false);
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
    expect(post.request.body.typeRisque).toBe('HARCELEMENT_MORAL');
    expect(post.request.body.modeDemande).toBe('FORMELLE_INDIVIDUELLE');
    expect(post.request.body.etapeProcedure).toBe('DEMANDE_FORMELLE_DEPOSEE');
    expect(post.request.body.conseillerIdentifie).toBe(true);
    expect(post.request.body.entretienPrealableRealise).toBe(true);
    expect(post.request.body.demandeEcriteSignee).toBe(true);
    expect(post.request.body.accuseReceptionRecu).toBe(true);
    expect(post.request.body.notificationEmployeurFaite).toBe(true);
    expect(post.request.body.dateDepotDemande).toBe('2026-04-15');
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse saisine CPAP calculee', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 => snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'typeRisque est requis' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'typeRisque est requis', 'Fermer', expect.anything(),
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

  it('verdict SAISINE_CONFORME => info, check_circle', () => {
    component.result.set(response());
    expect(component.verdictLabel()).toContain('conforme');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('check_circle');
  });

  it('verdict SAISINE_INFORMELLE_EN_COURS => neutral, forum', () => {
    component.result.set(response({
      verdict: 'SAISINE_INFORMELLE_EN_COURS',
      modeDemande: 'INFORMELLE',
      etapeProcedure: 'DEMANDE_INFORMELLE_EN_COURS',
    }));
    expect(component.verdictLabel()).toContain('informelle en cours');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('forum');
  });

  it('verdict SAISINE_FORMELLE_EN_COURS => info, pending_actions', () => {
    component.result.set(response({
      verdict: 'SAISINE_FORMELLE_EN_COURS',
    }));
    expect(component.verdictLabel()).toContain('formelle deposee');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('pending_actions');
  });

  it('verdict AVIS_RENDU_DELAI_RESPECTE => info, verified', () => {
    component.result.set(response({
      verdict: 'AVIS_RENDU_DELAI_RESPECTE',
      etapeProcedure: 'AVIS_RENDU',
      dateAvisRendu: '2026-07-10',
    }));
    expect(component.verdictLabel()).toContain('3 mois legaux');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict AVIS_RENDU_DELAI_DEPASSE => warn, schedule', () => {
    component.result.set(response({
      verdict: 'AVIS_RENDU_DELAI_DEPASSE',
      etapeProcedure: 'AVIS_RENDU',
      dateAvisRendu: '2026-10-30',
      delaiEnqueteRespecte: false,
    }));
    expect(component.verdictLabel()).toContain('hors delai');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('schedule');
  });

  it('verdict NON_CONFORME_FORMALITES_MANQUANTES => critical, report', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_FORMALITES_MANQUANTES',
      entretienPrealableRealise: false,
      formalitesPrealablesCompletes: false,
    }));
    expect(component.verdictLabel()).toContain('formalites prealables');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('report');
  });

  it('verdict NON_CONFORME_PAS_DE_CONSEILLER => critical, person_off', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_PAS_DE_CONSEILLER',
      conseillerIdentifie: false,
    }));
    expect(component.verdictLabel()).toContain('manquement employeur');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('person_off');
  });

  it('verdict A_QUALIFIER => neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_QUALIFIER',
    }));
    expect(component.verdictLabel()).toContain('Qualification ouverte');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null => labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('psychology');
  });

  // ============================================================
  // Badge dans header (8 cas)
  // ============================================================

  it('badge SAISINE_CONFORME = info', () => {
    expect(component.verdictBadge('SAISINE_CONFORME')).toBe('CONFORME');
    expect(component.verdictBadgeClass('SAISINE_CONFORME')).toBe('badge-info');
  });

  it('badge SAISINE_INFORMELLE_EN_COURS = neutral', () => {
    expect(component.verdictBadge('SAISINE_INFORMELLE_EN_COURS')).toContain('INFORMELLE');
    expect(component.verdictBadgeClass('SAISINE_INFORMELLE_EN_COURS')).toBe('badge-neutral');
  });

  it('badge SAISINE_FORMELLE_EN_COURS = info', () => {
    expect(component.verdictBadge('SAISINE_FORMELLE_EN_COURS')).toContain('FORMELLE');
    expect(component.verdictBadgeClass('SAISINE_FORMELLE_EN_COURS')).toBe('badge-info');
  });

  it('badge AVIS_RENDU_DELAI_RESPECTE = info', () => {
    expect(component.verdictBadge('AVIS_RENDU_DELAI_RESPECTE')).toContain('OK');
    expect(component.verdictBadgeClass('AVIS_RENDU_DELAI_RESPECTE')).toBe('badge-info');
  });

  it('badge AVIS_RENDU_DELAI_DEPASSE = warn', () => {
    expect(component.verdictBadge('AVIS_RENDU_DELAI_DEPASSE')).toContain('HORS DELAI');
    expect(component.verdictBadgeClass('AVIS_RENDU_DELAI_DEPASSE')).toBe('badge-warn');
  });

  it('badge NON_CONFORME_FORMALITES_MANQUANTES = critical', () => {
    expect(component.verdictBadge('NON_CONFORME_FORMALITES_MANQUANTES')).toContain('FORMALITES');
    expect(component.verdictBadgeClass('NON_CONFORME_FORMALITES_MANQUANTES')).toBe('badge-critical');
  });

  it('badge NON_CONFORME_PAS_DE_CONSEILLER = critical', () => {
    expect(component.verdictBadge('NON_CONFORME_PAS_DE_CONSEILLER')).toContain('CPAP');
    expect(component.verdictBadgeClass('NON_CONFORME_PAS_DE_CONSEILLER')).toBe('badge-critical');
  });

  it('badge A_QUALIFIER = neutre', () => {
    expect(component.verdictBadge('A_QUALIFIER')).toBe('A QUALIFIER');
    expect(component.verdictBadgeClass('A_QUALIFIER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — rendu verdict
  // ============================================================

  it('SAISINE_CONFORME => badge info + ventilations rendues', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-info');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('CONFORME');
    const ventilations = fixture.nativeElement.querySelectorAll('.ventilation-row');
    // Au moins 4 ventilations fixes + dates conditionnelles + protection.
    expect(ventilations.length).toBeGreaterThanOrEqual(6);
  });

  // ============================================================
  // Libelles
  // ============================================================

  it('libelle typeRisque : 4 cas + null', () => {
    expect(component.typeRisqueLabel('HARCELEMENT_MORAL')).toContain('Harcelement moral');
    expect(component.typeRisqueLabel('HARCELEMENT_SEXUEL')).toContain('Harcelement sexuel');
    expect(component.typeRisqueLabel('VIOLENCE_AU_TRAVAIL')).toContain('Violence');
    expect(component.typeRisqueLabel('AUTRES_RPS')).toContain('Autres RPS');
    expect(component.typeRisqueLabel(null)).toBe('—');
  });

  it('libelle modeDemande : 3 cas + null', () => {
    expect(component.modeDemandeLabel('INFORMELLE')).toContain('informelle');
    expect(component.modeDemandeLabel('FORMELLE_INDIVIDUELLE')).toContain('individuelle');
    expect(component.modeDemandeLabel('FORMELLE_COLLECTIVE')).toContain('collective');
    expect(component.modeDemandeLabel(null)).toBe('—');
  });

  it('libelle etapeProcedure : 6 cas + null', () => {
    expect(component.etapeProcedureLabel('INTENTION_DEMANDE')).toContain('Intention');
    expect(component.etapeProcedureLabel('ENTRETIEN_PREALABLE_REALISE')).toContain('Entretien');
    expect(component.etapeProcedureLabel('DEMANDE_INFORMELLE_EN_COURS')).toContain('informelle');
    expect(component.etapeProcedureLabel('DEMANDE_FORMELLE_DEPOSEE')).toContain('formelle deposee');
    expect(component.etapeProcedureLabel('AVIS_RENDU')).toContain('Avis motive');
    expect(component.etapeProcedureLabel('MESURES_PRISES_PAR_EMPLOYEUR')).toContain('Mesures');
    expect(component.etapeProcedureLabel(null)).toBe('—');
  });

  // ============================================================
  // Formats
  // ============================================================

  it('formatDate : ISO -> jj/mm/aaaa', () => {
    expect(component.formatDate('2026-04-15')).toBe('15/04/2026');
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

  it('formatJours : valeurs + null', () => {
    expect(component.formatJours(0)).toContain('0 jour');
    expect(component.formatJours(1)).toContain('1 jour');
    expect(component.formatJours(43)).toContain('jours');
    expect(component.formatJours(null)).toBe('—');
    expect(component.formatJours(undefined)).toBe('—');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non vide => banniere ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      avertissement: 'Suivre la notification des mesures employeur.',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('mesures');
  });

  // ============================================================
  // Synthese
  // ============================================================

  it('synthese affichee si presente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Saisine CPAP conforme — protection 12 mois active.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('CPAP');
  });

  // ============================================================
  // F-177 SF-177-12 — parite getPrefillCount static
  // ============================================================

  it('static getPrefillCount => 0 (V1)', () => {
    expect(BienEtreRpsConseillerPreventionSectionComponent.getPrefillCount({})).toBe(0);
    expect(BienEtreRpsConseillerPreventionSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateNaissanceSalarie: '1985-04-12',
        dateRuptureContrat: '2026-09-30',
        motifRupture: 'autre',
        anneesCarriereSalarie: 12,
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

  it('toolId pour jurisprudence = bien-etre-rps-conseiller-prevention', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('bien-etre-rps-conseiller-prevention');
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
    component.onTypeRisqueChange('HARCELEMENT_SEXUEL');
    expect(component.typeRisque()).toBe('HARCELEMENT_SEXUEL');

    component.onModeDemandeChange('INFORMELLE');
    expect(component.modeDemande()).toBe('INFORMELLE');

    component.onEtapeProcedureChange('ENTRETIEN_PREALABLE_REALISE');
    expect(component.etapeProcedure()).toBe('ENTRETIEN_PREALABLE_REALISE');

    component.onConseillerIdentifieChange(true);
    expect(component.conseillerIdentifie()).toBe(true);

    component.onEntretienPrealableRealiseChange(true);
    expect(component.entretienPrealableRealise()).toBe(true);

    component.onDemandeEcriteSigneeChange(true);
    expect(component.demandeEcriteSignee()).toBe(true);

    component.onAccuseReceptionRecuChange(true);
    expect(component.accuseReceptionRecu()).toBe(true);

    component.onNotificationEmployeurFaiteChange(true);
    expect(component.notificationEmployeurFaite()).toBe(true);

    component.onDateDepotDemandeChange('2026-04-15');
    expect(component.dateDepotDemande()).toBe('2026-04-15');

    component.onDateAvisRenduChange('2026-07-10');
    expect(component.dateAvisRendu()).toBe('2026-07-10');
  });
});
