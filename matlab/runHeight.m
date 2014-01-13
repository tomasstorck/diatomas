% close all;

figure
set(gcf,'PaperPosition',[0.634517 6.34517 20.3046 15.2284])
set(gcf,'PaperPositionMode','manual');
set(gcf,'PaperSize',[20.984 29.6774]);
set(gcf,'Position', [360 278 357 259]);

[tt0,ht0] = height('../ecoli_noanchor');
[tt1,ht1] = height('../ecoli_anchor');
[tt2,ht2] = height('../ecoli_fil');


dt = 4/60;
micron = 1e6;
hold on;
h0=plot(tt0*dt,ht0*micron,'k-');
h1=plot(tt1*dt,ht1*micron,'k--');
h2=plot(tt2*dt,ht2*micron,'k-.');

lw = 2;
set(h0,'LineWidth',lw);
set(h1,'LineWidth',lw);
set(h2,'LineWidth',lw);

legend('without anchoring and filial','anchoring only','filial only','Location','NorthWest')

xlabel('Time (h)');
ylabel('Biofilm height (micron)')

set(gcf,'color',[1 1 1])