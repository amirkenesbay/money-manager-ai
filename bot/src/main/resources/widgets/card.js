(()=>{const W=window.TelegramWidget,R=matchMedia('(prefers-reduced-motion: reduce)').matches,d=devicePixelRatio||1,c=document.querySelector('canvas'),x=c.getContext('2d'),E=['💵','🪙','💰','💸'],M=Array.from({length:9},(_,i)=>({x:(i+Math.random()*.8)/9,y:Math.random(),v:.5+Math.random()*.5,p:Math.random()*6.3,s:14+Math.random()*8,e:E[i%4]}));let w,h,l=0,B=[];
const fit=()=>{w=c.width=c.offsetWidth*d;h=c.height=c.offsetHeight*d};fit();addEventListener('resize',fit);
const burst=el=>{const a=el.getBoundingClientRect(),o=c.getBoundingClientRect(),t=performance.now();B=Array.from({length:22},()=>({x:a.left-o.left+a.width/2,y:a.top-o.top+a.height/2,u:(Math.random()-.5)*5,v:-Math.random()*5-1.5,c:[170,265,40,340][Math.random()*4|0],t,r:Math.random()*6}))};
const F=t=>{const k=R||!l?0:Math.min(t-l,50);l=t;x.clearRect(0,0,w,h);x.textAlign='center';x.textBaseline='middle';
for(const q of M){q.y+=q.v*k/9e3;if(q.y>1.15){q.y=-.15;q.x=Math.random()}x.save();x.globalAlpha=.22;x.translate((q.x+Math.sin(t/1400+q.p)*.03)*w,q.y*h);x.rotate(Math.sin(t/900+q.p)*.5);x.font=q.s*d+'px system-ui';x.fillText(q.e,0,0);x.restore()}
for(const b of B){const a=(t-b.t)/1e3;if(a>1.6)continue;x.save();x.translate((b.x+b.u*a*60)*d,(b.y+(b.v*a+9*a*a)*21)*d);x.rotate(b.r+a*8);x.fillStyle=`hsla(${b.c},70%,55%,${1-a/1.6})`;x.fillRect(-4*d,-2*d,8*d,4*d);x.restore()}
requestAnimationFrame(F)};requestAnimationFrame(F);
document.querySelectorAll('[data-n]').forEach(e=>{if(R)return;const n=+e.dataset.n,p=e.textContent,s=performance.now(),f=new Intl.NumberFormat('ru-RU');const T=z=>{const k=Math.min(1,(z-s)/900);e.textContent=p.replace(/[\d\s ]+\d/,f.format(Math.round(n*(1-Math.pow(1-k,3)))));k<1&&requestAnimationFrame(T)};requestAnimationFrame(T)});
const j=document.querySelector('.j');j&&setTimeout(()=>burst(j),300);
document.body.addEventListener('click',ev=>{W&&W.HapticFeedback&&W.HapticFeedback.selectionChanged();
if(j){j.classList.remove('bo');void j.offsetWidth;j.classList.add('bo');burst(j)}
if(document.querySelector('[data-b]')){const on=document.body.classList.toggle('fl');document.querySelectorAll('[data-a]').forEach(e=>e.textContent=on?e.dataset.b:e.dataset.a)}
const row=ev.target.closest('.c');if(row){const was=row.classList.contains('on');document.querySelectorAll('.c').forEach(e=>e.classList.remove('on'));was||row.classList.add('on');row.parentNode.classList.toggle('pk',!was)}});
W&&W.ready&&W.ready()})();
