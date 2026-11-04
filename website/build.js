// Builds the FindPal website from the brand mascot (docs/brand/source/mascot.js).
//   node website/build.js
// Writes website/index.html (a full page for any static host) and, when given a path as the
// first argument, a body-only copy for previews.
const fs = require('fs');
const path = require('path');
const { pal } = require('../docs/brand/source/mascot.js');

const P = (face, arm, extra, size, cls = '') =>
  `<span class="pal ${cls}" aria-hidden="true">${pal({ face, arm, extra, size })}</span>`;

const check = '<span class="y" aria-label="Yes">✓</span>';
const part = (t) => `<span class="p">${t}</span>`;
const no = '<span class="n" aria-label="No">—</span>';

const head = `<title>FindPal</title>
<meta name="description" content="FindPal finds anything on your Android phone in two letters: apps, contacts, files, OTPs and settings. Private and on-device.">
<meta name="claude:viewport-fit-sides" content="cover">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Baloo+2:wght@700;800&family=Inter+Tight:wght@700;800&family=Inter:wght@400;500;600&display=swap">`;

const css = `<style>
/* Layout: one 1080px column; dark brand sections alternate with cream scalloped bands, like the app's brand board. */
:root{
  --night:#120d09; --cocoa:#1f160f; --line:#3a2a1d;
  --cream:#ffe8c2; --peach:#ffc48a; --orange:#f07a2e; --ember:#c7682b;
  --text:#fff1dc; --muted:#bfae98; --ink:#1d140e; --ink-muted:#6e5039; --green:#7acc5a;
  --display:"Baloo 2","Inter Tight",system-ui,sans-serif;
  --head:"Inter Tight",Inter,system-ui,sans-serif;
  --body:Inter,system-ui,-apple-system,"Segoe UI",sans-serif;
  color-scheme:dark;
}
*{box-sizing:border-box}
html{scroll-behavior:smooth}
body{margin:0;background:var(--night);color:var(--text);font:400 17px/1.6 var(--body);-webkit-font-smoothing:antialiased;overflow-x:hidden}
.wrap{max-width:1080px;margin:0 auto;padding-left:max(20px,env(safe-area-inset-left,0px));padding-right:max(20px,env(safe-area-inset-right,0px))}
section{padding-block:88px}
h1,h2,h3{font-family:var(--head);font-weight:800;letter-spacing:-.03em;line-height:1.02;margin:0;text-wrap:balance}
h1{font-size:clamp(46px,8vw,92px)}
h2{font-size:clamp(34px,5.4vw,58px)}
h3{font-size:22px;letter-spacing:-.01em;line-height:1.2}
p{margin:0}
.lede{color:var(--muted);font-size:clamp(17px,2vw,21px);max-width:560px}
.eyebrow{font:600 13px var(--body);letter-spacing:.16em;text-transform:uppercase;color:var(--peach)}
.hl{color:var(--peach)}
a{color:inherit}
:focus-visible{outline:3px solid var(--orange);outline-offset:3px;border-radius:8px}
.pal{display:inline-block;line-height:0}
.pal svg{max-width:100%;height:auto}

/* nav */
header.nav{position:sticky;top:env(safe-area-inset-top,0px);z-index:20;background:rgba(18,13,9,.82);backdrop-filter:blur(12px);border-bottom:1px solid rgba(255,232,194,.06)}
body::before{content:"";position:fixed;inset:0 0 auto;height:env(safe-area-inset-top,0px);background:var(--night);z-index:21}
.nav .wrap{display:flex;align-items:center;justify-content:space-between;height:72px}
.brand{display:flex;align-items:center;gap:12px;text-decoration:none}
.brand .tile{width:44px;height:44px;border-radius:12px;background:radial-gradient(80% 80% at 30% 20%,#3a2414,var(--night));display:flex;align-items:center;justify-content:center;overflow:hidden;box-shadow:inset 0 0 0 1px rgba(255,255,255,.08)}
.brand .tile .pal{width:42px;margin:4px 0 0 2px}
.word{font:800 28px var(--display);letter-spacing:.01em;line-height:1}
.word b{color:var(--orange);font-weight:800}

/* buttons */
.btn{display:inline-flex;align-items:center;gap:12px;font:800 18px var(--head);letter-spacing:-.01em;color:var(--ink);text-decoration:none;background:linear-gradient(#fff3dc,#ffd9a0);border-radius:999px;padding:15px 28px;box-shadow:0 6px 0 var(--ember),0 16px 40px rgba(240,122,46,.22);transition:transform .12s,box-shadow .12s;border:0;cursor:pointer}
.btn:hover{transform:translateY(2px);box-shadow:0 4px 0 var(--ember),0 12px 30px rgba(240,122,46,.22)}
.btn.big{font-size:22px;padding:20px 36px}
.btn.small{font-size:16px;padding:10px 20px;box-shadow:0 4px 0 var(--ember)}
.soon{display:inline-block;margin-left:2px;font:600 12px var(--body);letter-spacing:.08em;text-transform:uppercase;color:var(--peach)}
.trust{display:flex;flex-wrap:wrap;gap:8px 18px;margin-top:22px;color:var(--muted);font-size:15px}
.trust span{display:inline-flex;gap:6px;align-items:center}
.trust b{color:var(--text);font-weight:600}

/* hero */
.hero{padding-block:72px 40px;background:radial-gradient(70% 60% at 20% 0%,rgba(240,122,46,.22),transparent 70%)}
.hero-grid{display:grid;grid-template-columns:1.1fr .9fr;gap:40px;align-items:center}
.hero-grid>*{min-width:0}
.hero h1 .hl{display:block}
.hero .lede{margin-top:22px}
.hero .cta{margin-top:34px}
.stage{position:relative;display:flex;justify-content:center;padding-top:96px}
.stage .pal.hi{position:absolute;left:-6px;top:0;width:170px;z-index:2}
.bubble{position:absolute;left:150px;top:22px;z-index:3;background:var(--cream);color:var(--ink);font:800 19px var(--display);padding:10px 20px;border-radius:999px;box-shadow:0 5px 0 var(--ember);white-space:nowrap}
.bubble:before{content:"";position:absolute;left:-12px;top:50%;margin-top:-9px;border:9px solid transparent;border-right:13px solid var(--cream);border-left:0}

/* phone */
.phone{width:300px;max-width:100%;aspect-ratio:300/600;border-radius:44px;background:#0b0806;box-shadow:0 0 0 9px #2a1d14,0 0 0 10px #3a2a1d,0 40px 80px rgba(0,0,0,.55);overflow:hidden;position:relative;display:flex;flex-direction:column}
.screen{flex:1;background:#f6f3ee;color:#1d1a16;display:flex;flex-direction:column;justify-content:flex-end;padding:14px 12px 16px;gap:8px;font-family:var(--body)}
.screen .top{display:flex;align-items:center;gap:8px;padding:6px 4px}
.screen .top .pal{width:34px}
.screen .top .w{font:800 22px var(--display);line-height:1}
.screen .top .w b{color:var(--orange)}
.stat{display:flex;align-items:baseline;gap:8px;background:#1d1a16;color:#f6f3ee;border-radius:18px;padding:12px 14px;margin:0 0 auto}
.stat .num{font:800 38px var(--display);color:var(--orange);line-height:1}
.stat small{display:block;font-size:12px;color:#bfae98}
.card{background:#fff;border-radius:16px;padding:10px 12px;display:flex;gap:10px;align-items:center;box-shadow:0 1px 2px rgba(0,0,0,.06);font-size:13px}
.card .ic{width:30px;height:30px;border-radius:9px;flex:none;display:flex;align-items:center;justify-content:center;color:#fff;font:800 13px var(--body)}
.card b{display:block;font-size:14px}
.card span{color:#7a7266;font-size:11.5px}
.tophit{background:#1d1a16;color:#f6f3ee;border-radius:20px;padding:12px}
.tophit .lab{font:700 10px var(--body);letter-spacing:.12em;color:#f7c59f}
.tophit .t{font:800 22px var(--head);letter-spacing:-.02em;margin-top:2px}
.tophit .t em{font-style:normal;color:#f7c59f}
.acts{display:flex;gap:6px;margin-top:10px}
.acts span{flex:1;border-radius:12px;background:rgba(255,255,255,.1);font:600 11px var(--body);text-align:center;padding:8px 2px}
.acts .wa{background:#1f3d2c;color:#9ff0bd}
.chips{display:flex;gap:6px;font:600 11px var(--body);overflow:hidden}
.chips span{background:#ebe5dc;color:#4b443a;border-radius:999px;padding:5px 10px;white-space:nowrap}
.chips span.on{background:#1d1a16;color:#fff}
.sbar{background:#fff;border-radius:14px;padding:11px 14px;display:flex;align-items:center;gap:8px;font:500 15px var(--body);box-shadow:0 6px 18px rgba(60,40,10,.14)}
.sbar .q{flex:1;white-space:nowrap;overflow:hidden}
.sbar .q:after{content:"";display:inline-block;width:2px;height:16px;background:var(--orange);margin-left:2px;vertical-align:-3px;animation:caret 1s steps(1) infinite}
.sbar .enter{font:600 10px var(--body);color:#7a7266;background:#f1ece4;border-radius:6px;padding:3px 6px;white-space:nowrap}
@keyframes caret{50%{opacity:0}}

/* cream bands */
.cream{background:var(--cream);color:var(--ink)}
.cream .lede{color:var(--ink-muted)}
.cream .eyebrow{color:#b8571f}
.cream .hl{color:var(--orange)}
.scallop{height:40px;background:radial-gradient(circle at 20px 0,var(--cream) 19px,transparent 20px) 0 0/40px 40px repeat-x}
.scallop.into{background:radial-gradient(circle at 20px 40px,var(--cream) 19px,transparent 20px) 0 0/40px 40px repeat-x}

/* time strip */
.strip{display:grid;grid-template-columns:repeat(5,1fr);gap:12px;margin-top:44px;text-align:center}
.strip>div{min-width:0}
.strip .pal{width:min(110px,100%)}
.strip b{display:block;font:800 30px var(--display);margin-top:6px}
.strip span{color:var(--muted);font-size:14px}
.meter{height:6px;border-radius:9px;margin-top:22px;background:linear-gradient(90deg,var(--green),#f2d27a 45%,var(--orange) 75%,#ef4f4f)}
.calc{margin-top:56px;display:grid;grid-template-columns:1fr auto;gap:28px;align-items:center;background:var(--cocoa);border:1px solid var(--line);border-radius:28px;padding:30px}
.calc>*{min-width:0}
.calc label{font:600 18px var(--head);display:block}
.calc input[type=range]{width:100%;margin-top:18px;accent-color:var(--orange)}
.calc .out{text-align:right}
.calc .out b{display:block;font:800 clamp(48px,7vw,72px) var(--display);color:var(--orange);line-height:1;font-variant-numeric:tabular-nums}
.calc small{color:var(--muted);font-size:13px;display:block;margin-top:10px}

/* comparison */
.table-wrap{overflow-x:auto;margin-top:40px;border-radius:24px;background:#fff6e6;box-shadow:0 2px 0 rgba(0,0,0,.04)}
table{border-collapse:collapse;width:100%;min-width:620px;font-size:16px}
th,td{padding:16px 18px;text-align:left;border-bottom:1px solid #f0d9b4}
th{font:700 13px var(--body);letter-spacing:.08em;text-transform:uppercase;color:var(--ink-muted)}
th.us,td.us{background:#ffdcae}
th.us{color:var(--ink)}
td:not(:first-child),th:not(:first-child){text-align:center;width:150px}
tr:last-child td{border-bottom:0}
.y{color:#2e7d4f;font-weight:800;font-size:20px}
.p{color:#9a5a22;font-size:13px;font-weight:600}
.n{color:#c9b394;font-weight:800}
.foot{font-size:13px;color:var(--ink-muted);margin-top:14px}

/* features */
.grid{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-top:44px}
.f{background:var(--cocoa);border:1px solid var(--line);border-radius:24px;padding:22px;min-width:0}
.f .ico{width:46px;height:46px;border-radius:14px;background:linear-gradient(145deg,#ffb067,var(--orange) 55%,#d9572a);display:flex;align-items:center;justify-content:center;margin-bottom:16px}
.f p{color:var(--muted);font-size:15px;margin-top:8px}
.f .ex{display:inline-block;margin-top:12px;font:500 13px var(--body);color:var(--peach);background:rgba(255,196,138,.08);border-radius:8px;padding:3px 8px}

/* india */
.two{display:grid;grid-template-columns:1fr 1fr;gap:48px;align-items:center}
.two>*{min-width:0}
.ilist{display:flex;flex-direction:column;gap:14px;margin-top:28px}
.ilist div{display:flex;gap:14px;align-items:flex-start;font-size:17px}
.ilist i{font-style:normal;flex:none;width:30px;height:30px;border-radius:50%;background:var(--orange);color:#fff;display:flex;align-items:center;justify-content:center;font:800 15px var(--body)}

/* privacy */
.priv{display:grid;grid-template-columns:repeat(2,1fr);gap:16px;margin-top:40px}
.priv>div{background:var(--cocoa);border:1px solid var(--line);border-radius:22px;padding:22px}
.priv p{color:var(--muted);font-size:15px;margin-top:6px}

/* pricing */
.prices{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-top:44px;align-items:stretch}
.price{position:relative;background:var(--cocoa);border:1px solid var(--line);border-radius:30px;padding:32px;min-width:0}
.price.pro{border-color:#b8925f;background:#221811}
.price .tier{font:700 13px var(--body);letter-spacing:.14em;text-transform:uppercase;color:var(--muted)}
.price.pro .tier{color:var(--peach)}
.price .amt{font:800 64px var(--display);line-height:1;margin-top:12px}
.price .amt small{font:500 16px var(--body);color:var(--muted);margin-left:6px}
.price ul{list-style:none;padding:0;margin:22px 0 0;display:flex;flex-direction:column;gap:12px}
.price li{display:flex;gap:10px;font-size:16px}
.price li:before{content:"";flex:none;width:8px;height:8px;border-radius:50%;background:var(--muted);margin-top:9px}
.price.pro li:before{background:var(--peach)}
.price .peek{position:absolute;right:18px;top:-74px;width:120px}

/* faq */
.faqhead{display:flex;align-items:center;gap:18px}
.faqhead .pal{width:96px}
details{border-bottom:1px solid var(--line);padding:22px 0}
details:first-of-type{border-top:1px solid var(--line);margin-top:30px}
summary{cursor:pointer;list-style:none;display:flex;justify-content:space-between;gap:16px;font:700 clamp(18px,2.4vw,22px) var(--head);letter-spacing:-.01em}
summary::-webkit-details-marker{display:none}
summary:after{content:"+";color:var(--peach);font-weight:500;font-size:28px;line-height:1;transition:transform .2s}
details[open] summary:after{transform:rotate(45deg)}
details p{color:var(--muted);margin-top:12px;max-width:720px}

/* final */
.final{text-align:center;background:radial-gradient(60% 70% at 50% 100%,rgba(240,122,46,.2),transparent 70%)}
.final .pal{width:220px}
.final h2{margin-top:10px}
.final .btn{margin-top:30px}
footer{border-top:1px solid var(--line);padding-block:40px;color:var(--muted);font-size:15px}
footer .wrap{display:flex;flex-wrap:wrap;gap:16px 32px;justify-content:space-between}
footer nav{display:flex;flex-wrap:wrap;gap:12px 24px}
footer a{text-decoration:none}
footer a:hover{color:var(--text)}

@media (max-width:900px){
  .hero-grid,.two{grid-template-columns:1fr}
  .grid{grid-template-columns:repeat(2,1fr)}
  .stage{padding-top:110px}
}
@media (max-width:560px){
  section{padding-block:64px}
  .grid,.priv,.prices{grid-template-columns:1fr}
  .strip{grid-template-columns:repeat(5,1fr);gap:4px}
  .strip b{font-size:20px}
  .strip span{font-size:11px}
  .calc{grid-template-columns:1fr}
  .calc .out{text-align:left}
  .stage .pal.hi{width:130px;left:0}
  .bubble{left:116px;top:16px;font-size:16px}
  .nav .btn.small{padding:8px 14px;font-size:14px}
  .word{font-size:24px}
  .btn.big{font-size:18px;padding:16px 24px}
  .price .peek{width:96px;top:-58px}
}

/* Pal motion: float, wave, blink, sparkle, snooze. All parts are SVG groups from mascot.js. */
.pal svg .eyes,.pal svg .arms,.pal svg .extra{transform-box:fill-box}
.pal svg .eyes{transform-origin:50% 50%;animation:blink 5s infinite}
.pal svg .pupils{transition:transform .15s ease-out;transform-box:fill-box}
.pal svg .arms-wave{transform-origin:92% 96%;animation:wave 2.4s ease-in-out infinite}
.pal svg .arms-point{transform-origin:8% 96%;animation:nudge 3s ease-in-out infinite}
.pal svg .arms-cheer{transform-origin:50% 100%;animation:cheer 1.2s ease-in-out infinite}
.pal svg .extra-sparkle{transform-origin:50% 50%;animation:twinkle 1.6s ease-in-out infinite}
.pal svg .extra-zzz{animation:zzz 2.8s ease-in-out infinite}
.pal svg .extra-sweat{animation:sweat 2s ease-in infinite}
.pal svg .eyes-sleepy,.pal svg .eyes-cool,.pal svg .eyes-stars{animation:none}
@keyframes blink{0%,46%,50%,100%{transform:scaleY(1)}48%{transform:scaleY(.08)}}
@keyframes wave{0%,100%{transform:rotate(0)}15%{transform:rotate(-14deg)}30%{transform:rotate(10deg)}45%{transform:rotate(-12deg)}60%{transform:rotate(0)}}
@keyframes nudge{0%,70%,100%{transform:rotate(0)}80%{transform:rotate(-10deg)}90%{transform:rotate(4deg)}}
@keyframes cheer{0%,100%{transform:translateY(0)}50%{transform:translateY(-6px)}}
@keyframes twinkle{0%,100%{opacity:1;transform:scale(1)}50%{opacity:.4;transform:scale(.7)}}
@keyframes zzz{0%{opacity:0;transform:translate(0,8px)}30%{opacity:1}100%{opacity:0;transform:translate(10px,-14px)}}
@keyframes sweat{0%{transform:translateY(0);opacity:1}100%{transform:translateY(10px);opacity:0}}
@keyframes float{0%,100%{transform:translateY(0)}50%{transform:translateY(-10px)}}
@keyframes breathe{0%,100%{transform:scale(1)}50%{transform:scale(1.04)}}
@keyframes wobble{0%,100%{transform:rotate(0)}25%{transform:rotate(-6deg)}75%{transform:rotate(6deg)}}
@keyframes hop{0%{transform:translateY(0) scale(1)}30%{transform:translateY(-28px) scale(1.06,.96)}60%{transform:translateY(0) scale(.96,1.05)}100%{transform:translateY(0) scale(1)}}
@keyframes pop{0%{transform:scale(.6);opacity:0}70%{transform:scale(1.06);opacity:1}100%{transform:scale(1)}}
@keyframes peek{0%,100%{transform:translateY(0) rotate(0)}50%{transform:translateY(-8px) rotate(-4deg)}}
@keyframes bounceIn{0%{transform:translateY(0) scale(1)}35%{transform:translateY(-22px) scale(1.05)}65%{transform:translateY(0) scale(.97)}100%{transform:none}}
.stage .pal.hi{animation:float 4s ease-in-out infinite;cursor:pointer}
.stage .pal.hi.hop{animation:hop .6s ease-out}
.bubble{animation:pop .5s .3s both cubic-bezier(.3,1.4,.6,1)}
.price .peek{animation:peek 3.4s ease-in-out infinite}
.faqhead .pal{animation:wobble 3s ease-in-out infinite;transform-origin:50% 90%}
.final .pal{animation:breathe 3.6s ease-in-out infinite;transform-origin:50% 80%}
.priv-pal .pal{animation:float 5s ease-in-out infinite}
.strip .pal{transform-origin:50% 100%}
.strip.in>div:nth-child(1) .pal{animation:bounceIn .7s .0s both}
.strip.in>div:nth-child(2) .pal{animation:bounceIn .7s .12s both}
.strip.in>div:nth-child(3) .pal{animation:bounceIn .7s .24s both}
.strip.in>div:nth-child(4) .pal{animation:bounceIn .7s .36s both}
.strip.in>div:nth-child(5) .pal{animation:bounceIn .7s .48s both}
@media (prefers-reduced-motion:reduce){
  html{scroll-behavior:auto}.sbar .q:after{animation:none}.btn{transition:none}
  .pal,.pal *,.bubble,.strip .pal{animation:none!important;transition:none!important}
}
</style>`;

const ico = (d) => `<svg width="24" height="24" viewBox="0 0 24 24" fill="#fff" aria-hidden="true"><path d="${d}"/></svg>`;
const I = {
  person: 'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z',
  key: 'M12.65 10A6 6 0 1 0 7 18a6 6 0 0 0 5.65-4H17v4h4v-4h2v-4H12.65zM7 14a2 2 0 1 1 0-4 2 2 0 0 1 0 4z',
  folder: 'M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z',
  bell: 'M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4a1.5 1.5 0 0 0-3 0v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z',
  calc: 'M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5.97 4.06L14.09 6l1.41 1.41L16.91 6l1.06 1.06-1.41 1.41 1.41 1.41-1.06 1.06-1.41-1.4-1.41 1.41-1.06-1.06 1.41-1.41-1.41-1.42zM6.25 7.72h5v1.5h-5v-1.5zM11.5 16h-2v2H8v-2H6v-1.5h2v-2h1.5v2h2V16zm6.5 1.25h-5v-1.5h5v1.5zm0-2.5h-5v-1.5h5v1.5z',
  spark: 'M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5z',
  thumb: 'M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L14.17 1 7.59 7.59C7.22 7.95 7 8.45 7 9v10c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z',
  tag: 'M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58.55 0 1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41 0-.55-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z',
};

const page = `
<header class="nav"><div class="wrap">
  <a class="brand" href="#top" aria-label="FindPal home"><span class="tile">${P('happy', 'none', null, 42)}</span><span class="word">FIND<b>PAL</b></span></a>
  <a class="btn small" href="#download">Get the app</a>
</div></header>

<main id="top">
<section class="hero"><div class="wrap hero-grid">
  <div>
    <p class="eyebrow">Universal search for Android</p>
    <h1 style="margin-top:18px">Find anything. <span class="hl">In two letters.</span></h1>
    <p class="lede">FindPal searches your whole phone from one bar: apps, people, files, OTPs, settings and quick sums. Type two letters, and the right thing is already under your thumb.</p>
    <div class="cta" id="download"><a class="btn big" href="#download" aria-label="Coming soon on Google Play">
      <svg width="24" height="24" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 3l14 9-14 9z" fill="#1d140e"/></svg>Coming soon on Google Play</a></div>
    <div class="trust"><span><b>Free</b> core search</span><span>·</span><span><b>No ads</b>, no tracking</span><span>·</span><span>Works <b>offline</b></span></div>
  </div>
  <div class="stage">
    ${P('happy', 'wave', null, 170, 'hi')}
    <div class="bubble">Hi! I find your stuff.</div>
    <div class="phone" role="img" aria-label="FindPal on a phone: typing wh shows WhatsApp as the top hit">
      <div class="screen">
        <div class="top">${P('happy', 'none', null, 34)}<span class="w">Find<b>Pal</b></span></div>
        <div class="stat"><span class="num" id="found">38</span><span>things found today<small>about 12 min saved</small></span></div>
        <div class="card" id="r2"><span class="ic" style="background:#e8743b">RP</span><div><b>Rahul Patel</b><span>Contact · +91 99251 22014</span></div></div>
        <div class="tophit"><div class="lab">TOP HIT</div><div class="t" id="tt"><em>Wh</em>atsApp</div>
          <div class="acts" id="ta"><span>Open</span><span>App info</span><span>Play Store</span></div></div>
        <div class="chips"><span class="on">All</span><span>Apps 2</span><span>Contacts 1</span><span>Web</span></div>
        <div class="sbar"><svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true"><path fill="#1d1a16" d="M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg><span class="q" id="q">wh</span><span class="enter" id="en">↵ Open</span></div>
      </div>
    </div>
  </div>
</div></section>

<section><div class="wrap">
  <h2>Every search <span class="hl">eats your time.</span></h2>
  <p class="lede" style="margin-top:18px">Finding one file the usual way: open the drawer, scroll, open the wrong app, go back, open Files, scroll again. Pal does it in about a second.</p>
  <div class="strip">
    <div>${P('stars', 'cheer', 'sparkle', 110)}<b>1 s</b><span>with FindPal</span></div>
    <div>${P('happy', 'none', null, 110)}<b>5 s</b><span>scroll the drawer</span></div>
    <div>${P('look', 'none', 'sweat', 110)}<b>10 s</b><span>open the wrong app</span></div>
    <div>${P('worried', 'none', null, 110)}<b>15 s</b><span>dig through Files</span></div>
    <div>${P('sleepy', 'none', 'zzz', 110)}<b>20 s+</b><span>give up</span></div>
  </div>
  <div class="meter" aria-hidden="true"></div>
  <div class="calc">
    <div><label for="n">How many times a day do you look for something on your phone?</label>
      <input id="n" type="range" min="5" max="60" step="1" value="20" aria-describedby="note">
      <small id="note">Estimate: FindPal saves about 8 seconds per search, 30 days a month.</small></div>
    <div class="out" aria-live="polite"><b id="mins">80</b><span>minutes saved a month</span></div>
  </div>
</div></section>

<div class="scallop into"></div>
<section class="cream"><div class="wrap">
  <p class="eyebrow">iPhone vs Android vs FindPal</p>
  <h2 style="margin-top:14px">Things your phone <span class="hl">can't do.</span></h2>
  <p class="lede" style="margin-top:18px">iPhone's Spotlight covers the basics. Android's built-in search covers less. FindPal does both, and a lot neither of them does.</p>
  <div class="table-wrap"><table>
    <thead><tr><th>Feature</th><th>iPhone Spotlight</th><th>Android search</th><th class="us">FindPal</th></tr></thead>
    <tbody>
      <tr><td>Apps, contacts and settings in one bar</td><td>${check}</td><td>${part('Varies by phone')}</td><td class="us">${check}</td></tr>
      <tr><td>Calculator, units and currency</td><td>${check}</td><td>${part('Via Google app')}</td><td class="us">${check}</td></tr>
      <tr><td>Files and folders by name</td><td>${check}</td><td>${part('Some phones')}</td><td class="us">${check}</td></tr>
      <tr><td>Type “rahul”, find “राहुल” (9 Indian scripts)</td><td>${no}</td><td>${no}</td><td class="us">${check}</td></tr>
      <tr><td>Search notifications from the last 7 days</td><td>${no}</td><td>${no}</td><td class="us">${check}</td></tr>
      <tr><td>Copy the latest OTP straight from search</td><td>${part('Autofill only')}</td><td>${part('Autofill only')}</td><td class="us">${check}</td></tr>
      <tr><td>WhatsApp, Telegram, Signal buttons on contacts</td><td>${no}</td><td>${no}</td><td class="us">${check}</td></tr>
      <tr><td>“payment” finds PhonePe, GPay, Paytm</td><td>${no}</td><td>${no}</td><td class="us">${check}</td></tr>
      <tr><td>Nicknames: type “ma” to call Mom</td><td>${no}</td><td>${no}</td><td class="us">${check}</td></tr>
      <tr><td>Results built for one thumb</td><td>${no}</td><td>${no}</td><td class="us">${check}</td></tr>
    </tbody>
  </table></div>
  <p class="foot">Built-in search changes between phone makers and versions. Compared with public features as of November 2026.</p>
</div></section>
<div class="scallop"></div>

<section><div class="wrap">
  <h2>Find. Tap. <span class="hl">Done.</span></h2>
  <p class="lede" style="margin-top:18px">Pal doesn't just find things. It does the next step for you.</p>
  <div class="grid">
    <div class="f"><div class="ico">${ico(I.person)}</div><h3>People, one tap away</h3><p>Call, SMS, WhatsApp, Telegram or Signal from the result. Partial numbers work too.</p><span class="ex">“98765” → Rahul</span></div>
    <div class="f"><div class="ico">${ico(I.key)}</div><h3>OTP, copied</h3><p>Your latest one-time code waits in search for 10 minutes. Tap to copy.</p><span class="ex">Copy OTP 482 913</span></div>
    <div class="f"><div class="ico">${ico(I.folder)}</div><h3>Files and folders</h3><p>Photos, PDFs, downloads and documents by name, in milliseconds.</p><span class="ex">“resume” → Resume_v2.pdf</span></div>
    <div class="f"><div class="ico">${ico(I.bell)}</div><h3>Notifications</h3><p>That message you swiped away this morning? Search the last 7 days.</p><span class="ex">“delivery”</span></div>
    <div class="f"><div class="ico">${ico(I.calc)}</div><h3>Quick answers</h3><p>Maths, units, live currency rates, timers and alarms, right in the bar.</p><span class="ex">“100 usd in inr”</span></div>
    <div class="f"><div class="ico">${ico(I.spark)}</div><h3>Search by meaning</h3><p>A small AI model on your phone knows what apps are for.</p><span class="ex">“taxi” → Uber, Ola</span></div>
    <div class="f"><div class="ico">${ico(I.tag)}</div><h3>Nicknames &amp; shortcuts</h3><p>Name anything. Start with f, c or yt to search just files, contacts or YouTube.</p><span class="ex">“yt lofi”</span></div>
    <div class="f"><div class="ico">${ico(I.thumb)}</div><h3>Made for one hand</h3><p>The bar sits above the keyboard and results grow upward, so the best match is under your thumb.</p><span class="ex">↵ shows what Enter does</span></div>
  </div>
</div></section>

<div class="scallop into"></div>
<section class="cream"><div class="wrap two">
  <div>
    <p class="eyebrow">Made for India</p>
    <h2 style="margin-top:14px">Type in English. <span class="hl">Find in हिंदी.</span></h2>
    <div class="ilist">
      <div><i>अ</i><span>Contacts saved in Hindi, Gujarati, Bengali, Tamil, Telugu, Kannada, Malayalam, Punjabi or Odia show up when you type their English spelling.</span></div>
      <div><i>₹</i><span>“payment” or “upi” finds PhonePe, GPay, Paytm and BHIM. “train” finds IRCTC.</span></div>
      <div><i>✓</i><span>Bank and UPI OTPs, one tap to copy.</span></div>
    </div>
  </div>
  <div style="display:flex;justify-content:center">
    <div class="phone" role="img" aria-label="Typing rahul finds the Hindi contact राहुल शर्मा">
      <div class="screen">
        <div class="top">${P('stars', 'none', null, 34)}<span class="w">Find<b>Pal</b></span></div>
        <div class="card"><span class="ic" style="background:#5f259f">Pe</span><div><b>PhonePe</b><span>App · “payment”</span></div></div>
        <div class="card"><span class="ic" style="background:#e8743b">रा</span><div><b>राहुल पटेल</b><span>Contact · +91 99251 22014</span></div></div>
        <div class="tophit"><div class="lab">TOP HIT</div><div class="t">राहुल शर्मा</div>
          <div class="acts"><span>Call</span><span>SMS</span><span class="wa">WhatsApp</span></div></div>
        <div class="chips"><span class="on">All</span><span>Contacts 3</span><span>Apps 1</span></div>
        <div class="sbar"><span class="q">rahul</span><span class="enter">↵ Call</span></div>
      </div>
    </div>
  </div>
</div></section>
<div class="scallop"></div>

<section><div class="wrap two">
  <div class="priv-pal" style="text-align:center">${P('cool', 'point', null, 260)}</div>
  <div>
    <h2>Your phone. Your data. <span class="hl">Pal never tells.</span></h2>
    <div class="priv">
      <div><h3>Stays on your phone</h3><p>Contacts, files, notifications and history are searched here and never uploaded.</p></div>
      <div><h3>Names, not contents</h3><p>Pal reads file names and folders, not what's inside your documents or photos.</p></div>
      <div><h3>Nothing you type is sent</h3><p>No ads, no tracking. The only download is public currency rates, and you can turn it off.</p></div>
      <div><h3>You're in control</h3><p>Every permission is optional. Delete all data from Settings at any time.</p></div>
    </div>
  </div>
</div></section>

<section id="pricing" style="padding-top:20px"><div class="wrap">
  <h2>Pricing</h2>
  <div class="prices">
    <div class="price"><div class="tier">Free</div><div class="amt">₹0</div>
      <ul><li>Search apps, contacts, files, folders, settings and calendar</li><li>Call, SMS and WhatsApp buttons, OTP copy</li><li>Indian-language name search</li><li>Calculator, units, currency, timers and alarms</li><li>Widget, Quick Settings tile and assistant gesture</li></ul></div>
    <div class="price pro"><span class="peek">${P('cool', 'none', null, 120)}</span><div class="tier">FindPal Pro</div><div class="amt" style="font-size:44px">Coming soon</div>
      <ul><li>Find text inside screenshots and photos</li><li>Ask AI: short answers with sources</li><li>Search inside PDFs and documents</li><li>Extra themes</li></ul></div>
  </div>
</div></section>

<section style="padding-top:30px"><div class="wrap">
  <div class="faqhead">${P('worried', 'none', null, 96)}<h2>Questions?</h2></div>
  <details><summary>Is FindPal free?</summary><p>Yes. Everything listed under Free stays free. FindPal Pro will add extras like searching text in screenshots and AI answers; core search is never locked.</p></details>
  <details><summary>Does FindPal read my files or messages?</summary><p>It reads file names and folders to build a search index on your phone, not what's inside them. Notifications are only kept if you turn that on, for 7 days, on your phone.</p></details>
  <details><summary>Does it need the internet?</summary><p>No. Search works offline. The only network request downloads public exchange rates when you convert a currency, and you can switch that off.</p></details>
  <details><summary>Do I have to change my home screen?</summary><p>No. FindPal is a normal app. Open it from the widget, the Quick Settings tile, the assistant gesture (long-press Home or swipe from a corner) or the app icon.</p></details>
  <details><summary>Which phones does it work on?</summary><p>Android 8.0 and newer, including Samsung, Xiaomi, Pixel, OnePlus, vivo and OPPO phones.</p></details>
  <details><summary>Is FindPal on iPhone?</summary><p>Not yet. iPhone doesn't let apps search other apps' data the way Android does, so FindPal is Android-first.</p></details>
</div></section>

<section class="final"><div class="wrap">
  ${P('sleepy', 'none', 'zzz', 220)}
  <h2>Stop digging. <span class="hl">Start finding.</span></h2>
  <a class="btn big" href="#download"><svg width="24" height="24" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 3l14 9-14 9z" fill="#1d140e"/></svg>Coming soon on Google Play</a>
</div></section>
</main>

<footer><div class="wrap">
  <div><span class="word" style="font-size:22px">Find<b>Pal</b></span><p style="margin-top:8px">© 2026 FindPal. Made in India.</p></div>
  <nav aria-label="Footer"><a href="#top">Privacy Policy</a><a href="#top">Terms of Use</a><a href="#top">Contact</a></nav>
</div></footer>

<script>
(function(){
  // Time-saved estimate: searches per day × 8 s × 30 days.
  var n=document.getElementById('n'), m=document.getElementById('mins');
  function upd(){ m.textContent=Math.round(n.value*8*30/60); }
  n.addEventListener('input',upd); upd();

  // Hero phone: types example queries and swaps the top hit.
  var demos=[
    {q:'wh',t:'<em>Wh</em>atsApp',a:['Open','App info','Play Store'],e:'↵ Open'},
    {q:'rahul',t:'<em>Rahul</em> Sharma',a:['Call','SMS','<span>WhatsApp</span>'],e:'↵ Call',wa:true},
    {q:'100 usd in inr',t:'Live rate in ₹',a:['Copy','₹ → $','Share'],e:'↵ Copy'},
    {q:'timer 10 min',t:'Timer · 10 min',a:['Start','Edit','Clock'],e:'↵ Start'}
  ];
  var q=document.getElementById('q'), tt=document.getElementById('tt'), ta=document.getElementById('ta'), en=document.getElementById('en'), found=document.getElementById('found');
  if (window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches) return;

  // Hero Pal: eyes follow the pointer; tap for a happy hop.
  var hero=document.querySelector('.stage .pal.hi'), bubble=document.querySelector('.stage .bubble');
  var pupils=hero.querySelector('.pupils');
  window.addEventListener('pointermove',function(e){
    var r=hero.getBoundingClientRect(), cx=r.left+r.width*.45, cy=r.top+r.height*.4;
    var dx=e.clientX-cx, dy=e.clientY-cy, d=Math.max(1,Math.hypot(dx,dy)), k=Math.min(1,d/300)*6;
    pupils.style.transform='translate('+(dx/d*k).toFixed(1)+'px,'+(dy/d*k).toFixed(1)+'px)';
  },{passive:true});
  hero.addEventListener('click',function(){
    hero.classList.remove('hop'); void hero.offsetWidth; hero.classList.add('hop');
    bubble.textContent='Found it!';
    setTimeout(function(){ bubble.textContent='Hi! I find your stuff.'; hero.classList.remove('hop'); },1400);
  });

  // "Eats your time" strip: Pals bounce in one by one when scrolled into view.
  var strip=document.querySelector('.strip');
  if ('IntersectionObserver' in window){
    new IntersectionObserver(function(es,o){ es.forEach(function(e){ if(e.isIntersecting){ strip.classList.add('in'); o.disconnect(); } }); },{threshold:.4}).observe(strip);
  }
  var i=0, count=38;
  function show(d){
    var k=0; q.textContent='';
    var t=setInterval(function(){
      k++; q.textContent=d.q.slice(0,k);
      if(k>=d.q.length){ clearInterval(t);
        tt.innerHTML=d.t; en.textContent=d.e;
        ta.innerHTML=d.a.map(function(x,j){return '<span'+(d.wa&&j==2?' class="wa"':'')+'>'+x.replace(/<[^>]+>/g,'')+'</span>';}).join('');
        found.textContent=++count;
      }
    },110);
  }
  show(demos[0]);
  setInterval(function(){ i=(i+1)%demos.length; show(demos[i]); },3200);
})();
</script>`;

const full = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
${head}
${css}
</head>
<body>
${page}
</body>
</html>
`;
fs.writeFileSync(path.join(__dirname, 'index.html'), full);
if (process.argv[2]) fs.writeFileSync(process.argv[2], `${head}\n${css}\n${page}\n`);
console.log('built', Math.round(full.length / 1024) + ' KB');
