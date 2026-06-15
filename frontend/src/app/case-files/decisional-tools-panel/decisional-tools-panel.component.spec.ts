import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { By } from '@angular/platform-browser';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DecisionToolsPanelComponent } from './decisional-tools-panel.component';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { DecisionToolModalService } from './decision-tool-modal/decision-tool-modal.service';
import { AncienneteSectionComponent } from '../anciennete-section/anciennete-section.component';

describe('DecisionToolsPanelComponent', () => {
  let component: DecisionToolsPanelComponent;
  let fixture: ComponentFixture<DecisionToolsPanelComponent>;
  let httpMock: HttpTestingController;
  let snackBar: jest.Mocked<MatSnackBar>;

  const CASE_FILE_ID = '55555555-5555-5555-5555-555555555555';
  const API_URL = `/api/v1/case-files/${CASE_FILE_ID}/decision-tools-visibility`;

  beforeEach(async () => {
    snackBar = { open: jest.fn() } as unknown as jest.Mocked<MatSnackBar>;

    await TestBed.configureTestingModule({
      imports: [DecisionToolsPanelComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        { provide: MatSnackBar, useValue: snackBar },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DecisionToolsPanelComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
  });

  // F-192 SF-192-02 — le panel charge aussi `retained-pistes-alignment` au mount.
  // Les tests qui ne s'en occupent pas directement laissent l'appel ouvert :
  // on le flush silencieusement via afterEach pour préserver `httpMock.verify()`.
  afterEach(() => {
    httpMock.match(r => r.url.endsWith('/retained-pistes-alignment'))
      .forEach(r => {
        // takeUntilDestroyed peut canceller la requête avant le flush ; on
        // l'ignore (pas de leak réel, les requêtes cancellées ne polluent
        // pas verify()).
        try { r.flush([], { status: 200, statusText: 'OK' }); } catch { /* cancelled */ }
      });
    // F-194 SF-194-02 — idem pour pieces-manquantes-alignment.
    httpMock.match(r => r.url.endsWith('/pieces-manquantes-alignment'))
      .forEach(r => {
        try { r.flush([], { status: 200, statusText: 'OK' }); } catch { /* cancelled */ }
      });
    // F-195 SF-195-02 — idem pour risques-alignment.
    httpMock.match(r => r.url.endsWith('/risques-alignment'))
      .forEach(r => {
        try { r.flush([], { status: 200, statusText: 'OK' }); } catch { /* cancelled */ }
      });
    // F-228 SF-228-01 — idem pour ai-questions-alignment (charge également
    // ouverte par DecisionToolAlignmentsLoader désormais).
    httpMock.match(r => r.url.endsWith('/ai-questions-alignment'))
      .forEach(r => {
        try { r.flush([], { status: 200, statusText: 'OK' }); } catch { /* cancelled */ }
      });
    // F-292 (fix) — le panel charge aussi le dashboard (outils calculés) au
    // mount + sur refresh$ ; flush silencieux pour préserver verify().
    httpMock.match(r => r.url.endsWith('/dashboard'))
      .forEach(r => {
        try {
          r.flush(
            { caseFileId: 'cf', legalDomain: 'TRAVAIL', riskScore: null, riskLevel: null, tiles: [] },
            { status: 200, statusText: 'OK' },
          );
        } catch { /* cancelled */ }
      });
    httpMock.verify();
  });

  it('renders both always-on and contextual groups, keeps catalog chips', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('GET');
    req.flush({
      alwaysOn: ['F-DT-07-anciennete-conges-prime', 'F-DT-04-fiche-prudhomale'],
      contextual: ['F-DT-08-licenciement-validity'],
      catalog: ['F-DT-10-rupture-conv-validity', 'F-132-rupture-conv-indemnite'],
    });

    expect(component.resolvedAlwaysOn().map((x) => x.toolId))
      .toEqual(['F-DT-07-anciennete-conges-prime', 'F-DT-04-fiche-prudhomale']);
    expect(component.resolvedContextual().map((x) => x.toolId))
      .toEqual(['F-DT-08-licenciement-validity']);
    expect(component.visibility()!.catalog).toHaveLength(2);
    expect(component.isEmpty()).toBe(false);
  });

  it('shows empty state when alwaysOn and contextual are both empty', () => {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({ alwaysOn: [], contextual: [], catalog: [] });

    expect(component.isEmpty()).toBe(true);
  });

  // ── F-244 SF-244-02 — @Output prefillTotalChange (badge agrégé onglet) ──

  it('SF-244-02: prefillTotalChange émet 0 quand aucun outil visible', () => {
    const emitted: number[] = [];
    component.prefillTotalChange.subscribe(n => emitted.push(n));
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({ alwaysOn: [], contextual: [], catalog: [] });

    expect(emitted.length).toBeGreaterThan(0);
    expect(emitted[emitted.length - 1]).toBe(0);
  });

  it('SF-244-02: prefillTotalChange émet la somme des prefillCountFor des outils résolus', () => {
    const emitted: number[] = [];
    component.prefillTotalChange.subscribe(n => emitted.push(n));
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({
      alwaysOn: ['F-DT-07-anciennete-conges-prime', 'F-DT-04-fiche-prudhomale'],
      contextual: ['F-DT-08-licenciement-validity'],
      catalog: [],
    });

    const resolved = [...component.resolvedAlwaysOn(), ...component.resolvedContextual()];
    const expectedTotal = resolved.reduce(
      (sum, item) => sum + (component.prefillCountFor(item.toolId) ?? 0), 0);
    expect(emitted[emitted.length - 1]).toBe(expectedTotal);
  });

  it('SF-244-02: prefillTotalChange ré-émet après un changement de synthesis', () => {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({
      alwaysOn: ['F-DT-07-anciennete-conges-prime'],
      contextual: [],
      catalog: [],
    });
    const emitSpy = jest.spyOn(component.prefillTotalChange, 'emit');
    component.synthesis = { travailExtractedData: { ancienneteAnnees: 5 } };
    component.ngOnChanges({
      synthesis: { previousValue: null, currentValue: component.synthesis, firstChange: false, isFirstChange: () => false },
    });
    expect(emitSpy).toHaveBeenCalled();
  });

  it('skips unknown tool_id with a console warning', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({
      alwaysOn: ['F-DT-07-anciennete-conges-prime', 'F-XX-999-unknown'],
      contextual: [],
      catalog: [],
    });

    const resolved = component.resolvedAlwaysOn().map((x) => x.toolId);
    expect(resolved).toEqual(['F-DT-07-anciennete-conges-prime']);
    expect(warnSpy).toHaveBeenCalledWith(expect.stringContaining('F-XX-999-unknown'));
    warnSpy.mockRestore();
  });

  it('shows snackbar on HTTP error and leaves lists empty', () => {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush('err', { status: 500, statusText: 'Server Error' });

    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Impossible de charger'),
      'Fermer',
      expect.any(Object)
    );
    expect(component.visibility()).toEqual({ alwaysOn: [], contextual: [], catalog: [] });
  });

  it('forwards tool-specific inputs for F-DT-08 licenciement (workspaceCountry + aiData + procedureChecks)', () => {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({
      alwaysOn: ['F-DT-08-licenciement-validity'],
      contextual: [],
      catalog: [],
    });

    component.workspaceCountry = 'BELGIQUE';
    component.synthesis = {
      licenciementValidityDetection: { foo: 'bar' },
      piecesManquantesDetails: { any: 'thing' },
    };
    component.procedureChecks = [{ id: 'c1' }];
    component.aiQuestions = [{ id: 'q1' }];

    const entry = component.resolveEntry('F-DT-08-licenciement-validity')!;
    const inputs = component.componentInputsFor(entry);

    expect(inputs).toEqual({
      caseFileId: CASE_FILE_ID,
      workspaceCountry: 'BELGIQUE',
      aiData: { foo: 'bar' },
      procedureChecks: [{ id: 'c1' }],
      aiQuestions: [{ id: 'q1' }],
      piecesManquantes: { any: 'thing' },
      // F-163 SF-163-02a/b/c/d — standaloneMode propagé par défaut false depuis le panel.
      standaloneMode: false,
    });
  });

  it('forwards F-IM-05 inputs including triggerEvents and piecesManquantes', () => {
    component.synthesis = {
      immigrationExtractedData: { inferredChecklistType: 'X' },
      immigrationTriggerEvents: [{ e: 1 }],
      piecesManquantesDetails: { p: 1 },
    };
    component.procedureChecks = [];
    component.aiQuestions = [];

    const entry = component.resolveEntry('F-IM-05-arbre-decisionnel-titre')!;
    const inputs = component.componentInputsFor(entry);

    expect(inputs['aiData']).toEqual({ inferredChecklistType: 'X' });
    expect(inputs['triggerEvents']).toEqual([{ e: 1 }]);
    expect(inputs['piecesManquantes']).toEqual({ p: 1 });
  });

  // F-194 SF-194-02 — F-DT-04 reçoit `piecesObtenues` extrait de l'alignement
  it('F-194 SF-194-02 — F-DT-04 receives piecesObtenues from piecesAlignment signal', () => {
    component.piecesAlignment.set([
      {
        pieceLibelle: 'Contrat de travail',
        statut: 'OBTENUE',
        toolIdsCibles: ['F-DT-04-fiche-prudhomale'],
        destinataire: null,
        raisonNonApp: null,
      },
      {
        pieceLibelle: 'Lettre de licenciement',
        statut: 'A_DEMANDER',
        toolIdsCibles: ['F-DT-04-fiche-prudhomale'],
        destinataire: 'Client',
        raisonNonApp: null,
      },
      {
        pieceLibelle: 'Acte de mariage',
        statut: 'OBTENUE',
        toolIdsCibles: ['F-FA-07-checklist-divorce'],
        destinataire: null,
        raisonNonApp: null,
      },
    ]);

    const entry = component.resolveEntry('F-DT-04-fiche-prudhomale')!;
    const inputs = component.componentInputsFor(entry);

    // Seules les pièces statut OBTENUE pour F-DT-04 doivent remonter.
    expect(inputs['piecesObtenues']).toEqual(['Contrat de travail']);
  });

  // F-194 SF-194-02 — piecesBadgeFor calcule le verdict pour la card panel
  it('F-194 SF-194-02 — piecesBadgeFor computes badge from filtered alignment', () => {
    component.piecesAlignment.set([
      {
        pieceLibelle: 'Pièce 1',
        statut: 'A_DEMANDER',
        toolIdsCibles: ['F-DT-04-fiche-prudhomale'],
      },
      {
        pieceLibelle: 'Pièce 2',
        statut: 'OBTENUE',
        toolIdsCibles: ['F-DT-04-fiche-prudhomale'],
      },
    ]);
    const badge = component.piecesBadgeFor('F-DT-04-fiche-prudhomale');
    expect(badge).not.toBeNull();
    expect(badge!.kind).toBe('missing');
    expect(badge!.counts.aDemander).toBe(1);
    expect(badge!.counts.obtenues).toBe(1);
  });

  it('F-194 SF-194-02 — piecesBadgeFor returns null when none mapped', () => {
    component.piecesAlignment.set([]);
    expect(component.piecesBadgeFor('F-DT-04-fiche-prudhomale')).toBeNull();
  });

  // F-177 SF-177-12 — la card du panel doit afficher le badge `auto_awesome`
  // dès que le composant outil expose un static `getPrefillCount` qui renvoie > 0.
  it('prefillCountFor returns count for instrumented Immigration tool (Chen 5 case)', () => {
    component.synthesis = {
      immigrationExtractedData: {
        nationaliteUe: false,
        typeTitreSejourCode: 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE',
      },
      immigrationTriggerEvents: [{ eventCode: 'MARIAGE_RESSORTISSANT_FR' }],
      piecesManquantesDetails: [],
    };
    expect(component.prefillCountFor('F-IM-05-arbre-decisionnel-titre')).toBe(3);
  });

  it('prefillCountFor returns null for an unknown tool id (fallback safe)', () => {
    expect(component.prefillCountFor('F-XX-999-unknown')).toBeNull();
  });

  it('prefillCountFor returns null for an unknown tool_id', () => {
    // F-236 SF-236-02 Waves A+B+C : tous les outils du TOOL_REGISTRY exposent
    // désormais `getPrefillCount`. Le cas "absence" se teste donc uniquement
    // via un tool_id inexistant (résolution échoue → null).
    expect(component.prefillCountFor('F-INEXISTANT-fake-tool-xyz')).toBeNull();
  });

  it('resolves F-132-rupture-amiable-info to RuptureAmiableInfoSectionComponent', () => {
    const entry = component.resolveEntry('F-132-rupture-amiable-info');
    expect(entry).not.toBeNull();
    expect(entry!.component.name).toBe('RuptureAmiableInfoSectionComponent');
  });

  it('resolves registered tool IDs to their Angular component types', () => {
    expect(component.resolveEntry('F-DT-07-anciennete-conges-prime')).not.toBeNull();
    expect(component.resolveEntry('F-IM-05-arbre-decisionnel-titre')).not.toBeNull();
    expect(component.resolveEntry('F-132-rupture-conv-indemnite')).not.toBeNull();
    // SF-DT-27-02 : motif grave BE intégré au TOOL_REGISTRY.
    expect(component.resolveEntry('F-DT-27-motif-grave-be')).not.toBeNull();
  });

  it('SF-DT-27-02: maps F-DT-27-motif-grave-be to MotifGraveBeSectionComponent with canonical inputs', () => {
    component.caseFileId = 'cf-123';
    component.workspaceCountry = 'BELGIQUE';
    component.synthesis = {
      travailExtractedData: { dateLicenciement: '2026-04-02', salaireBrutMensuel: 3000 },
      piecesManquantesDetails: { foo: 'bar' },
    };
    component.procedureChecks = [{ p: 1 }];
    component.aiQuestions = [{ q: 1 }];

    const entry = component.resolveEntry('F-DT-27-motif-grave-be')!;
    expect(entry).not.toBeNull();
    expect(entry.component.name).toBe('MotifGraveBeSectionComponent');

    const inputs = component.componentInputsFor(entry);
    expect(inputs['caseFileId']).toBe('cf-123');
    expect(inputs['workspaceCountry']).toBe('BELGIQUE');
    expect(inputs['aiData']).toEqual({ dateLicenciement: '2026-04-02', salaireBrutMensuel: 3000 });
    expect(inputs['procedureChecks']).toEqual([{ p: 1 }]);
    expect(inputs['aiQuestions']).toEqual([{ q: 1 }]);
    expect(inputs['piecesManquantes']).toEqual({ foo: 'bar' });
  });

  it('SF-FA-17-02 : resolves F-FA-17-partage-judiciaire to PartageJudiciaireSectionComponent + bindings IA', () => {
    const entry = component.resolveEntry('F-FA-17-partage-judiciaire');
    expect(entry).not.toBeNull();
    expect(entry!.component.name).toBe('PartageJudiciaireSectionComponent');

    component.caseFileId = 'case-pj-1';
    component.workspaceCountry = 'FRANCE';
    component.synthesis = {
      familleExtractedData: {
        pvDifficultesEtablisDetected: true,
        nombreCoindivisairesDetecte: 3,
      },
      piecesManquantesDetails: [{ texte: 'PV difficultés', critereCode: 'PARTAGE_JUDICIAIRE_PV' }],
    };
    component.procedureChecks = [{ id: 'c1' } as any];
    component.aiQuestions = [{ id: 'q1' } as any];

    const inputs = component.componentInputsFor(entry!);
    expect(inputs['caseFileId']).toBe('case-pj-1');
    expect(inputs['workspaceCountry']).toBe('FRANCE');
    expect(inputs['aiData']).toEqual({
      pvDifficultesEtablisDetected: true,
      nombreCoindivisairesDetecte: 3,
    });
    expect(inputs['procedureChecks']).toEqual([{ id: 'c1' }]);
    expect(inputs['aiQuestions']).toEqual([{ id: 'q1' }]);
    expect(inputs['piecesManquantes']).toEqual([
      { texte: 'PV difficultés', critereCode: 'PARTAGE_JUDICIAIRE_PV' },
    ]);
  });

  it('SF-FA-18-02 : resolves F-FA-18-reconnaissance-paternelle to ReconnaissancePaternelleSectionComponent + bindings IA', () => {
    const entry = component.resolveEntry('F-FA-18-reconnaissance-paternelle');
    expect(entry).not.toBeNull();
    expect(entry!.component.name).toBe('ReconnaissancePaternelleSectionComponent');

    component.caseFileId = 'case-rp-1';
    component.workspaceCountry = 'FRANCE';
    component.synthesis = {
      familleExtractedData: {
        consentementLibreDuPereDetected: true,
        paterniteVraisemblableDetected: true,
        dateNaissanceEnfantDetectee: '2024-03-15',
      },
      piecesManquantesDetails: [
        { texte: 'Acte naissance', critereCode: 'RECONNAISSANCE_PATERNELLE_PROCEDURE' },
      ],
    };
    component.procedureChecks = [{ id: 'c1' } as any];
    component.aiQuestions = [{ id: 'q1' } as any];

    const inputs = component.componentInputsFor(entry!);
    expect(inputs['caseFileId']).toBe('case-rp-1');
    expect(inputs['workspaceCountry']).toBe('FRANCE');
    expect(inputs['aiData']).toEqual({
      consentementLibreDuPereDetected: true,
      paterniteVraisemblableDetected: true,
      dateNaissanceEnfantDetectee: '2024-03-15',
    });
    expect(inputs['procedureChecks']).toEqual([{ id: 'c1' }]);
    expect(inputs['aiQuestions']).toEqual([{ id: 'q1' }]);
    expect(inputs['piecesManquantes']).toEqual([
      { texte: 'Acte naissance', critereCode: 'RECONNAISSANCE_PATERNELLE_PROCEDURE' },
    ]);
  });

  it('SF-DT-21-02 : resolves F-DT-21-travail-dissimule to TravailDissimuleSectionComponent + bindings IA', () => {
    const entry = component.resolveEntry('F-DT-21-travail-dissimule');
    expect(entry).not.toBeNull();
    expect(entry!.component.name).toBe('TravailDissimuleSectionComponent');

    component.caseFileId = 'case-td-1';
    component.workspaceCountry = 'FRANCE';
    component.synthesis = {
      travailExtractedData: { salaireBrutMensuel: 2500 },
      piecesManquantesDetails: [{ texte: 'Bulletins', critereCode: 'SALAIRE_BRUT_MENSUEL' }],
    };
    component.procedureChecks = [{ id: 'c1' } as any];
    component.aiQuestions = [{ id: 'q1' } as any];

    const inputs = component.componentInputsFor(entry!);
    expect(inputs['caseFileId']).toBe('case-td-1');
    expect(inputs['workspaceCountry']).toBe('FRANCE');
    expect(inputs['aiData']).toEqual({ salaireBrutMensuel: 2500 });
    expect(inputs['procedureChecks']).toEqual([{ id: 'c1' }]);
    expect(inputs['aiQuestions']).toEqual([{ id: 'q1' }]);
    expect(inputs['piecesManquantes']).toEqual([
      { texte: 'Bulletins', critereCode: 'SALAIRE_BRUT_MENSUEL' },
    ]);
  });

  // ── F-197 SF-197-02 — Override avocat propagé via aiData ─────────────────

  it('F-197 SF-197-02 — aucun override : aiData passé tel quel (no-op)', () => {
    component.synthesis = {
      travailExtractedData: { salaireBrutMensuel: 2500, motifLicenciement: 'X' },
    };
    component.typeLitigeOverride = null;

    const entry = component.resolveEntry('F-DT-07-anciennete-conges-prime')!;
    const inputs = component.componentInputsFor(entry);

    // typeLitigeAvocatOverride absent (no-op gracieux)
    expect(inputs['aiData']).toEqual({ salaireBrutMensuel: 2500, motifLicenciement: 'X' });
  });

  it('F-197 SF-197-02 — override Travail FR : aiData.typeLitigeAvocatOverride posé', () => {
    component.synthesis = {
      travailExtractedData: { salaireBrutMensuel: 3000 },
    };
    component.typeLitigeOverride = {
      typeLitigeAvocat: 'LICENCIEMENT_ECONOMIQUE',
      typeProcedureAvocat: null,
      raison: null,
    };

    const entry = component.resolveEntry('F-DT-07-anciennete-conges-prime')!;
    const inputs = component.componentInputsFor(entry);

    // Le tool F-DT-07 ne forwarde que `caseFileId` + `aiData` (closure inputs).
    // L'override est injecté dans le `aiData` via `augmentSynthesisWithOverride()`.
    expect(inputs['aiData']).toEqual({
      salaireBrutMensuel: 3000,
      typeLitigeAvocatOverride: 'LICENCIEMENT_ECONOMIQUE',
    });
  });

  it('F-197 SF-197-02 — override Immigration : immigrationExtractedData.typeProcedureAvocatOverride posé', () => {
    component.synthesis = {
      immigrationExtractedData: { typeProcedureDetectee: 'OQTF_AVEC_DELAI' },
    };
    component.typeLitigeOverride = {
      typeLitigeAvocat: null,
      typeProcedureAvocat: 'OQTF_SANS_DELAI',
      raison: 'Détection IA erronée',
    };

    const entry = component.resolveEntry('F-IM-08-oqtf-avec-delai-fr')!;
    const inputs = component.componentInputsFor(entry);

    expect((inputs['aiData'] as any).typeProcedureAvocatOverride).toBe('OQTF_SANS_DELAI');
  });

  it('F-197 SF-197-02 — synthesis null : retour null sans crash', () => {
    component.synthesis = null;
    component.typeLitigeOverride = {
      typeLitigeAvocat: 'PRISE_ACTE_RUPTURE',
      typeProcedureAvocat: null,
      raison: null,
    };

    const entry = component.resolveEntry('F-DT-07-anciennete-conges-prime')!;
    const inputs = component.componentInputsFor(entry);
    // aiData = ctx.synthesis?.travailExtractedData → undefined si synthesis est null
    expect(inputs['aiData']).toBeUndefined();
  });

  // ── SF-169-01 — Grid 2 colonnes + groupement par thème métier ────────────

  it('SF-169-01 T-01: THEME_BY_TOOL_ID couvre tous les tool_ids du TOOL_REGISTRY', () => {
    const registryIds = Array.from(DecisionToolsPanelComponent.TOOL_REGISTRY.keys());
    const mappedIds = Array.from(DecisionToolsPanelComponent.THEME_BY_TOOL_ID.keys());
    const unmapped = registryIds.filter((id) => !mappedIds.includes(id));
    expect(unmapped).toEqual([]);
  });

  it('SF-169-01 T-02: outils classés dans le bon thème', () => {
    const map = DecisionToolsPanelComponent.THEME_BY_TOOL_ID;
    expect(map.get('F-DT-25-indemnite-preavis')).toBe('INDEMNITES');
    expect(map.get('F-DT-08-licenciement-validity')).toBe('VALIDITE');
    expect(map.get('F-DT-03-prescription-litige')).toBe('DELAIS');
    expect(map.get('F-DT-04-fiche-prudhomale')).toBe('DOCUMENTS');
    expect(map.get('F-IM-05-arbre-decisionnel-titre')).toBe('DIAGNOSTIC');
  });

  it('SF-169-01 T-03: thème sans outils est exclu de themedTools()', () => {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({
      alwaysOn: ['F-DT-25-indemnite-preavis'],
      contextual: [],
      catalog: [],
    });

    const themed = component.themedTools();
    expect(themed.get('INDEMNITES')?.length).toBe(1);
    expect(themed.has('VALIDITE')).toBe(false);
    expect(themed.has('DELAIS')).toBe(false);
    expect(themed.has('DOCUMENTS')).toBe(false);
    expect(themed.has('DIAGNOSTIC')).toBe(false);
  });

  // ── SF-177-11 — Bascule cards + modal ────────────────────────────────────

  it('SF-177-11 T-01 : cardMetadataFor lit TOOL_LABEL/TOOL_ICON statics du composant', () => {
    const entry = component.resolveEntry('F-DT-07-anciennete-conges-prime')!;
    const meta = component.cardMetadataFor(entry, 'F-DT-07-anciennete-conges-prime');
    expect(meta.label).toBe(AncienneteSectionComponent.TOOL_LABEL);
    expect(meta.icon).toBe(AncienneteSectionComponent.TOOL_ICON);
  });

  it('SF-177-11 T-02 : cardMetadataFor fallback {toolId, extension} si statics absents', () => {
    class StubWithoutStatics {}
    const fakeEntry = {
      component: StubWithoutStatics as any,
      inputs: () => ({}),
    };
    const meta = component.cardMetadataFor(fakeEntry as any, 'X-FAKE');
    expect(meta).toEqual({ label: 'X-FAKE', icon: 'extension' });
  });

  it('SF-177-11 T-03 : openTool délègue au modalService avec forceExpanded:true + meta', () => {
    const openSpy = jest
      .spyOn(TestBed.inject(DecisionToolModalService), 'open')
      .mockReturnValue({ close: jest.fn() } as any);

    component.caseFileId = 'cf-177-11';
    component.workspaceCountry = 'FRANCE';
    component.synthesis = { travailExtractedData: { foo: 'bar' } };
    component.procedureChecks = [];
    component.aiQuestions = [];

    const entry = component.resolveEntry('F-DT-07-anciennete-conges-prime')!;
    component.openTool('F-DT-07-anciennete-conges-prime', entry);

    expect(openSpy).toHaveBeenCalledTimes(1);
    const args = openSpy.mock.calls[0][0];
    expect(args.toolId).toBe('F-DT-07-anciennete-conges-prime');
    expect(args.title).toBe(AncienneteSectionComponent.TOOL_LABEL);
    expect(args.icon).toBe(AncienneteSectionComponent.TOOL_ICON);
    expect(args.component).toBe(AncienneteSectionComponent);
    expect(args.inputs['forceExpanded']).toBe(true);
    expect(args.inputs['caseFileId']).toBe('cf-177-11');
    expect(args.onSave).toBeUndefined();
  });

  it('SF-169-01 T-04: tool_id non mappé tombe sur DIAGNOSTIC + warn console', () => {
    // Note : F-IM-19-mineurs est mappé en DIAGNOSTIC, on triche en spyant le mapping
    // pour simuler un toolId présent dans TOOL_REGISTRY mais absent de THEME_BY_TOOL_ID.
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    const originalGet = DecisionToolsPanelComponent.THEME_BY_TOOL_ID.get.bind(
      DecisionToolsPanelComponent.THEME_BY_TOOL_ID
    );
    const getSpy = jest
      .spyOn(DecisionToolsPanelComponent.THEME_BY_TOOL_ID, 'get')
      .mockImplementation((id: string) =>
        id === 'F-DT-25-indemnite-preavis' ? undefined : originalGet(id)
      );

    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({
      alwaysOn: ['F-DT-25-indemnite-preavis'],
      contextual: [],
      catalog: [],
    });

    const themed = component.themedTools();
    expect(themed.get('DIAGNOSTIC')?.map((x) => x.toolId)).toEqual([
      'F-DT-25-indemnite-preavis',
    ]);
    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining('F-DT-25-indemnite-preavis')
    );

    getSpy.mockRestore();
    warnSpy.mockRestore();
  });

  // ── SF-268-02 — outils visibles empilés + Catalogue repliable/groupé ──────

  it('SF-268-02 : les outils VISIBLES restent en sections empilées (pas d\'onglet)', () => {
    fixture.detectChanges();
    // F-DT-25 → INDEMNITES ; F-DT-08 → VALIDITE ; F-DT-04 → DOCUMENTS.
    httpMock.expectOne(API_URL).flush({
      alwaysOn: ['F-DT-25-indemnite-preavis', 'F-DT-04-fiche-prudhomale'],
      contextual: ['F-DT-08-licenciement-validity'],
      catalog: [],
    });
    fixture.detectChanges();

    // Aucun onglet : les 3 thèmes non vides sont des sections visibles d'emblée.
    expect(fixture.debugElement.query(By.css('mat-tab-group'))).toBeNull();
    const sections = fixture.debugElement.queryAll(By.css('.theme-section'));
    expect(sections.length).toBe(3);
    const titles = fixture.debugElement
      .queryAll(By.css('.theme-title'))
      .map((d) => (d.nativeElement.textContent as string).trim());
    expect(titles).toEqual(['Indemnités & calculs', 'Validité & contestation', 'Documents']);
  });

  it('SF-268-02 : le Catalogue est REPLIÉ par défaut (toggle visible, aucune chip)', () => {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({
      alwaysOn: ['F-DT-25-indemnite-preavis'],
      contextual: [],
      catalog: ['F-DT-10-rupture-conv-validity', 'F-132-rupture-conv-indemnite'],
    });
    fixture.detectChanges();

    // L'en-tête repliable existe et annonce le nombre d'outils…
    const toggle = fixture.debugElement.query(By.css('.catalog-toggle'));
    expect(toggle).toBeTruthy();
    expect(component.catalogExpanded()).toBe(false);
    expect(
      (fixture.debugElement.query(By.css('.catalog-toggle__count')).nativeElement.textContent as string).trim(),
    ).toBe('2 outils disponibles');
    // …mais aucune chip n'est rendue tant que c'est replié (désencombrement).
    expect(fixture.debugElement.queryAll(By.css('.catalog-chip')).length).toBe(0);
  });

  it('SF-268-02 : déplier le Catalogue affiche les chips groupées par thème', () => {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({
      alwaysOn: ['F-DT-25-indemnite-preavis'],
      contextual: [],
      catalog: ['F-DT-10-rupture-conv-validity', 'F-132-rupture-conv-indemnite'],
    });
    fixture.detectChanges();

    fixture.debugElement.query(By.css('.catalog-toggle')).nativeElement.click();
    fixture.detectChanges();

    expect(component.catalogExpanded()).toBe(true);
    // Les chips apparaissent, réparties en groupes de thème.
    expect(fixture.debugElement.queryAll(By.css('.catalog-chip')).length).toBe(2);
    expect(fixture.debugElement.queryAll(By.css('.catalog-group')).length).toBeGreaterThan(0);
  });

  it('SF-268-02 : themedCatalog() groupe le catalogue par thème dans l\'ordre canonique', () => {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({
      alwaysOn: ['F-DT-25-indemnite-preavis'],
      contextual: [],
      catalog: ['F-DT-10-rupture-conv-validity', 'F-132-rupture-conv-indemnite'],
    });

    const groups = component.themedCatalog();
    // Tous les outils du catalogue sont répartis, sans perte ni doublon.
    const flat = groups.flatMap((g) => g.toolIds);
    expect(flat.sort()).toEqual(
      ['F-132-rupture-conv-indemnite', 'F-DT-10-rupture-conv-validity'].sort(),
    );
    // L'ordre des groupes suit THEMES_ORDERED (index croissant).
    const order = component.themesOrdered.map((t) => t.key);
    const indices = groups.map((g) => order.indexOf(g.key));
    expect(indices).toEqual([...indices].sort((a, b) => a - b));
  });

  it('SF-268-02 : le Catalogue est replié au rechargement de la visibilité', () => {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({
      alwaysOn: ['F-DT-25-indemnite-preavis'],
      contextual: [],
      catalog: ['F-DT-10-rupture-conv-validity'],
    });
    component.catalogExpanded.set(true);

    // Un nouveau chargement (ex. fin d'analyse) replie le catalogue.
    (component as any).loadVisibility(false);
    httpMock.expectOne(API_URL).flush({
      alwaysOn: ['F-DT-25-indemnite-preavis'],
      contextual: [],
      catalog: ['F-DT-10-rupture-conv-validity'],
    });
    expect(component.catalogExpanded()).toBe(false);
  });
});

describe('DecisionToolsPanelComponent — SF-IA-04-04 refresh on CaseDashboardRefreshService', () => {
  let component: DecisionToolsPanelComponent;
  let fixture: ComponentFixture<DecisionToolsPanelComponent>;
  let httpMock: HttpTestingController;
  let refreshService: CaseDashboardRefreshService;

  const CASE_FILE_ID = '55555555-5555-5555-5555-555555555555';
  const API_URL = `/api/v1/case-files/${CASE_FILE_ID}/decision-tools-visibility`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DecisionToolsPanelComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        { provide: MatSnackBar, useValue: { open: jest.fn() } },
        CaseDashboardRefreshService,
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    refreshService = TestBed.inject(CaseDashboardRefreshService);
    fixture = TestBed.createComponent(DecisionToolsPanelComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
  });

  // F-192 SF-192-02 — le panel charge aussi `retained-pistes-alignment` au mount.
  // Les tests qui ne s'en occupent pas directement laissent l'appel ouvert :
  // on le flush silencieusement via afterEach pour préserver `httpMock.verify()`.
  afterEach(() => {
    httpMock.match(r => r.url.endsWith('/retained-pistes-alignment'))
      .forEach(r => {
        // takeUntilDestroyed peut canceller la requête avant le flush ; on
        // l'ignore (pas de leak réel, les requêtes cancellées ne polluent
        // pas verify()).
        try { r.flush([], { status: 200, statusText: 'OK' }); } catch { /* cancelled */ }
      });
    // F-194 SF-194-02 — idem pour pieces-manquantes-alignment.
    httpMock.match(r => r.url.endsWith('/pieces-manquantes-alignment'))
      .forEach(r => {
        try { r.flush([], { status: 200, statusText: 'OK' }); } catch { /* cancelled */ }
      });
    // F-195 SF-195-02 — idem pour risques-alignment.
    httpMock.match(r => r.url.endsWith('/risques-alignment'))
      .forEach(r => {
        try { r.flush([], { status: 200, statusText: 'OK' }); } catch { /* cancelled */ }
      });
    // F-228 SF-228-01 — idem pour ai-questions-alignment (charge également
    // ouverte par DecisionToolAlignmentsLoader désormais).
    httpMock.match(r => r.url.endsWith('/ai-questions-alignment'))
      .forEach(r => {
        try { r.flush([], { status: 200, statusText: 'OK' }); } catch { /* cancelled */ }
      });
    // F-292 (fix) — le panel charge aussi le dashboard (outils calculés) au
    // mount + sur refresh$ ; flush silencieux pour préserver verify().
    httpMock.match(r => r.url.endsWith('/dashboard'))
      .forEach(r => {
        try {
          r.flush(
            { caseFileId: 'cf', legalDomain: 'TRAVAIL', riskScore: null, riskLevel: null, tiles: [] },
            { status: 200, statusText: 'OK' },
          );
        } catch { /* cancelled */ }
      });
    httpMock.verify();
  });

  it('reloads visibility silently when refresh service emits', fakeAsync(() => {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush({ alwaysOn: [], contextual: [], catalog: [] });
    expect(component.loading()).toBe(false);

    refreshService.triggerRefresh();
    tick(300);

    const reloadReq = httpMock.expectOne(API_URL);
    expect(component.loading()).toBe(false);
    reloadReq.flush({ alwaysOn: ['F-DT-08-licenciement-validity'], contextual: [], catalog: [] });

    expect(component.visibility()!.alwaysOn).toEqual(['F-DT-08-licenciement-validity']);
  }));

  // ── SF-238-01 — resolveDisplayLabel ────────────────────────────────────
  describe('SF-238-01 — resolveDisplayLabel', () => {
    it('returns the human displayLabel for a known tool', () => {
      // F-DT-08-licenciement-validity → "Licenciement — validité (FR)"
      const label = component.resolveDisplayLabel('F-DT-08-licenciement-validity');
      expect(label).not.toBe('F-DT-08-licenciement-validity'); // pas le tool_id brut
      expect(label.length).toBeGreaterThan(0);
      expect(label).toMatch(/Licenciement|validité/i);
    });

    it('returns the toolId as fallback for an unknown tool (forward-compat)', () => {
      expect(component.resolveDisplayLabel('F-XX-999-unknown')).toBe('F-XX-999-unknown');
    });
  });

  // ── SF-238-02 — activation manuelle ────────────────────────────────────
  describe('SF-238-02 — activation manuelle', () => {
    const MANUAL_URL = `/api/v1/case-files/${CASE_FILE_ID}/decision-tools-visibility/manual-activations`;

    it('POST manual-activations on chip click and triggers refresh on success', () => {
      const refreshSpy = jest.spyOn(refreshService, 'triggerRefresh');
      fixture.detectChanges();
      httpMock.expectOne(API_URL).flush({
        alwaysOn: [],
        contextual: [],
        catalog: ['F-DT-10-rupture-conv-validity'],
      });

      component.activateManually('F-DT-10-rupture-conv-validity');
      expect(component.isActivating('F-DT-10-rupture-conv-validity')).toBe(true);

      const req = httpMock.expectOne(MANUAL_URL);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ toolId: 'F-DT-10-rupture-conv-validity' });

      req.flush({
        id: '11111111-1111-1111-1111-111111111111',
        toolId: 'F-DT-10-rupture-conv-validity',
        activatedAt: '2026-05-11T10:00:00Z',
      });

      expect(component.isActivating('F-DT-10-rupture-conv-validity')).toBe(false);
      expect(refreshSpy).toHaveBeenCalled();
    });

    it('shows snackbar on POST error and clears activating state', () => {
      const snackBarMock = TestBed.inject(MatSnackBar) as unknown as { open: jest.Mock };
      fixture.detectChanges();
      httpMock.expectOne(API_URL).flush({
        alwaysOn: [],
        contextual: [],
        catalog: ['F-DT-10-rupture-conv-validity'],
      });

      component.activateManually('F-DT-10-rupture-conv-validity');
      const req = httpMock.expectOne(MANUAL_URL);
      req.flush('boom', { status: 500, statusText: 'Server Error' });

      expect(component.isActivating('F-DT-10-rupture-conv-validity')).toBe(false);
      expect(snackBarMock.open).toHaveBeenCalledWith(
        expect.stringContaining('Activation impossible'),
        'Fermer',
        expect.objectContaining({ duration: 4000 }),
      );
    });

    it('shows info snackbar on 409 (déjà activé) and triggers refresh', () => {
      const snackBarMock = TestBed.inject(MatSnackBar) as unknown as { open: jest.Mock };
      const refreshSpy = jest.spyOn(refreshService, 'triggerRefresh');
      fixture.detectChanges();
      httpMock.expectOne(API_URL).flush({
        alwaysOn: [],
        contextual: [],
        catalog: ['F-DT-10-rupture-conv-validity'],
      });

      component.activateManually('F-DT-10-rupture-conv-validity');
      const req = httpMock.expectOne(MANUAL_URL);
      req.flush('conflict', { status: 409, statusText: 'Conflict' });

      expect(snackBarMock.open).toHaveBeenCalledWith(
        expect.stringContaining('déjà activé'),
        'Fermer',
        expect.objectContaining({ duration: 3000 }),
      );
      expect(refreshSpy).toHaveBeenCalled();
    });

    it('ignores double-click while activation is in flight', () => {
      fixture.detectChanges();
      httpMock.expectOne(API_URL).flush({
        alwaysOn: [],
        contextual: [],
        catalog: ['F-DT-10-rupture-conv-validity'],
      });

      component.activateManually('F-DT-10-rupture-conv-validity');
      // 2e clic immédiat : doit être ignoré (pas de 2e POST).
      component.activateManually('F-DT-10-rupture-conv-validity');

      const reqs = httpMock.match(MANUAL_URL);
      expect(reqs.length).toBe(1);
      reqs[0].flush({ id: 'x', toolId: 'F-DT-10-rupture-conv-validity', activatedAt: '2026-05-11T10:00:00Z' });
    });
  });
});
