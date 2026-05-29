const http = require('http');

const loginData = JSON.stringify({username: 'admin', password: 'Sunafil2026!'});
const loginReq = http.request({
  hostname: 'localhost', port: 8080, path: '/api/auth/login', method: 'POST',
  headers: {'Content-Type': 'application/json', 'Content-Length': loginData.length}
}, (res) => {
  let body = '';
  res.on('data', d => body += d);
  res.on('end', () => {
    console.log('LOGIN ->', res.statusCode, body.substring(0, 200));
    const token = JSON.parse(body).token;
    
    // Simulate POST crear ticket
    const boundary = '----boundary123';
    const parts = [
      '--' + boundary,
      'Content-Disposition: form-data; name="asunto"',
      '',
      'Ticket de prueba post-restructuracion',
      '--' + boundary,
      'Content-Disposition: form-data; name="cod_ire"',
      '',
      'DINI',
      '--' + boundary + '--',
      ''
    ].join('\r\n');
    
    const postData = Buffer.from(parts);
    const r = http.request({
      hostname: 'localhost', port: 8080, path: '/api/tickets/crear', method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'multipart/form-data; boundary=' + boundary,
        'Content-Length': postData.length
      }
    }, (res) => {
      let b = '';
      res.on('data', d => b += d);
      res.on('end', () => console.log('POST /crear ->', res.statusCode, 'body:', b.substring(0, 300)));
    });
    r.write(postData);
    r.end();
  });
});
loginReq.write(loginData);
loginReq.end();
