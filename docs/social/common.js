// Shared styles and helpers for the FindPal social image scripts.
const fs = require('fs');
const path = require('path');
const { pal } = require('../brand/source/mascot.js');
const root = path.join(__dirname, '..', '..');
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
const phone = (inner) => `<div class="phone"><div class="screen"><div class="head">${P('happy', 'none', null, 64)}<span>Find<b>Pal</b></span></div>${inner}</div></div>`;

module.exports = { P, css, logo, phone };
