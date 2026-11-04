// "Pal", the FindPal mascot: a magnifying glass whose lens is its face.
// pal({face, arm, extra, size}) returns an SVG string. viewBox 0 0 200 220.
function pal(opts = {}) {
  const face = opts.face || 'happy';
  const arm = opts.arm || 'none';
  const id = 'p' + Math.random().toString(36).slice(2, 8);
  const eyes = {
    happy: `
      <ellipse cx="72" cy="86" rx="17" ry="20" fill="#fff" stroke="#2b1a10" stroke-width="3.5"/>
      <ellipse cx="118" cy="86" rx="17" ry="20" fill="#fff" stroke="#2b1a10" stroke-width="3.5"/>
      <circle cx="76" cy="90" r="9" fill="#2b1a10"/><circle cx="122" cy="90" r="9" fill="#2b1a10"/>
      <circle cx="79" cy="86" r="3.5" fill="#fff"/><circle cx="125" cy="86" r="3.5" fill="#fff"/>`,
    look: `
      <ellipse cx="72" cy="86" rx="17" ry="20" fill="#fff" stroke="#2b1a10" stroke-width="3.5"/>
      <ellipse cx="118" cy="86" rx="17" ry="20" fill="#fff" stroke="#2b1a10" stroke-width="3.5"/>
      <circle cx="81" cy="84" r="9" fill="#2b1a10"/><circle cx="127" cy="84" r="9" fill="#2b1a10"/>
      <circle cx="84" cy="80" r="3.5" fill="#fff"/><circle cx="130" cy="80" r="3.5" fill="#fff"/>
      <path d="M58 64 q14 -8 28 0" stroke="#2b1a10" stroke-width="4" fill="none" stroke-linecap="round"/>
      <path d="M106 60 q14 -6 26 4" stroke="#2b1a10" stroke-width="4" fill="none" stroke-linecap="round"/>`,
    stars: `
      <path d="M72 66 l6 13 14 2 -10 10 3 14 -13 -7 -13 7 3 -14 -10 -10 14 -2z" fill="#FFC93C" stroke="#2b1a10" stroke-width="3" stroke-linejoin="round"/>
      <path d="M118 66 l6 13 14 2 -10 10 3 14 -13 -7 -13 7 3 -14 -10 -10 14 -2z" fill="#FFC93C" stroke="#2b1a10" stroke-width="3" stroke-linejoin="round"/>`,
    sleepy: `
      <path d="M58 90 q14 12 28 0" stroke="#2b1a10" stroke-width="4.5" fill="none" stroke-linecap="round"/>
      <path d="M104 90 q14 12 28 0" stroke="#2b1a10" stroke-width="4.5" fill="none" stroke-linecap="round"/>`,
    cool: `
      <path d="M50 76 h44 a4 4 0 0 1 4 4 v6 a18 18 0 0 1 -18 18 h-12 a18 18 0 0 1 -18 -18 v-6 a4 4 0 0 1 4 -4z" fill="#1b120c"/>
      <path d="M100 76 h44 a4 4 0 0 1 4 4 v6 a18 18 0 0 1 -18 18 h-12 a18 18 0 0 1 -18 -18 v-6 a4 4 0 0 1 4 -4z" fill="#1b120c"/>
      <path d="M94 82 h10" stroke="#1b120c" stroke-width="5"/>
      <path d="M58 82 l10 -2 M108 82 l10 -2" stroke="#ffffff66" stroke-width="4" stroke-linecap="round"/>`,
    worried: `
      <ellipse cx="72" cy="88" rx="17" ry="20" fill="#fff" stroke="#2b1a10" stroke-width="3.5"/>
      <ellipse cx="118" cy="88" rx="17" ry="20" fill="#fff" stroke="#2b1a10" stroke-width="3.5"/>
      <circle cx="72" cy="94" r="10" fill="#2b1a10"/><circle cx="118" cy="94" r="10" fill="#2b1a10"/>
      <circle cx="75" cy="90" r="4" fill="#fff"/><circle cx="121" cy="90" r="4" fill="#fff"/>
      <path d="M58 70 l26 -8 M132 70 l-26 -8" stroke="#2b1a10" stroke-width="4" stroke-linecap="round"/>`,
  }[face];
  const mouth = {
    happy: `<path d="M82 118 q13 12 26 0" stroke="#2b1a10" stroke-width="4.5" fill="none" stroke-linecap="round"/>`,
    look: `<path d="M86 120 q10 4 20 -2" stroke="#2b1a10" stroke-width="4.5" fill="none" stroke-linecap="round"/>`,
    stars: `<ellipse cx="95" cy="124" rx="9" ry="11" fill="#7a2e14" stroke="#2b1a10" stroke-width="3.5"/><ellipse cx="95" cy="129" rx="5" ry="4" fill="#ff8a7a"/>`,
    sleepy: `<path d="M86 118 q9 7 18 0" stroke="#2b1a10" stroke-width="4.5" fill="none" stroke-linecap="round"/>`,
    cool: `<path d="M80 118 q15 12 30 -2" stroke="#2b1a10" stroke-width="4.5" fill="none" stroke-linecap="round"/>`,
    worried: `<path d="M84 126 q11 -9 22 0" stroke="#2b1a10" stroke-width="4.5" fill="none" stroke-linecap="round"/>`,
  }[face];
  const blush = face === 'cool' ? '' : `
      <ellipse cx="56" cy="112" rx="10" ry="6" fill="#ff8f6b" opacity=".55"/>
      <ellipse cx="134" cy="112" rx="10" ry="6" fill="#ff8f6b" opacity=".55"/>`;
  const arms = {
    none: '',
    wave: `<g><path d="M30 132 C 6 128, -10 108, -8 84" stroke="#2b1a10" stroke-width="17" fill="none" stroke-linecap="round"/>
            <path d="M30 132 C 6 128, -10 108, -8 84" stroke="#F07A2E" stroke-width="10" fill="none" stroke-linecap="round"/>
            <circle cx="-8" cy="78" r="12" fill="#FFA35C" stroke="#2b1a10" stroke-width="3.5"/>
            <path d="M-20 62 l-5 -7 M-9 58 l0 -9 M2 61 l5 -7" stroke="#FFE2B8" stroke-width="3.5" stroke-linecap="round"/></g>`,
    point: `<g><path d="M162 132 C 186 128, 202 108, 200 86" stroke="#2b1a10" stroke-width="17" fill="none" stroke-linecap="round"/>
             <path d="M162 132 C 186 128, 202 108, 200 86" stroke="#F07A2E" stroke-width="10" fill="none" stroke-linecap="round"/>
             <path d="M200 74 v-20" stroke="#2b1a10" stroke-width="11" stroke-linecap="round"/><path d="M200 74 v-20" stroke="#FFA35C" stroke-width="5" stroke-linecap="round"/>
             <circle cx="200" cy="80" r="12" fill="#FFA35C" stroke="#2b1a10" stroke-width="3.5"/></g>`,
    cheer: `<g><path d="M30 132 C 6 128, -10 108, -8 84" stroke="#2b1a10" stroke-width="17" fill="none" stroke-linecap="round"/>
             <path d="M30 132 C 6 128, -10 108, -8 84" stroke="#F07A2E" stroke-width="10" fill="none" stroke-linecap="round"/>
             <circle cx="-8" cy="78" r="12" fill="#FFA35C" stroke="#2b1a10" stroke-width="3.5"/>
             <path d="M162 132 C 186 128, 202 108, 200 86" stroke="#2b1a10" stroke-width="17" fill="none" stroke-linecap="round"/>
             <path d="M162 132 C 186 128, 202 108, 200 86" stroke="#F07A2E" stroke-width="10" fill="none" stroke-linecap="round"/>
             <circle cx="200" cy="78" r="12" fill="#FFA35C" stroke="#2b1a10" stroke-width="3.5"/></g>`,
  }[arm];
  const extra = opts.extra === 'zzz'
    ? `<text x="150" y="40" font-family="Baloo 2" font-weight="800" font-size="26" fill="#FFE2B8">z</text><text x="168" y="22" font-family="Baloo 2" font-weight="800" font-size="18" fill="#FFE2B8">z</text>`
    : opts.extra === 'sparkle'
    ? `<path d="M168 30 l4 10 10 4 -10 4 -4 10 -4 -10 -10 -4 10 -4z" fill="#FFC93C"/><path d="M26 30 l3 7 7 3 -7 3 -3 7 -3 -7 -7 -3 7 -3z" fill="#FFC93C"/>`
    : opts.extra === 'sweat'
    ? `<path d="M156 52 q8 12 0 18 q-8 -6 0 -18z" fill="#8fd3ff" stroke="#2b1a10" stroke-width="2.5"/>`
    : '';
  const s = opts.size || 200;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${s}" height="${s * 0.98}" viewBox="-26 -18 252 246">
  <defs>
    <linearGradient id="${id}r" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#FFA35C"/><stop offset=".55" stop-color="#F07A2E"/><stop offset="1" stop-color="#D9572A"/></linearGradient>
    <radialGradient id="${id}g" cx=".38" cy=".32" r=".8"><stop offset="0" stop-color="#FFFDF8"/><stop offset=".7" stop-color="#FFF1DC"/><stop offset="1" stop-color="#FFE0B8"/></radialGradient>
    <linearGradient id="${id}h" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#5a3420"/><stop offset="1" stop-color="#2b1a10"/></linearGradient>
  </defs>
  ${arms}
  <!-- handle = body -->
  <g transform="rotate(-38 132 168)">
    <rect x="114" y="140" width="36" height="74" rx="18" fill="url(#${id}h)" stroke="#2b1a10" stroke-width="4"/>
    <path d="M116 168 h32 M116 182 h32" stroke="#7a4a2c" stroke-width="4"/>
    <rect x="112" y="132" width="40" height="18" rx="7" fill="#F07A2E" stroke="#2b1a10" stroke-width="4"/>
  </g>
  <!-- lens rim -->
  <circle cx="95" cy="96" r="80" fill="url(#${id}r)" stroke="#2b1a10" stroke-width="5"/>
  <circle cx="95" cy="96" r="62" fill="url(#${id}g)" stroke="#2b1a10" stroke-width="4"/>
  <path d="M52 62 a50 50 0 0 1 30 -22" stroke="#fff" stroke-width="8" fill="none" stroke-linecap="round" opacity=".9"/>
  <path d="M44 128 a54 54 0 0 1 -6 -18" stroke="#fff" stroke-width="5" fill="none" stroke-linecap="round" opacity=".6"/>
  ${blush}
  ${eyes}
  ${mouth}
  ${extra}
</svg>`;
}
if (typeof module !== 'undefined') module.exports = { pal };
