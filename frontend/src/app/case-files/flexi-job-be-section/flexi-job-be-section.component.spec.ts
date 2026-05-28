import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  FlexiJobBeSectionComponent,
} from './flexi-job-be-section.component';
import {
  FlexiJobBeResponse,
} from '../../core/models/flexi-job-be.model';

describe('FlexiJobBeSectionComponent', () => {
  let component: FlexiJobBeSectionComponent;
  let fixture: ComponentFixture<FlexiJobBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/flexi-job-be';

  /**
   * Construit une réponse backend par défaut — verdict ELIGIBLE
   * (statut SALARIE_4_5_TEMPS_T_MOINS_3, secteur HORECA_302, 5 conditions OK).
   */
  function response(
    overrides: Partial<FlexiJobBeResponse> = {},
  ): FlexiJobBeResponse {
    return {
      caseFileId: 'case-1',
      datePremiereOccupationFlexi: '2024-05-01',
      statutTravailleur: 'SALARIE_4_5_TEMPS_T_MOINS_3',
      secteurEmployeur: 'HORECA_302',
      dejaOccupeChezMemeEmployeur: false,
      contratCadreSigne: true,
      dimonaFlxDeclaree: true,
      flexiSalaireHoraireBrut: 13.5,
      flexiSalaireMinimumApplicable: 12.33,
      revenuAnnuelFlexiCumule: 8000,
      plafondAnnuelExonere: 12000,
      verdict: 'ELIGIBLE',
      travailleurEligible: true,
      secteurEligible: true,
      cumulRespecte: true,
      formalismeRespecte: true,
      remunerationConforme: true,
      montantExcedentaireAuPlafond: 0,
      raison: null,
      synthese: 'Occupation flexi-job régulière — toutes conditions cumulatives réunies.',
      baseJuridique: 'Loi-programme du 26/12/2013 + extensions 2015/2018/2023 + AR 02/06/2024',
      avertissement: '',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        FlexiJobBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(FlexiJobBeSectionComponent);
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
    expect(component.result()!.verdict).toBe('ELIGIBLE');
    expect(component.showForm()).toBe(false);
    expect(component.datePremiereOccupationFlexi()).toBe('2024-05-01');
    expect(component.statutTravailleur()).toBe('SALARIE_4_5_TEMPS_T_MOINS_3');
    expect(component.secteurEmployeur()).toBe('HORECA_302');
    expect(component.dejaOccupeChezMemeEmployeur()).toBe(false);
    expect(component.contratCadreSigne()).toBe(true);
    expect(component.dimonaFlxDeclaree()).toBe(true);
    expect(component.flexiSalaireHoraireBrut()).toBe(13.5);
    expect(component.flexiSalaireMinimumApplicable()).toBe(12.33);
    expect(component.revenuAnnuelFlexiCumule()).toBe(8000);
    expect(component.plafondAnnuelExonere()).toBe(12000);
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
    component.datePremiereOccupationFlexi.set('2024-05-01');
    component.statutTravailleur.set('SALARIE_4_5_TEMPS_T_MOINS_3');
    component.secteurEmployeur.set('HORECA_302');
    component.dejaOccupeChezMemeEmployeur.set(false);
    component.contratCadreSigne.set(true);
    component.dimonaFlxDeclaree.set(true);
    component.flexiSalaireHoraireBrut.set(13.5);
    component.flexiSalaireMinimumApplicable.set(12.33);
    component.revenuAnnuelFlexiCumule.set(8000);
    component.plafondAnnuelExonere.set(12000);
  }

  it('formValid() : true avec 10 champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans date occupation', () => {
    fillForm();
    component.datePremiereOccupationFlexi.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans statutTravailleur', () => {
    fillForm();
    component.statutTravailleur.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans secteurEmployeur', () => {
    fillForm();
    component.secteurEmployeur.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dejaOccupeChezMemeEmployeur précisé', () => {
    fillForm();
    component.dejaOccupeChezMemeEmployeur.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans contratCadreSigne précisé', () => {
    fillForm();
    component.contratCadreSigne.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dimonaFlxDeclaree précisé', () => {
    fillForm();
    component.dimonaFlxDeclaree.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec salaire horaire négatif', () => {
    fillForm();
    component.flexiSalaireHoraireBrut.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec minimum <= 0', () => {
    fillForm();
    component.flexiSalaireMinimumApplicable.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec revenu cumulé négatif', () => {
    fillForm();
    component.revenuAnnuelFlexiCumule.set(-100);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec plafond <= 0', () => {
    fillForm();
    component.plafondAnnuelExonere.set(0);
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
    expect(post.request.body.statutTravailleur).toBe('SALARIE_4_5_TEMPS_T_MOINS_3');
    expect(post.request.body.secteurEmployeur).toBe('HORECA_302');
    expect(post.request.body.flexiSalaireHoraireBrut).toBe(13.5);
    expect(post.request.body.revenuAnnuelFlexiCumule).toBe(8000);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Flexi-job analysé', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'plafondAnnuelExonere doit être strictement positif' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'plafondAnnuelExonere doit être strictement positif', 'Fermer', expect.anything(),
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

  it('verdict ELIGIBLE → label, verdict-ok, verified', () => {
    component.result.set(response({ verdict: 'ELIGIBLE' }));
    expect(component.verdictLabel()).toContain('régulière');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict FRAGILE_CONTRAT_OU_DIMONA_MANQUANT → label, verdict-warn, warning_amber', () => {
    component.result.set(response({
      verdict: 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT',
      contratCadreSigne: false,
      formalismeRespecte: false,
    }));
    expect(component.verdictLabel()).toContain('Formalisme');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('warning_amber');
  });

  it('verdict FRAGILE_PLAFOND_DEPASSE → label, verdict-warn, warning_amber', () => {
    component.result.set(response({
      verdict: 'FRAGILE_PLAFOND_DEPASSE',
      revenuAnnuelFlexiCumule: 15000,
      remunerationConforme: false,
      montantExcedentaireAuPlafond: 3000,
    }));
    expect(component.verdictLabel()).toContain('plafond');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('warning_amber');
  });

  it('verdict INELIGIBLE_TRAVAILLEUR_HORS_CONDITION → label, verdict-critical, cancel', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_TRAVAILLEUR_HORS_CONDITION',
      statutTravailleur: 'AUTRE',
      travailleurEligible: false,
    }));
    expect(component.verdictLabel()).toContain('Travailleur');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('cancel');
  });

  it('verdict INELIGIBLE_SECTEUR_NON_ELIGIBLE → label, verdict-critical, cancel', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_SECTEUR_NON_ELIGIBLE',
      secteurEmployeur: 'AUTRE_NON_ELIGIBLE',
      secteurEligible: false,
    }));
    expect(component.verdictLabel()).toContain('Secteur non éligible');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('cancel');
  });

  it('verdict INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR → label, verdict-critical, cancel', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR',
      dejaOccupeChezMemeEmployeur: true,
      cumulRespecte: false,
    }));
    expect(component.verdictLabel()).toContain('Cumul interdit');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('cancel');
  });

  it('verdict A_ANALYSER → label, verdict-neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_ANALYSER',
      secteurEmployeur: 'ZONE_GRISE_EXTENSION_RECENTE',
    }));
    expect(component.verdictLabel()).toContain('Zone grise');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('restaurant_menu');
  });

  // ============================================================
  // Badge dans header (7 cas)
  // ============================================================

  it('badge ELIGIBLE = vert "ÉLIGIBLE"', () => {
    expect(component.verdictBadge('ELIGIBLE')).toBe('ÉLIGIBLE');
    expect(component.verdictBadgeClass('ELIGIBLE')).toBe('badge-ok');
  });

  it('badge FRAGILE_CONTRAT_OU_DIMONA_MANQUANT = ambre', () => {
    expect(component.verdictBadge('FRAGILE_CONTRAT_OU_DIMONA_MANQUANT')).toBe('FORMALISME FRAGILE');
    expect(component.verdictBadgeClass('FRAGILE_CONTRAT_OU_DIMONA_MANQUANT')).toBe('badge-warn');
  });

  it('badge FRAGILE_PLAFOND_DEPASSE = ambre', () => {
    expect(component.verdictBadge('FRAGILE_PLAFOND_DEPASSE')).toBe('PLAFOND DÉPASSÉ');
    expect(component.verdictBadgeClass('FRAGILE_PLAFOND_DEPASSE')).toBe('badge-warn');
  });

  it('badge INELIGIBLE_TRAVAILLEUR_HORS_CONDITION = rouge', () => {
    expect(component.verdictBadge('INELIGIBLE_TRAVAILLEUR_HORS_CONDITION')).toBe('INÉLIGIBLE (TRAVAILLEUR)');
    expect(component.verdictBadgeClass('INELIGIBLE_TRAVAILLEUR_HORS_CONDITION')).toBe('badge-critical');
  });

  it('badge INELIGIBLE_SECTEUR_NON_ELIGIBLE = rouge', () => {
    expect(component.verdictBadge('INELIGIBLE_SECTEUR_NON_ELIGIBLE')).toBe('INÉLIGIBLE (SECTEUR)');
    expect(component.verdictBadgeClass('INELIGIBLE_SECTEUR_NON_ELIGIBLE')).toBe('badge-critical');
  });

  it('badge INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR = rouge', () => {
    expect(component.verdictBadge('INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR')).toBe('CUMUL INTERDIT');
    expect(component.verdictBadgeClass('INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR')).toBe('badge-critical');
  });

  it('badge A_ANALYSER = neutre', () => {
    expect(component.verdictBadge('A_ANALYSER')).toBe('À ANALYSER');
    expect(component.verdictBadgeClass('A_ANALYSER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — verdict ELIGIBLE rendu (badge + 5 dimensions OK)
  // ============================================================

  it('ELIGIBLE → badge vert dans le DOM + 5 dimensions cumulatives toutes OK', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('ÉLIGIBLE');
    const okRows = fixture.nativeElement.querySelectorAll('.dimension-row.dim-ok');
    expect(okRows.length).toBe(5);
    const koRows = fixture.nativeElement.querySelectorAll('.dimension-row.dim-ko');
    expect(koRows.length).toBe(0);
  });

  it('FRAGILE_PLAFOND_DEPASSE → badge ambre + excédent affiché', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'FRAGILE_PLAFOND_DEPASSE',
      revenuAnnuelFlexiCumule: 15000,
      remunerationConforme: false,
      montantExcedentaireAuPlafond: 3000,
      avertissement: 'Partie excédentaire de 3 000 € soumise à cotisations normales',
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-warn');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('PLAFOND');
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('3000');
    expect(text).toContain('Excédent');
  });

  it('INELIGIBLE_SECTEUR_NON_ELIGIBLE → badge rouge + dimension secteur KO', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INELIGIBLE_SECTEUR_NON_ELIGIBLE',
      secteurEmployeur: 'AUTRE_NON_ELIGIBLE',
      secteurEligible: false,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('SECTEUR');
    const koRows = fixture.nativeElement.querySelectorAll('.dimension-row.dim-ko');
    expect(koRows.length).toBeGreaterThanOrEqual(1);
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellé statutTravailleur : 3 cas + null', () => {
    expect(component.statutTravailleurLabel('PENSIONNE')).toContain('Pensionné');
    expect(component.statutTravailleurLabel('SALARIE_4_5_TEMPS_T_MOINS_3')).toContain('4/5');
    expect(component.statutTravailleurLabel('AUTRE')).toContain('Autre');
    expect(component.statutTravailleurLabel(null)).toBe('—');
  });

  it('libellé secteurEmployeur : 9 cas + null', () => {
    expect(component.secteurEmployeurLabel('HORECA_302')).toContain('HoReCa');
    expect(component.secteurEmployeurLabel('BOULANGERIE_COIFFURE')).toContain('Boulangerie');
    expect(component.secteurEmployeurLabel('COMMERCE_DETAIL')).toContain('Commerce');
    expect(component.secteurEmployeurLabel('AGRICULTURE')).toContain('Agriculture');
    expect(component.secteurEmployeurLabel('SOINS_DE_SANTE')).toContain('Soins');
    expect(component.secteurEmployeurLabel('SECTEUR_PUBLIC')).toContain('public');
    expect(component.secteurEmployeurLabel('SPORT_CULTURE_EVENEMENTIEL')).toContain('Sport');
    expect(component.secteurEmployeurLabel('AUTRE_NON_ELIGIBLE')).toContain('non listé');
    expect(component.secteurEmployeurLabel('ZONE_GRISE_EXTENSION_RECENTE')).toContain('Extension');
    expect(component.secteurEmployeurLabel(null)).toBe('—');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non vide → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT',
      contratCadreSigne: false,
      formalismeRespecte: false,
      avertissement: 'Contrat-cadre absent — risque requalification ONSS',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('requalification');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Toutes conditions cumulatives réunies — flexi-job HoReCa éligible.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('HoReCa éligible');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(FlexiJobBeSectionComponent.getPrefillCount({})).toBe(0);
    expect(FlexiJobBeSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateNaissanceSalarie: '1962-03-15',
        dateRuptureContrat: '2026-04-10',
        motifRupture: 'autre',
        anneesCarriereSalarie: 35,
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

  it('toolId pour jurisprudence = flexi-job-be', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('flexi-job-be');
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
    component.onDatePremiereOccupationFlexiChange('2024-06-01');
    expect(component.datePremiereOccupationFlexi()).toBe('2024-06-01');

    component.onStatutTravailleurChange('PENSIONNE');
    expect(component.statutTravailleur()).toBe('PENSIONNE');

    component.onSecteurEmployeurChange('COMMERCE_DETAIL');
    expect(component.secteurEmployeur()).toBe('COMMERCE_DETAIL');

    component.onDejaOccupeChezMemeEmployeurChange(true);
    expect(component.dejaOccupeChezMemeEmployeur()).toBe(true);

    component.onContratCadreSigneChange(false);
    expect(component.contratCadreSigne()).toBe(false);

    component.onDimonaFlxDeclareeChange(true);
    expect(component.dimonaFlxDeclaree()).toBe(true);

    component.onFlexiSalaireHoraireBrutChange(13.5);
    expect(component.flexiSalaireHoraireBrut()).toBe(13.5);

    component.onFlexiSalaireHoraireBrutChange('14.25');
    expect(component.flexiSalaireHoraireBrut()).toBe(14.25);

    component.onFlexiSalaireHoraireBrutChange(null);
    expect(component.flexiSalaireHoraireBrut()).toBeNull();

    component.onFlexiSalaireHoraireBrutChange('');
    expect(component.flexiSalaireHoraireBrut()).toBeNull();

    component.onFlexiSalaireHoraireBrutChange('abc');
    expect(component.flexiSalaireHoraireBrut()).toBeNull();

    component.onFlexiSalaireMinimumApplicableChange(12.5);
    expect(component.flexiSalaireMinimumApplicable()).toBe(12.5);

    component.onRevenuAnnuelFlexiCumuleChange(9500);
    expect(component.revenuAnnuelFlexiCumule()).toBe(9500);

    component.onPlafondAnnuelExonereChange(14000);
    expect(component.plafondAnnuelExonere()).toBe(14000);
  });

  // ============================================================
  // Static metadata F-177
  // ============================================================

  it('TOOL_LABEL et TOOL_ICON statiques exposés', () => {
    expect(FlexiJobBeSectionComponent.TOOL_LABEL).toBe('FLEXI-JOB (BE)');
    expect(FlexiJobBeSectionComponent.TOOL_ICON).toBe('restaurant_menu');
  });
});
