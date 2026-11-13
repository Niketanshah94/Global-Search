# FindPal website

`index.html` is a single self-contained page (no build step needed to host it): drop it on
GitHub Pages, Netlify, Vercel or any static host. Pal is drawn as inline SVG; fonts load from
Google Fonts (Baloo 2, Inter Tight, Inter).

To change content, edit `build.js` and run `node website/build.js`; it regenerates `index.html`
from the brand mascot in `docs/brand/source/mascot.js`.

Pages: `index.html` (home), `privacy.html` (privacy policy, the URL Google Play asks for) and
`terms.html` (terms of use, including subscription and free-trial terms). Upload all three to the
same folder on your server.

Before launch: replace `SUPPORT_EMAIL`, `DEVELOPER_NAME` and `GOVERNING_LAW` in the three pages,
and point the "Coming soon on Google Play" buttons at the Play listing.
