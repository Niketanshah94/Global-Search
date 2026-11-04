# FindPal website

`index.html` is a single self-contained page (no build step needed to host it): drop it on
GitHub Pages, Netlify, Vercel or any static host. Pal is drawn as inline SVG; fonts load from
Google Fonts (Baloo 2, Inter Tight, Inter).

To change content, edit `build.js` and run `node website/build.js`; it regenerates `index.html`
from the brand mascot in `docs/brand/source/mascot.js`.

Before launch: point the "Coming soon on Google Play" buttons at the Play listing, and replace the
footer's Privacy Policy, Terms and Contact links with real pages (Play requires a privacy policy URL).
