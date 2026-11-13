// Renders Google Play store graphics into docs/store/images/:
//   feature-graphic.png (1024x500) and screenshot-1..7.png (1080x1920, 9:16).
//   node docs/store/build-images.js        (needs Playwright with Chromium)
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const { P, css: baseCss } = require('../social/common.js');

const out = path.join(__dirname, 'images');
fs.mkdirSync(out, { recursive: true });

const css = baseCss + `
body{width:auto;height:auto}
.shot{position:relative;width:1080px;height:1920px;overflow:hidden;padding:110px 80px 0;display:flex;flex-direction:column;align-items:center;text-align:center}
.shot h1{font-size:96px}
.shot .sub{margin:26px auto 0;font-size:38px}
.stage{position:relative;margin-top:70px}
.big{width:760px;border-radius:96px;padding:20px;background:#0b0806;box-shadow:0 0 0 16px #2a1d14,0 60px 120px rgba(0,0,0,.5);text-align:left}
.big .scr{border-radius:78px;background:#120d09;height:1240px;padding:40px 30px 34px;display:flex;flex-direction:column;gap:18px;justify-content:flex-end;overflow:hidden}
.big .hd{display:flex;align-items:center;gap:14px;margin-bottom:auto;font:800 52px "Baloo 2";color:#fff1dc}
.big .hd b{color:#f07a2e}
.big .hd svg{width:88px;height:auto}
.big .hd small{display:block;font:500 24px Inter;color:#bfae98}
.big .card{padding:22px 24px;border-radius:30px}
.big .card .ic{width:68px;height:68px;font-size:26px;border-radius:20px}
.big .card b{font-size:32px}
.big .card span{font-size:23px}
.big .top{padding:26px;border-radius:36px}
.big .top .t{font-size:54px}
.big .top .sub2{font:500 24px Inter;color:#7a5a40}
.big .acts span{font-size:24px;padding:18px 4px;border-radius:22px}
.big .sbar{font-size:36px;padding:26px 28px;border-radius:30px}
.big .sbar .q:after{height:38px}
.big .sbar .en{font-size:22px}
.chips2{display:flex;gap:12px;flex-wrap:wrap}
.chips2 span{font:600 24px Inter;color:#fff1dc;background:#1f160f;border-radius:999px;padding:12px 22px;box-shadow:0 0 0 2px #3a2a1d}
.chips2 span.on{background:#ffe8c2;color:#1d140e;box-shadow:none}
.pill{display:flex;align-items:center;gap:14px;background:#ffe8c2;color:#1d140e;border-radius:999px;padding:24px 30px;font:700 30px Inter}
.pill em{font-style:normal;color:#d9572a;font:800 40px "Baloo 2"}
.ans{display:flex;justify-content:space-between;align-items:center;background:#1f160f;color:#fff1dc;border-radius:30px;padding:26px 28px;font:500 32px Inter}
.ans b{color:#f07a2e;font:800 36px Inter}
.pal-side{position:absolute;right:-110px;top:-120px}
.plan{border:2px solid #3a2a1d;border-radius:30px;padding:24px 26px;display:flex;justify-content:space-between;align-items:center;color:#fff1dc;font:800 34px Inter}
.plan.on{border:3px solid #f07a2e}
.plan small{display:block;font:500 22px Inter;color:#bfae98;margin-top:4px}
.plan i{font-style:normal;font:700 20px Inter;background:#ffe8c2;color:#1d140e;border-radius:999px;padding:4px 12px;margin-left:10px;vertical-align:4px}
.cta{background:linear-gradient(#fff3dc,#ffd9a0);color:#1d140e;border-radius:999px;text-align:center;font:800 34px Inter;padding:28px;box-shadow:0 9px 0 #d9572a}
.ben{display:flex;gap:16px;align-items:center;color:#fff1dc;font:600 30px Inter}
.ben i{font-style:normal;flex:none;width:42px;height:42px;border-radius:50%;background:#f07a2e;color:#1d140e;display:flex;align-items:center;justify-content:center;font:800 24px Inter}
`;

const screen = (inner, head = true) => `<div class="big"><div class="scr">${head ? `<div class="hd">${P('happy', 'none', null, 88)}<div><span>Find<b>Pal</b></span><small>Find anything in two letters</small></div></div>` : ''}${inner}</div></div>`;
const card = (bg, l, t, s) => `<div class="card"><span class="ic" style="background:${bg}">${l}</span><div><b>${t}</b><span>${s}</span></div></div>`;
const top = (t, s, acts) => `<div class="top"><div class="lab">TOP HIT</div><div class="t">${t}</div><div class="sub2">${s}</div><div class="acts">${acts}</div></div>`;
const sbar = (q, en) => `<div class="sbar"><span>🔍</span><span class="q">${q}</span><span class="en">${en}</span></div>`;

const shots = [
  ['screenshot-1', 'dark', `<h1>Find anything.<br><span class="hl">In two letters.</span></h1>
    <div class="sub">Apps, people, files and settings, right above your thumb.</div>
    <div class="stage">${screen(`${card('#3a9d8f', 'W', 'Weather', 'App')}${card('#5b6cd9', 'W', 'Wallet', 'App')}
      ${top('<em>Wh</em>atsApp', 'App · opened 14 times today', '<span>Open</span><span>App info</span><span>Play Store</span>')}
      <div class="chips2"><span class="on">All</span><span>Apps 3</span><span>Contacts 2</span><span>Files 1</span></div>${sbar('wh', '↵ Open')}`)}
      <div class="pal-side">${P('stars', 'cheer', 'sparkle', 300)}</div></div>`],
  ['screenshot-2', 'cream', `<h1>Type in English.<br><span class="hl">Find in हिंदी.</span></h1>
    <div class="sub">Contacts in 9 Indian scripts match their English spelling.</div>
    <div class="stage">${screen(`${card('#e8743b', 'रा', 'राहुल पटेल', 'Contact · +91 99251 22014')}${card('#d9485f', 'R', 'Rahul Office', 'Contact')}
      ${top('राहुल शर्मा', 'Contact · matched “rahul”', '<span>Call</span><span>SMS</span><span class="wa">WhatsApp</span>')}
      ${sbar('rahul', '↵ Call')}`)}</div>`],
  ['screenshot-3', 'orange', `<h1>Your OTP.<br><span class="hl">One tap away.</span></h1>
    <div class="sub">The latest code waits at the top, ready to copy.</div>
    <div class="stage">${screen(`<div class="pill">🔑 Copy OTP <em>482 913</em> · HDFC</div>
      ${card('#f2b54a', 'Rs', 'Paytm', 'Recent app')}${card('#2f8fd8', 'P', 'PhonePe', 'Recent app')}
      ${sbar('Search…', '')}`)}</div>`],
  ['screenshot-4', 'dark', `<h1>A search bar<br><span class="hl">that does maths.</span></h1>
    <div class="sub">Sums, currency, units, timers and alarms. Just type.</div>
    <div class="stage">${screen(`<div class="ans">2400*15%<b>360</b></div><div class="ans">100 usd in inr<b>₹ live rate</b></div>
      <div class="ans">5 ft in cm<b>152.4 cm</b></div><div class="ans">timer 10 min<b>⏱ Start</b></div>${sbar('timer 10 min', '↵ Start')}`)}</div>`],
  ['screenshot-5', 'cream', `<h1>Search by meaning.<br><span class="hl">“payment” finds UPI.</span></h1>
    <div class="sub">A small AI model on your phone knows what your apps are for.</div>
    <div class="stage">${screen(`${card('#2f8fd8', 'Pt', 'Paytm', 'App · “payment”')}${card('#3a9d8f', 'G', 'Google Pay', 'App · “payment”')}${card('#e0567a', 'B', 'BHIM', 'App · “payment”')}
      ${top('PhonePe', 'App · matched by meaning', '<span>Open</span><span>App info</span><span>Play Store</span>')}${sbar('payment', '↵ Open')}`)}</div>`],
  ['screenshot-6', 'dark', `<h1 style="font-size:84px">Your phone. Your data.<br><span class="hl">Pal never tells.</span></h1>
    <div class="sub">Searched on your phone, never uploaded. No ads. No tracking.</div>
    <div class="stage" style="margin-top:110px">${P('cool', 'point', 'sparkle', 520)}</div>
    <div style="display:flex;flex-direction:column;gap:26px;margin-top:70px;text-align:left">
      ${['Everything is searched on your phone', 'Only names are indexed, never file contents', 'No ads, no analytics, no tracking', 'Every permission is optional']
        .map((t) => `<div class="ben" style="font-size:38px"><i style="width:56px;height:56px;font-size:30px">✓</i>${t}</div>`).join('')}
    </div>`],
  ['screenshot-7', 'dark', `<h1>Try Pro free<br><span class="hl">for 7 days.</span></h1>
    <div class="sub">Free searches your contacts. Pro searches your whole phone.</div>
    <div class="stage">${screen(`<div style="text-align:center">${P('cool', 'point', null, 200)}</div>
      ${['Apps, files, settings and calendar', 'WhatsApp, SMS, Telegram and email', 'OTP copy and quick answers', 'Search by meaning'].map((t) => `<div class="ben"><i>✓</i>${t}</div>`).join('')}
      <div class="plan on"><div>Yearly<i>Save 33%</i><small>7 days free · $3.33 a month</small></div>$39.99/yr</div>
      <div class="plan"><div>Monthly<small>7 days free</small></div>$4.99/mo</div>
      <div class="cta">Start 7-day free trial</div>`, false)}</div>`],
];

const feature = `<div style="width:1024px;height:500px;position:relative;overflow:hidden;background:radial-gradient(80% 90% at 15% 30%,#4a2a12,#120d09 70%);display:flex;align-items:center;padding:0 60px;gap:20px">
  <div style="flex:none;margin-left:-20px">${P('happy', 'wave', 'sparkle', 380)}</div>
  <div style="color:#fff1dc">
    <div style="font:800 96px/1 'Baloo 2'">Find<span style="color:#f07a2e">Pal</span></div>
    <div style="font:800 46px/1.05 'Inter Display',Inter;letter-spacing:-.03em;margin-top:14px">Search your whole phone<br><span style="color:#ffc48a">in two letters.</span></div>
    <div style="margin-top:26px;display:inline-flex;align-items:center;gap:12px;background:#1f160f;border-radius:20px;padding:14px 20px;font:500 24px Inter;box-shadow:0 0 0 2px #3a2a1d">🔍 wh<span style="width:2px;height:26px;background:#f07a2e"></span><span style="margin-left:18px;background:#ffe8c2;color:#1d140e;border-radius:12px;padding:6px 14px;font-weight:800">WhatsApp</span></div>
  </div></div>`;

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1080, height: 1920 }, deviceScaleFactor: 1 });
  for (const [name, bg, html] of shots) {
    await page.setContent(`<!doctype html><html><head><meta charset="utf-8"><style>${css}</style></head><body><div class="shot ${bg}">${html}</div></body></html>`);
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({ path: path.join(out, `${name}.png`), clip: { x: 0, y: 0, width: 1080, height: 1920 } });
  }
  await page.setViewportSize({ width: 1024, height: 500 });
  await page.setContent(`<!doctype html><html><head><meta charset="utf-8"><style>${css}</style></head><body>${feature}</body></html>`);
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({ path: path.join(out, 'feature-graphic.png'), clip: { x: 0, y: 0, width: 1024, height: 500 } });
  fs.copyFileSync(path.join(__dirname, '..', 'brand', 'findpal-icon-512.png'), path.join(out, 'icon-512.png'));
  // Overview sheet for review.
  const files = shots.map(([n]) => n);
  const thumbs = files.map((n) => `<img src="data:image/png;base64,${fs.readFileSync(path.join(out, n + '.png')).toString('base64')}" style="width:270px;height:480px;border-radius:12px">`).join('');
  const sheet = await browser.newPage({ viewport: { width: 2030, height: 1100 } });
  await sheet.setContent(`<body style="margin:0;background:#0b0806;padding:24px;display:flex;flex-wrap:wrap;gap:16px">${thumbs}<img src="data:image/png;base64,${fs.readFileSync(path.join(out, 'feature-graphic.png')).toString('base64')}" style="width:1024px;height:500px;border-radius:12px"></body>`);
  await sheet.screenshot({ path: path.join(out, 'overview.png'), fullPage: true });
  await browser.close();
})();
