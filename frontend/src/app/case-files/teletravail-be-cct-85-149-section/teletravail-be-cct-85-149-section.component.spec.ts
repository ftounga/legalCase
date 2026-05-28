import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  TeletravailBeCct85149SectionComponent,
} from './teletravail-be-cct-85-149-section.component';
import {
  TeletravailBeCct85149Response,
} from '../../core/models/teletravail-be-cct-85-149.model';

describe('TeletravailBeCct85149SectionComponent', () => {
  let component: TeletravailBeCct85149SectionComponent;
  let fixture: ComponentFixture<TeletravailBeCct85149SectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/teletravail-be-cct-85-149';

  /**
   * Construit une réponse backend par défaut — verdict
   * CONFORME_CCT_85_STRUCTUREL (toutes conditions cumulatives réunies pour
   * un télétravail structurel régulier).
   */
  function response(
    overrides: Partial<TeletravailBeCct85149Response> = {},
  ): TeletravailBeCct85149Response {
    return {
      caseFileId: 'case-1',
      dateDebutTeletravail: '2024-04-01',
      typeTeletravail: 'STRUCTUREL_CCT_85',
      volontariatReciproque: true,
      conventionEcriteIndividuelle: true,
      equipementFourniOuIndemnise: true,
      indemniteForfaitaireMensuelleEuros: 130,
      plafondIndemniteMensuelleEuros: 154.74,
      droitsSociauxMaintenus: true,
      deconnexionDefinie: true,
      effectifEntreprise: 50,
      verdict: 'CONFORME_CCT_85_STRUCTUREL',
      conventionRespectee: true,
      equipementRespecte: true,
      droitsRespectes: true,
      deconnexionRespectee: true,
      indemniteExcedentaire: 0,
      raison: 'CONFORME_STRUCTUREL',
      synthese: 'Télétravail structurel pleinement conforme à la CCT n° 85.',
      baseJuridique: 'CCT n° 85 du 09/11/2005 + CCT n° 149 du 26/01/2021 + Loi du 03/10/2022',
      avertissement: '',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        TeletravailBeCct85149SectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(TeletravailBeCct85149SectionComponent);
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
    expect(component.result()!.verdict).toBe('CONFORME_CCT_85_STRUCTUREL');
    expect(component.showForm()).toBe(false);
    expect(component.dateDebutTeletravail()).toBe('2024-04-01');
    expect(component.typeTeletravail()).toBe('STRUCTUREL_CCT_85');
    expect(component.volontariatReciproque()).toBe(true);
    expect(component.conventionEcriteIndividuelle()).toBe(true);
    expect(component.equipementFourniOuIndemnise()).toBe(true);
    expect(component.indemniteForfaitaireMensuelleEuros()).toBe(130);
    expect(component.plafondIndemniteMensuelleEuros()).toBe(154.74);
    expect(component.droitsSociauxMaintenus()).toBe(true);
    expect(component.deconnexionDefinie()).toBe(true);
    expect(component.effectifEntreprise()).toBe(50);
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
    component.dateDebutTeletravail.set('2024-04-01');
    component.typeTeletravail.set('STRUCTUREL_CCT_85');
    component.volontariatReciproque.set(true);
    component.conventionEcriteIndividuelle.set(true);
    component.equipementFourniOuIndemnise.set(true);
    component.indemniteForfaitaireMensuelleEuros.set(130);
    component.plafondIndemniteMensuelleEuros.set(154.74);
    component.droitsSociauxMaintenus.set(true);
    component.deconnexionDefinie.set(true);
    component.effectifEntreprise.set(50);
  }

  it('formValid() : true avec 10 champs requis remplis', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans dateDebutTeletravail', () => {
    fillForm();
    component.dateDebutTeletravail.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans typeTeletravail', () => {
    fillForm();
    component.typeTeletravail.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans volontariatReciproque précisé', () => {
    fillForm();
    component.volontariatReciproque.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans conventionEcriteIndividuelle précisée', () => {
    fillForm();
    component.conventionEcriteIndividuelle.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans equipementFourniOuIndemnise précisé', () => {
    fillForm();
    component.equipementFourniOuIndemnise.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec indemnité négative', () => {
    fillForm();
    component.indemniteForfaitaireMensuelleEuros.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec plafond <= 0', () => {
    fillForm();
    component.plafondIndemniteMensuelleEuros.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans droitsSociauxMaintenus précisé', () => {
    fillForm();
    component.droitsSociauxMaintenus.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans deconnexionDefinie précisée', () => {
    fillForm();
    component.deconnexionDefinie.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec effectif négatif', () => {
    fillForm();
    component.effectifEntreprise.set(-1);
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
    expect(post.request.body.typeTeletravail).toBe('STRUCTUREL_CCT_85');
    expect(post.request.body.volontariatReciproque).toBe(true);
    expect(post.request.body.indemniteForfaitaireMensuelleEuros).toBe(130);
    expect(post.request.body.effectifEntreprise).toBe(50);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Conformité télétravail analysée', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'plafondIndemniteMensuelleEuros doit être strictement positif' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'plafondIndemniteMensuelleEuros doit être strictement positif', 'Fermer', expect.anything(),
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

  it('verdict CONFORME_CCT_85_STRUCTUREL → label, verdict-ok, verified', () => {
    component.result.set(response({ verdict: 'CONFORME_CCT_85_STRUCTUREL' }));
    expect(component.verdictLabel()).toContain('structurel conforme');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict CONFORME_CCT_149_OCCASIONNEL → label, verdict-ok, verified', () => {
    component.result.set(response({
      verdict: 'CONFORME_CCT_149_OCCASIONNEL',
      typeTeletravail: 'OCCASIONNEL_CCT_149',
      conventionEcriteIndividuelle: false,
      conventionRespectee: true,
    }));
    expect(component.verdictLabel()).toContain('occasionnel conforme');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict NON_CONFORME_CONVENTION_ECRITE_MANQUANTE → label, verdict-critical, cancel', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_CONVENTION_ECRITE_MANQUANTE',
      conventionEcriteIndividuelle: false,
      conventionRespectee: false,
    }));
    expect(component.verdictLabel()).toContain('Convention écrite');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('cancel');
  });

  it('verdict NON_CONFORME_EQUIPEMENT_NON_FOURNI → label, verdict-critical, cancel', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_EQUIPEMENT_NON_FOURNI',
      equipementFourniOuIndemnise: false,
      equipementRespecte: false,
    }));
    expect(component.verdictLabel()).toContain('Équipement');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('cancel');
  });

  it('verdict NON_CONFORME_DROITS_REDUITS → label, verdict-critical, cancel', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_DROITS_REDUITS',
      droitsSociauxMaintenus: false,
      droitsRespectes: false,
    }));
    expect(component.verdictLabel()).toContain('Droits réduits');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('cancel');
  });

  it('verdict FRAGILE_DECONNEXION_NON_DEFINIE → label, verdict-warn, warning_amber', () => {
    component.result.set(response({
      verdict: 'FRAGILE_DECONNEXION_NON_DEFINIE',
      deconnexionDefinie: false,
      deconnexionRespectee: false,
    }));
    expect(component.verdictLabel()).toContain('déconnexion');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('warning_amber');
  });

  it('verdict A_ANALYSER → label, verdict-neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_ANALYSER',
      typeTeletravail: 'INDETERMINE',
    }));
    expect(component.verdictLabel()).toContain('indéterminé');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('home_work');
  });

  // ============================================================
  // Badge dans header (7 cas)
  // ============================================================

  it('badge CONFORME_CCT_85_STRUCTUREL = vert', () => {
    expect(component.verdictBadge('CONFORME_CCT_85_STRUCTUREL')).toBe('CONFORME CCT 85');
    expect(component.verdictBadgeClass('CONFORME_CCT_85_STRUCTUREL')).toBe('badge-ok');
  });

  it('badge CONFORME_CCT_149_OCCASIONNEL = vert', () => {
    expect(component.verdictBadge('CONFORME_CCT_149_OCCASIONNEL')).toBe('CONFORME CCT 149');
    expect(component.verdictBadgeClass('CONFORME_CCT_149_OCCASIONNEL')).toBe('badge-ok');
  });

  it('badge NON_CONFORME_CONVENTION_ECRITE_MANQUANTE = rouge', () => {
    expect(component.verdictBadge('NON_CONFORME_CONVENTION_ECRITE_MANQUANTE')).toBe('CONVENTION MANQUANTE');
    expect(component.verdictBadgeClass('NON_CONFORME_CONVENTION_ECRITE_MANQUANTE')).toBe('badge-critical');
  });

  it('badge NON_CONFORME_EQUIPEMENT_NON_FOURNI = rouge', () => {
    expect(component.verdictBadge('NON_CONFORME_EQUIPEMENT_NON_FOURNI')).toBe('ÉQUIPEMENT NON FOURNI');
    expect(component.verdictBadgeClass('NON_CONFORME_EQUIPEMENT_NON_FOURNI')).toBe('badge-critical');
  });

  it('badge NON_CONFORME_DROITS_REDUITS = rouge', () => {
    expect(component.verdictBadge('NON_CONFORME_DROITS_REDUITS')).toBe('DROITS RÉDUITS');
    expect(component.verdictBadgeClass('NON_CONFORME_DROITS_REDUITS')).toBe('badge-critical');
  });

  it('badge FRAGILE_DECONNEXION_NON_DEFINIE = ambre', () => {
    expect(component.verdictBadge('FRAGILE_DECONNEXION_NON_DEFINIE')).toBe('DÉCONNEXION FRAGILE');
    expect(component.verdictBadgeClass('FRAGILE_DECONNEXION_NON_DEFINIE')).toBe('badge-warn');
  });

  it('badge A_ANALYSER = neutre', () => {
    expect(component.verdictBadge('A_ANALYSER')).toBe('À ANALYSER');
    expect(component.verdictBadgeClass('A_ANALYSER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — verdict CONFORME rendu (badge + 4 dimensions OK)
  // ============================================================

  it('CONFORME_CCT_85_STRUCTUREL → badge vert dans le DOM + 4 dimensions toutes OK', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('CONFORME CCT 85');
    const okRows = fixture.nativeElement.querySelectorAll('.dimension-row.dim-ok');
    expect(okRows.length).toBe(4);
    const koRows = fixture.nativeElement.querySelectorAll('.dimension-row.dim-ko');
    expect(koRows.length).toBe(0);
  });

  it('NON_CONFORME_CONVENTION_ECRITE_MANQUANTE → badge rouge + dimension KO affichée', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'NON_CONFORME_CONVENTION_ECRITE_MANQUANTE',
      conventionEcriteIndividuelle: false,
      conventionRespectee: false,
      avertissement: 'Convention écrite absente — présomption d\'irrégularité',
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('CONVENTION');
    const koRows = fixture.nativeElement.querySelectorAll('.dimension-row.dim-ko');
    expect(koRows.length).toBeGreaterThanOrEqual(1);
  });

  it('indemnité excédentaire > 0 → bloc montant affiché', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      indemniteForfaitaireMensuelleEuros: 200,
      plafondIndemniteMensuelleEuros: 154.74,
      indemniteExcedentaire: 45.26,
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('45.26');
    expect(text).toContain('excédentaire');
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellé typeTeletravail : 3 cas + null', () => {
    expect(component.typeTeletravailLabel('STRUCTUREL_CCT_85')).toContain('Structurel');
    expect(component.typeTeletravailLabel('OCCASIONNEL_CCT_149')).toContain('Occasionnel');
    expect(component.typeTeletravailLabel('INDETERMINE')).toContain('Indéterminé');
    expect(component.typeTeletravailLabel(null)).toBe('—');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non vide → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'FRAGILE_DECONNEXION_NON_DEFINIE',
      deconnexionDefinie: false,
      deconnexionRespectee: false,
      effectifEntreprise: 50,
      avertissement: 'Modalités de déconnexion absentes — sanction SPF ETCS possible',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('déconnexion');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Télétravail structurel pleinement conforme à la CCT n° 85.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('conforme');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(TeletravailBeCct85149SectionComponent.getPrefillCount({})).toBe(0);
    expect(TeletravailBeCct85149SectionComponent.getPrefillCount({
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

  it('toolId pour jurisprudence = teletravail-be-cct-85-149', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('teletravail-be-cct-85-149');
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
    component.onDateDebutTeletravailChange('2024-06-01');
    expect(component.dateDebutTeletravail()).toBe('2024-06-01');

    component.onTypeTeletravailChange('OCCASIONNEL_CCT_149');
    expect(component.typeTeletravail()).toBe('OCCASIONNEL_CCT_149');

    component.onVolontariatReciproqueChange(true);
    expect(component.volontariatReciproque()).toBe(true);

    component.onConventionEcriteIndividuelleChange(false);
    expect(component.conventionEcriteIndividuelle()).toBe(false);

    component.onEquipementFourniOuIndemniseChange(true);
    expect(component.equipementFourniOuIndemnise()).toBe(true);

    component.onIndemniteForfaitaireMensuelleEurosChange(120);
    expect(component.indemniteForfaitaireMensuelleEuros()).toBe(120);

    component.onIndemniteForfaitaireMensuelleEurosChange('150');
    expect(component.indemniteForfaitaireMensuelleEuros()).toBe(150);

    component.onIndemniteForfaitaireMensuelleEurosChange(null);
    expect(component.indemniteForfaitaireMensuelleEuros()).toBeNull();

    component.onIndemniteForfaitaireMensuelleEurosChange('');
    expect(component.indemniteForfaitaireMensuelleEuros()).toBeNull();

    component.onIndemniteForfaitaireMensuelleEurosChange('abc');
    expect(component.indemniteForfaitaireMensuelleEuros()).toBeNull();

    component.onPlafondIndemniteMensuelleEurosChange(154.74);
    expect(component.plafondIndemniteMensuelleEuros()).toBe(154.74);

    component.onDroitsSociauxMaintenusChange(true);
    expect(component.droitsSociauxMaintenus()).toBe(true);

    component.onDeconnexionDefinieChange(false);
    expect(component.deconnexionDefinie()).toBe(false);

    component.onEffectifEntrepriseChange(25);
    expect(component.effectifEntreprise()).toBe(25);
  });

  // ============================================================
  // Static metadata F-177
  // ============================================================

  it('TOOL_LABEL et TOOL_ICON statiques exposés', () => {
    expect(TeletravailBeCct85149SectionComponent.TOOL_LABEL).toBe('TÉLÉTRAVAIL (BE — CCT 85 / 149)');
    expect(TeletravailBeCct85149SectionComponent.TOOL_ICON).toBe('home_work');
  });
});
