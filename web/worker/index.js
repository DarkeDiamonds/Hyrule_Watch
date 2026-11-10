import {parseOffer} from './parser.js';
import {products} from './products.js';
import {html,css,client} from './ui.js';
const headers={'X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer','Content-Security-Policy':"default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; base-uri 'none'; frame-ancestors 'none'"};
export default {async fetch(request,env,ctx){
 const url=new URL(request.url);
 if(request.method!=='GET')return new Response('Method not allowed',{status:405,headers:{...headers,Allow:'GET'}});
 const assets={'/':[html,'text/html; charset=utf-8'],'/style.css':[css,'text/css; charset=utf-8'],'/app.js':[client,'application/javascript; charset=utf-8']};
 if(assets[url.pathname]){const [body,type]=assets[url.pathname];return new Response(body,{headers:{...headers,'Content-Type':type,'Cache-Control':'no-cache'}});}
 if(url.pathname!=='/api/check')return new Response('Not found',{status:404,headers});
 const product=products.find(p=>p.id===url.searchParams.get('id'));
 if(!product)return Response.json({error:'Unknown listing'},{status:400,headers});
 const cache=globalThis.caches?.default,key=new Request(url.origin+'/api/check?id='+product.id);
 if(cache){const cached=await cache.match(key);if(cached)return cached;}
 let result={stock:'Unknown',price:'',note:'Needs a browser check.'};
 try{
  const response=await fetch(product.url,{redirect:'manual',signal:AbortSignal.timeout(12000),headers:{'User-Agent':'HyruleWatch/1.0 (personal stock tracker)','Accept':'text/html','Cache-Control':'no-cache'}});
  if(response.status!==200)throw new Error('Retailer returned HTTP '+response.status);
  if(!response.headers.get('content-type')?.includes('text/html'))throw new Error('Retailer did not return a product page');
  const reader=response.body.getReader(),chunks=[];let length=0;
  for(;;){const {value,done}=await reader.read();if(done)break;length+=value.length;if(length>4*1024*1024){await reader.cancel();throw new Error('Product page exceeded size limit');}chunks.push(value);}
  const bytes=new Uint8Array(length);let offset=0;for(const chunk of chunks){bytes.set(chunk,offset);offset+=chunk.length;}
  result={...parseOffer(new TextDecoder().decode(bytes),product.variant,product.url),note:'Retailer listing report. Confirm price and delivery at checkout.'};
  if(result.stock==='Availability unverified')result.note='A general stock flag does not confirm you can buy this item. Check the retailer.';
  if(result.stock==='Not orderable online')result.note='Nintendo marks this exact product unavailable to order online.';
  if(result.stock==='Unknown')result.note='No reliable availability data. Open the retailer to check.';
 }catch{result.note='Automated check unavailable or blocked. Open the retailer to check.';}
 const response=Response.json({...result,id:product.id,checked:new Date().toISOString(),zipVerified:false},{headers:{...headers,'Cache-Control':'public, max-age=60'}});
 if(cache&&ctx)ctx.waitUntil(cache.put(key,response.clone()));
 return response;
}};
