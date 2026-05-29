const http = require('http');

async function checkApi(path) {
  return new Promise((resolve) => {
    http.get(`http://localhost:8080${path}`, (res) => {
      let data = '';
      res.on('data', (chunk) => data += chunk);
      res.on('end', () => {
        console.log(`PATH: ${path} | STATUS: ${res.statusCode}`);
        if (res.statusCode === 200) {
          try {
            const json = JSON.parse(data);
            console.log(`DATA LENGTH: ${Array.isArray(json) ? json.length : 'Object'}`);
            if (Array.isArray(json) && json.length > 0) console.log('FIRST ITEM:', JSON.stringify(json[0]).substring(0, 100));
          } catch(e) { console.log('DATA:', data.substring(0, 100)); }
        } else {
          console.log('ERROR BODY:', data.substring(0, 200));
        }
        resolve();
      });
    }).on('error', (err) => {
      console.log(`PATH: ${path} | FAILED: ${err.message}`);
      resolve();
    });
  });
}

async function run() {
  await checkApi('/api/admin/intendencias');
  await checkApi('/api/admin/todos-los-tickets');
  await checkApi('/api/admin/usuarios');
}

run();
