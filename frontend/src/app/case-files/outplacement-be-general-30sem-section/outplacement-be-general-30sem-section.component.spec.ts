import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  OutplacementBeGeneral30semSectionComponent,
} from './outplacement-be-general-30sem-section.component';
import {
  OutplacementBeGeneral30semResponse,
} from '../../core/models/outplacement-be-general-30sem.model';

describe('OutplacementBeGeneral30semSectionComponent', () => {
  let component: OutplacementBeGeneral30semSectionComponent;
  let fixture: ComponentFixture<OutplacementBeGeneral30semSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/outplacement-be-general-30sem';

  /**
   * Construit une réponse backend par défaut — verdict CONFORME
   * (toutes conditions remplies).
   */
  function response(
    overrides: Partial<OutplacementBeGeneral30semResponse> = {},
  ): OutplacementBeGeneral30semResponse {
    return {
      caseFileId: 'case-1',
      licenciementEffectif: true,
      motifGrave: false,
      semainesPreavisOuIndemnite: 32,
      dateFinContrat: '2026-04-30',
      offreNotifiee: true,
      dateNotificationOffre: '2026-05-02',
      heuresProgramme: 60,
      dureeProgrammeMois: 12,
      offreEcrite: true,
      offreIndividuelle: true,
      contenuMinimumRespecte: true,
      verdict: 'CONFORME',
      conforme: true,
      conditionLicenciementRemplie: true,
      conditionMotifNonGraveRemplie: true,
      conditionPreavis30semRemplie: true,
      conditionOffreDansLes15joursRemplie: true,
      conditionDureeProgrammeRemplie: true,
      conditionFormeOffreRemplie: true,
      indemniteForfaitaireTravailleur: 0,
      synthese: 'Offre d\'outplacement conforme aux exigences légales.',
      baseJuridique: 'Loi 05/09/2001 art. 11 + AR 21/10/2007',
      avertissement: '',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        OutplacementBeGeneral30semSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(OutplacementBeGeneral30semSectionComponent);
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
    expect(component.licenciementEffectif()).toBe(true);
    expect(component.motifGrave()).toBe(false);
    expect(component.semainesPreavisOuIndemnite()).toBe(32);
    expect(component.dateFinContrat()).toBe('2026-04-30');
    expect(component.offreNotifiee()).toBe(true);
    expect(component.dateNotificationOffre()).toBe('2026-05-02');
    expect(component.heuresProgramme()).toBe(60);
    expect(component.dureeProgrammeMois()).toBe(12);
    expect(component.offreEcrite()).toBe(true);
    expect(component.offreIndividuelle()).toBe(true);
    expect(component.contenuMinimumRespecte()).toBe(true);
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

  function setValidFormDefaults(): void {
    component.licenciementEffectif.set(true);
    component.motifGrave.set(false);
    component.semainesPreavisOuIndemnite.set(32);
    component.dateFinContrat.set('2026-04-30');
    component.offreNotifiee.set(true);
    component.heuresProgramme.set(60);
    component.dureeProgrammeMois.set(12);
    component.offreEcrite.set(true);
    component.offreIndividuelle.set(true);
    component.contenuMinimumRespecte.set(true);
  }

  it('formValid() : false sans champs', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true avec 10 champs requis remplis', () => {
    setValidFormDefaults();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans licenciementEffectif', () => {
    setValidFormDefaults();
    component.licenciementEffectif.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dateFinContrat', () => {
    setValidFormDefaults();
    component.dateFinContrat.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans contenuMinimumRespecte', () => {
    setValidFormDefaults();
    component.contenuMinimumRespecte.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : dateNotificationOffre optionnelle — sans valeur le form reste valide', () => {
    setValidFormDefaults();
    component.dateNotificationOffre.set(null);
    expect(component.formValid()).toBe(true);
  });

  it('onOffreNotifieeChange(false) → reset dateNotificationOffre', () => {
    component.dateNotificationOffre.set('2026-05-02');
    component.onOffreNotifieeChange(false);
    expect(component.offreNotifiee()).toBe(false);
    expect(component.dateNotificationOffre()).toBeNull();
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  it('calculate() → POST + dashboardRefresh + snack', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    setValidFormDefaults();
    component.dateNotificationOffre.set('2026-05-02');
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.licenciementEffectif).toBe(true);
    expect(post.request.body.motifGrave).toBe(false);
    expect(post.request.body.semainesPreavisOuIndemnite).toBe(32);
    expect(post.request.body.dateFinContrat).toBe('2026-04-30');
    expect(post.request.body.offreNotifiee).toBe(true);
    expect(post.request.body.dateNotificationOffre).toBe('2026-05-02');
    expect(post.request.body.heuresProgramme).toBe(60);
    expect(post.request.body.dureeProgrammeMois).toBe(12);
    expect(post.request.body.offreEcrite).toBe(true);
    expect(post.request.body.offreIndividuelle).toBe(true);
    expect(post.request.body.contenuMinimumRespecte).toBe(true);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Outplacement BE analysé', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 → snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    setValidFormDefaults();
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
    setValidFormDefaults();
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
    setValidFormDefaults();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({}, { status: 404, statusText: 'Not Found' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Dossier introuvable', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : FRANCE → snack indispo + pas de POST', () => {
    component.workspaceCountry = 'FRANCE';
    setValidFormDefaults();
    component.calculate();
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Outil indisponible pour ce workspace', 'Fermer', expect.anything(),
    );
    httpMock.expectNone((r) => r.url === BASE_URL && r.method === 'POST');
  });

  // ============================================================
  // Verdict — label / classe / icône (7 cas)
  // ============================================================

  it('verdict CONFORME → label, verdict-ok, verified', () => {
    component.result.set(response({ verdict: 'CONFORME' }));
    expect(component.verdictLabel()).toContain('conforme');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict NON_DU_DEMISSION → label, verdict-info, logout', () => {
    component.result.set(response({
      verdict: 'NON_DU_DEMISSION',
      conforme: false,
      conditionLicenciementRemplie: false,
    }));
    expect(component.verdictLabel()).toContain('démission');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('logout');
  });

  it('verdict NON_DU_MOTIF_GRAVE → label, verdict-info, gpp_bad', () => {
    component.result.set(response({
      verdict: 'NON_DU_MOTIF_GRAVE',
      conforme: false,
      conditionMotifNonGraveRemplie: false,
    }));
    expect(component.verdictLabel()).toContain('motif grave');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('gpp_bad');
  });

  it('verdict NON_DU_PREAVIS_INSUFFISANT → label, verdict-info, timer_off', () => {
    component.result.set(response({
      verdict: 'NON_DU_PREAVIS_INSUFFISANT',
      conforme: false,
      conditionPreavis30semRemplie: false,
    }));
    expect(component.verdictLabel()).toContain('30 semaines');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('timer_off');
  });

  it('verdict NON_CONFORME_OFFRE_TARDIVE → label, verdict-critical, schedule', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_OFFRE_TARDIVE',
      conforme: false,
      conditionOffreDansLes15joursRemplie: false,
      indemniteForfaitaireTravailleur: 1500,
    }));
    expect(component.verdictLabel()).toContain('tardive');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('schedule');
  });

  it('verdict NON_CONFORME_DUREE_INSUFFISANTE → label, verdict-critical, trending_down', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_DUREE_INSUFFISANTE',
      conforme: false,
      conditionDureeProgrammeRemplie: false,
    }));
    expect(component.verdictLabel()).toContain('insuffisante');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('trending_down');
  });

  it('verdict NON_CONFORME_FORME → label, verdict-critical, rule', () => {
    component.result.set(response({
      verdict: 'NON_CONFORME_FORME',
      conforme: false,
      conditionFormeOffreRemplie: false,
    }));
    expect(component.verdictLabel()).toContain('Forme');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('rule');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('gavel');
  });

  // ============================================================
  // Badge dans header (7 cas)
  // ============================================================

  it('badge CONFORME = vert "CONFORME"', () => {
    expect(component.verdictBadge('CONFORME')).toBe('CONFORME');
    expect(component.verdictBadgeClass('CONFORME')).toBe('badge-ok');
  });

  it('badge NON_DU_DEMISSION = gris info', () => {
    expect(component.verdictBadge('NON_DU_DEMISSION')).toContain('DÉMISSION');
    expect(component.verdictBadgeClass('NON_DU_DEMISSION')).toBe('badge-info');
  });

  it('badge NON_DU_MOTIF_GRAVE = gris info', () => {
    expect(component.verdictBadge('NON_DU_MOTIF_GRAVE')).toContain('MOTIF GRAVE');
    expect(component.verdictBadgeClass('NON_DU_MOTIF_GRAVE')).toBe('badge-info');
  });

  it('badge NON_DU_PREAVIS_INSUFFISANT = gris info', () => {
    expect(component.verdictBadge('NON_DU_PREAVIS_INSUFFISANT')).toContain('30 SEM');
    expect(component.verdictBadgeClass('NON_DU_PREAVIS_INSUFFISANT')).toBe('badge-info');
  });

  it('badge NON_CONFORME_OFFRE_TARDIVE = rouge', () => {
    expect(component.verdictBadge('NON_CONFORME_OFFRE_TARDIVE')).toBe('OFFRE TARDIVE');
    expect(component.verdictBadgeClass('NON_CONFORME_OFFRE_TARDIVE')).toBe('badge-critical');
  });

  it('badge NON_CONFORME_DUREE_INSUFFISANTE = rouge', () => {
    expect(component.verdictBadge('NON_CONFORME_DUREE_INSUFFISANTE')).toBe('DURÉE INSUFFISANTE');
    expect(component.verdictBadgeClass('NON_CONFORME_DUREE_INSUFFISANTE')).toBe('badge-critical');
  });

  it('badge NON_CONFORME_FORME = rouge', () => {
    expect(component.verdictBadge('NON_CONFORME_FORME')).toBe('FORME NON CONFORME');
    expect(component.verdictBadgeClass('NON_CONFORME_FORME')).toBe('badge-critical');
  });

  it('CONFORME → badge vert dans le DOM', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({ verdict: 'CONFORME' }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('CONFORME');
  });

  it('NON_CONFORME_OFFRE_TARDIVE → badge rouge dans le DOM + bannière indemnité', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'NON_CONFORME_OFFRE_TARDIVE',
      conforme: false,
      conditionOffreDansLes15joursRemplie: false,
      indemniteForfaitaireTravailleur: 1500,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('TARDIVE');
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('1500');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non vide → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'CONFORME',
      avertissement: 'Vérifier les annexes AR ONEM en vigueur',
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
      synthese: 'Offre d\'outplacement conforme aux exigences légales.',
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
    expect(OutplacementBeGeneral30semSectionComponent.getPrefillCount({})).toBe(0);
    expect(OutplacementBeGeneral30semSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateRuptureContrat: '2026-04-30',
        motifRupture: 'licenciement',
        salaireBrutMensuel: 4200,
        anneesCarriereSalarie: 12,
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

  it('toolId pour jurisprudence = outplacement-be-general-30sem', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('outplacement-be-general-30sem');
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
    component.onLicenciementEffectifChange(true);
    expect(component.licenciementEffectif()).toBe(true);

    component.onMotifGraveChange(false);
    expect(component.motifGrave()).toBe(false);

    component.onSemainesPreavisOuIndemniteChange(32);
    expect(component.semainesPreavisOuIndemnite()).toBe(32);

    component.onDateFinContratChange('2026-04-30');
    expect(component.dateFinContrat()).toBe('2026-04-30');

    component.onOffreNotifieeChange(true);
    expect(component.offreNotifiee()).toBe(true);

    component.onDateNotificationOffreChange('2026-05-02');
    expect(component.dateNotificationOffre()).toBe('2026-05-02');

    component.onHeuresProgrammeChange(60);
    expect(component.heuresProgramme()).toBe(60);

    component.onDureeProgrammeMoisChange(12);
    expect(component.dureeProgrammeMois()).toBe(12);

    component.onOffreEcriteChange(true);
    expect(component.offreEcrite()).toBe(true);

    component.onOffreIndividuelleChange(true);
    expect(component.offreIndividuelle()).toBe(true);

    component.onContenuMinimumRespecteChange(true);
    expect(component.contenuMinimumRespecte()).toBe(true);
  });

  // ============================================================
  // Static metadata F-177
  // ============================================================

  it('TOOL_LABEL et TOOL_ICON statiques exposés', () => {
    expect(OutplacementBeGeneral30semSectionComponent.TOOL_LABEL).toBe('OUTPLACEMENT BE GÉNÉRAL 30 SEM');
    expect(OutplacementBeGeneral30semSectionComponent.TOOL_ICON).toBe('work_history');
  });
});
