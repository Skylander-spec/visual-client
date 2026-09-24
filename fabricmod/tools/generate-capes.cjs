// Reuse the existing launcher generator without changing or starting the launcher.
// Run: launcher/node_modules/electron/dist/electron.exe fabricmod/tools/generate-capes.cjs
const { app, BrowserWindow } = require('electron');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../..');
const ts = require(path.join(root, 'launcher/node_modules/typescript'));
const presets = [
  ['Aurora', 'cyan gruen aurora', 'Aurora', 42],
  ['Amethyst', 'lila amethyst', 'Geometrie', 73],
  ['Sternenlicht', 'blau sternenhimmel', 'Sternenhimmel', 101],
  ['Goldregen', 'gold regen', 'Regen', 63],
  ['Frost', 'eis frost', 'Geometrie', 88],
  ['Glut', 'rot feuer', 'Flammen', 29]
];
app.whenReady().then(async () => {
  const win = new BrowserWindow({ show: false, webPreferences: { sandbox: true } });
  try {
    await win.loadURL('data:text/html,<html><body></body></html>');
    const code = ts.transpileModule(fs.readFileSync(path.join(root,
      'launcher/src/renderer/src/lib/capeGen.ts'), 'utf8'),
      { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText;
    const output = path.join(root, 'fabricmod/src/main/resources/assets/visualsfabric/capes');
    fs.mkdirSync(output, { recursive: true });
    for (const [name, prompt, style, seed] of presets) {
      const url = await win.webContents.executeJavaScript(
        `(function(){const exports={};${code};return exports.generateCape(${JSON.stringify(prompt)},${JSON.stringify(style)},${seed});})()`);
      fs.writeFileSync(path.join(output, `${name}.png`), Buffer.from(url.split(',')[1], 'base64'));
      console.log(name);
    }
  } catch (error) { console.error(error); process.exitCode = 1; }
  finally { win.destroy(); app.quit(); }
});
