import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  DroitDeconnexionBeSectionComponent,
} from './droit-deconnexion-be-section.component';
import {
  DroitDeconnexionBeResponse,
} from '../../core/models/droit-deconnexion-be.model';

describe('DroitDeconnexionBeSectionComponent', () => {
  let component: DroitDeconnexionBeSectionComponent;
  let fixture: ComponentFixture<DroitDeconnexionBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/droit-deconnexion-be';

  /**
   * Construit une réponse backend par défaut — verdict
   * CONFORME_ACCORD_COMPLET (employeur 50 travailleurs, CCT déposée
   * 01/04/2023, 3 thèmes art. 16 § 2 traités, consultation effectuée).
   */
  function response(
    overrides: Partial<DroitDeconnexionBeResponse> = {},
  ): DroitDeconnexionBeResponse {
    return {
      caseFileId: 'case-1',
      effectifEntreprise: 50,
      statutAccord: 'CCT_ENTREPRISE_DEPOSEE',
      dateEntreeVigueurInstrument: '2023-04-01',
      modalitesPratiquesDeconnexionDefinies: true,
      sensibilisationFormationPrevue: true,
      modalitesOrganisationTravailDefinies: true,
      consultationOrganeConcertationEffectuee: true,
      manquementSignaleCbeOuSpf: false,
      verdict: 'CONFORME_ACCORD_COMPLET',
      seuilAtteint: true,
      instrumentFormalise: true,
      contenuComplet: true,
      modalitesPratiquesRespectees: true,
      sensibilisationRespectee: true,
      modalitesOrganisationRespectees: true,
      consultationRespectee: true,
      raison: 'CONFORME_ACCORD_COMPLET',
      synthese: 'Obligation déconnexion respectée (art. 16 Loi 03/10/2022 — 3 thèmes traités).',
      baseJuridique: 'Loi du 03/10/2022 « Deal pour l\'emploi » M.B. 10/11/2022, art. 16 + AR du 19/02/2023 + CCT n° 149 ; Loi du 08/04/1965 art. 11-12 ; C. pén. social art. 137.',
      avertissement: 'Articulation avec la CCT n° 149 (cadre supplétif) à vérifier au cas par cas.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        DroitDeconnexionBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(DroitDeconnexionBeSectionComponent);
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
    expect(banner.textContent).toContain('El Khomri');
  });

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 → hydrate result + mode résultat + rehydrate form pour édition', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('CONFORME_ACCORD_COMPLET');
    expect(component.showForm()).toBe(false);
    expect(component.effectifEntreprise()).toBe(50);
    expect(component.statutAccord()).toBe('CCT_ENTREPRISE_DEPOSEE');
    expect(component.dateEntreeVigueurInstrument()).toBe('2023-04-01');
    expect(component.modalitesPratiquesDeconnexionDefinies()).toBe(true);
    expect(component.sensibilisationFormationPrevue()).toBe(true);
    expect(component.modalitesOrganisationTravailDefinies()).toBe(true);
    expect(component.consultationOrganeConcertationEffectuee()).toBe(true);
    expect(component.manquementSignaleCbeOuSpf()).toBe(false);
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
    component.effectifEntreprise.set(50);
    component.statutAccord.set('CCT_ENTREPRISE_DEPOSEE');
    component.dateEntreeVigueurInstrument.set('2023-04-01');
    component.modalitesPratiquesDeconnexionDefinies.set(true);
    component.sensibilisationFormationPrevue.set(true);
    component.modalitesOrganisationTravailDefinies.set(true);
    component.consultationOrganeConcertationEffectuee.set(true);
    component.manquementSignaleCbeOuSpf.set(false);
  }

  it('formValid() : true avec les champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans effectifEntreprise', () => {
    fillForm();
    component.effectifEntreprise.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec effectif < 0', () => {
    fillForm();
    component.effectifEntreprise.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec effectif > 1_000_000', () => {
    fillForm();
    component.effectifEntreprise.set(1_000_001);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans statutAccord', () => {
    fillForm();
    component.statutAccord.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans modalitesPratiques précisé', () => {
    fillForm();
    component.modalitesPratiquesDeconnexionDefinies.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans sensibilisation précisé', () => {
    fillForm();
    component.sensibilisationFormationPrevue.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans modalitesOrganisation précisé', () => {
    fillForm();
    component.modalitesOrganisationTravailDefinies.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans consultation précisé', () => {
    fillForm();
    component.consultationOrganeConcertationEffectuee.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans manquementSignale précisé', () => {
    fillForm();
    component.manquementSignaleCbeOuSpf.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : CCT_ENTREPRISE_DEPOSEE sans dateEntreeVigueur → false', () => {
    fillForm();
    component.dateEntreeVigueurInstrument.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : REGLEMENT_TRAVAIL_MODIFIE sans dateEntreeVigueur → false', () => {
    fillForm();
    component.statutAccord.set('REGLEMENT_TRAVAIL_MODIFIE');
    component.dateEntreeVigueurInstrument.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : NEGOCIATION_EN_COURS sans dateEntreeVigueur → true', () => {
    fillForm();
    component.statutAccord.set('NEGOCIATION_EN_COURS');
    component.dateEntreeVigueurInstrument.set(null);
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : AUCUNE_INITIATIVE sans dateEntreeVigueur → true', () => {
    fillForm();
    component.statutAccord.set('AUCUNE_INITIATIVE');
    component.dateEntreeVigueurInstrument.set(null);
    expect(component.formValid()).toBe(true);
  });

  it('isInstrumentFormalise() true pour CCT_ENTREPRISE_DEPOSEE ou REGLEMENT_TRAVAIL_MODIFIE', () => {
    expect(component.isInstrumentFormalise()).toBe(false);
    component.statutAccord.set('CCT_ENTREPRISE_DEPOSEE');
    expect(component.isInstrumentFormalise()).toBe(true);
    component.statutAccord.set('REGLEMENT_TRAVAIL_MODIFIE');
    expect(component.isInstrumentFormalise()).toBe(true);
    component.statutAccord.set('NEGOCIATION_EN_COURS');
    expect(component.isInstrumentFormalise()).toBe(false);
    component.statutAccord.set('AUCUNE_INITIATIVE');
    expect(component.isInstrumentFormalise()).toBe(false);
    component.statutAccord.set('INDETERMINE');
    expect(component.isInstrumentFormalise()).toBe(false);
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
    expect(post.request.body.effectifEntreprise).toBe(50);
    expect(post.request.body.statutAccord).toBe('CCT_ENTREPRISE_DEPOSEE');
    expect(post.request.body.dateEntreeVigueurInstrument).toBe('2023-04-01');
    expect(post.request.body.modalitesPratiquesDeconnexionDefinies).toBe(true);
    expect(post.request.body.sensibilisationFormationPrevue).toBe(true);
    expect(post.request.body.modalitesOrganisationTravailDefinies).toBe(true);
    expect(post.request.body.consultationOrganeConcertationEffectuee).toBe(true);
    expect(post.request.body.manquementSignaleCbeOuSpf).toBe(false);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Conformité droit à la déconnexion analysée', 'OK', expect.anything(),
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
  // Verdict — label / classe / icône (6 cas)
  // ============================================================

  it('verdict CONFORME_ACCORD_COMPLET → label, verdict-ok, check_circle', () => {
    component.result.set(response());
    expect(component.verdictLabel()).toContain('Conforme');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('check_circle');
  });

  it('verdict MANQUEMENT_GRAVE_AUCUNE_INITIATIVE → label, verdict-critical, gavel', () => {
    component.result.set(response({
      verdict: 'MANQUEMENT_GRAVE_AUCUNE_INITIATIVE',
      statutAccord: 'AUCUNE_INITIATIVE',
      dateEntreeVigueurInstrument: null,
      instrumentFormalise: false,
      contenuComplet: false,
      modalitesPratiquesRespectees: false,
      sensibilisationRespectee: false,
      modalitesOrganisationRespectees: false,
      consultationRespectee: false,
    }));
    expect(component.verdictLabel()).toContain('manquement frontal');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('gavel');
  });

  it('verdict NON_CONFORME_INSTRUMENT_MANQUANT → label, verdict-warn, description', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_INSTRUMENT_MANQUANT',
      statutAccord: 'NEGOCIATION_EN_COURS',
      dateEntreeVigueurInstrument: null,
      instrumentFormalise: false,
      contenuComplet: false,
    }));
    expect(component.verdictLabel()).toContain('aucun instrument formalisé');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('description');
  });

  it('verdict NON_CONFORME_CONTENU_INCOMPLET → label, verdict-warn, edit_document', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_CONTENU_INCOMPLET',
      modalitesPratiquesDeconnexionDefinies: false,
      modalitesPratiquesRespectees: false,
      contenuComplet: false,
    }));
    expect(component.verdictLabel()).toContain('thèmes obligatoires manquants');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('edit_document');
  });

  it('verdict HORS_CHAMP_SEUIL_NON_ATTEINT → label, verdict-neutral, do_not_disturb_on', () => {
    component.result.set(response({
      verdict: 'HORS_CHAMP_SEUIL_NON_ATTEINT',
      effectifEntreprise: 10,
      seuilAtteint: false,
    }));
    expect(component.verdictLabel()).toContain('non applicable');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('do_not_disturb_on');
  });

  it('verdict A_ANALYSER → label, verdict-neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_ANALYSER',
      statutAccord: 'INDETERMINE',
      dateEntreeVigueurInstrument: null,
    }));
    expect(component.verdictLabel()).toContain('Configuration');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('phonelink_erase');
  });

  // ============================================================
  // Badge dans header (6 cas)
  // ============================================================

  it('badge CONFORME_ACCORD_COMPLET = vert', () => {
    expect(component.verdictBadge('CONFORME_ACCORD_COMPLET')).toBe('CONFORME');
    expect(component.verdictBadgeClass('CONFORME_ACCORD_COMPLET')).toBe('badge-ok');
  });

  it('badge MANQUEMENT_GRAVE_AUCUNE_INITIATIVE = rouge', () => {
    expect(component.verdictBadge('MANQUEMENT_GRAVE_AUCUNE_INITIATIVE')).toBe('MANQUEMENT GRAVE');
    expect(component.verdictBadgeClass('MANQUEMENT_GRAVE_AUCUNE_INITIATIVE')).toBe('badge-critical');
  });

  it('badge NON_CONFORME_INSTRUMENT_MANQUANT = ambre', () => {
    expect(component.verdictBadge('NON_CONFORME_INSTRUMENT_MANQUANT')).toBe('INSTRUMENT MANQUANT');
    expect(component.verdictBadgeClass('NON_CONFORME_INSTRUMENT_MANQUANT')).toBe('badge-warn');
  });

  it('badge NON_CONFORME_CONTENU_INCOMPLET = ambre', () => {
    expect(component.verdictBadge('NON_CONFORME_CONTENU_INCOMPLET')).toBe('CONTENU INCOMPLET');
    expect(component.verdictBadgeClass('NON_CONFORME_CONTENU_INCOMPLET')).toBe('badge-warn');
  });

  it('badge HORS_CHAMP_SEUIL_NON_ATTEINT = neutre', () => {
    expect(component.verdictBadge('HORS_CHAMP_SEUIL_NON_ATTEINT')).toBe('HORS CHAMP');
    expect(component.verdictBadgeClass('HORS_CHAMP_SEUIL_NON_ATTEINT')).toBe('badge-neutral');
  });

  it('badge A_ANALYSER = neutre', () => {
    expect(component.verdictBadge('A_ANALYSER')).toBe('À ANALYSER');
    expect(component.verdictBadgeClass('A_ANALYSER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — verdict CONFORME rendu (badge + 7 conformités)
  // ============================================================

  it('CONFORME → badge vert dans le DOM + 7 conformités affichées', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('CONFORME');
    const conformites = fixture.nativeElement.querySelectorAll('.conformite-row');
    expect(conformites.length).toBe(7);
  });

  it('MANQUEMENT_GRAVE → badge rouge + bannière critique', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'MANQUEMENT_GRAVE_AUCUNE_INITIATIVE',
      statutAccord: 'AUCUNE_INITIATIVE',
      dateEntreeVigueurInstrument: null,
      instrumentFormalise: false,
      contenuComplet: false,
      modalitesPratiquesRespectees: false,
      sensibilisationRespectee: false,
      modalitesOrganisationRespectees: false,
      consultationRespectee: false,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('MANQUEMENT GRAVE');
    const banner = fixture.nativeElement.querySelector('.result-banner.verdict-critical');
    expect(banner).not.toBeNull();
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellé statutAccord : 5 cas + null', () => {
    expect(component.statutAccordLabel('CCT_ENTREPRISE_DEPOSEE')).toContain('CCT');
    expect(component.statutAccordLabel('REGLEMENT_TRAVAIL_MODIFIE')).toContain('règlement');
    expect(component.statutAccordLabel('NEGOCIATION_EN_COURS')).toContain('Concertation');
    expect(component.statutAccordLabel('AUCUNE_INITIATIVE')).toContain('Aucun');
    expect(component.statutAccordLabel('INDETERMINE')).toContain('indéterminé');
    expect(component.statutAccordLabel(null)).toBe('—');
  });

  // ============================================================
  // Formats
  // ============================================================

  it('formatEffectif inclut la locale FR + suffix travailleurs', () => {
    expect(component.formatEffectif(50)).toContain('50');
    expect(component.formatEffectif(50)).toContain('travailleurs');
    expect(component.formatEffectif(null)).toBe('—');
    expect(component.formatEffectif(undefined)).toBe('—');
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
      avertissement: 'Articulation CCT 149 supplétive à vérifier au cas par cas.',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('CCT 149');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Obligation déconnexion respectée — 3 thèmes art. 16 § 2 traités.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('3 thèmes');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(DroitDeconnexionBeSectionComponent.getPrefillCount({})).toBe(0);
    expect(DroitDeconnexionBeSectionComponent.getPrefillCount({
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

  it('toolId pour jurisprudence = droit-deconnexion-be', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('droit-deconnexion-be');
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
    component.onEffectifEntrepriseChange('25');
    expect(component.effectifEntreprise()).toBe(25);

    component.onStatutAccordChange('REGLEMENT_TRAVAIL_MODIFIE');
    expect(component.statutAccord()).toBe('REGLEMENT_TRAVAIL_MODIFIE');

    component.onDateEntreeVigueurInstrumentChange('2024-06-01');
    expect(component.dateEntreeVigueurInstrument()).toBe('2024-06-01');

    component.onModalitesPratiquesDeconnexionDefiniesChange(true);
    expect(component.modalitesPratiquesDeconnexionDefinies()).toBe(true);

    component.onSensibilisationFormationPrevueChange(false);
    expect(component.sensibilisationFormationPrevue()).toBe(false);

    component.onModalitesOrganisationTravailDefiniesChange(true);
    expect(component.modalitesOrganisationTravailDefinies()).toBe(true);

    component.onConsultationOrganeConcertationEffectueeChange(true);
    expect(component.consultationOrganeConcertationEffectuee()).toBe(true);

    component.onManquementSignaleCbeOuSpfChange(false);
    expect(component.manquementSignaleCbeOuSpf()).toBe(false);

    component.onEffectifEntrepriseChange(null);
    expect(component.effectifEntreprise()).toBeNull();

    component.onEffectifEntrepriseChange('');
    expect(component.effectifEntreprise()).toBeNull();
  });
});
