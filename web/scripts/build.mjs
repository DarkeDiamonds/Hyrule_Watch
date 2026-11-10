import {readFile,mkdir,writeFile,copyFile} from 'node:fs/promises';
await mkdir('dist/server',{recursive:true});await mkdir('dist/.openai',{recursive:true});
// No dependencies: inline the three local ES modules into the Worker entrypoint.
let code='';for(const path of ['parser','products','ui','index']){let text=await readFile('worker/'+path+'.js','utf8');text=text.replace(/^import .*;\n/gm,'');if(path!=='index')text=text.replace(/^export /gm,'');code+=text+'\n';}
await writeFile('dist/server/index.js',code);await copyFile('.openai/hosting.json','dist/.openai/hosting.json');
console.log('Built dependency-free Worker.');
