import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  TransfertEntrepriseCct32bisSectionComponent,
} from './transfert-entreprise-cct-32bis-section.component';
import {
  TransfertEntrepriseCct32bisResponse,
} from '../../core/models/transfert-entreprise-cct-32bis.model';

describe('TransfertEntrepriseCct32bisSectionComponent', () => {
  let component: TransfertEntrepriseCct32bisSectionComponent;
  let fixture: ComponentFixture<TransfertEntrepriseCct32bisSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/transfert-entreprise-cct-32bis';

  /**
   * Construit une réponse backend par défaut — verdict TRANSFERT_CONFORME
   * (vente fonds, identité préservée, info-consult respectée, droits maintenus).
   */
  function response(
    overrides: Partial<TransfertEntrepriseCct32bisResponse> = {},
  ): TransfertEntrepriseCct32bisResponse {
    return {
      caseFileId: 'case-1',
      dateTransfert: '2026-04-15',
      typeOperation: 'VENTE_FONDS_COMMERCE',
      identiteEconomique: 'PRESERVEE',
      infoConsultPrealableRespectee: true,
      conseilEntrepriseConsulte: true,
      maintienContratsAutomatique: true,
      maintienAncienneteRespecte: true,
      maintienRemunerationRespecte: true,
      conventionsCollectivesMaintenues: true,
      verdict: 'TRANSFERT_CONFORME',
      eligibleCct32bis: true,
      procedureConforme: true,
      droitsMaintenus: true,
      dateFinResponsabiliteSolidaire: '2027-04-15',
      raison: 'TRANSFERT_QUALIFIANT_CONFORME',
      synthese: 'Vente du fonds qualifiante CCT 32bis — info-consult respectée — droits maintenus.',
      baseJuridique: 'CCT n° 32bis 07/06/1985 + Loi 17/03/1965 + Directive 2001/23/CE',
      avertissement: '',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        TransfertEntrepriseCct32bisSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(TransfertEntrepriseCct32bisSectionComponent);
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
    expect(component.result()!.verdict).toBe('TRANSFERT_CONFORME');
    expect(component.showForm()).toBe(false);
    expect(component.dateTransfert()).toBe('2026-04-15');
    expect(component.typeOperation()).toBe('VENTE_FONDS_COMMERCE');
    expect(component.identiteEconomique()).toBe('PRESERVEE');
    expect(component.infoConsultPrealableRespectee()).toBe(true);
    expect(component.conseilEntrepriseConsulte()).toBe(true);
    expect(component.maintienContratsAutomatique()).toBe(true);
    expect(component.maintienAncienneteRespecte()).toBe(true);
    expect(component.maintienRemunerationRespecte()).toBe(true);
    expect(component.conventionsCollectivesMaintenues()).toBe(true);
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

  it('formValid() : true avec 9 champs requis remplis', () => {
    component.dateTransfert.set('2026-04-15');
    component.typeOperation.set('VENTE_FONDS_COMMERCE');
    component.identiteEconomique.set('PRESERVEE');
    component.infoConsultPrealableRespectee.set(true);
    component.conseilEntrepriseConsulte.set(true);
    component.maintienContratsAutomatique.set(true);
    component.maintienAncienneteRespecte.set(true);
    component.maintienRemunerationRespecte.set(true);
    component.conventionsCollectivesMaintenues.set(true);
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans dateTransfert', () => {
    component.typeOperation.set('VENTE_FONDS_COMMERCE');
    component.identiteEconomique.set('PRESERVEE');
    component.infoConsultPrealableRespectee.set(true);
    component.conseilEntrepriseConsulte.set(true);
    component.maintienContratsAutomatique.set(true);
    component.maintienAncienneteRespecte.set(true);
    component.maintienRemunerationRespecte.set(true);
    component.conventionsCollectivesMaintenues.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans typeOperation', () => {
    component.dateTransfert.set('2026-04-15');
    component.identiteEconomique.set('PRESERVEE');
    component.infoConsultPrealableRespectee.set(true);
    component.conseilEntrepriseConsulte.set(true);
    component.maintienContratsAutomatique.set(true);
    component.maintienAncienneteRespecte.set(true);
    component.maintienRemunerationRespecte.set(true);
    component.conventionsCollectivesMaintenues.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans identiteEconomique', () => {
    component.dateTransfert.set('2026-04-15');
    component.typeOperation.set('VENTE_FONDS_COMMERCE');
    component.infoConsultPrealableRespectee.set(true);
    component.conseilEntrepriseConsulte.set(true);
    component.maintienContratsAutomatique.set(true);
    component.maintienAncienneteRespecte.set(true);
    component.maintienRemunerationRespecte.set(true);
    component.conventionsCollectivesMaintenues.set(true);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  function fillForm() {
    component.dateTransfert.set('2026-04-15');
    component.typeOperation.set('VENTE_FONDS_COMMERCE');
    component.identiteEconomique.set('PRESERVEE');
    component.infoConsultPrealableRespectee.set(true);
    component.conseilEntrepriseConsulte.set(true);
    component.maintienContratsAutomatique.set(true);
    component.maintienAncienneteRespecte.set(true);
    component.maintienRemunerationRespecte.set(true);
    component.conventionsCollectivesMaintenues.set(true);
  }

  it('calculate() → POST + snack succès', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.dateTransfert).toBe('2026-04-15');
    expect(post.request.body.typeOperation).toBe('VENTE_FONDS_COMMERCE');
    expect(post.request.body.identiteEconomique).toBe('PRESERVEE');
    expect(post.request.body.infoConsultPrealableRespectee).toBe(true);
    expect(post.request.body.conseilEntrepriseConsulte).toBe(true);
    expect(post.request.body.maintienContratsAutomatique).toBe(true);
    expect(post.request.body.maintienAncienneteRespecte).toBe(true);
    expect(post.request.body.maintienRemunerationRespecte).toBe(true);
    expect(post.request.body.conventionsCollectivesMaintenues).toBe(true);
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Transfert CCT 32bis analysé', 'OK', expect.anything(),
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

  it('verdict TRANSFERT_CONFORME → label, verdict-ok, verified', () => {
    component.result.set(response({ verdict: 'TRANSFERT_CONFORME' }));
    expect(component.verdictLabel()).toContain('Transfert conforme');
    expect(component.verdictClass()).toBe('verdict-ok');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict TRANSFERT_NON_CONFORME_INFO_CONSULT → label, verdict-warn, warning_amber', () => {
    component.result.set(response({
      verdict: 'TRANSFERT_NON_CONFORME_INFO_CONSULT',
      infoConsultPrealableRespectee: false,
      procedureConforme: false,
    }));
    expect(component.verdictLabel()).toContain('info-consultation');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('warning_amber');
  });

  it('verdict INELIGIBLE_TYPE_OPERATION → label, verdict-critical, block', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_TYPE_OPERATION',
      typeOperation: 'CESSION_ACTIONS',
      eligibleCct32bis: false,
      droitsMaintenus: false,
    }));
    expect(component.verdictLabel()).toContain('non qualifiante');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('block');
  });

  it('verdict INELIGIBLE_PAS_ENTITE_ECONOMIQUE → label, verdict-critical, block', () => {
    component.result.set(response({
      verdict: 'INELIGIBLE_PAS_ENTITE_ECONOMIQUE',
      identiteEconomique: 'NON_PRESERVEE',
      eligibleCct32bis: false,
      droitsMaintenus: false,
    }));
    expect(component.verdictLabel()).toContain('pas d\'entité économique');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('block');
  });

  it('verdict A_ANALYSER → label, verdict-neutral, help', () => {
    component.result.set(response({
      verdict: 'A_ANALYSER',
      identiteEconomique: 'A_ANALYSER',
      eligibleCct32bis: false,
    }));
    expect(component.verdictLabel()).toContain('À analyser');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help');
  });

  it('result null → labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('gavel');
  });

  // ============================================================
  // Badge dans header (5 cas)
  // ============================================================

  it('badge TRANSFERT_CONFORME = vert "CONFORME"', () => {
    expect(component.verdictBadge('TRANSFERT_CONFORME')).toBe('CONFORME');
    expect(component.verdictBadgeClass('TRANSFERT_CONFORME')).toBe('badge-ok');
  });

  it('badge TRANSFERT_NON_CONFORME_INFO_CONSULT = ambre "INFO-CONSULT KO"', () => {
    expect(component.verdictBadge('TRANSFERT_NON_CONFORME_INFO_CONSULT')).toBe('INFO-CONSULT KO');
    expect(component.verdictBadgeClass('TRANSFERT_NON_CONFORME_INFO_CONSULT')).toBe('badge-warn');
  });

  it('badge INELIGIBLE_TYPE_OPERATION = rouge "TYPE NON QUALIFIANT"', () => {
    expect(component.verdictBadge('INELIGIBLE_TYPE_OPERATION')).toBe('TYPE NON QUALIFIANT');
    expect(component.verdictBadgeClass('INELIGIBLE_TYPE_OPERATION')).toBe('badge-critical');
  });

  it('badge INELIGIBLE_PAS_ENTITE_ECONOMIQUE = rouge "PAS D\'ENTITÉ"', () => {
    expect(component.verdictBadge('INELIGIBLE_PAS_ENTITE_ECONOMIQUE')).toBe('PAS D\'ENTITÉ');
    expect(component.verdictBadgeClass('INELIGIBLE_PAS_ENTITE_ECONOMIQUE')).toBe('badge-critical');
  });

  it('badge A_ANALYSER = neutre "À ANALYSER"', () => {
    expect(component.verdictBadge('A_ANALYSER')).toBe('À ANALYSER');
    expect(component.verdictBadgeClass('A_ANALYSER')).toBe('badge-neutral');
  });

  it('TRANSFERT_CONFORME → badge vert dans le DOM + droits maintenus + responsabilité solidaire', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-ok');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('CONFORME');
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('Droits maintenus');
    expect(text).toContain('2027-04-15');
    expect(text).toContain('Responsabilité solidaire');
  });

  it('INELIGIBLE_TYPE_OPERATION → badge rouge dans le DOM', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'INELIGIBLE_TYPE_OPERATION',
      typeOperation: 'CESSION_ACTIONS',
      eligibleCct32bis: false,
      droitsMaintenus: false,
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-critical');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('TYPE');
  });

  it('TRANSFERT_NON_CONFORME_INFO_CONSULT → bannière info-consult KO + responsabilité solidaire', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'TRANSFERT_NON_CONFORME_INFO_CONSULT',
      infoConsultPrealableRespectee: false,
      procedureConforme: false,
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('Info-consultation préalable non respectée');
  });

  // ============================================================
  // Libellés
  // ============================================================

  it('libellés typeOperation : 9 cas + null', () => {
    expect(component.typeOperationLabel('VENTE_FONDS_COMMERCE')).toContain('Vente');
    expect(component.typeOperationLabel('FUSION_ABSORPTION')).toContain('Fusion');
    expect(component.typeOperationLabel('SCISSION')).toContain('Scission');
    expect(component.typeOperationLabel('APPORT_UNIVERSALITE')).toContain('Apport');
    expect(component.typeOperationLabel('DEMEMBREMENT')).toContain('Démembrement');
    expect(component.typeOperationLabel('FAILLITE')).toContain('Faillite');
    expect(component.typeOperationLabel('LIQUIDATION_JUDICIAIRE')).toContain('Liquidation');
    expect(component.typeOperationLabel('CESSION_ACTIONS')).toContain('Cession');
    expect(component.typeOperationLabel('TRANSFERT_INTRA_GROUPE_SANS_CESSION')).toContain('intra-groupe');
    expect(component.typeOperationLabel(null)).toBe('—');
  });

  it('libellés identiteEconomique : 3 cas + null', () => {
    expect(component.identiteEconomiqueLabel('PRESERVEE')).toContain('Préservée');
    expect(component.identiteEconomiqueLabel('NON_PRESERVEE')).toContain('Non préservée');
    expect(component.identiteEconomiqueLabel('A_ANALYSER')).toContain('analyser');
    expect(component.identiteEconomiqueLabel(null)).toBe('—');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non vide → bannière ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      verdict: 'TRANSFERT_CONFORME',
      avertissement: 'Vérifier le maintien CCT sectorielle en vigueur',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('CCT sectorielle');
  });

  // ============================================================
  // Synthèse
  // ============================================================

  it('synthèse affichée si présente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Vente fonds qualifiante — droits maintenus.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: any) => a.textContent).join(' ');
    expect(text).toContain('qualifiante');
  });

  // ============================================================
  // F-177 SF-177-12 — parité getPrefillCount static
  // ============================================================

  it('static getPrefillCount → 0 (V1)', () => {
    expect(TransfertEntrepriseCct32bisSectionComponent.getPrefillCount({})).toBe(0);
    expect(TransfertEntrepriseCct32bisSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateNaissanceSalarie: '1976-04-10',
        dateRuptureContrat: '2026-04-10',
        motifRupture: 'transfert',
        anneesCarriereSalarie: 20,
        salaireBrutMensuel: 3200,
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

  it('toolId pour jurisprudence = transfert-entreprise-cct-32bis', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('transfert-entreprise-cct-32bis');
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
    component.onDateTransfertChange('2026-04-15');
    expect(component.dateTransfert()).toBe('2026-04-15');

    component.onTypeOperationChange('FUSION_ABSORPTION');
    expect(component.typeOperation()).toBe('FUSION_ABSORPTION');

    component.onIdentiteEconomiqueChange('PRESERVEE');
    expect(component.identiteEconomique()).toBe('PRESERVEE');

    component.onInfoConsultPrealableRespecteeChange(true);
    expect(component.infoConsultPrealableRespectee()).toBe(true);

    component.onConseilEntrepriseConsulteChange(false);
    expect(component.conseilEntrepriseConsulte()).toBe(false);

    component.onMaintienContratsAutomatiqueChange(true);
    expect(component.maintienContratsAutomatique()).toBe(true);

    component.onMaintienAncienneteRespecteChange(true);
    expect(component.maintienAncienneteRespecte()).toBe(true);

    component.onMaintienRemunerationRespecteChange(false);
    expect(component.maintienRemunerationRespecte()).toBe(false);

    component.onConventionsCollectivesMaintenuesChange(true);
    expect(component.conventionsCollectivesMaintenues()).toBe(true);
  });

  // ============================================================
  // Static metadata F-177
  // ============================================================

  it('TOOL_LABEL et TOOL_ICON statiques exposés', () => {
    expect(TransfertEntrepriseCct32bisSectionComponent.TOOL_LABEL).toBe('TRANSFERT CCT 32BIS (BE)');
    expect(TransfertEntrepriseCct32bisSectionComponent.TOOL_ICON).toBe('swap_horiz');
  });
});
