// Local engine test interface. Never print the endpoint's authentication token.
const fs=require('fs'),net=require('net'),readline=require('readline');
const endpoint=JSON.parse(fs.readFileSync(process.argv[2],'utf8'));
const socket=net.connect(endpoint.port,'127.0.0.1');socket.setTimeout(20000,()=>socket.destroy(Error('Control timed out')));
const lines=readline.createInterface({input:socket});let authenticated=false;
socket.on('connect',()=>socket.write('auth '+endpoint.token+'\n'));
lines.on('line',line=>{if(!authenticated){authenticated=true;socket.write(process.argv.slice(3).join(' ')+'\n');}else{console.log(line);socket.end();lines.close();}});
socket.on('error',e=>{console.error(e.message);process.exitCode=1});
