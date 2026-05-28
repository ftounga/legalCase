import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  InastriStatutTravailleurIndependantSectionComponent,
} from './inastri-statut-travailleur-independant-section.component';
import {
  InastriStatutTravailleurIndependantResponse,
} from '../../core/models/inastri-statut-travailleur-independant.model';

describe('InastriStatutTravailleurIndependantSectionComponent', () => {
  let component: InastriStatutTravailleurIndependantSectionComponent;
  let fixture: ComponentFixture<InastriStatutTravailleurIndependantSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/inastri-statut-travailleur-independant';

  /**
   * Réponse backend par défaut — verdict SALARIE_REQUALIFIE (cas le plus
   * représentatif : critères généraux pointant vers la subordination).
   */
  function response(
    overrides: Partial<InastriStatutTravailleurIndependantResponse> = {},
  ): InastriStatutTravailleurIndependantResponse {
    return {
      caseFileId: 'case-1',
      volonteParties: 'IMPLICITE_OU_AMBIGUE',
      liberteOrganisationTemps: 'AUCUNE_SALARIE',
      liberteOrganisationTravail: 'AUCUNE_SALARIE',
      controleHierarchique: 'AUCUNE_SALARIE',
      secteur: 'AUTRE',
      nbCriteresSectorielsSalarie: 0,
      statutDeclare: 'INDEPENDANT_INASTI',
      dimonaPresente: false,
      autreClientPresent: false,
      verdict: 'SALARIE_REQUALIFIE',
      scoreCriteresGenerauxIndependant: 0,
      scoreCriteresGenerauxSalarie: 4,
      presumptionSectorielleApplicable: false,
      presumptionSectorielleDeclenchee: false,
      presumptionGeneraleSalariatMobilisable: true,
      requalificationSalariatRecommandee: true,
      dimonaCoherenteAvecStatutDeclare: true,
      monoClientIndiceSalariat: true,
      raison: 'SUBORDINATION_CARACTERISEE',
      synthese: 'Relation à requalifier en salariat — 4 critères généraux pointent vers subordination.',
      baseJuridique: 'Loi 27/06/1969 + Loi-programme I 27/12/2006 art. 333 + C. pén. soc. art. 328 § 1.',
      avertissement: 'Primauté de l\'exécution réelle (Cass. BE 23/12/2002). Charge probatoire renversable.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        InastriStatutTravailleurIndependantSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(InastriStatutTravailleurIndependantSectionComponent);
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

  it('FRANCE => bannière "réservé droit BE" + masquage form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.empty-result');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('belge');
    expect(banner.textContent).toContain('Bart Buysse');
  });

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 => hydrate result + mode résultat + rehydrate form', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('SALARIE_REQUALIFIE');
    expect(component.showForm()).toBe(false);
    expect(component.volonteParties()).toBe('IMPLICITE_OU_AMBIGUE');
    expect(component.liberteOrganisationTemps()).toBe('AUCUNE_SALARIE');
    expect(component.secteur()).toBe('AUTRE');
    expect(component.nbCriteresSectorielsSalarie()).toBe(0);
    expect(component.statutDeclare()).toBe('INDEPENDANT_INASTI');
    expect(component.dimonaPresente()).toBe(false);
    expect(component.autreClientPresent()).toBe(false);
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
    component.volonteParties.set('IMPLICITE_OU_AMBIGUE');
    component.liberteOrganisationTemps.set('AUCUNE_SALARIE');
    component.liberteOrganisationTravail.set('AUCUNE_SALARIE');
    component.controleHierarchique.set('AUCUNE_SALARIE');
    component.secteur.set('AUTRE');
    component.nbCriteresSectorielsSalarie.set(0);
    component.statutDeclare.set('INDEPENDANT_INASTI');
    component.dimonaPresente.set(false);
    component.autreClientPresent.set(false);
  }

  it('formValid() : true avec les champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans volonteParties', () => {
    fillForm();
    component.volonteParties.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans liberteOrganisationTemps', () => {
    fillForm();
    component.liberteOrganisationTemps.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans liberteOrganisationTravail', () => {
    fillForm();
    component.liberteOrganisationTravail.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans controleHierarchique', () => {
    fillForm();
    component.controleHierarchique.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans secteur', () => {
    fillForm();
    component.secteur.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans nbCriteresSectorielsSalarie', () => {
    fillForm();
    component.nbCriteresSectorielsSalarie.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec nb critères < 0', () => {
    fillForm();
    component.nbCriteresSectorielsSalarie.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec nb critères > 9', () => {
    fillForm();
    component.nbCriteresSectorielsSalarie.set(10);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true avec nb critères = 9 (borne max)', () => {
    fillForm();
    component.nbCriteresSectorielsSalarie.set(9);
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans statutDeclare', () => {
    fillForm();
    component.statutDeclare.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dimonaPresente', () => {
    fillForm();
    component.dimonaPresente.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans autreClientPresent', () => {
    fillForm();
    component.autreClientPresent.set(null);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  it('calculate() => POST + snack succès', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.volonteParties).toBe('IMPLICITE_OU_AMBIGUE');
    expect(post.request.body.secteur).toBe('AUTRE');
    expect(post.request.body.nbCriteresSectorielsSalarie).toBe(0);
    expect(post.request.body.statutDeclare).toBe('INDEPENDANT_INASTI');
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse INASTI statut indépendant calculée', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 => snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'nbCriteresSectorielsSalarie : doit être entre 0 et 9' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'nbCriteresSectorielsSalarie : doit être entre 0 et 9', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : erreur 500 => snack générique', () => {
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
  // Verdict — label / classe / icône (5 cas)
  // ============================================================

  it('verdict SALARIE_REQUALIFIE => critical, gavel', () => {
    component.result.set(response());
    expect(component.verdictLabel()).toContain('Salarié requalifié');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('gavel');
  });

  it('verdict FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE => critical, report', () => {
    component.result.set(response({
      verdict: 'FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE',
      secteur: 'CONSTRUCTION',
      nbCriteresSectorielsSalarie: 6,
      presumptionSectorielleApplicable: true,
      presumptionSectorielleDeclenchee: true,
    }));
    expect(component.verdictLabel()).toContain('présomption sectorielle');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('report');
  });

  it('verdict PRESUMPTION_GENERALE_SALARIAT => critical, warning', () => {
    component.result.set(response({
      verdict: 'PRESUMPTION_GENERALE_SALARIAT',
      statutDeclare: 'SANS_DECLARATION',
      dimonaPresente: false,
    }));
    expect(component.verdictLabel()).toContain('art. 328');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('warning');
  });

  it('verdict INDEPENDANT_CONFIRME => info, check_circle', () => {
    component.result.set(response({
      verdict: 'INDEPENDANT_CONFIRME',
      volonteParties: 'INDEPENDANT_EXPLICITE',
      liberteOrganisationTemps: 'TOTALE_INDEPENDANT',
      liberteOrganisationTravail: 'TOTALE_INDEPENDANT',
      controleHierarchique: 'TOTALE_INDEPENDANT',
      autreClientPresent: true,
      monoClientIndiceSalariat: false,
      scoreCriteresGenerauxIndependant: 4,
      scoreCriteresGenerauxSalarie: 0,
      presumptionGeneraleSalariatMobilisable: false,
      requalificationSalariatRecommandee: false,
    }));
    expect(component.verdictLabel()).toContain('Indépendant confirmé');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('check_circle');
  });

  it('verdict A_QUALIFIER => neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_QUALIFIER',
      liberteOrganisationTemps: 'PARTIELLE',
      liberteOrganisationTravail: 'PARTIELLE',
      controleHierarchique: 'PARTIELLE',
      scoreCriteresGenerauxIndependant: 1,
      scoreCriteresGenerauxSalarie: 1,
    }));
    expect(component.verdictLabel()).toContain('Qualification ouverte');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null => labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('badge');
  });

  // ============================================================
  // Badge dans header (5 cas)
  // ============================================================

  it('badge INDEPENDANT_CONFIRME = info', () => {
    expect(component.verdictBadge('INDEPENDANT_CONFIRME')).toBe('INDÉPENDANT');
    expect(component.verdictBadgeClass('INDEPENDANT_CONFIRME')).toBe('badge-info');
  });

  it('badge SALARIE_REQUALIFIE = critical', () => {
    expect(component.verdictBadge('SALARIE_REQUALIFIE')).toContain('SALARIÉ');
    expect(component.verdictBadgeClass('SALARIE_REQUALIFIE')).toBe('badge-critical');
  });

  it('badge FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE = critical', () => {
    expect(component.verdictBadge('FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE')).toContain('SECTORIELLE');
    expect(component.verdictBadgeClass('FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE')).toBe('badge-critical');
  });

  it('badge PRESUMPTION_GENERALE_SALARIAT = critical', () => {
    expect(component.verdictBadge('PRESUMPTION_GENERALE_SALARIAT')).toContain('GÉNÉRALE');
    expect(component.verdictBadgeClass('PRESUMPTION_GENERALE_SALARIAT')).toBe('badge-critical');
  });

  it('badge A_QUALIFIER = neutre', () => {
    expect(component.verdictBadge('A_QUALIFIER')).toBe('À QUALIFIER');
    expect(component.verdictBadgeClass('A_QUALIFIER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — rendu verdict
  // ============================================================

  it('SALARIE_REQUALIFIE => badge rouge + 10 ventilations', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('SALARIÉ');
    const ventilations = fixture.nativeElement.querySelectorAll('.ventilation-row');
    // 2 scores + secteur + nb critères + 6 flags = 10.
    expect(ventilations.length).toBe(10);
  });

  it('INDEPENDANT_CONFIRME => badge info', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INDEPENDANT_CONFIRME',
      volonteParties: 'INDEPENDANT_EXPLICITE',
      liberteOrganisationTemps: 'TOTALE_INDEPENDANT',
      liberteOrganisationTravail: 'TOTALE_INDEPENDANT',
      controleHierarchique: 'TOTALE_INDEPENDANT',
      autreClientPresent: true,
      monoClientIndiceSalariat: false,
      scoreCriteresGenerauxIndependant: 4,
      scoreCriteresGenerauxSalarie: 0,
      presumptionGeneraleSalariatMobilisable: false,
      requalificationSalariatRecommandee: false,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-info');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('INDÉPENDANT');
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellé volonteParties : 3 cas + null', () => {
    expect(component.volontePartiesLabel('INDEPENDANT_EXPLICITE')).toContain('indépendante');
    expect(component.volontePartiesLabel('SALARIE_EXPLICITE')).toContain('salariée');
    expect(component.volontePartiesLabel('IMPLICITE_OU_AMBIGUE')).toContain('implicite');
    expect(component.volontePartiesLabel(null)).toBe('—');
  });

  it('libellé liberte : 3 cas + null', () => {
    expect(component.liberteLabel('TOTALE_INDEPENDANT')).toContain('totale');
    expect(component.liberteLabel('PARTIELLE')).toContain('partielle');
    expect(component.liberteLabel('AUCUNE_SALARIE')).toContain('Aucune');
    expect(component.liberteLabel(null)).toBe('—');
  });

  it('libellé secteur : 6 cas + null', () => {
    expect(component.secteurLabel('CONSTRUCTION')).toContain('Construction');
    expect(component.secteurLabel('TRANSPORT_DE_CHOSES')).toContain('Transport');
    expect(component.secteurLabel('GARDIENNAGE')).toContain('Gardiennage');
    expect(component.secteurLabel('NETTOYAGE')).toContain('Nettoyage');
    expect(component.secteurLabel('AGRICULTURE_HORTICULTURE')).toContain('Agriculture');
    expect(component.secteurLabel('AUTRE')).toContain('non visé');
    expect(component.secteurLabel(null)).toBe('—');
  });

  it('libellé statutDeclare : 3 cas + null', () => {
    expect(component.statutDeclareLabel('INDEPENDANT_INASTI')).toContain('indépendant');
    expect(component.statutDeclareLabel('SALARIE_DIMONA')).toContain('salarié');
    expect(component.statutDeclareLabel('SANS_DECLARATION')).toContain('Aucune');
    expect(component.statutDeclareLabel(null)).toBe('—');
  });

  // ============================================================
  // Formats
  // ============================================================

  it('formatBoolean : Oui / Non / —', () => {
    expect(component.formatBoolean(true)).toBe('Oui');
    expect(component.formatBoolean(false)).toBe('Non');
    expect(component.formatBoolean(null)).toBe('—');
    expect(component.formatBoolean(undefined)).toBe('—');
  });

  it('formatInt : entier / null', () => {
    expect(component.formatInt(4)).toContain('4');
    expect(component.formatInt(0)).toBe('0');
    expect(component.formatInt(null)).toBe('—');
    expect(component.formatInt(undefined)).toBe('—');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non vide => bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      avertissement: 'Vérifier la primauté de l\'exécution réelle.',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('exécution');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Relation requalifiée en salariat.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('salariat');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount => 0 (V1)', () => {
    expect(InastriStatutTravailleurIndependantSectionComponent.getPrefillCount({})).toBe(0);
    expect(InastriStatutTravailleurIndependantSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateNaissanceSalarie: '1985-06-12',
        dateRuptureContrat: '2026-04-10',
        motifRupture: 'autre',
        anneesCarriereSalarie: 8,
        salaireBrutMensuel: 2400,
        salaireBrutAnnuel: 28800,
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
  // F-JU-03 — citation jurisprudentielle bien câblée
  // ============================================================

  it('toolId pour jurisprudence = inastri-statut-travailleur-independant', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('inastri-statut-travailleur-independant');
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

  it('handlers mettent à jour les signals correctement', () => {
    component.onVolontePartiesChange('INDEPENDANT_EXPLICITE');
    expect(component.volonteParties()).toBe('INDEPENDANT_EXPLICITE');

    component.onLiberteOrganisationTempsChange('TOTALE_INDEPENDANT');
    expect(component.liberteOrganisationTemps()).toBe('TOTALE_INDEPENDANT');

    component.onLiberteOrganisationTravailChange('PARTIELLE');
    expect(component.liberteOrganisationTravail()).toBe('PARTIELLE');

    component.onControleHierarchiqueChange('AUCUNE_SALARIE');
    expect(component.controleHierarchique()).toBe('AUCUNE_SALARIE');

    component.onSecteurChange('CONSTRUCTION');
    expect(component.secteur()).toBe('CONSTRUCTION');

    component.onNbCriteresSectorielsSalarieChange('6');
    expect(component.nbCriteresSectorielsSalarie()).toBe(6);

    component.onStatutDeclareChange('SALARIE_DIMONA');
    expect(component.statutDeclare()).toBe('SALARIE_DIMONA');

    component.onDimonaPresenteChange(true);
    expect(component.dimonaPresente()).toBe(true);

    component.onAutreClientPresentChange(true);
    expect(component.autreClientPresent()).toBe(true);

    component.onNbCriteresSectorielsSalarieChange('');
    expect(component.nbCriteresSectorielsSalarie()).toBeNull();

    component.onNbCriteresSectorielsSalarieChange(null);
    expect(component.nbCriteresSectorielsSalarie()).toBeNull();
  });
});
