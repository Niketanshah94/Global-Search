const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');
const { pal } = require('./mascot.js');
const dir = __dirname;
const out = path.join(dir, 'out');
fs.mkdirSync(out, { recursive: true });

// Embedded as data URIs: pages built with setContent cannot load file:// fonts.
const b64 = (f) => fs.readFileSync(path.join(dir, 'fonts', f)).toString('base64');
const fontCss = `
@font-face{font-family:"Baloo 2";font-weight:700;src:url(data:font/ttf;base64,${b64('baloo2-700.ttf')}) format("truetype")}
@font-face{font-family:"Baloo 2";font-weight:800;src:url(data:font/ttf;base64,${b64('baloo2-800.ttf')}) format("truetype")}`;

// App icon: Pal's face on a warm squircle (no arms; the lens tilts so the handle shows).
function icon(size, bg = 'dark') {
  const fill = bg === 'dark'
    ? 'radial-gradient(80% 80% at 30% 20%, #3a2414, #120d09)'
    : 'linear-gradient(145deg,#FFB067,#F07A2E 55%,#D9572A)';
  return `<div style="width:${size}px;height:${size}px;border-radius:${size * 0.23}px;background:${fill};display:flex;align-items:center;justify-content:center;overflow:hidden;box-shadow:inset 0 0 0 ${Math.max(1, size / 160)}px rgba(255,255,255,.08)">
    <div style="width:${size * 0.96}px;margin:${size * 0.06}px 0 0 ${size * 0.04}px">${pal({ face: 'happy', size: size * 0.96 })}</div></div>`;
}

function wordmark(size, onDark = true, caps = false) {
  const a = onDark ? '#FFF1DC' : '#1D140E';
  const text = caps ? '<span style="letter-spacing:.02em">FIND</span><span style="color:#F07A2E;letter-spacing:.02em">PAL</span>'
                    : 'Find<span style="color:#F07A2E">Pal</span>';
  return `<span style="font:800 ${size}px 'Baloo 2';color:${a};line-height:1;letter-spacing:-.01em">${text}</span>`;
}

const css = `${fontCss}
*{box-sizing:border-box;margin:0;padding:0}
body{background:#0b0806;font-family:Inter,sans-serif;color:#FFF1DC;width:1600px;overflow:hidden}
.sec{padding:56px 72px}
.dark{background:#120d09}
.cream{background:#FFE8C2;color:#1D140E}
h2{font:800 15px Inter;letter-spacing:.18em;text-transform:uppercase;color:#F7A35C;margin-bottom:26px}
.cream h2{color:#B8571F}
h3{font:800 40px "Inter Display";letter-spacing:-.02em;line-height:1.05}
.row{display:flex;gap:40px;align-items:center}
.note{font:500 15px/1.5 Inter;color:#b9a892}
.cream .note{color:#7a5a40}
.scallop{height:44px;background:radial-gradient(circle at 22px 0,#FFE8C2 21px,transparent 22px) 0 0/44px 44px repeat-x;background-color:#120d09}
.scallop.up{background:radial-gradient(circle at 22px 44px,#FFE8C2 21px,transparent 22px) 0 0/44px 44px repeat-x;background-color:#120d09}
.btn{display:inline-flex;align-items:center;gap:12px;font:700 24px "Inter Display";color:#1D140E;background:linear-gradient(#FFF0D6,#FFDCA6);border-radius:999px;padding:20px 38px;box-shadow:0 7px 0 #C7682B,0 18px 40px rgba(240,122,46,.25)}
.bubble{position:relative;background:#FFE8C2;color:#1D140E;font:700 22px "Baloo 2";padding:14px 26px;border-radius:999px;box-shadow:0 5px 0 #C7682B}
.bubble:before{content:"";position:absolute;left:-14px;top:50%;margin-top:-12px;border:12px solid transparent;border-right:16px solid #FFE8C2;border-left:0}
.bubble-down:before{left:50%;top:auto;bottom:-16px;margin:0 0 0 -12px;border:12px solid transparent;border-top:16px solid #FFE8C2;border-bottom:0}
.sw{width:170px;border-radius:22px;overflow:hidden;background:#1d150f}
.sw div{height:110px}
.sw p{padding:12px 14px;font:600 14px Inter;color:#FFF1DC}
.sw p span{display:block;font:500 12px "JetBrains Mono";color:#b9a892;margin-top:4px}
.face{display:flex;flex-direction:column;align-items:center;gap:8px;width:200px}
.face b{font:800 20px "Baloo 2";color:#FFF1DC}
.face span{font:500 13px Inter;color:#b9a892;text-align:center}
.stat{display:inline-flex;align-items:baseline;gap:10px;background:#1d150f;border:1px solid #3a2a1d;border-radius:20px;padding:18px 26px}
`;

const faces = [
  ['happy', 'wave', null, 'Hello', 'Onboarding, hero, widget'],
  ['look', 'none', 'sweat', 'Searching', 'Loading, indexing'],
  ['stars', 'cheer', 'sparkle', 'Found it!', 'Top hit, success, share cards'],
  ['sleepy', 'none', 'zzz', 'Resting', 'Empty states, paused indexing'],
  ['cool', 'point', null, 'Pro', 'Pricing, FindPal Pro'],
  ['worried', 'none', null, 'Oops', 'No results, permission denied'],
];

const board = `<!doctype html><html><head><meta charset="utf-8"><style>${css}</style></head><body>
<section class="sec dark" style="padding-top:72px;background:radial-gradient(60% 90% at 20% 0%,#3b220f,#120d09 70%)">
  <div style="display:flex;justify-content:space-between;align-items:flex-start">
    <div><h2>FindPal · brand board v1</h2>
      <div class="row" style="gap:26px">${icon(132)}${wordmark(118)}</div>
      <p style="font:800 46px 'Inter Display';letter-spacing:-.02em;margin-top:34px">Lost something? <span style="color:#FFC48A">Ask your Pal.</span></p>
      <p class="note" style="font-size:18px;margin-top:10px;max-width:640px">Pal is a magnifying glass whose lens is its face. It finds anything on your phone in two letters, and it is always happy to help.</p>
    </div>
    <div style="display:flex;flex-direction:column;align-items:center;gap:18px;margin-top:10px;margin-right:30px">
      <div class="bubble bubble-down" style="white-space:nowrap">Hi! I find your stuff.</div>
      ${pal({ face: 'happy', arm: 'wave', size: 320 })}</div>
  </div>
</section>
<div class="scallop"></div>
<section class="sec cream">
  <h2>Logo</h2>
  <div class="row" style="gap:60px;align-items:flex-start">
    <div><div style="background:#120d09;border-radius:28px;padding:34px 40px" class="row">${icon(84)}<span style="margin-left:18px">${wordmark(72)}</span></div><p class="note" style="margin-top:10px">Primary lockup, on dark</p></div>
    <div><div style="background:#fff;border-radius:28px;padding:34px 40px" class="row">${icon(84, 'orange')}<span style="margin-left:18px">${wordmark(72, false)}</span></div><p class="note" style="margin-top:10px">On light</p></div>
    <div><div style="background:#120d09;border-radius:28px;padding:44px 40px">${wordmark(64, true, true)}</div><p class="note" style="margin-top:10px">Caps wordmark: nav bars, merch</p></div>
  </div>
  <h2 style="margin-top:48px">App icon</h2>
  <div class="row" style="gap:44px;align-items:flex-end">
    <div>${icon(220)}<p class="note" style="margin-top:10px">Primary (dark)</p></div>
    <div>${icon(220, 'orange')}<p class="note" style="margin-top:10px">Alternate (orange)</p></div>
    <div class="row" style="gap:22px;align-items:flex-end">${icon(96)}${icon(64)}${icon(48)}${icon(32)}</div>
    <div class="note" style="max-width:280px">Readable down to 32 px: the lens and two eyes are the whole idea. On Android the face sits in the adaptive-icon safe zone.</div>
  </div>
</section>
<div class="scallop up"></div>
<section class="sec dark">
  <h2>Pal's expressions</h2>
  <div class="row" style="gap:36px;align-items:flex-start">
    ${faces.map(f => `<div class="face">${pal({ face: f[0], arm: f[1], extra: f[2], size: 180 })}<b>${f[3]}</b><span>${f[4]}</span></div>`).join('')}
  </div>
</section>
<section class="sec dark" style="padding-top:10px">
  <div class="row" style="gap:80px;align-items:flex-start">
    <div>
      <h2>Colour</h2>
      <div class="row" style="gap:16px;flex-wrap:wrap;width:740px">
        ${[['#120D09', 'Night', 'backgrounds'], ['#2A1A10', 'Cocoa', 'cards, handle'], ['#FFE8C2', 'Cream', 'buttons, sections'], ['#FFC48A', 'Peach', 'highlights'], ['#F07A2E', 'Pal Orange', 'brand, accents'], ['#D9572A', 'Ember', 'button edge, icons'], ['#7ACC5A', 'Found Green', 'success'], ['#FFC93C', 'Star', 'delight']]
          .map(c => `<div class="sw"><div style="background:${c[0]}${c[0] === '#120D09' ? ';box-shadow:inset 0 0 0 1px #3a2a1d' : ''}"></div><p>${c[1]}<span>${c[0]} · ${c[2]}</span></p></div>`).join('')}
      </div>
    </div>
    <div>
      <h2>Type</h2>
      <p style="font:800 72px 'Baloo 2';line-height:1">Baloo 2</p>
      <p class="note">ExtraBold · logo, Pal's speech, big numbers. By Ek Type, an Indian type foundry.</p>
      <p style="font:800 46px 'Inter Display';letter-spacing:-.02em;margin-top:26px">Find anything.</p>
      <p class="note">Inter Display ExtraBold · headlines, tight tracking</p>
      <p style="font:400 22px Inter;margin-top:20px;color:#e9dcc8">Search apps, people, files and OTPs on your phone.</p>
      <p class="note">Inter Regular · body</p>
    </div>
  </div>
</section>
<div class="scallop"></div>
<section class="sec cream">
  <h2>Components &amp; voice</h2>
  <div class="row" style="gap:48px;align-items:center;flex-wrap:wrap">
    <span class="btn"><svg width="26" height="26" viewBox="0 0 24 24"><path d="M5 3l14 9-14 9z" fill="#1D140E"/></svg>Get it on Google Play</span>
    <span class="bubble" style="margin-left:14px">Found it! 🎉</span>
    <span class="stat" style="background:#120d09"><span style="font:800 54px 'Baloo 2';color:#F07A2E;line-height:1">38</span><span style="font:600 18px Inter;color:#FFF1DC">things found today<br><span style="color:#b9a892;font-weight:500">12 min saved</span></span></span>
  </div>
  <div class="row" style="gap:60px;margin-top:44px;align-items:flex-start">
    <div style="max-width:470px"><p style="font:800 34px 'Inter Display';letter-spacing:-.02em;line-height:1.1">Find anything.<br>In two letters.</p><p class="note" style="margin-top:8px">Hero line</p></div>
    <div style="max-width:470px"><p style="font:800 34px 'Inter Display';letter-spacing:-.02em;line-height:1.1">Your phone. Your data.<br>Pal never tells.</p><p class="note" style="margin-top:8px">Privacy line</p></div>
    <div style="max-width:420px"><p class="note" style="font-size:17px;color:#5a3a22"><b>Voice:</b> Pal talks like a friendly helper. Short, warm, first person ("I found 3 Rahuls"). Never techy, never pushy. Hindi or Hinglish touches are welcome in India campaigns ("MilGaya!").</p></div>
  </div>
</section>
</body></html>`;

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1600, height: 1000 }, deviceScaleFactor: 2 });
  await page.setContent(board, { waitUntil: 'load' });
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({ path: path.join(out, 'findpal-brand-board.png'), fullPage: true });

  // Standalone assets
  const asset = async (html, file, w, h, transparent = true) => {
    const p = await browser.newPage({ viewport: { width: w, height: h }, deviceScaleFactor: 1 });
    await p.setContent(`<!doctype html><html><head><style>${fontCss}*{margin:0}body{width:${w}px;height:${h}px;display:flex;align-items:center;justify-content:center;background:transparent}</style></head><body>${html}</body></html>`);
    await p.evaluate(() => document.fonts.ready);
    await p.screenshot({ path: path.join(out, file), omitBackground: transparent });
    await p.close();
  };
  await asset(icon(512), 'findpal-icon-512.png', 512, 512);
  await asset(icon(512, 'orange'), 'findpal-icon-512-orange.png', 512, 512);
  await asset(`<div style="display:flex;align-items:center;gap:36px;background:#120d09;padding:60px 80px;border-radius:0">${icon(180)}${wordmark(160)}</div>`, 'findpal-logo-dark.png', 1100, 340, false);
  for (const f of faces) {
    await asset(pal({ face: f[0], arm: f[1], extra: f[2], size: 600 }), `pal-${f[3].toLowerCase().replace(/[^a-z]/g, '')}.png`, 640, 620);
  }
  for (const f of faces) {
    fs.writeFileSync(path.join(out, `pal-${f[3].toLowerCase().replace(/[^a-z]/g, '')}.svg`), pal({ face: f[0], arm: f[1], extra: f[2], size: 600 }));
  }
  await browser.close();
})();
