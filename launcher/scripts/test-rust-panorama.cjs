const {test}=require('node:test');const assert=require('node:assert/strict');const fs=require('node:fs');const path=require('node:path');const os=require('node:os');const vm=require('node:vm');const ts=require('typescript');const Zip=require('adm-zip');const {EventEmitter}=require('node:events');
function load(file,imports){const m={exports:{}};vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(__dirname,'../src/main',file),'utf8'),{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS,esModuleInterop:true}}).outputText,{module:m,exports:m.exports,process,URL,Buffer,console,require:id=>id in imports?imports[id]:require(id)});return m.exports;}
function temp(t){const dir=fs.mkdtempSync(path.join(os.tmpdir(),'visual-engine-'));t.after(()=>fs.rmSync(dir,{recursive:true,force:true}));return dir;}
function panorama(root,version,size){const zip=new Zip();for(let i=0;i<6;i++){const b=Buffer.alloc(1200);Buffer.from('89504e470d0a1a0a','hex').copy(b);b.writeUInt32BE(size,16);b.writeUInt32BE(size,20);zip.addFile(`assets/minecraft/textures/gui/title/background/panorama_${i}.png`,b)}const dir=path.join(root,'versions',version);fs.mkdirSync(dir,{recursive:true});zip.writeZip(path.join(dir,`${version}.jar`));}
test('panorama accepts classic 256px faces and prefers higher resolution',t=>{const root=temp(t);panorama(root,'1.8.9',256);const api=()=>load('panorama.ts',{'electron':{app:{getPath:()=>root}},'./paths':{MC_ROOT:root}});assert.equal(api().panoramaFaces().length,6);panorama(root,'1.21.11',1024);assert.equal(Buffer.from(api().panoramaFaces()[0].split(',')[1],'base64').readUInt32BE(16),1024)});
test('missing panorama is retried after assets become available',t=>{const root=temp(t);const api=load('panorama.ts',{'electron':{app:{getPath:()=>root}},'./paths':{MC_ROOT:root}});assert.equal(api.panoramaFaces(),null);panorama(root,'1.8.9',256);assert.equal(api.panoramaFaces().length,6)});
test('prepared Rust starts the engine directly without a launcher or account call',async t=>{const root=temp(t);const bundled=path.join(root,'build/rust');fs.mkdirSync(bundled,{recursive:true});const binary=Buffer.from('visual fixture');fs.writeFileSync(path.join(bundled,process.platform==='win32'?'visual-client.exe':'visual-client'),binary);fs.writeFileSync(path.join(bundled,'build.json'),JSON.stringify({sha256:require('crypto').createHash('sha256').update(binary).digest('hex')}));const dir=path.join(root,'horizon-launcher/versions/v0.2.97');fs.mkdirSync(path.join(dir,'.local/horizon-client/26.3'),{recursive:true});fs.writeFileSync(path.join(dir,process.platform==='win32'?'horizon-client.exe':'horizon-client'),'fixture');fs.writeFileSync(path.join(dir,'.local/horizon-client/26.3/horizon-resources.json'),'{}');const calls=[];let played=false;const api=load('horizon.ts',{'electron':{app:{getAppPath:()=>root}},'child_process':{execFile:require('child_process').execFile,spawn:(exe,args,opts)=>{calls.push({exe,args,opts});const p=new EventEmitter();queueMicrotask(()=>{p.emit('spawn');p.emit('close',0)});return p}},'./paths':{DATA_DIR:root,MC_ROOT:root,instanceDir:()=>root},'./profiles':{getProfile:()=>({engine:'horizon',mcVersion:'26.3'}),touchLastPlayed:()=>{played=true}},'./modrinth':{downloadFile:()=>{throw Error('unexpected download')}},'./errors':{VError:Error}});await api.playHorizon('test',()=>{});assert.equal(calls.length,1);assert.match(calls[0].exe,/visual-client(?:\.exe)?$/);assert.deepEqual(Array.from(calls[0].args),['--resources',path.join(dir,'.local/horizon-client/26.3')]);assert.equal(played,true);assert.match(fs.readFileSync(path.join(root,'horizon/options.txt'),'utf8'),/lang:en_us/);assert.equal(calls[0].opts.cwd,path.join(root,'horizon'));assert.equal(calls[0].opts.env.HORIZON_HOME,path.join(root,'horizon'));const graphics=path.join(root,'horizon/horizon-graphics.txt');assert.match(fs.readFileSync(graphics,'utf8'),/lodDistance:256/);assert.match(fs.readFileSync(graphics,'utf8'),/fog:false/);fs.writeFileSync(graphics,'lodDistance:64\nfarDetail:full\n');await api.playHorizon('test',()=>{});assert.equal(fs.readFileSync(graphics,'utf8'),'lodDistance:64\nfarDetail:full\n');fs.unlinkSync(path.join(root,'horizon/.visual-clear-distance-v2'));fs.writeFileSync(graphics,'lodDistance:96\nfog:true\n');fs.writeFileSync(path.join(root,'horizon/horizon.txt'),'lodDistance:512\n');await api.playHorizon('test',()=>{});assert.equal(fs.readFileSync(graphics,'utf8'),'lodDistance:96\nfog:false\n');fs.unlinkSync(graphics);fs.unlinkSync(path.join(root,'horizon/.visual-clear-distance-v2'));await api.playHorizon('test',()=>{});assert.equal(fs.readFileSync(graphics,'utf8'),'lodDistance:512\nfog:false\n')});
test('modern asset index supplies HD panorama even without a client JAR',t=>{const root=temp(t);const objects={};for(let i=0;i<6;i++){const hash=String(i+1).padStart(40,'0');const b=Buffer.alloc(1200);Buffer.from('89504e470d0a1a0a','hex').copy(b);b.writeUInt32BE(1024,16);b.writeUInt32BE(1024,20);objects[`minecraft/textures/gui/title/background/panorama_${i}.png`]={hash,size:b.length};const dir=path.join(root,'assets/objects',hash.slice(0,2));fs.mkdirSync(dir,{recursive:true});fs.writeFileSync(path.join(dir,hash),b)}fs.mkdirSync(path.join(root,'assets/indexes'),{recursive:true});fs.writeFileSync(path.join(root,'assets/indexes/34.json'),JSON.stringify({objects}));const api=load('panorama.ts',{'electron':{app:{getPath:()=>root}},'./paths':{MC_ROOT:root}});const faces=api.panoramaFaces();assert.equal(faces.length,6);assert.equal(Buffer.from(faces[0].split(',')[1],'base64').readUInt32BE(16),1024)});

test('fresh Visual profile prepares Minecraft resources without a Horizon installation', async t => {
  const root = temp(t), dir = path.join(root, 'rust-engine/v0.2.97');
  fs.mkdirSync(dir, {recursive:true}); fs.writeFileSync(path.join(dir,'.visual-ready'),'v0.2.97'); fs.writeFileSync(path.join(dir,process.platform==='win32'?'horizon-client.exe':'horizon-client'),'fixture');
  const sha = b => require('crypto').createHash('sha1').update(b).digest('hex');
  const client=Buffer.from('client'), server=Buffer.from('server'), asset=Buffer.from('asset');
  const index=Buffer.from(JSON.stringify({objects:{sample:{hash:sha(asset)}}}));
  const manifest=Buffer.from(JSON.stringify({libraries:[{downloads:{artifact:{path:'test/library.jar',url:'https://libraries.minecraft.net/library.jar',sha1:sha(asset)}}}],downloads:{client:{url:'https://piston-data.mojang.com/client'},server:{url:'https://piston-data.mojang.com/server'}},assetIndex:{id:'34',url:'https://piston-meta.mojang.com/index',sha1:sha(index)}}));
  fs.writeFileSync(path.join(dir,'research-lock.json'),JSON.stringify({minecraft:{manifest_sha1:sha(manifest),client_sha1:sha(client),server_sha1:sha(server),java_major:25}}));
  const downloads=[]; let prepared=false;
  const api=load('horizon.ts',{
    electron:{app:{getAppPath:()=>root}},
    './paths':{DATA_DIR:root,MC_ROOT:path.join(root,'minecraft'),instanceDir:()=>root},
    './profiles':{getProfile:()=>({engine:'horizon',mcVersion:'26.3'})},
    './launch':{ensureJava:async()=> 'C:/java/bin/java.exe'},
    './errors':{VError:Error},
    './modrinth':{downloadFile:async(url,dest)=>{downloads.push(url); fs.writeFileSync(dest,url.includes('/packages/')?manifest:url.endsWith('/client')?client:url.endsWith('/server')?server:url.endsWith('/index')?index:asset)}},
    child_process:{execFile:(exe,args,opts,callback)=>{
      assert.match(exe,/horizon-prepare/); assert.equal(fs.existsSync(path.join(root,'horizon-launcher')),false);
      assert.equal(fs.readFileSync(path.join(root,'minecraft/versions/26.3/26.3.jar'),'utf8'),'client');
      const resources=path.join(dir,'.local/horizon-client/26.3'); fs.mkdirSync(resources,{recursive:true});fs.writeFileSync(path.join(resources,'horizon-resources.json'),'{}');
      assert.equal(fs.readFileSync(path.join(root,'minecraft/libraries/test/library.jar'),'utf8'),'asset');prepared=true;callback(null,'','');
    }}
  });
  await api.setupHorizon('fresh'); assert.equal(prepared,true); assert.equal(downloads.length,6);
  assert.equal(fs.existsSync(path.join(root,'minecraft/assets/indexes/34.json')),true);
  await api.setupHorizon('fresh'); assert.equal(downloads.length,6);
});

test('original Windows archive installs from scratch with its nested directory', {skip:!process.env.VISUAL_TEST_ENGINE_ZIP}, async t => {
 const root=temp(t); let manifestRequested=false;
 const api=load('horizon.ts',{
  electron:{app:{getAppPath:()=>root}},'./paths':{DATA_DIR:root,MC_ROOT:path.join(root,'minecraft'),instanceDir:()=>root},
  './profiles':{getProfile:()=>({engine:'horizon',mcVersion:'26.3'})},'./errors':{VError:Error},
  './modrinth':{downloadFile:async(url,dest)=>{if(url.includes('github.com/'))fs.copyFileSync(process.env.VISUAL_TEST_ENGINE_ZIP,dest);else {manifestRequested=true;throw Error('fixture: stop before Minecraft download')}}}
 });
 await assert.rejects(api.setupHorizon('fresh'),/fixture: stop before Minecraft download/);
 assert.equal(manifestRequested,true);
 assert.equal(fs.existsSync(path.join(root,'horizon-launcher')),false);
 assert.equal(fs.existsSync(path.join(root,'rust-engine/v0.2.97/horizon-windows-x64/horizon-prepare.exe')),true);
 assert.equal(fs.existsSync(path.join(root,'rust-engine/v0.2.97/.visual-ready')),true);
});

test('setup retains a diagnostic log and exposes unexpected failure details', async t => {
 const root=temp(t); class TestError extends Error {constructor(key,vars){super(key);this.key=key;this.vars=vars}}
 const api=load('horizon.ts',{
  electron:{app:{}},'./paths':{DATA_DIR:root,MC_ROOT:root,instanceDir:()=>root},
  './profiles':{getProfile:()=>({engine:'horizon',mcVersion:'26.3'})},'./errors':{VError:TestError},
  './modrinth':{downloadFile:async()=>{throw Error('fixture connection closed')}}
 });
 await assert.rejects(api.setupHorizon('test'),err=>err.key==='engine.prepareDetail'&&err.vars.detail==='fixture connection closed');
 assert.match(fs.readFileSync(path.join(root,'rust-setup.log'),'utf8'),/fixture connection closed/);
});
