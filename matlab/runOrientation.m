% close all;
figure
set(gcf,'PaperPosition',[0.634517 6.34517 20.3046 15.2284])
set(gcf,'PaperPositionMode','manual');
set(gcf,'PaperSize',[20.984 29.6774]);
set(gcf,'Position', [360 278 357 259]);

[tt0,o0] = orientation('../ecoli_noanchor');
[tt1,o1] = orientation('../ecoli_anchor');
[tt2,o2] = orientation('../ecoli_fil');

dt = 4/60;
hold on;
h0=plot(tt0*dt,o0,'k-');
h1=plot(tt1*dt,o1,'k--');
h2=plot(tt2*dt,o2,'k-.');

lw = 2;
set(h0,'LineWidth',lw);
set(h1,'LineWidth',lw);
set(h2,'LineWidth',lw);

legend('no anchoring/filial','anchoring only','filial only','Location','NorthWest')

xlabel('Time (h)');
ylabel('Orientation correlation (-)')

set(gcf,'color',[1 1 1])