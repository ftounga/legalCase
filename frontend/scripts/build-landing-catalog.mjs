#!/usr/bin/env node
/**
 * build-landing-catalog.mjs — Génère frontend/src/app/landing/landing-tools-catalog.ts
 *
 * Source de vérité :
 *   1. Labels + ids  : Map TOOL_REGISTRY de
 *      frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts
 *   2. domain + country : seeds SQL de la table `decision_tool_visibility_rules`
 *      (backend/src/main/resources/db/changelog/migrations/*.xml)
 *   3. type : inféré du label (heuristique, sans accent/casse).
 *
 * Reproductible — relancer après tout changement de TOOL_REGISTRY :
 *   node scripts/build-landing-catalog.mjs
 *
 * Aucune dépendance externe (lecture fichiers + regex uniquement).
 *
 * SF-158-04 (F-158 V4) — remplace l'ancien build-catalog.py disparu du repo.
 */

import { readFileSync, writeFileSync, readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join, resolve } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(__dirname, '..', '..');
const FRONTEND_ROOT = resolve(__dirname, '..');

const REGISTRY_FILE = join(
  FRONTEND_ROOT,
  'src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts',
);
const MIGRATIONS_DIR = join(
  REPO_ROOT,
  'backend/src/main/resources/db/changelog/migrations',
);
const OUTPUT_FILE = join(
  FRONTEND_ROOT,
  'src/app/landing/landing-tools-catalog.ts',
);

// --------------------------------------------------------------------------
// 1. Parse TOOL_REGISTRY → { id: label }
// --------------------------------------------------------------------------
function parseRegistry() {
  const src = readFileSync(REGISTRY_FILE, 'utf8');
  const start = src.indexOf('TOOL_REGISTRY');
  if (start < 0) {
    throw new Error('TOOL_REGISTRY introuvable dans ' + REGISTRY_FILE);
  }
  const body = src.slice(start);
  // ['<id>', { displayLabel: '<label>' (label peut contenir des \' échappés)
  const re = /\[\s*'([^']+)'\s*,\s*\{\s*\n?\s*displayLabel:\s*'((?:[^'\\]|\\.)*)'/g;
  const map = new Map();
  let m;
  while ((m = re.exec(body)) !== null) {
    const id = m[1];
    // label : on conserve la séquence d'échappement TS telle quelle (\') —
    // elle sera ré-émise à l'identique dans une string single-quote.
    const label = m[2];
    if (!map.has(id)) {
      map.set(id, label);
    }
  }
  return map;
}

// --------------------------------------------------------------------------
// 2. Parse seeds visibility → { tool_id: { domain, country } } (1re règle)
// --------------------------------------------------------------------------
const DOMAIN_MAP = {
  DROIT_DU_TRAVAIL: 'TRAVAIL',
  DROIT_IMMIGRATION: 'IMMIGRATION',
  DROIT_FAMILLE: 'FAMILLE',
};

function mapCountry(raw) {
  if (!raw) return 'FR';
  const v = raw.toUpperCase();
  if (v === 'BELGIQUE' || v === 'BE') return 'BE';
  return 'FR'; // FRANCE / FR / autre
}

/**
 * Tokenise le contenu d'un VALUES(...) SQL en respectant les quotes.
 * Retourne la liste des valeurs ('foo' -> "foo", NULL -> null).
 */
function splitSqlValues(inner) {
  const out = [];
  let i = 0;
  const n = inner.length;
  while (i < n) {
    // skip whitespace + commas
    while (i < n && (inner[i] === ' ' || inner[i] === '\n' || inner[i] === '\t' || inner[i] === ',' || inner[i] === '\r')) {
      i++;
    }
    if (i >= n) break;
    if (inner[i] === "'") {
      // quoted string ; '' = quote échappée
      i++;
      let val = '';
      while (i < n) {
        if (inner[i] === "'") {
          if (inner[i + 1] === "'") { val += "'"; i += 2; continue; }
          i++; break;
        }
        val += inner[i++];
      }
      out.push(val);
    } else {
      // unquoted token (NULL, number, etc.)
      let val = '';
      while (i < n && inner[i] !== ',') { val += inner[i++]; }
      val = val.trim();
      out.push(val.toUpperCase() === 'NULL' ? null : val);
    }
  }
  return out;
}

function parseVisibilitySeeds() {
  const files = readdirSync(MIGRATIONS_DIR).filter((f) => f.endsWith('.xml'));
  const byTool = new Map();
  // colonnes attendues : (id, legal_domain, country, tool_id, layer, trigger_field, trigger_value, priority)
  // -> index domain=1, country=2, tool_id=3
  const insertRe =
    /INSERT\s+INTO\s+decision_tool_visibility_rules\s*\(([^)]*)\)\s*VALUES\s*\(([\s\S]*?)\)\s*;/gi;

  for (const f of files) {
    const content = readFileSync(join(MIGRATIONS_DIR, f), 'utf8');
    if (!content.includes('decision_tool_visibility_rules')) continue;
    let m;
    while ((m = insertRe.exec(content)) !== null) {
      const cols = m[1].split(',').map((c) => c.trim().toLowerCase());
      const values = splitSqlValues(m[2]);
      const idxDomain = cols.indexOf('legal_domain');
      const idxCountry = cols.indexOf('country');
      const idxTool = cols.indexOf('tool_id');
      if (idxTool < 0) continue;
      const toolId = values[idxTool];
      if (!toolId) continue;
      if (byTool.has(toolId)) continue; // 1re règle gagne
      const rawDomain = idxDomain >= 0 ? values[idxDomain] : null;
      const rawCountry = idxCountry >= 0 ? values[idxCountry] : null;
      const domain = DOMAIN_MAP[rawDomain] || null; // peut être null si CONVENTION_PREAVIS etc.
      if (!domain) continue; // pas un domaine landing exploitable -> on dérivera par préfixe
      byTool.set(toolId, { domain, country: mapCountry(rawCountry) });
    }
  }
  return byTool;
}

// --------------------------------------------------------------------------
// 3. Type inféré du label (heuristique, sans accent/casse)
// --------------------------------------------------------------------------
function stripAccents(s) {
  return s.normalize('NFD').replace(/[̀-ͯ]/g, '');
}

function inferType(label) {
  const l = stripAccents(label).toLowerCase();
  const has = (...words) => words.some((w) => l.includes(stripAccents(w).toLowerCase()));
  if (has('gener', 'requete', 'recours', 'fiche', 'memoire', 'courrier', 'conclusion')) return 'GÉNÉRATEUR';
  if (has('comparateur', 'comparat')) return 'COMPARATEUR';
  if (has('validite', 'score', 'scoring', 'eligibilite', 'recevabilite')) return 'SCORING';
  if (has('detect', 'nul', 'vice', 'risque')) return 'DÉTECTEUR';
  if (has('calcul', 'indemnite', 'montant', 'rappel', 'prime', 'salaire')) return 'CALCULATEUR';
  return 'OUTIL';
}

// --------------------------------------------------------------------------
// 3 bis. Dérivation domain/country par préfixe d'id (fallback hors seeds)
// --------------------------------------------------------------------------
function deriveDomainFromId(id) {
  const u = id.toUpperCase();
  if (u.startsWith('F-DT') || u.startsWith('DT')) return 'TRAVAIL';
  if (u.startsWith('F-FA') || u.startsWith('FA')) return 'FAMILLE';
  if (u.startsWith('F-IM') || u.startsWith('IM')) return 'IMMIGRATION';
  return 'TRAVAIL'; // défaut
}

function deriveCountryFromIdOrLabel(id, label) {
  const hay = (id + ' ' + label).toLowerCase();
  if (hay.includes('-be') || hay.includes('(be)')) return 'BE';
  return 'FR';
}

// --------------------------------------------------------------------------
// 4. Jointure + écriture
// --------------------------------------------------------------------------
function main() {
  const registry = parseRegistry();
  const seeds = parseVisibilitySeeds();

  const derived = []; // ids sans seed visibility (cas dérivés par défaut)
  const tools = [];

  for (const [id, label] of registry) {
    let domain;
    let country;
    const seed = seeds.get(id);
    if (seed) {
      domain = seed.domain;
      country = seed.country;
    } else {
      domain = deriveDomainFromId(id);
      country = deriveCountryFromIdOrLabel(id, label);
      derived.push({ id, domain, country });
    }
    tools.push({ id, label, domain, country, type: inferType(label) });
  }

  // tri domain -> country -> id
  tools.sort((a, b) => {
    if (a.domain !== b.domain) return a.domain < b.domain ? -1 : 1;
    if (a.country !== b.country) return a.country < b.country ? -1 : 1;
    return a.id < b.id ? -1 : a.id > b.id ? 1 : 0;
  });

  // garde-fous
  const empties = tools.filter((t) => !t.label || !t.label.trim());
  if (empties.length) {
    throw new Error('Labels vides détectés : ' + empties.map((t) => t.id).join(', '));
  }

  // entête + cas dérivés loggés en commentaire
  const lines = [];
  lines.push('// Auto-généré depuis TOOL_REGISTRY (decisional-tools-panel.component.ts)');
  lines.push('// + seeds decision_tool_visibility_rules (legal_domain/country).');
  lines.push('// NE PAS ÉDITER À LA MAIN — régénérer via :');
  lines.push('//   node scripts/build-landing-catalog.mjs');
  lines.push('// (SF-158-04 F-158 V4 — remplace l\'ancien build-catalog.py).');
  lines.push('//');
  lines.push(`// ${tools.length} outils. Répartition :`);
  for (const dom of ['TRAVAIL', 'IMMIGRATION', 'FAMILLE']) {
    const fr = tools.filter((t) => t.domain === dom && t.country === 'FR').length;
    const be = tools.filter((t) => t.domain === dom && t.country === 'BE').length;
    lines.push(`//   ${dom}: FR=${fr} BE=${be}`);
  }
  lines.push('//');
  if (derived.length) {
    lines.push(`// ${derived.length} id(s) sans règle de visibilité — domain/country dérivés par défaut (préfixe id / label) :`);
    for (const d of derived) {
      lines.push(`//   ${d.id} -> ${d.domain}/${d.country}`);
    }
  } else {
    lines.push('// Tous les ids du registre ont une règle de visibilité (aucune dérivation par défaut).');
  }
  lines.push('');
  lines.push("export type LandingToolDomain = 'TRAVAIL' | 'IMMIGRATION' | 'FAMILLE';");
  lines.push("export type LandingToolCountry = 'FR' | 'BE';");
  lines.push("export type LandingToolType = 'CALCULATEUR' | 'SCORING' | 'COMPARATEUR' | 'GÉNÉRATEUR' | 'DÉTECTEUR' | 'OUTIL';");
  lines.push('');
  lines.push('export interface LandingTool {');
  lines.push('  id: string;');
  lines.push('  label: string;');
  lines.push('  domain: LandingToolDomain;');
  lines.push('  country: LandingToolCountry;');
  lines.push('  type: LandingToolType;');
  lines.push('}');
  lines.push('');
  lines.push('export const LANDING_TOOLS_CATALOG: LandingTool[] = [');
  for (const t of tools) {
    lines.push(
      `  { id: '${t.id}', label: '${t.label}', domain: '${t.domain}', country: '${t.country}', type: '${t.type}' },`,
    );
  }
  lines.push('];');
  lines.push('');

  writeFileSync(OUTPUT_FILE, lines.join('\n'), 'utf8');

  // résumé console
  console.log(`Catalogue généré : ${tools.length} outils -> ${OUTPUT_FILE}`);
  for (const dom of ['TRAVAIL', 'IMMIGRATION', 'FAMILLE']) {
    const fr = tools.filter((t) => t.domain === dom && t.country === 'FR').length;
    const be = tools.filter((t) => t.domain === dom && t.country === 'BE').length;
    console.log(`  ${dom}: FR=${fr} BE=${be}`);
  }
  console.log(`  Total FR=${tools.filter((t) => t.country === 'FR').length} BE=${tools.filter((t) => t.country === 'BE').length}`);
  console.log(`  Dérivés sans seed : ${derived.length}`);
  if (derived.length) {
    derived.forEach((d) => console.log(`    ${d.id} -> ${d.domain}/${d.country}`));
  }
}

main();
