% close all;

figure
set(gcf,'PaperPosition',[0.634517 6.34517 20.3046 15.2284])
set(gcf,'PaperPositionMode','manual');
set(gcf,'PaperSize',[20.984 29.6774]);
set(gcf,'Position', [360 278 357 259]);


range = 0:10;
N = length(range);

tt0 = cell(N,1);
tt1 = cell(N,1);
tt2 = cell(N,1);
ht0 = cell(N,1);
ht1 = cell(N,1);
ht2 = cell(N,1);

for ii=range
	[tt0(:,ii),ht0(:,ii)] = height(['../ecoli_noanchor_seed' num2str(ii)]);
	[tt1(:,ii),ht1(:,ii)] = height(['../ecoli_anchor_seed' num2str(ii)]);
	[tt2(:,ii),ht2(:,ii)] = height(['../ecoli_fil_seed' num2str(ii)]);
end

tt0 =		mean(tt0,2);		% Same as taking an index, tt should match for all. This will return funny values if something funny happened
ht0mu =		mean(ht0,2);
ht0std =	std()

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