const fs = require('fs');
const assert = require('assert');
const source = fs.readFileSync('web-auth.js', 'utf8');
assert(source.includes("const SITE_BASE=new URL('./',document.baseURI).href;"), 'web auth must derive assets from the document base URI so srcdoc follows the active site origin');
assert(!source.includes("const SITE_BASE='https://beaustwrt248-netizen.github.io/Buys-Mock/';"), 'web auth must not hard-code the legacy GitHub Pages path');
assert(source.includes('${SITE_BASE}admin/turnstile.html?v=4'), 'Turnstile iframe must use the active site base');
assert(source.includes('${SITE_BASE}web-assets/morley_buys_login_bg_app.mp4?v=2'), 'login background must use the active site base');
assert(source.includes('gotrue_meta_security:{captcha_token:captchaToken}'), 'CAPTCHA token submission must remain enforced');
console.log('Buyshub web auth origin checks passed');
