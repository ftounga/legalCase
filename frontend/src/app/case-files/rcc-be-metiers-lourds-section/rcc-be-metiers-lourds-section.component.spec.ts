import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  RccBeMetiersLourdsSectionComponent,
} from './rcc-be-metiers-lourds-section.component';
import {
  RccBeMetiersLourdsResponse,
} from '../../core/models/rcc-be-metiers-lourds.model';

describe('RccBeMetiersLourdsSectionComponent', () => {
  let component: RccBeMetiersLourdsSectionComponent;
  let fixture: ComponentFixture<RccBeMetiersLourdsSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/rcc-be-metiers-lourds';

  /**
   * Construit une réponse backend par défaut — verdict ELIGIBLE
   * (60 ans, 38 ans de carrière, 6/10 ans en métier lourd, licenciement
   * effectif, métier pénible).
   */
  function response(
    overrides: Partial<RccBeMetiersLourdsResponse> = {},
  ): RccBeMetiersLourdsResponse {
    return {
      caseFileId: 'case-1',
      ageFinContrat: 60,
      anneesCarriereTotal: 38,
      anneesMetierLourdRecent10: 6,
      anneesMetierLourdRecent15: 9,
      typeMetierLourd: 'TRAVAIL_PENIBLE',
      licenciementEffectif: true,
      verdict: 'ELIGIBLE',
      raisonIneligibilite: null,
      synthese: 'Toutes les conditions cumulatives sont remplies.',
      baseJuridique: 'CCT n° 17 + AR du 03/05/2007 art. 3',
      avertissement: 'Liste métiers lourds : vérifier annexes AR en vigueur.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        RccBeMetiersLourdsSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RccBeMetiersLourdsSectionComponent);
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
    expect(component.ageFinContrat()).toBe(60);
    expect(component.anneesCarriereTotal()).toBe(38);
    expect(component.anneesMetierLourdRecent10()).toBe(6);
    expect(component.anneesMetierLourdRecent15()).toBe(9);
    expect(component.typeMetierLourd()).toBe('TRAVAIL_PENIBLE');
    expect(component.licenciementEffectif()).toBe(true);
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

  it('formValid() : true avec 5 champs requis remplis', () => {
    component.ageFinContrat.set(60);
    component.anneesCarriereTotal.set(38);
    component.anneesMetierLourdRecent10.set(6);
    component.anneesMetierLourdRecent15.set(9);
    component.typeMetierLourd.set('TRAVAIL_PENIBLE');
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans ageFinContrat', () => {
    component.anneesCarriereTotal.set(38);
    component.anneesMetierLourdRecent10.set(6);
    component.anneesMetierLourdRecent15.set(9);
    component.typeMetierLourd.set('TRAVAIL_PENIBLE');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans anneesCarriereTotal', () => {
    component.ageFinContrat.set(60);
    component.anneesMetierLourdRecent10.set(6);
    component.anneesMetierLourdRecent15.set(9);
    component.typeMetierLourd.set('TRAVAIL_PENIBLE');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans typeMetierLourd', () => {
    component.ageFinContrat.set(60);
    component.anneesCarriereTotal.set(38);
    component.anneesMetierLourdRecent10.set(6);
    component.anneesMetierLourdRecent15.set(9);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  it('calculate() → POST + dashboardRefresh + snack', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.ageFinContrat.set(60);
    component.anneesCarriereTotal.set(38);
    component.anneesMetierLourdRecent10.set(6);
    component.anneesMetierLourdRecent15.set(9);
    component.typeMetierLourd.set('TRAVAIL_PENIBLE');
    component.licenciementEffectif.set(true);
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.ageFinContrat).toBe(60);
    expect(post.request.body.anneesCarriereTotal).toBe(38);
    expect(post.request.body.anneesMetierLourdRecent10).toBe(6);
    expect(post.request.body.anneesMetierLourdRecent15).toBe(9);
    expect(post.request.body.typeMetierLourd).toBe('TRAVAIL_PENIBLE');
    expect(post.request.body.licenciementEffectif).toBe(true);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Éligibilité RCC métiers lourds analysée', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.ageFinContrat.set(55);
    component.anneesCarriereTotal.set(30);
    component.anneesMetierLourdRecent10.set(2);
    component.anneesMetierLourdRecent15.set(3);
    component.typeMetierLourd.set('AUTRE');
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
    component.ageFinContrat.set(60);
    component.anneesCarriereTotal.set(38);
    component.anneesMetierLourdRecent10.set(6);
    component.anneesMetierLourdRecent15.set(9);
    component.typeMetierLourd.set('TRAVAIL_PENIBLE');
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
    component.ageFinContrat.set(60);
    component.anneesCarriereTotal.set(38);
    component.anneesMetierLourdRecent10.set(6);
    component.anneesMetierLourdRecent15.set(9);
    component.typeMetierLourd.set('TRAVAIL_PENIBLE');
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({}, { status: 404, statusText: 'Not Found' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Dossier introuvable', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : FRANCE → snack indispo + pas de POST', () => {
    component.workspaceCountry = 'FRANCE';
    component.ageFinContrat.set(60);
    component.anneesCarriereTotal.set(38);
    component.anneesMetierLourdRecent10.set(6);
    component.anneesMetierLourdRecent15.set(9);
    component.typeMetierLourd.set('TRAVAIL_PENIBLE');
    component.calculate();
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Outil indisponible pour ce workspace', 'Fermer', expect.anything(),
    );
    httpMock.expectNone((r) => r.url === BASE_URL && r.method === 'POST');
  });

  // ============================================================
  // Verdict — label / classe / icône
  // ============================================================

  it('verdict ELIGIBLE → label, verdict-ok, verified', () => {
    component.result.set(response({ verdict: 'ELIGIBLE' }));
    expect(component.verdictLabel()).toBe('Éligible au RCC métiers lourds');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict INELIGIBLE → label, verdict-critical, gpp_bad', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE',
      raisonIneligibilite: 'AGE_INSUFFISANT',
    }));
    expect(component.verdictLabel()).toBe('Non éligible au RCC métiers lourds');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('gpp_bad');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('gavel');
  });

  // ============================================================
  // Badge dans header (2 cas)
  // ============================================================

  it('badge ELIGIBLE = vert "ÉLIGIBLE"', () => {
    expect(component.verdictBadge('ELIGIBLE')).toBe('ÉLIGIBLE');
    expect(component.verdictBadgeClass('ELIGIBLE')).toBe('badge-ok');
  });

  it('badge INELIGIBLE = rouge "NON ÉLIGIBLE"', () => {
    expect(component.verdictBadge('INELIGIBLE')).toBe('NON ÉLIGIBLE');
    expect(component.verdictBadgeClass('INELIGIBLE')).toBe('badge-critical');
  });

  it('ELIGIBLE → badge vert dans le DOM', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({ verdict: 'ELIGIBLE' }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('ÉLIGIBLE');
  });

  it('INELIGIBLE → badge rouge dans le DOM', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INELIGIBLE',
      raisonIneligibilite: 'AGE_INSUFFISANT',
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('NON');
  });

  // ============================================================
  // Raison d'inéligibilité — 4 cas
  // ============================================================

  it('raison DEMISSION_NON_ELIGIBLE → label démission', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE',
      raisonIneligibilite: 'DEMISSION_NON_ELIGIBLE',
    }));
    expect(component.raisonIneligibiliteLabel()).toContain('Démission');
  });

  it('raison AGE_INSUFFISANT → label âge < 58', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE',
      raisonIneligibilite: 'AGE_INSUFFISANT',
    }));
    expect(component.raisonIneligibiliteLabel()).toContain('58');
  });

  it('raison CARRIERE_INSUFFISANTE → label carrière < 35', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE',
      raisonIneligibilite: 'CARRIERE_INSUFFISANTE',
    }));
    expect(component.raisonIneligibiliteLabel()).toContain('35');
  });

  it('raison DUREE_METIER_LOURD_INSUFFISANTE → label 5/10 ou 7/15', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE',
      raisonIneligibilite: 'DUREE_METIER_LOURD_INSUFFISANTE',
    }));
    expect(component.raisonIneligibiliteLabel()).toContain('5');
    expect(component.raisonIneligibiliteLabel()).toContain('7');
  });

  it('ELIGIBLE → raisonIneligibiliteLabel() vide', () => {
    component.result.set(response({ verdict: 'ELIGIBLE', raisonIneligibilite: null }));
    expect(component.raisonIneligibiliteLabel()).toBe('');
  });

  it('INELIGIBLE → bannière raison rendue dans le DOM', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INELIGIBLE',
      raisonIneligibilite: 'CARRIERE_INSUFFISANTE',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    // Au moins une alert (raison) — possible deuxième si avertissement.
    expect(alerts.length).toBeGreaterThanOrEqual(1);
    const text = Array.from(alerts).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('35');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non vide → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'ELIGIBLE',
      avertissement: 'Vérifier les annexes AR métiers lourds en vigueur',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('annexes');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Toutes les conditions cumulatives sont remplies.',
    }));
    fixture.detectChanges();
    const synth = fixture.nativeElement.querySelector('.result-fondement');
    expect(synth).not.toBeNull();
    expect(synth.textContent).toContain('conditions');
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellés typeMetierLourd : 4 cas + null', () => {
    expect(component.typeMetierLourdLabel('EQUIPES_SUCCESSIVES_NUIT')).toContain('Équipes');
    expect(component.typeMetierLourdLabel('INTERRUPTIONS_HORAIRES_VARIABLES')).toContain('Interruptions');
    expect(component.typeMetierLourdLabel('TRAVAIL_PENIBLE')).toContain('pénible');
    expect(component.typeMetierLourdLabel('AUTRE')).toContain('Autre');
    expect(component.typeMetierLourdLabel(null)).toBe('—');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(RccBeMetiersLourdsSectionComponent.getPrefillCount({})).toBe(0);
    expect(RccBeMetiersLourdsSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateNaissanceSalarie: '1965-03-15',
        dateRuptureContrat: '2025-12-31',
        motifRupture: 'licenciement',
        anneesCarriereSalarie: 38,
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

  it('toolId pour jurisprudence = rcc-be-metiers-lourds', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('rcc-be-metiers-lourds');
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
    component.onAgeFinContratChange(60);
    expect(component.ageFinContrat()).toBe(60);

    component.onAnneesCarriereTotalChange(38);
    expect(component.anneesCarriereTotal()).toBe(38);

    component.onAnneesMetierLourdRecent10Change(6);
    expect(component.anneesMetierLourdRecent10()).toBe(6);

    component.onAnneesMetierLourdRecent15Change(9);
    expect(component.anneesMetierLourdRecent15()).toBe(9);

    component.onTypeMetierLourdChange('EQUIPES_SUCCESSIVES_NUIT');
    expect(component.typeMetierLourd()).toBe('EQUIPES_SUCCESSIVES_NUIT');

    component.onLicenciementEffectifChange(false);
    expect(component.licenciementEffectif()).toBe(false);
  });

  // ============================================================
  // Static metadata F-177
  // ============================================================

  it('TOOL_LABEL et TOOL_ICON statiques exposés', () => {
    expect(RccBeMetiersLourdsSectionComponent.TOOL_LABEL).toBe('RCC MÉTIERS LOURDS (BE)');
    expect(RccBeMetiersLourdsSectionComponent.TOOL_ICON).toBe('construction');
  });
});
