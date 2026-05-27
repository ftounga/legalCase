import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  LicenciementBeCct109DeraisonnableSectionComponent,
} from './licenciement-be-cct109-deraisonnable-section.component';
import {
  LicenciementBeCct109DeraisonnableResponse,
} from '../../core/models/licenciement-be-cct109-deraisonnable.model';

describe('LicenciementBeCct109DeraisonnableSectionComponent', () => {
  let component: LicenciementBeCct109DeraisonnableSectionComponent;
  let fixture: ComponentFixture<LicenciementBeCct109DeraisonnableSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/licenciement-be-cct109-deraisonnable';

  /**
   * Construit une réponse backend par défaut (motif vague, 3 sem.,
   * aucun aggravant — échelon JAUNE).
   */
  function response(
    overrides: Partial<LicenciementBeCct109DeraisonnableResponse> = {},
  ): LicenciementBeCct109DeraisonnableResponse {
    return {
      caseFileId: 'case-1',
      motifCommunique: true,
      motifLieAPersonne: 'MOTIF_VAGUE',
      discriminationSuspectee: false,
      represaillesSuspectees: false,
      proceduresRespectees: true,
      remunerationHebdomadaireBrute: 700,
      argumentsPatronal: null,
      echelonCct109: '3_SEMAINES',
      nombreSemaines: 3,
      indemniteCct109: 2100,
      justificationEchelon: 'Motif vague sans aggravant → minimum CCT 109 (3 semaines).',
      cumulAvecIcp: true,
      baseJuridique: 'CCT n° 109 du 12/02/2014, art. 8-9',
      avertissement: null,
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        LicenciementBeCct109DeraisonnableSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(LicenciementBeCct109DeraisonnableSectionComponent);
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
    expect(component.result()!.echelonCct109).toBe('3_SEMAINES');
    expect(component.showForm()).toBe(false);
    // Rehydratation form.
    expect(component.motifCommunique()).toBe(true);
    expect(component.motifLieAPersonne()).toBe('MOTIF_VAGUE');
    expect(component.discriminationSuspectee()).toBe(false);
    expect(component.represaillesSuspectees()).toBe(false);
    expect(component.proceduresRespectees()).toBe(true);
    expect(component.remunerationHebdomadaireBrute()).toBe(700);
    expect(component.argumentsPatronal()).toBeNull();
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

  it('formValid() : true avec champs requis remplis', () => {
    component.motifCommunique.set(true);
    component.motifLieAPersonne.set('MOTIF_VAGUE');
    component.remunerationHebdomadaireBrute.set(700);
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans motifCommunique', () => {
    component.motifLieAPersonne.set('MOTIF_VAGUE');
    component.remunerationHebdomadaireBrute.set(700);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans motifLieAPersonne', () => {
    component.motifCommunique.set(true);
    component.remunerationHebdomadaireBrute.set(700);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false si rémunération hebdo = 0', () => {
    component.motifCommunique.set(true);
    component.motifLieAPersonne.set('MOTIF_VAGUE');
    component.remunerationHebdomadaireBrute.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false si rémunération hebdo négative', () => {
    component.motifCommunique.set(true);
    component.motifLieAPersonne.set('MOTIF_VAGUE');
    component.remunerationHebdomadaireBrute.set(-100);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true avec motifCommunique=false (booléen valorisé)', () => {
    component.motifCommunique.set(false);
    component.motifLieAPersonne.set('SANS_MOTIF');
    component.remunerationHebdomadaireBrute.set(800);
    expect(component.formValid()).toBe(true);
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  it('calculate() → POST + dashboardRefresh + snack', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.motifCommunique.set(false);
    component.motifLieAPersonne.set('SANS_MOTIF');
    component.discriminationSuspectee.set(true);
    component.represaillesSuspectees.set(true);
    component.proceduresRespectees.set(false);
    component.remunerationHebdomadaireBrute.set(800);
    component.argumentsPatronal.set('Réorganisation profonde');
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.motifCommunique).toBe(false);
    expect(post.request.body.motifLieAPersonne).toBe('SANS_MOTIF');
    expect(post.request.body.discriminationSuspectee).toBe(true);
    expect(post.request.body.represaillesSuspectees).toBe(true);
    expect(post.request.body.proceduresRespectees).toBe(false);
    expect(post.request.body.remunerationHebdomadaireBrute).toBe(800);
    expect(post.request.body.argumentsPatronal).toBe('Réorganisation profonde');
    post.flush(response({
      echelonCct109: '17_SEMAINES',
      nombreSemaines: 17,
      indemniteCct109: 13600,
    }));
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Score CCT 109 BE analysé', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.motifCommunique.set(true);
    component.motifLieAPersonne.set('MOTIF_VAGUE');
    component.remunerationHebdomadaireBrute.set(700);
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'remunerationHebdomadaireBrute doit être > 0' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'remunerationHebdomadaireBrute doit être > 0', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : erreur 500 → snack générique', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    component.motifCommunique.set(true);
    component.motifLieAPersonne.set('MOTIF_VAGUE');
    component.remunerationHebdomadaireBrute.set(700);
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({}, { status: 500, statusText: 'Server Error' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Une erreur est survenue. Veuillez réessayer.', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : workspace FR → snack indispo + pas de POST', () => {
    component.workspaceCountry = 'FRANCE';
    component.motifCommunique.set(true);
    component.motifLieAPersonne.set('MOTIF_VAGUE');
    component.remunerationHebdomadaireBrute.set(700);
    component.calculate();
    httpMock.expectNone((r) => r.url === BASE_URL && r.method === 'POST');
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Outil indisponible pour ce workspace', 'Fermer', expect.anything(),
    );
  });

  // ============================================================
  // Échelon — labels / classes / icônes
  // ============================================================

  it('échelon NON_DERAISONNABLE → label vert + check_circle', () => {
    component.result.set(response({ echelonCct109: 'NON_DERAISONNABLE', nombreSemaines: 0, indemniteCct109: 0 }));
    expect(component.echelonLabel('NON_DERAISONNABLE')).toContain('Non déraisonnable');
    expect(component.echelonClass()).toBe('echelon-ok');
    expect(component.echelonIcon()).toBe('check_circle');
  });

  it('échelon 3_SEMAINES → label jaune + info', () => {
    component.result.set(response({ echelonCct109: '3_SEMAINES' }));
    expect(component.echelonLabel('3_SEMAINES')).toContain('3 semaines');
    expect(component.echelonClass()).toBe('echelon-warn');
    expect(component.echelonIcon()).toBe('info');
  });

  it('échelon 8_SEMAINES → label orange + warning_amber', () => {
    component.result.set(response({ echelonCct109: '8_SEMAINES' }));
    expect(component.echelonLabel('8_SEMAINES')).toContain('8 semaines');
    expect(component.echelonClass()).toBe('echelon-orange');
    expect(component.echelonIcon()).toBe('warning_amber');
  });

  it('échelon 12_SEMAINES → label rouge + gpp_bad', () => {
    component.result.set(response({ echelonCct109: '12_SEMAINES' }));
    expect(component.echelonLabel('12_SEMAINES')).toContain('12 semaines');
    expect(component.echelonClass()).toBe('echelon-critical');
    expect(component.echelonIcon()).toBe('gpp_bad');
  });

  it('échelon 17_SEMAINES → label rouge foncé + dangerous', () => {
    component.result.set(response({ echelonCct109: '17_SEMAINES' }));
    expect(component.echelonLabel('17_SEMAINES')).toContain('17 semaines');
    expect(component.echelonClass()).toBe('echelon-max');
    expect(component.echelonIcon()).toBe('dangerous');
  });

  it('result null → labels neutres', () => {
    expect(component.echelonLabel(null)).toBe('—');
    expect(component.echelonClass()).toBe('');
    expect(component.echelonIcon()).toBe('rule');
  });

  // ============================================================
  // Badge header — gradient 5 niveaux
  // ============================================================

  it('badge header pour 5 échelons : libellés + classes', () => {
    expect(component.echelonBadgeShort('NON_DERAISONNABLE')).toBe('NON DÉRAISONNABLE');
    expect(component.echelonBadgeShortClass('NON_DERAISONNABLE')).toBe('badge-ok');
    expect(component.echelonBadgeShort('3_SEMAINES')).toBe('3 SEMAINES');
    expect(component.echelonBadgeShortClass('3_SEMAINES')).toBe('badge-warn');
    expect(component.echelonBadgeShort('8_SEMAINES')).toBe('8 SEMAINES');
    expect(component.echelonBadgeShortClass('8_SEMAINES')).toBe('badge-orange');
    expect(component.echelonBadgeShort('12_SEMAINES')).toBe('12 SEMAINES');
    expect(component.echelonBadgeShortClass('12_SEMAINES')).toBe('badge-critical');
    expect(component.echelonBadgeShort('17_SEMAINES')).toBe('17 SEMAINES');
    expect(component.echelonBadgeShortClass('17_SEMAINES')).toBe('badge-max');
  });

  it('17_SEMAINES → badge rouge foncé visible dans header DOM', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({ echelonCct109: '17_SEMAINES', nombreSemaines: 17, indemniteCct109: 13600 }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-max');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('17 SEMAINES');
  });

  it('NON_DERAISONNABLE → badge vert visible dans header DOM', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({ echelonCct109: 'NON_DERAISONNABLE', nombreSemaines: 0, indemniteCct109: 0 }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('NON DÉRAISONNABLE');
  });

  // ============================================================
  // Indemnité CCT 109 — affichage
  // ============================================================

  it('nombreSemaines > 0 → bloc indemnité rendu avec montant + semaines', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      echelonCct109: '8_SEMAINES',
      nombreSemaines: 8,
      indemniteCct109: 5600,
    }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('.indemnite-block');
    expect(block).not.toBeNull();
    const amount = block.querySelector('.indemnite-amount');
    // Format français : 5 600,00 € (espace insécable possible)
    expect(amount.textContent).toContain('5');
    expect(amount.textContent).toContain('€');
    const detail = block.querySelector('.indemnite-detail');
    expect(detail.textContent).toContain('8');
  });

  it('NON_DERAISONNABLE (0 sem.) → pas de bloc indemnité', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      echelonCct109: 'NON_DERAISONNABLE',
      nombreSemaines: 0,
      indemniteCct109: 0,
    }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('.indemnite-block');
    expect(block).toBeNull();
  });

  // ============================================================
  // Bannière cumul ICP — info systématique
  // ============================================================

  it('cumulAvecIcp=true → bannière info ICP rendue (systématique)', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({ cumulAvecIcp: true }));
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.result-info-icp');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('ICP');
    expect(banner.textContent).toContain('statut unique');
  });

  // ============================================================
  // Justification de l'échelon — police monospace
  // ============================================================

  it('justification rendue en JetBrains Mono', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      justificationEchelon: 'Motif valable + procédures respectées → NON_DERAISONNABLE.',
    }));
    fixture.detectChanges();
    const justif = fixture.nativeElement.querySelector('.justification-text');
    expect(justif).not.toBeNull();
    expect(justif.classList.contains('mono')).toBe(true);
    expect(justif.textContent).toContain('NON_DERAISONNABLE');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non null → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      avertissement: 'Discrimination + représailles : tribunal du travail recommandé.',
    }));
    fixture.detectChanges();
    const alert = fixture.nativeElement.querySelector('.result-alert');
    expect(alert).not.toBeNull();
    expect(alert.textContent).toContain('tribunal');
  });

  // ============================================================
  // Motifs liés à la personne — 4 cas labels
  // ============================================================

  it('libellés motifLieAPersonne : 4 cas + null', () => {
    expect(component.motifLieAPersonneLabel('SANS_MOTIF')).toContain('Aucun motif');
    expect(component.motifLieAPersonneLabel('MOTIF_VAGUE')).toContain('vague');
    expect(component.motifLieAPersonneLabel('MOTIF_PRECIS_DISPUTE')).toContain('contesté');
    expect(component.motifLieAPersonneLabel('MOTIF_PROUVE_VALIDE')).toContain('prouvé');
    expect(component.motifLieAPersonneLabel(null)).toBe('—');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(LicenciementBeCct109DeraisonnableSectionComponent.getPrefillCount({})).toBe(0);
    expect(LicenciementBeCct109DeraisonnableSectionComponent.getPrefillCount({
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

  it('toolId pour jurisprudence = licenciement-be-cct109-deraisonnable', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('licenciement-be-cct109-deraisonnable');
  });

  // ============================================================
  // Toggle collapse
  // ============================================================

  it('toggleCollapse() flips collapsed signal', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  // ============================================================
  // forceExpanded
  // ============================================================

  it('forceExpanded → collapsed=false au ngOnInit', () => {
    component.forceExpanded = true;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    expect(component.collapsed()).toBe(false);
  });
});
