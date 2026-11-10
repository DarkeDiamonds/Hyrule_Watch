import {parseOffer} from './parser.js';
import {products} from './products.js';
import {html,css,client} from './ui.js';
// ponytail: at most seven cached reports per warm isolate; use durable cache only if needed.
const reports=new Map();
const headers={'X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer','Content-Security-Policy':"default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; base-uri 'none'; frame-ancestors 'none'"};
export default {async fetch(request,env,ctx){
 const url=new URL(request.url);
 if(request.method!=='GET')return new Response('Method not allowed',{status:405,headers:{...headers,Allow:'GET'}});
 const assets={'/':[html,'text/html; charset=utf-8'],'/style.css':[css,'text/css; charset=utf-8'],'/app.js':[client,'application/javascript; charset=utf-8']};
 if(assets[url.pathname]){const [body,type]=assets[url.pathname];return new Response(body,{headers:{...headers,'Content-Type':type,'Cache-Control':'no-cache'}});}
 if(url.pathname!=='/api/check')return new Response('Not found',{status:404,headers});
 const product=products.find(p=>p.id===url.searchParams.get('id'));
 if(!product)return Response.json({error:'Unknown listing'},{status:400,headers});
 const zip=url.searchParams.get('zip')??'80227',radiusMiles=Number(url.searchParams.get('radius')??100);
 if(!/^\d{5}$/.test(zip)||![25,50,100,200].includes(radiusMiles))return Response.json({error:'Use a five-digit US ZIP and a supported radius'},{status:400,headers});
 function localReport(report){return Response.json({...report,stock:'Local stock unverified',note:'No store-level stock has been verified in this area. Check pickup availability with the retailer.',area:{zip,radiusMiles},localSearchSupported:false,radiusApplied:false,zipVerified:false,availabilityPolicy:2},{headers:{...headers,'Cache-Control':'no-store'}});}
 const cached=reports.get(product.id);
 if(cached&&cached.expires>Date.now())return localReport(cached.data);
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
 const data={...result,id:product.id,checked:new Date().toISOString(),zipVerified:false};
 reports.set(product.id,{data,expires:Date.now()+60000});
 return localReport(data);
}};
