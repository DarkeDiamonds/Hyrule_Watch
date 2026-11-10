export const UNKNOWN = {stock:'Unknown',price:''};
export const ONLINE = 'Online stock reported';
const matches = (value,variant) => {
 const n=String(value||'').toLowerCase();
 return ['zelda','40th','switch','2'].every(s=>n.includes(s)) && (variant==='console' ? !['controller','case','protector'].some(s=>n.includes(s)) : n.includes('controller')&&n.includes('stand')===(variant==='stand'));
};
export function parseOffer(html,variant,address){
 const found=new Map();let data;
 function offers(node){
  if(Array.isArray(node)){node.forEach(offers);return;}
  if(!node||typeof node!=='object')return;
  if(node.offers){offers(node.offers);return;}
  const raw=String(node.availability||'').replace(/^https?:\/\/schema.org\//,'');
  const stock=({InStock:'In stock',LimitedAvailability:'In stock',OutOfStock:'Reported out of stock',SoldOut:'Reported out of stock',Discontinued:'Reported out of stock',PreOrder:'Preorder',PreSale:'Preorder',BackOrder:'Backorder reported'})[raw]||'Unknown';
  const v=Number(node.price);const price=node.priceCurrency==='USD'&&Number.isFinite(v)&&v>0&&v<=10000?v.toFixed(2):'';
  const result={stock,price};found.set(JSON.stringify(result),result);
 }
 function products(node){
  if(Array.isArray(node)){node.forEach(products);return;}
  if(!node||typeof node!=='object')return;
  if([].concat(node['@type']||[]).includes('Product')){if(matches(node.name,variant))offers(node.offers);return;}
  products(node['@graph']);products(node.mainEntity);
 }
 for(const m of html.matchAll(/<script\b([^>]*)>(.*?)<\/script\s*>/gis)){
  try{
   if(/\btype\s*=\s*['"]application\/ld\+json['"]/i.test(m[1]))products(JSON.parse(m[2]));
   if(/\bid\s*=\s*['"]__NEXT_DATA__['"]/i.test(m[1]))data=JSON.parse(m[2]);
  }catch{}
 }
 const raw=found.size===1?[...found.values()][0]:{...UNKNOWN};let salable;
 try{
  const url=new URL(address),sku=url.pathname.match(/^\/us\/store\/products\/[^/]+-(\d+)\/?$/)?.[1];
  if(url.protocol==='https:'&&['www.nintendo.com','nintendo.com'].includes(url.hostname)&&sku){
   const props=data.props.pageProps,identity=props.analytics.product,product=props.initialApolloState['Product:{"sku":"'+sku+'"}'];
   if(String(identity.sku)===sku&&String(product.sku)===sku&&matches(identity.name,variant)&&matches(product.name,variant)&&typeof product.isSalableQty==='boolean')salable=product.isSalableQty;
  }
 }catch{}
 if(salable===false)return {...raw,stock:'Not orderable online'};
 if(raw.stock==='In stock'||raw.stock==='Preorder')return {...raw,stock:salable===true?(raw.stock==='In stock'?ONLINE:'Online preorder reported'):'Availability unverified'};
 return raw;
}
