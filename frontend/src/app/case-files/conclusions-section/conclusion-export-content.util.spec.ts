import {
  buildExportContent,
  buildFullExportContent,
  buildMetadataHeader,
  buildSignatureBlock,
  escapeMarkdown,
  extractPlaceholders,
} from './conclusion-export-content.util';

const BODY = 'POUR : M. X\n\n## FAITS ET PROCÉDURE\nLe salarié…';

describe('F-281 — buildMetadataHeader', () => {
  it('3 labels → cartouche 3 lignes en gras + filet', () => {
    const out = buildMetadataHeader({
      jurisdictionLabel: 'Conseil de prud\'hommes',
      stageLabel: 'Bureau de jugement (fond)',
      positionLabel: 'Demandeur (salarié)',
    });
    expect(out).toContain("**Juridiction :** Conseil de prud'hommes");
    expect(out).toContain('**Stade :** Bureau de jugement \\(fond\\)');
    expect(out).toContain('**Position :** Demandeur \\(salarié\\)');
    expect(out.endsWith('---')).toBe(true);
  });

  it('1 seul label renseigné → 1 ligne + filet', () => {
    const out = buildMetadataHeader({
      jurisdictionLabel: 'Tribunal judiciaire',
      stageLabel: null,
      positionLabel: '',
    });
    expect(out).toBe('**Juridiction :** Tribunal judiciaire\n\n---');
  });

  it('aucun label → chaîne vide (export neutre)', () => {
    expect(buildMetadataHeader({ jurisdictionLabel: null, stageLabel: null, positionLabel: null })).toBe('');
    expect(buildMetadataHeader({})).toBe('');
    expect(buildMetadataHeader(null)).toBe('');
    expect(buildMetadataHeader(undefined)).toBe('');
  });

  it('libellés échappés (texte, jamais interprété)', () => {
    const out = buildMetadataHeader({ jurisdictionLabel: 'CPH #1 *Paris*', stageLabel: null, positionLabel: null });
    expect(out).toContain('\\#1 \\*Paris\\*');
  });
});

describe('F-281 — buildSignatureBlock', () => {
  it('activé + lieu + date → bloc complet', () => {
    const out = buildSignatureBlock(true, 'Paris', '12/06/2026');
    expect(out).toContain('---');
    expect(out).toContain('Fait à Paris, le 12/06/2026');
    expect(out).toContain("[Nom et qualité de l'avocat]");
  });

  it('activé mais lieu/date vides → placeholders [Lieu] / [Date]', () => {
    const out = buildSignatureBlock(true, '', '   ');
    expect(out).toContain('Fait à [Lieu], le [Date]');
    expect(out).toContain("[Nom et qualité de l'avocat]");
  });

  it('désactivé → chaîne vide', () => {
    expect(buildSignatureBlock(false, 'Paris', '12/06/2026')).toBe('');
  });

  it('lieu/date échappés (texte)', () => {
    const out = buildSignatureBlock(true, 'Lyon #2', '01.01.2026');
    expect(out).toContain('Fait à Lyon \\#2, le 01\\.01\\.2026');
  });
});

describe('F-281 — buildFullExportContent', () => {
  it('compose métadonnées (tête) + corps + signature (pied) sans muter le corps', () => {
    const meta = buildMetadataHeader({ jurisdictionLabel: 'CPH', stageLabel: null, positionLabel: null });
    const sig = buildSignatureBlock(true, 'Paris', '12/06/2026');
    const out = buildFullExportContent('', meta, BODY, sig);
    const metaIdx = out.indexOf('**Juridiction :** CPH');
    const bodyIdx = out.indexOf('## FAITS ET PROCÉDURE');
    const sigIdx = out.indexOf('Fait à Paris');
    expect(metaIdx).toBeGreaterThanOrEqual(0);
    expect(bodyIdx).toBeGreaterThan(metaIdx);
    expect(sigIdx).toBeGreaterThan(bodyIdx);
    // Le corps d'origine est présent inchangé.
    expect(out).toContain(BODY);
  });

  it('en-tête cabinet préfixé en plus (avant les métadonnées)', () => {
    const meta = buildMetadataHeader({ jurisdictionLabel: 'CPH', stageLabel: null, positionLabel: null });
    const out = buildFullExportContent('Cabinet Durand', meta, BODY, '');
    const cabIdx = out.indexOf('# Cabinet Durand');
    const metaIdx = out.indexOf('**Juridiction :** CPH');
    expect(cabIdx).toBeGreaterThanOrEqual(0);
    expect(metaIdx).toBeGreaterThan(cabIdx);
  });

  it('tout vide (header + meta + signature) → content STRICTEMENT inchangé (non-régression neutre)', () => {
    expect(buildFullExportContent('', '', BODY, '')).toBe(BODY);
    expect(buildFullExportContent('   ', '  ', BODY, '  ')).toBe(BODY);
  });

  it('métadonnées seules (sans en-tête ni signature) préfixées au corps', () => {
    const meta = buildMetadataHeader({ jurisdictionLabel: 'CPH', stageLabel: 'Conciliation', positionLabel: null });
    const out = buildFullExportContent('', meta, BODY, '');
    expect(out.startsWith('**Juridiction :** CPH')).toBe(true);
    expect(out.endsWith(BODY)).toBe(true);
  });
});

describe('F-281 — escapeMarkdown', () => {
  it('échappe les caractères structurants markdown', () => {
    expect(escapeMarkdown('# *Gras* [x](y)')).toBe('\\# \\*Gras\\* \\[x\\]\\(y\\)');
  });
});

// Garde de non-régression : le helper SF-266-03 reste exposé et fonctionnel
// depuis le module extrait (importé tel quel par le composant et ses specs).
describe('F-281 — extractPlaceholders (déplacé, non régressé)', () => {
  it('détecte les placeholders, déduplique, ignore liens md et renvois [1]', () => {
    const md = 'Texte [Lieu] et [Date]. Voir [arrêt](http://x) et [1]. À nouveau [Lieu].';
    expect(extractPlaceholders(md)).toEqual(['[Lieu]', '[Date]']);
  });

  it('contenu vide → liste vide', () => {
    expect(extractPlaceholders('')).toEqual([]);
  });
});
