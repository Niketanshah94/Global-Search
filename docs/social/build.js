// Renders the FindPal Instagram launch series (1080x1350, 4:5) into docs/social/instagram/.
//   node docs/social/build.js        (needs Playwright with Chromium)
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const out = path.join(__dirname, 'instagram');
fs.mkdirSync(out, { recursive: true });

const { P, css, logo, phone } = require('./common.js');
const foot = (dark = true, n) => `<div class="foot">${logo(dark)}<span class="soon">${n ? n + ' / 10 · ' : ''}Coming soon on Google Play</span></div>`;

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
