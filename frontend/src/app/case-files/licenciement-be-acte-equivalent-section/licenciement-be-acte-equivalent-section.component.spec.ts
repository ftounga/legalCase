import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  LicenciementBeActeEquivalentSectionComponent,
} from './licenciement-be-acte-equivalent-section.component';
import {
  LicenciementBeActeEquivalentResponse,
} from '../../core/models/licenciement-be-acte-equivalent.model';

describe('LicenciementBeActeEquivalentSectionComponent', () => {
  let component: LicenciementBeActeEquivalentSectionComponent;
  let fixture: ComponentFixture<LicenciementBeActeEquivalentSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/licenciement-be-acte-equivalent';

  /**
   * Construit une réponse backend par défaut — verdict ACTE_EQUIPOLLENT_PROBABLE
   * (modification SALAIRE substantielle d'un élément essentiel, salarié a
   * protesté, ICP indicatif calculée).
   */
  function response(
    overrides: Partial<LicenciementBeActeEquivalentResponse> = {},
  ): LicenciementBeActeEquivalentResponse {
    return {
      caseFileId: 'case-1',
      typeModification: 'SALAIRE',
      ampleurModification: 'SUBSTANTIELLE',
      elementEssentielDuContrat: true,
      dateModification: '2026-04-15',
      salarieAProteste: true,
      delaiDepuisModificationJours: 12,
      remunerationHebdomadaireBrute: 1000,
      dureePreavisCalculeeSemaines: 16,
      verdict: 'ACTE_EQUIPOLLENT_PROBABLE',
      fondementJuridique: 'Loi 03/07/1978 art. 20 + Cass. BE 23/12/1957',
      icpIndicatif: 16000,
      risqueAcceptationTacite: false,
      delaiRecommandeProtestationJours: 30,
      baseJuridique: 'Loi 03/07/1978 art. 20',
      avertissement: null,
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        LicenciementBeActeEquivalentSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(LicenciementBeActeEquivalentSectionComponent);
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
    expect(component.result()!.verdict).toBe('ACTE_EQUIPOLLENT_PROBABLE');
    expect(component.showForm()).toBe(false);
    // Rehydratation form.
    expect(component.typeModification()).toBe('SALAIRE');
    expect(component.ampleurModification()).toBe('SUBSTANTIELLE');
    expect(component.elementEssentielDuContrat()).toBe(true);
    expect(component.dateModification()).toBe('2026-04-15');
    expect(component.salarieAProteste()).toBe(true);
    expect(component.delaiDepuisModificationJours()).toBe(12);
    expect(component.remunerationHebdomadaireBrute()).toBe(1000);
    expect(component.dureePreavisCalculeeSemaines()).toBe(16);
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

  it('formValid() : true avec 3 champs requis remplis', () => {
    component.typeModification.set('SALAIRE');
    component.ampleurModification.set('SUBSTANTIELLE');
    component.dateModification.set('2026-04-15');
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans typeModification', () => {
    component.ampleurModification.set('SUBSTANTIELLE');
    component.dateModification.set('2026-04-15');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans ampleurModification', () => {
    component.typeModification.set('SALAIRE');
    component.dateModification.set('2026-04-15');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dateModification', () => {
    component.typeModification.set('SALAIRE');
    component.ampleurModification.set('SUBSTANTIELLE');
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  it('calculate() → POST + dashboardRefresh + snack', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.typeModification.set('SALAIRE');
    component.ampleurModification.set('SUBSTANTIELLE');
    component.dateModification.set('2026-04-15');
    component.elementEssentielDuContrat.set(true);
    component.salarieAProteste.set(true);
    component.remunerationHebdomadaireBrute.set(1000);
    component.dureePreavisCalculeeSemaines.set(16);
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.typeModification).toBe('SALAIRE');
    expect(post.request.body.ampleurModification).toBe('SUBSTANTIELLE');
    expect(post.request.body.dateModification).toBe('2026-04-15');
    expect(post.request.body.elementEssentielDuContrat).toBe(true);
    expect(post.request.body.salarieAProteste).toBe(true);
    expect(post.request.body.remunerationHebdomadaireBrute).toBe(1000);
    expect(post.request.body.dureePreavisCalculeeSemaines).toBe(16);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Acte équipollent analysé', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack "invalides"', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.typeModification.set('FONCTION');
    component.ampleurModification.set('MINEURE');
    component.dateModification.set('2026-04-15');
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
    component.typeModification.set('FONCTION');
    component.ampleurModification.set('MINEURE');
    component.dateModification.set('2026-04-15');
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({}, { status: 500, statusText: 'Server Error' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Une erreur est survenue. Veuillez réessayer.', 'Fermer', expect.anything(),
    );
  });

  // ============================================================
  // Verdict — label / classe / icône
  // ============================================================

  it('verdict ACTE_EQUIPOLLENT_PROBABLE → label, verdict-critical, gpp_bad', () => {
    component.result.set(response({ verdict: 'ACTE_EQUIPOLLENT_PROBABLE' }));
    expect(component.verdictLabel()).toBe('Acte équipollent à rupture probable');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('gpp_bad');
  });

  it('verdict PAS_ACTE_EQUIPOLLENT → label, verdict-ok, verified', () => {
    component.result.set(response({
      verdict: 'PAS_ACTE_EQUIPOLLENT',
      ampleurModification: 'MINEURE',
      icpIndicatif: null,
    }));
    expect(component.verdictLabel()).toBe('Pas d\'acte équipollent — Ius Variandi');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict RISQUE_ACCEPTATION_TACITE → label, verdict-critical, warning', () => {
    component.result.set(response({
      verdict: 'RISQUE_ACCEPTATION_TACITE',
      salarieAProteste: false,
      risqueAcceptationTacite: true,
      icpIndicatif: null,
    }));
    expect(component.verdictLabel()).toBe('Risque d\'acceptation tacite');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('warning');
  });

  it('verdict A_ANALYSER → label, verdict-warn, help_outline', () => {
    component.result.set(response({
      verdict: 'A_ANALYSER',
      ampleurModification: 'INDETERMINEES',
      icpIndicatif: null,
    }));
    expect(component.verdictLabel()).toBe('Analyse approfondie requise');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('gavel');
  });

  // ============================================================
  // Badge dans header (4 cas verdicts)
  // ============================================================

  it('badge ACTE_EQUIPOLLENT_PROBABLE = rouge "ACTE ÉQUIPOLLENT"', () => {
    expect(component.verdictBadge('ACTE_EQUIPOLLENT_PROBABLE')).toBe('ACTE ÉQUIPOLLENT');
    expect(component.verdictBadgeClass('ACTE_EQUIPOLLENT_PROBABLE')).toBe('badge-critical');
  });

  it('badge RISQUE_ACCEPTATION_TACITE = rouge "RISQUE TACITE"', () => {
    expect(component.verdictBadge('RISQUE_ACCEPTATION_TACITE')).toBe('RISQUE TACITE');
    expect(component.verdictBadgeClass('RISQUE_ACCEPTATION_TACITE')).toBe('badge-critical');
  });

  it('badge PAS_ACTE_EQUIPOLLENT = vert "PAS D\'ACTE"', () => {
    expect(component.verdictBadge('PAS_ACTE_EQUIPOLLENT')).toBe('PAS D\'ACTE');
    expect(component.verdictBadgeClass('PAS_ACTE_EQUIPOLLENT')).toBe('badge-ok');
  });

  it('badge A_ANALYSER = orange "À ANALYSER"', () => {
    expect(component.verdictBadge('A_ANALYSER')).toBe('À ANALYSER');
    expect(component.verdictBadgeClass('A_ANALYSER')).toBe('badge-warn');
  });

  it('ACTE_EQUIPOLLENT_PROBABLE → badge rouge dans le DOM', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({ verdict: 'ACTE_EQUIPOLLENT_PROBABLE' }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('ACTE');
  });

  it('PAS_ACTE_EQUIPOLLENT → badge vert dans le DOM', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'PAS_ACTE_EQUIPOLLENT',
      ampleurModification: 'MINEURE',
      icpIndicatif: null,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('PAS');
  });

  it('A_ANALYSER → badge orange dans le DOM', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'A_ANALYSER',
      ampleurModification: 'INDETERMINEES',
      icpIndicatif: null,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-warn');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('ANALYSER');
  });

  // ============================================================
  // ICP indicatif affiché
  // ============================================================

  it('ACTE_EQUIPOLLENT_PROBABLE + ICP → bloc ICP rendu avec montant', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'ACTE_EQUIPOLLENT_PROBABLE',
      icpIndicatif: 16000,
    }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('.icp-block');
    expect(block).not.toBeNull();
    const amount = block.querySelector('.icp-amount');
    expect(amount.textContent).toContain('16');
    expect(amount.textContent).toContain('€');
  });

  it('PAS_ACTE_EQUIPOLLENT → pas de bloc ICP', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'PAS_ACTE_EQUIPOLLENT',
      icpIndicatif: null,
    }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('.icp-block');
    expect(block).toBeNull();
  });

  it('ACTE_EQUIPOLLENT_PROBABLE sans ICP → pas de bloc', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'ACTE_EQUIPOLLENT_PROBABLE',
      icpIndicatif: null,
    }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('.icp-block');
    expect(block).toBeNull();
  });

  // ============================================================
  // Délai protestation 30 j
  // ============================================================

  it('risqueAcceptationTacite=true → bloc délai critical', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'RISQUE_ACCEPTATION_TACITE',
      risqueAcceptationTacite: true,
      delaiRecommandeProtestationJours: 30,
    }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('.delai-protestation.delai-critical');
    expect(block).not.toBeNull();
    expect(block.textContent).toContain('Risque');
  });

  it('risqueAcceptationTacite=false → bloc délai sans critical', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'PAS_ACTE_EQUIPOLLENT',
      risqueAcceptationTacite: false,
      delaiRecommandeProtestationJours: 30,
    }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('.delai-protestation');
    expect(block).not.toBeNull();
    expect(block.classList.contains('delai-critical')).toBe(false);
    expect(block.textContent).toContain('30');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non null → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      avertissement: 'Délai de protestation dépassé — risque acceptation tacite',
    }));
    fixture.detectChanges();
    const alert = fixture.nativeElement.querySelector('.result-alert');
    expect(alert).not.toBeNull();
    expect(alert.textContent).toContain('protestation');
  });

  // ============================================================
  // Fondement juridique
  // ============================================================

  it('fondement juridique affiché si présent', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      fondementJuridique: 'Loi 03/07/1978 art. 20 + Cass. BE 23/12/1957',
    }));
    fixture.detectChanges();
    const fondement = fixture.nativeElement.querySelector('.result-fondement');
    expect(fondement).not.toBeNull();
    expect(fondement.textContent).toContain('Cass. BE');
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellés typeModification : 5 cas + null', () => {
    expect(component.typeModificationLabel('SALAIRE')).toContain('Salaire');
    expect(component.typeModificationLabel('LIEU_TRAVAIL')).toContain('Lieu');
    expect(component.typeModificationLabel('FONCTION')).toContain('Fonction');
    expect(component.typeModificationLabel('HORAIRE')).toContain('Horaire');
    expect(component.typeModificationLabel('AUTRE')).toContain('Autre');
    expect(component.typeModificationLabel(null)).toBe('—');
  });

  it('libellés ampleurModification : 3 cas + null', () => {
    expect(component.ampleurModificationLabel('MINEURE')).toContain('Mineure');
    expect(component.ampleurModificationLabel('SUBSTANTIELLE')).toContain('Substantielle');
    expect(component.ampleurModificationLabel('INDETERMINEES')).toContain('Indéterminée');
    expect(component.ampleurModificationLabel(null)).toBe('—');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(LicenciementBeActeEquivalentSectionComponent.getPrefillCount({})).toBe(0);
    expect(LicenciementBeActeEquivalentSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: { dateLicenciement: '2026-05-10', salaireBrutMensuel: 3500 },
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

  it('toolId pour jurisprudence = licenciement-be-acte-equivalent', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('licenciement-be-acte-equivalent');
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
    component.onTypeModificationChange('LIEU_TRAVAIL');
    expect(component.typeModification()).toBe('LIEU_TRAVAIL');

    component.onAmpleurModificationChange('SUBSTANTIELLE');
    expect(component.ampleurModification()).toBe('SUBSTANTIELLE');

    component.onElementEssentielDuContratChange(true);
    expect(component.elementEssentielDuContrat()).toBe(true);

    component.onDateModificationChange('2026-04-15');
    expect(component.dateModification()).toBe('2026-04-15');

    component.onSalarieAProtesteChange(true);
    expect(component.salarieAProteste()).toBe(true);

    component.onDelaiDepuisModificationJoursChange(20);
    expect(component.delaiDepuisModificationJours()).toBe(20);

    component.onRemunerationHebdomadaireBruteChange(1200);
    expect(component.remunerationHebdomadaireBrute()).toBe(1200);

    component.onDureePreavisCalculeeSemainesChange(20);
    expect(component.dureePreavisCalculeeSemaines()).toBe(20);
  });

  it('onDateModificationChange("") → null (clear)', () => {
    component.dateModification.set('2026-04-15');
    component.onDateModificationChange('');
    expect(component.dateModification()).toBeNull();
  });
});
