const http = require('http');
http.get('http://localhost:3000', (r) => {
  let d = '';
  r.on('data', c => d += c);
  r.on('end', () => {
    console.log('Status:', r.statusCode);
    console.log('Length:', d.length);
    console.log('Has <title>:', /<title>/.test(d));
    console.log('Has #app:', d.includes('id="app"'));
    console.log('Vite module:', d.includes('/@vite/client'));
  });
}).on('error', e => console.log('ERROR:', e.message));
