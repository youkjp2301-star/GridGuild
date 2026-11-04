import fs from 'node:fs';
const paths = process.argv.slice(2);
fs.mkdirSync('.tools/docs', {recursive:true});
for (const path of paths) {
 const url = 'https://document.pleos.ai/en/api-reference/connect-sdk-pleos/' + path;
 const response = await fetch(url);
 const html = await response.text();
 const text = html.replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi,'').replace(/<style\b[^>]*>[\s\S]*?<\/style>/gi,'').replace(/<\/(p|div|h\d|tr|pre|li)>/g,'\n').replace(/<[^>]+>/g,' ').replace(/&quot;/g,'"').replace(/&#x27;|&#39;/g,"'").replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&amp;/g,'&').replace(/[ \t]+/g,' ');
 const name = path.replaceAll('/','_');
 fs.writeFileSync('.tools/docs/'+name+'.txt',text);
 console.log('\nURL '+url+' '+response.status+'\n'+text.slice(-22000));
}
