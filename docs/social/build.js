// Renders the FindPal Instagram launch series (1080x1350, 4:5) into docs/social/instagram/.
//   node docs/social/build.js        (needs Playwright with Chromium)
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const { pal } = require('../brand/source/mascot.js');

const root = path.join(__dirname, '..', '..');
const out = path.join(__dirname, 'instagram');
fs.mkdirSync(out, { recursive: true });
const baloo = fs.readFileSync(path.join(root, 'core/ui/src/main/res/font/baloo2_extrabold.ttf')).toString('base64');

const P = (face, arm, extra, size) => pal({ face, arm, extra, size });

const css = `
@font-face{font-family:"Baloo 2";font-weight:800;src:url(data:font/ttf;base64,${baloo}) format("truetype")}
*{box-sizing:border-box;margin:0;padding:0}
body{width:1080px;height:1350px;overflow:hidden;font-family:Inter,system-ui,sans-serif}
.post{position:relative;width:1080px;height:1350px;padding:84px 80px;display:flex;flex-direction:column;overflow:hidden}
.dark{background:radial-gradient(70% 55% at 20% 0%,#3b220f,#120d09 70%);color:#fff1dc}
.cream{background:#ffe8c2;color:#1d140e}
.orange{background:linear-gradient(150deg,#ffb067,#f07a2e 55%,#d9572a);color:#1d140e}
h1{font:800 104px/0.98 "Inter Display","Inter",sans-serif;letter-spacing:-.04em}
h2{font:800 78px/1 "Inter Display","Inter",sans-serif;letter-spacing:-.035em}
.hl{color:#ffc48a}
.cream .hl,.orange .hl{color:#d9572a}
.orange .hl{color:#fff3dc}
.sub{font:500 36px/1.35 Inter,sans-serif;margin-top:28px;opacity:.82;max-width:880px}
.eyebrow{font:600 26px Inter;letter-spacing:.18em;text-transform:uppercase;color:#f7a35c;margin-bottom:26px}
.cream .eyebrow{color:#b8571f}
.foot{position:absolute;left:80px;right:80px;bottom:64px;display:flex;align-items:center;justify-content:space-between;font:600 26px Inter}
.logo{display:flex;align-items:center;gap:14px;font:800 44px "Baloo 2";line-height:1}
.logo b{color:#f07a2e}
.orange .logo b{color:#fff3dc}
.logo .tile{width:62px;height:62px;border-radius:16px;background:radial-gradient(80% 80% at 30% 20%,#3a2414,#120d09);display:flex;align-items:center;justify-content:center;overflow:hidden}
.logo .tile svg{width:60px;height:auto;margin:6px 0 0 3px}
.soon{opacity:.75}
.bubble{display:inline-block;background:#ffe8c2;color:#1d140e;font:800 42px "Baloo 2";padding:16px 34px;border-radius:999px;box-shadow:0 8px 0 #c7682b}
.dark .bubble{box-shadow:0 8px 0 #c7682b}
.cream .bubble{background:#120d09;color:#fff1dc;box-shadow:0 8px 0 #3a2414}
.phone{width:470px;border-radius:64px;background:#0b0806;padding:14px;box-shadow:0 0 0 12px #2a1d14,0 50px 90px rgba(0,0,0,.45)}
.screen{border-radius:52px;background:#120d09;padding:30px 22px 26px;display:flex;flex-direction:column;gap:14px;height:720px;justify-content:flex-end}
.screen .head{display:flex;align-items:center;gap:12px;margin-bottom:auto;font:800 40px "Baloo 2";color:#fff1dc}
.screen .head b{color:#f07a2e}
.screen .head svg{width:64px;height:auto}
.card{background:#1f160f;color:#fff1dc;border-radius:26px;padding:18px 20px;display:flex;gap:16px;align-items:center}
.card .ic{width:56px;height:56px;border-radius:16px;flex:none;display:flex;align-items:center;justify-content:center;font:800 22px Inter;color:#fff}
.card b{display:block;font:700 26px Inter}
.card span{font:500 19px Inter;color:#bfae98}
.top{background:#ffe8c2;color:#1d140e;border-radius:30px;padding:20px}
.top .lab{font:700 16px Inter;letter-spacing:.14em;color:#b8571f}
.top .t{font:800 42px "Inter Display",Inter;letter-spacing:-.02em;margin-top:2px}
.top .t em{font-style:normal;color:#d9572a}
.acts{display:flex;gap:10px;margin-top:14px}
.acts span{flex:1;border-radius:18px;background:rgba(29,20,14,.08);font:700 19px Inter;text-align:center;padding:14px 4px}
.acts .wa{background:#1f3d2c;color:#9ff0bd}
.sbar{background:#1f160f;color:#fff1dc;border-radius:24px;padding:20px 22px;display:flex;align-items:center;gap:12px;font:500 28px Inter;box-shadow:0 0 0 2px #3a2a1d}
.sbar .q{flex:1}
.sbar .q:after{content:"";display:inline-block;width:3px;height:30px;background:#f07a2e;margin-left:3px;vertical-align:-5px}
.sbar .en{font:600 17px Inter;color:#bfae98;background:#2a1d14;border-radius:10px;padding:6px 10px}
.chip{display:inline-flex;align-items:center;gap:14px;background:#120d09;color:#fff1dc;font:700 34px Inter;padding:22px 34px;border-radius:999px}
.num{font:800 44px "Baloo 2";color:#f07a2e}
`;

const logo = (dark = true) => `<div class="logo" style="color:${dark ? '#fff1dc' : '#1d140e'}"><span class="tile">${P('happy', 'none', null, 60)}</span><span>Find<b>Pal</b></span></div>`;
const foot = (dark = true, n) => `<div class="foot">${logo(dark)}<span class="soon">${n ? n + ' / 10 · ' : ''}Coming soon on Google Play</span></div>`;
const phone = (inner) => `<div class="phone"><div class="screen"><div class="head">${P('happy', 'none', null, 64)}Find<b>Pal</b></div>${inner}</div></div>`;

const posts = [
  // 1 Meet Pal
  ['01-meet-pal', `<div class="post dark">
    <p class="eyebrow">Meet your new pal</p>
    <h1>Lost something?<br><span class="hl">Ask your Pal.</span></h1>
    <p class="sub">Pal finds anything on your Android phone. Apps, people, files, OTPs and settings, in two letters.</p>
    <div style="position:absolute;right:40px;bottom:150px;display:flex;flex-direction:column;align-items:center;gap:20px">
      <span class="bubble">Hi! I find your stuff.</span>${P('happy', 'wave', null, 560)}</div>
    ${foot(true, 1)}</div>`],
  // 2 Two letters
  ['02-two-letters', `<div class="post cream">
    <h1>Find anything.<br><span class="hl">In two letters.</span></h1>
    <div style="display:flex;justify-content:center;margin-top:56px">${phone(`
      <div class="card"><span class="ic" style="background:#3a9d8f">W</span><div><b>Weather</b><span>App</span></div></div>
      <div class="top"><div class="lab">TOP HIT</div><div class="t"><em>Wh</em>atsApp</div><div class="acts"><span>Open</span><span>App info</span><span>Play Store</span></div></div>
      <div class="sbar"><span class="q">wh</span><span class="en">↵ Open</span></div>`)}</div>
    <div style="position:absolute;right:56px;top:470px">${P('stars', 'cheer', 'sparkle', 250)}</div>
    ${foot(false, 2)}</div>`],
  // 3 Hindi
  ['03-hindi-names', `<div class="post dark">
    <p class="eyebrow">Made for India</p>
    <h1>Type in English.<br><span class="hl">Find in हिंदी.</span></h1>
    <p class="sub">Contacts saved in 9 Indian scripts show up when you type their English spelling.</p>
    <div style="display:flex;justify-content:center;margin-top:50px;transform:scale(.92);transform-origin:top center">${phone(`
      <div class="card"><span class="ic" style="background:#e8743b">रा</span><div><b>राहुल पटेल</b><span>Contact · +91 99251 22014</span></div></div>
      <div class="top"><div class="lab">TOP HIT</div><div class="t">राहुल शर्मा</div><div class="acts"><span>Call</span><span>SMS</span><span class="wa">WhatsApp</span></div></div>
      <div class="sbar"><span class="q">rahul</span><span class="en">↵ Call</span></div>`)}</div>
    ${foot(true, 3)}</div>`],
  // 4 OTP
  ['04-otp', `<div class="post orange">
    <h1>Your OTP.<br><span class="hl">One tap away.</span></h1>
    <p class="sub" style="opacity:.9">No more switching apps and squinting at 6 digits. Pal keeps your latest code ready to copy.</p>
    <div style="margin-top:80px;display:flex;flex-direction:column;align-items:center;gap:40px">
      <div class="chip"><svg width="40" height="40" viewBox="0 0 24 24"><path fill="#ffc48a" d="M12.65 10A6 6 0 1 0 7 18a6 6 0 0 0 5.65-4H17v4h4v-4h2v-4H12.65zM7 14a2 2 0 1 1 0-4 2 2 0 0 1 0 4z"/></svg>Copy OTP <span class="num">482 913</span> · HDFC</div>
      ${P('stars', 'cheer', 'sparkle', 420)}</div>
    ${foot(false, 4)}</div>`],
  // 5 Time
  ['05-time', `<div class="post dark">
    <h1>Every search<br><span class="hl">eats your time.</span></h1>
    <p class="sub">Drawer. Scroll. Wrong app. Files. Scroll again. Or just ask Pal.</p>
    <div style="display:grid;grid-template-columns:repeat(5,1fr);gap:8px;margin-top:90px;text-align:center">
      ${[['stars', 'cheer', 'sparkle', '1 s', 'FindPal'], ['happy', 'none', null, '5 s', 'drawer'], ['look', 'none', 'sweat', '10 s', 'wrong app'], ['worried', 'none', null, '15 s', 'Files'], ['sleepy', 'none', 'zzz', '20 s+', 'give up']]
        .map(([f, a, e, t, l]) => `<div>${P(f, a, e, 170)}<div style="font:800 54px 'Baloo 2';margin-top:8px">${t}</div><div style="font:500 26px Inter;color:#bfae98">${l}</div></div>`).join('')}
    </div>
    <div style="height:10px;border-radius:9px;margin-top:40px;background:linear-gradient(90deg,#7acc5a,#f2d27a 45%,#f07a2e 75%,#ef4f4f)"></div>
    ${foot(true, 5)}</div>`],
  // 6 Payment meaning search
  ['06-payment', `<div class="post cream">
    <p class="eyebrow">Search by meaning</p>
    <h1>Type “payment”.<br><span class="hl">Get your UPI apps.</span></h1>
    <p class="sub">A small AI model on your phone knows what your apps are for. “taxi” finds Uber and Ola. “food” finds Swiggy.</p>
    <div style="display:flex;flex-direction:column;gap:18px;margin-top:60px;width:560px">
      ${[['#5f259f', 'Pe', 'PhonePe'], ['#1a73e8', 'G', 'Google Pay'], ['#00baf2', 'P', 'Paytm'], ['#f47920', 'B', 'BHIM']]
        .map(([c, i, n]) => `<div class="card" style="background:#fff;color:#1d140e;box-shadow:0 3px 0 #f0d9b4"><span class="ic" style="background:${c}">${i}</span><div><b>${n}</b><span style="color:#6e5039">App · “payment”</span></div></div>`).join('')}
    </div>
    <div style="position:absolute;right:30px;bottom:140px">${P('cool', 'point', null, 380)}</div>
    ${foot(false, 6)}</div>`],
  // 7 Quick answers
  ['07-quick-answers', `<div class="post dark">
    <h1>The search bar<br><span class="hl">that does maths.</span></h1>
    <p class="sub">And conversions, timers and alarms. Just type.</p>
    <div style="display:flex;flex-direction:column;gap:22px;margin-top:64px;width:640px">
      ${[['2400*15%', '360'], ['100 usd in inr', 'live rate ₹'], ['5 ft in cm', '152.4 cm'], ['timer 10 min', '⏱ started']]
        .map(([q, a]) => `<div class="sbar" style="font-size:34px;padding:26px 28px"><span style="flex:1">${q}</span><span style="font:800 40px 'Baloo 2';color:#f07a2e">${a}</span></div>`).join('')}
    </div>
    <div style="position:absolute;right:40px;bottom:150px">${P('look', 'none', 'sweat', 300)}</div>
    ${foot(true, 7)}</div>`],
  // 8 Privacy
  ['08-privacy', `<div class="post cream">
    <h1>Your phone.<br>Your data.<br><span class="hl">Pal never tells.</span></h1>
    <div style="display:flex;flex-direction:column;gap:22px;margin-top:56px;max-width:620px">
      ${['Everything is searched on your phone', 'Names only, never what’s inside your files', 'No ads. No tracking.', 'Every permission is optional']
        .map((t) => `<div style="display:flex;gap:18px;align-items:center;font:600 34px Inter"><span style="flex:none;width:48px;height:48px;border-radius:50%;background:#120d09;color:#7acc5a;display:flex;align-items:center;justify-content:center;font:800 28px Inter">✓</span>${t}</div>`).join('')}
    </div>
    <div style="position:absolute;right:20px;bottom:140px">${P('cool', 'point', null, 380)}</div>
    ${foot(false, 8)}</div>`],
  // 9 Moods
  ['09-which-pal', `<div class="post dark">
    <h1>Which Pal<br><span class="hl">are you today?</span></h1>
    <p class="sub">Comment your number. 👇</p>
    <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:26px 10px;margin-top:56px;text-align:center">
      ${[['happy', 'wave', null, 'Hello'], ['look', 'none', 'sweat', 'Searching'], ['stars', 'cheer', 'sparkle', 'Found it!'], ['sleepy', 'none', 'zzz', 'Monday'], ['cool', 'point', null, 'Pro'], ['worried', 'none', null, 'Lost my keys']]
        .map(([f, a, e, l], i) => `<div>${P(f, a, e, 230)}<div style="font:800 40px 'Baloo 2';margin-top:4px"><span style="color:#f07a2e">${i + 1}.</span> ${l}</div></div>`).join('')}
    </div>
    ${foot(true, 9)}</div>`],
  // 10 Launch
  ['10-coming-soon', `<div class="post orange" style="align-items:center;text-align:center">
    <p class="eyebrow" style="color:#3a1a06">Launching soon</p>
    <h1>Stop digging.<br><span class="hl">Start finding.</span></h1>
    <div style="margin-top:50px">${P('stars', 'cheer', 'sparkle', 520)}</div>
    <div style="margin-top:30px;display:inline-flex;align-items:center;gap:16px;font:800 40px 'Inter Display',Inter;color:#1d140e;background:linear-gradient(#fff3dc,#ffd9a0);border-radius:999px;padding:26px 48px;box-shadow:0 9px 0 #8a3c14">
      <svg width="34" height="34" viewBox="0 0 24 24"><path d="M5 3l14 9-14 9z" fill="#1d140e"/></svg>Coming soon on Google Play</div>
    <p class="sub" style="margin-top:34px;opacity:.9">Follow to be first when FindPal lands.</p>
    <div class="foot" style="justify-content:center">${logo(false)}</div></div>`],
];

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1080, height: 1350 }, deviceScaleFactor: 1 });
  for (const [name, html] of posts) {
    await page.setContent(`<!doctype html><html><head><meta charset="utf-8"><style>${css}</style></head><body>${html}</body></html>`);
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({ path: path.join(out, `findpal-ig-${name}.png`) });
  }
  // Contact sheet for a quick overview.
  const thumbs = posts.map(([n]) => `<img src="data:image/png;base64,${fs.readFileSync(path.join(out, `findpal-ig-${n}.png`)).toString('base64')}" style="width:324px;height:405px;border-radius:14px">`).join('');
  const sheet = await browser.newPage({ viewport: { width: 1740, height: 900 } });
  await sheet.setContent(`<body style="margin:0;background:#0b0806;padding:30px;display:grid;grid-template-columns:repeat(5,324px);gap:24px">${thumbs}</body>`);
  await sheet.screenshot({ path: path.join(out, 'findpal-ig-overview.png'), fullPage: true });
  await browser.close();
})();
