import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  CongeEducationPayeRegionSectionComponent,
} from './conge-education-paye-region-section.component';
import {
  CongeEducationPayeRegionResponse,
} from '../../core/models/conge-education-paye-region.model';

describe('CongeEducationPayeRegionSectionComponent', () => {
  let component: CongeEducationPayeRegionSectionComponent;
  let fixture: ComponentFixture<CongeEducationPayeRegionSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/conge-education-paye-region';

  /**
   * Construit une réponse backend par défaut — verdict ELIGIBLE_PLEIN_DROIT
   * (Wallonie, formation professionnelle qualifiante, 150 h demandées,
   * temps plein, formation agréée — plafond 180 h, allouées 150 h).
   */
  function response(
    overrides: Partial<CongeEducationPayeRegionResponse> = {},
  ): CongeEducationPayeRegionResponse {
    return {
      caseFileId: 'case-1',
      region: 'WALLONIE',
      typeFormation: 'PROFESSIONNELLE_QUALIFIANTE',
      heuresFormationAnnuelles: 150,
      tauxOccupation: 1.0,
      publicFragilise: false,
      formationAgreeeRegion: true,
      verdict: 'ELIGIBLE_PLEIN_DROIT',
      plafondRegionalHeures: 180,
      heuresCongeAllouees: 150,
      regimeApplicable: 'Wallonie',
      raison: 'DROIT_RECONNU',
      synthese: 'Droit au congé-éducation payé reconnu en région Wallonie (Décret WBR 19/02/2014 + AGW 19/12/2014) : 150 h allouées sur 150 h demandées.',
      baseJuridique: 'Loi du 22/01/1985 Section 6 + Loi spéciale 06/01/2014 + Décret WBR 19/02/2014 + AGW 19/12/2014 ; Décret 12/12/2014 (VOV) ; Ordonnance 02/07/2015 + AGBR 14/04/2016',
      avertissement: 'Les plafonds d\'heures et la liste des formations agréées sont fixés par arrêté régional, périodiquement révisés.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        CongeEducationPayeRegionSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(CongeEducationPayeRegionSectionComponent);
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
    expect(component.result()!.verdict).toBe('ELIGIBLE_PLEIN_DROIT');
    expect(component.showForm()).toBe(false);
    expect(component.region()).toBe('WALLONIE');
    expect(component.typeFormation()).toBe('PROFESSIONNELLE_QUALIFIANTE');
    expect(component.heuresFormationAnnuelles()).toBe(150);
    expect(component.tauxOccupation()).toBe(1.0);
    expect(component.publicFragilise()).toBe(false);
    expect(component.formationAgreeeRegion()).toBe(true);
  });

  it('GET 404 → reste en mode formulaire vierge', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.result()).toBeNull();
    expect(component.showForm()).toBe(true);
    // Taux d'occupation par défaut métier conservé.
    expect(component.tauxOccupation()).toBe(1.0);
  });

  // ============================================================
  // Form valid
  // ============================================================

  it('formValid() : false sans champs', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true avec 4 champs requis remplis', () => {
    component.region.set('WALLONIE');
    component.typeFormation.set('PROFESSIONNELLE_QUALIFIANTE');
    component.heuresFormationAnnuelles.set(150);
    // tauxOccupation défaut 1.0
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans region', () => {
    component.typeFormation.set('GENERALE');
    component.heuresFormationAnnuelles.set(100);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans typeFormation', () => {
    component.region.set('FLANDRE');
    component.heuresFormationAnnuelles.set(100);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans heuresFormationAnnuelles', () => {
    component.region.set('WALLONIE');
    component.typeFormation.set('GENERALE');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false si heuresFormationAnnuelles négatif', () => {
    component.region.set('WALLONIE');
    component.typeFormation.set('GENERALE');
    component.heuresFormationAnnuelles.set(-10);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false si tauxOccupation > 1', () => {
    component.region.set('WALLONIE');
    component.typeFormation.set('GENERALE');
    component.heuresFormationAnnuelles.set(100);
    component.tauxOccupation.set(1.5);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false si tauxOccupation < 0', () => {
    component.region.set('WALLONIE');
    component.typeFormation.set('GENERALE');
    component.heuresFormationAnnuelles.set(100);
    component.tauxOccupation.set(-0.1);
    expect(component.formValid()).toBe(false);
  });

  it('handler taux null → défaut 1.0', () => {
    component.onTauxOccupationChange(null);
    expect(component.tauxOccupation()).toBe(1.0);
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  function fillForm() {
    component.region.set('WALLONIE');
    component.typeFormation.set('PROFESSIONNELLE_QUALIFIANTE');
    component.heuresFormationAnnuelles.set(150);
    component.tauxOccupation.set(1.0);
    component.publicFragilise.set(false);
    component.formationAgreeeRegion.set(true);
  }

  it('calculate() → POST + snack succès', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.region).toBe('WALLONIE');
    expect(post.request.body.typeFormation).toBe('PROFESSIONNELLE_QUALIFIANTE');
    expect(post.request.body.heuresFormationAnnuelles).toBe(150);
    expect(post.request.body.tauxOccupation).toBe(1.0);
    expect(post.request.body.publicFragilise).toBe(false);
    expect(post.request.body.formationAgreeeRegion).toBe(true);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Congé-éducation payé analysé', 'OK', expect.anything(),
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
  // Verdict — label / classe / icône (5 cas)
  // ============================================================

  it('verdict ELIGIBLE_PLEIN_DROIT → label, verdict-ok, verified', () => {
    component.result.set(response({ verdict: 'ELIGIBLE_PLEIN_DROIT' }));
    expect(component.verdictLabel()).toContain('plafond régional intégral');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict ELIGIBLE_PRORATA → label, verdict-ok, check_circle', () => {
    component.result.set(response({
      verdict: 'ELIGIBLE_PRORATA',
      tauxOccupation: 0.5,
      plafondRegionalHeures: 180,
      heuresCongeAllouees: 90,
    }));
    expect(component.verdictLabel()).toContain('proratisé');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('check_circle');
  });

  it('verdict INELIGIBLE_HORS_FORMATION_AGREEE → label, critical, block', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_HORS_FORMATION_AGREEE',
      formationAgreeeRegion: false,
      plafondRegionalHeures: 0,
      heuresCongeAllouees: 0,
    }));
    expect(component.verdictLabel()).toContain('hors liste agréée');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('block');
  });

  it('verdict INELIGIBLE_OCCUPATION_INSUFFISANTE → label, critical, block', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_OCCUPATION_INSUFFISANTE',
      tauxOccupation: 0.3,
      plafondRegionalHeures: 0,
      heuresCongeAllouees: 0,
    }));
    expect(component.verdictLabel()).toContain('occupation sous le seuil');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('block');
  });

  it('verdict A_ANALYSER → label, verdict-info, help', () => {
    component.result.set(response({ verdict: 'A_ANALYSER' }));
    expect(component.verdictLabel()).toContain('À analyser');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('help');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('school');
  });

  // ============================================================
  // Badge dans header (5 cas)
  // ============================================================

  it('badge ELIGIBLE_PLEIN_DROIT = vert "PLEIN DROIT"', () => {
    expect(component.verdictBadge('ELIGIBLE_PLEIN_DROIT')).toBe('PLEIN DROIT');
    expect(component.verdictBadgeClass('ELIGIBLE_PLEIN_DROIT')).toBe('badge-ok');
  });

  it('badge ELIGIBLE_PRORATA = vert "PRORATA"', () => {
    expect(component.verdictBadge('ELIGIBLE_PRORATA')).toBe('PRORATA');
    expect(component.verdictBadgeClass('ELIGIBLE_PRORATA')).toBe('badge-ok');
  });

  it('badge INELIGIBLE_HORS_FORMATION_AGREEE = rouge "HORS LISTE"', () => {
    expect(component.verdictBadge('INELIGIBLE_HORS_FORMATION_AGREEE')).toBe('HORS LISTE');
    expect(component.verdictBadgeClass('INELIGIBLE_HORS_FORMATION_AGREEE')).toBe('badge-critical');
  });

  it('badge INELIGIBLE_OCCUPATION_INSUFFISANTE = rouge "OCC. INSUFF."', () => {
    expect(component.verdictBadge('INELIGIBLE_OCCUPATION_INSUFFISANTE')).toBe('OCC. INSUFF.');
    expect(component.verdictBadgeClass('INELIGIBLE_OCCUPATION_INSUFFISANTE')).toBe('badge-critical');
  });

  it('badge A_ANALYSER = info "À ANALYSER"', () => {
    expect(component.verdictBadge('A_ANALYSER')).toBe('À ANALYSER');
    expect(component.verdictBadgeClass('A_ANALYSER')).toBe('badge-info');
  });

  // ============================================================
  // DOM integration : badge header
  // ============================================================

  it('ELIGIBLE_PLEIN_DROIT → badge vert + heures allouées dans le DOM', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();

    const badge = fixture.nativeElement.querySelector('.section-badge');
    expect(badge).not.toBeNull();
    expect(badge.textContent.trim()).toBe('PLEIN DROIT');
    expect(badge.classList.contains('badge-ok')).toBe(true);

    const html = fixture.nativeElement.textContent;
    expect(html).toContain('Wallonie');
    expect(html).toContain('150');
    expect(html).toContain('180');
  });

  it('INELIGIBLE_HORS_FORMATION_AGREEE → badge rouge dans le DOM', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INELIGIBLE_HORS_FORMATION_AGREEE',
      formationAgreeeRegion: false,
      plafondRegionalHeures: 0,
      heuresCongeAllouees: 0,
    }));
    fixture.detectChanges();

    const badge = fixture.nativeElement.querySelector('.section-badge');
    expect(badge.classList.contains('badge-critical')).toBe(true);
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
    expect(CongeEducationPayeRegionSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: { dateRuptureContrat: '2026-09-30', motifRupture: 'autre' },
    })).toBe(0);
  });

  // ============================================================
  // Tool metadata statique (F-177 SF-177-03b)
  // ============================================================

  it('TOOL_LABEL + TOOL_ICON exposés', () => {
    expect(CongeEducationPayeRegionSectionComponent.TOOL_LABEL).toBe('CONGÉ-ÉDUCATION PAYÉ (BE — RÉGIONALISÉ)');
    expect(CongeEducationPayeRegionSectionComponent.TOOL_ICON).toBe('school');
  });

  // ============================================================
  // Labels enums lisibles
  // ============================================================

  it('regionLabel mappe les 3 enums + fallback', () => {
    expect(component.regionLabel('WALLONIE')).toContain('Wallonie');
    expect(component.regionLabel('FLANDRE')).toContain('VOV');
    expect(component.regionLabel('BRUXELLES')).toContain('Bruxelles');
    expect(component.regionLabel(null)).toBe('—');
  });

  it('typeFormationLabel mappe les 5 enums + fallback', () => {
    expect(component.typeFormationLabel('PROFESSIONNELLE_QUALIFIANTE')).toContain('qualifiante');
    expect(component.typeFormationLabel('GENERALE')).toContain('générale');
    expect(component.typeFormationLabel('ENSEIGNEMENT_SUPERIEUR_ALTERNANCE')).toContain('alternance');
    expect(component.typeFormationLabel('RECONVERSION_PUBLIC_FRAGILISE')).toContain('Reconversion');
    expect(component.typeFormationLabel('HORS_LISTE_AGREEE')).toContain('hors liste');
    expect(component.typeFormationLabel(null)).toBe('—');
  });
});
