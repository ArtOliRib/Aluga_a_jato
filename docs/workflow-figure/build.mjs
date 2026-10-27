import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import {
  fitProjection,
  isoCamera,
  isoProject,
  slabOf,
  extrude,
  cylinder,
  planOutline,
  dotsOf,
  topTicks,
  knurl,
  coil,
  solidSvg,
  lineSvg,
  dotsSvg,
  wireSvg,
  faceTextSvg,
  figureSvg,
  plateHtml,
  pageHtml,
  depthOf,
  ISO_CSS,
  translateAlong,
} from "../../../.config/opencode/skills/anatomy/kit/iso-kit.mjs";
import { lathe, frameAlong, disc, ringBand, bandsOf, saddleOf, rodOut } from "../../../.config/opencode/skills/anatomy/kit/lathe.mjs";
import { tubePieces, tubeSvg } from "../../../.config/opencode/skills/anatomy/kit/tube.mjs";
import { recorder, settle, auditOrExit } from "../../../.config/opencode/skills/anatomy/kit/audit.mjs";
import { kitScript, glScript } from "../../../.config/opencode/skills/anatomy/scripts/inline-kit.mjs";

const __dirname = dirname(fileURLToPath(import.meta.url));

const args = process.argv.slice(2);
const light = args.includes("--light");
const audit = args.includes("--audit");

const theme = light ? "light" : "dark";
const out = join(__dirname, light ? "index-light.html" : "index.html");

// Concept: "Aluga A Jato - Fluxo completo de proteção de dados"
// Object: A processing station - input conveyor -> classifier -> normalizer -> HMAC calculator (index) and AES-GCM (cipher) -> encrypted DB with blind indexes -> reports from clear data
// The machine shows how personal data is split by usage

const P = {
  W: 1100,
  H: 700,
  fig: 1,
  title: "Aluga A Jato — Fluxo de proteção de dados",
  hint: "Hover p/ ver fluxo • Dados pessoais → Índice HMAC + Cifra AES-256-GCM",
  readout: "classifica por USO • índice ≠ cifra • relatórios só em claro",
};

// Camera
const cam = fitProjection(
  [
    [-60, -40],
    [140, -40],
    [140, 100],
    [-60, 100],
  ],
  P.W,
  P.H,
  { pad: 70, azimuth: 48 }
);
const C = isoCamera(cam);

// Build geometry
const solids = [];
const lines = [];
const dots = [];
const wires = [];
const texts = [];

function add(s) {
  solids.push(s);
}
function addLine(l) {
  lines.push(l);
}
function addDot(d) {
  dots.push(d);
}
function addWire(w) {
  wires.push(w);
}
function addText(t) {
  texts.push(t);
}

// Base plate
add(
  extrude(
    planOutline([{ x: -40, y: -25, w: 200, d: 140, r: 4 }]),
    { z: 0, h: 2, tone: "lo", face: "floor" }
  )
);

// Feet
for (let i = 0; i < 4; i++) {
  for (let j = 0; j < 4; j++) {
    add(
      extrude(
        planOutline([{ x: -30 + i * 50, y: -15 + j * 35, w: 6, d: 6, r: 1 }]),
        { z: 2, h: 3, tone: "lo" }
      )
    );
  }
}

// Structure frame
add(
  slabOf({ x: -35, y: -20, z: 5, w: 190, d: 130, h: 4, tone: "mid" })
);

// Input station - dados entram
add(
  extrude(
    planOutline([{ x: -30, y: -10, w: 30, d: 25, r: 2 }]),
    { z: 9, h: 18, tone: "mid" }
  )
);
add(
  slabOf({ x: -28, y: -8, z: 27, w: 26, d: 21, h: 1, tone: "hi", accent: true })
);

// Label plate
add(
  slabOf({ x: -29, y: 20, z: 9, w: 28, d: 3, h: 1, tone: "mid" })
);

// Classifier - classifica por uso
add(
  cylinder({ cx: 25, cy: 2, z: 9, r: 12, h: 16, tone: "mid" })
);
add(
  cylinder({ cx: 25, cy: 2, z: 25, r: 10, h: 2, tone: "hi" })
);

// Normalizer + CampoProtegido
add(
  extrude(
    planOutline([{ x: 45, y: -12, w: 35, d: 28, r: 3 }]),
    { z: 9, h: 20, tone: "mid" }
  )
);
add(
  slabOf({ x: 47, y: -10, z: 29, w: 31, d: 24, h: 2, tone: "hi" })
);

// HMAC calculator (index)
add(
  extrude(
    planOutline([{ x: 90, y: -25, w: 28, d: 22, r: 2 }]),
    { z: 9, h: 14, tone: "mid" }
  )
);
add(
  slabOf({ x: 92, y: -23, z: 23, w: 24, d: 18, h: 1, tone: "mid" })
);

// AES-GCM calculator (cipher)
add(
  extrude(
    planOutline([{ x: 90, y: 10, w: 28, d: 22, r: 2 }]),
    { z: 9, h: 14, tone: "mid" }
  )
);
add(
  slabOf({ x: 92, y: 12, z: 23, w: 24, d: 18, h: 1, tone: "mid" })
);

// Database with two forms
add(
  extrude(
    planOutline([{ x: 50, y: 35, w: 70, d: 35, r: 4 }]),
    { z: 9, h: 22, tone: "mid" }
  )
);
// DB drawers
for (let i = 0; i < 3; i++) {
  add(
    slabOf({ x: 55 + i * 22, y: 38, z: 12, w: 18, d: 28, h: 4, tone: "lo" })
  );
  add(
    slabOf({ x: 55 + i * 22, y: 38, z: 17, w: 18, d: 28, h: 4, tone: "lo" })
  );
  add(
    slabOf({ x: 55 + i * 22, y: 38, z: 22, w: 18, d: 28, h: 4, tone: "lo" })
  );
}
add(
  slabOf({ x: 52, y: 37, z: 31, w: 66, d: 31, h: 2, tone: "hi" })
);

// Reports section - only clear data
add(
  extrude(
    planOutline([{ x: -25, y: 55, w: 60, d: 28, r: 3 }]),
    { z: 9, h: 16, tone: "mid" }
  )
);
add(
  slabOf({ x: -23, y: 57, z: 25, w: 56, d: 24, h: 2, tone: "lit" })
);

// Conveyor/pipes
// Input to classifier
addWire({
  p: [
    { x: 0, y: 2.5, z: 12 },
    { x: 13, y: 2.5, z: 12 },
  ],
  r: 1.2,
  tone: "mid",
});
// Classifier to normalizer
addWire({
  p: [
    { x: 37, y: 2, z: 12 },
    { x: 45, y: 2, z: 12 },
  ],
  r: 1.2,
  tone: "mid",
});
// Normalizer to HMAC
addWire({
  p: [
    { x: 62.5, y: 2, z: 15 },
    { x: 75, y: -14, z: 15 },
    { x: 90, y: -14, z: 15 },
  ],
  r: 1,
  tone: "mid",
});
// Normalizer to AES
addWire({
  p: [
    { x: 62.5, y: 2, z: 15 },
    { x: 75, y: 21, z: 15 },
    { x: 90, y: 21, z: 15 },
  ],
  r: 1,
  tone: "mid",
});
// HMAC to DB
addWire({
  p: [
    { x: 104, y: -14, z: 12 },
    { x: 104, y: 10, z: 12 },
    { x: 85, y: 25, z: 12 },
    { x: 85, y: 35, z: 12 },
  ],
  r: 1,
  tone: "mid",
});
// AES to DB
addWire({
  p: [
    { x: 104, y: 21, z: 12 },
    { x: 104, y: 30, z: 12 },
    { x: 90, y: 35, z: 12 },
  ],
  r: 1,
  tone: "mid",
});
// Clear data from aluguel/alugueis to reports
addWire({
  p: [
    { x: 50, y: 52.5, z: 12 },
    { x: 35, y: 52.5, z: 12 },
    { x: 35, y: 55, z: 12 },
  ],
  r: 1,
  tone: "lit",
});

// Gauges/indicators
add(
  cylinder({ cx: -15, cy: -5, z: 28, r: 3, h: 1, tone: "hi" })
);
add(
  cylinder({ cx: 25, cy: 2, z: 28, r: 2.5, h: 0.5, tone: "hi" })
);

// Knobs
add(
  cylinder({ cx: 110, cy: -14, z: 24, r: 2, h: 2, tone: "mid" })
);
add(
  cylinder({ cx: 110, cy: 21, z: 24, r: 2, h: 2, tone: "mid" })
);

// Screws
for (let i = 0; i < 4; i++) {
  addDot({ x: -28 + i * 8, y: -8, z: 28.5 });
  addDot({ x: -28 + i * 8, y: 11, z: 28.5 });
}

// Project all solids
const items = [];
function projSol(s) {
  const p = solidSvg(s, C);
  if (p) items.push({ kind: "solid", svg: p, z: depthOf(p, C) });
}

for (const s of solids) projSol(s);

const R = recorder(items, { order: true });
settle(R, items);
if (audit) auditOrExit(R, items);

// Project lines/wires/dots
const wiresSvg = wires.map((w) => wireSvg(w, C)).filter(Boolean);
const dotsSvgList = dots.map((d) => dotsSvg(d, C)).filter(Boolean);
const linesSvg = lines.map((l) => lineSvg(l, C)).filter(Boolean);

// Labels
const labels = [
  { x: -15, y: 22, z: 10, text: "ENTRADA" },
  { x: -15, y: 19, z: 10, text: "(POST /alugueis)" },
  { x: 25, y: 16, z: 10, text: "CLASSIFICAR" },
  { x: 25, y: 13, z: 10, text: "POR USO" },
  { x: 62.5, y: 18, z: 10, text: "CampoProtegido" },
  { x: 62.5, y: 15, z: 10, text: "(NORMALIZAÇÃO)" },
  { x: 104, y: -1, z: 10, text: "HMAC" },
  { x: 104, y: -4, z: 10, text: "(ÍNDICE)" },
  { x: 104, y: 34, z: 10, text: "AES-256-GCM" },
  { x: 104, y: 31, z: 10, text: "(CIFRA)" },
  { x: 85, y: 72, z: 10, text: "BANCO" },
  { x: 85, y: 69, z: 10, text: "índice + cifra" },
  { x: 5, y: 85, z: 10, text: "RELATÓRIOS" },
  { x: 5, y: 82, z: 10, text: "(só dados em claro)" },
];

const labelSvg = labels
  .map((l) => {
    const proj = isoProject({ x: l.x, y: l.y, z: l.z }, C);
    return `<text x="${proj.x}" y="${proj.y}" class="iso-label">${l.text}</text>`;
  })
  .join("\n");

// Sort items by z
items.sort((a, b) => a.z - b.z);
const solidsOut = items.map((i) => i.svg).join("\n");

const figure = figureSvg({
  width: P.W,
  height: P.H,
  svg: `${solidsOut}\n${wiresSvg.join("\n")}\n${dotsSvgList.join("\n")}\n${linesSvg.join("\n")}\n${labelSvg}`,
  theme,
});

const card = plateHtml({
  fig: P.fig,
  title: P.title,
  hint: P.hint,
  readout: P.readout,
});

const html = pageHtml({
  title: P.title,
  body: `${figure}\n${card}`,
  theme,
  script: kitScript(),
});

mkdirSync(__dirname, { recursive: true });
writeFileSync(out, html);
console.log("wrote", out);
if (audit) console.log("audit passed");
