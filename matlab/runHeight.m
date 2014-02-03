
range = 1:9;
Nsim = length(range);

Ni = 100;								% Rather too big than too small

tt0c = nan(Ni,Nsim);					% We'll use nan for no data available 
tt1c = nan(Ni,Nsim);
tt2c = nan(Ni,Nsim);
ht0c = nan(Ni,Nsim);
ht1c = nan(Ni,Nsim);
ht2c = nan(Ni,Nsim);

jj=0;
for ii=range
    fprintf('%d... ',ii);
    jj=jj+1;
	[tt0,ht0] = height(['../results/ecoli_noanchor_seed' num2str(ii)]);
	[tt1,ht1] = height(['../results/ecoli_anchor_seed' num2str(ii)]);
	[tt2,ht2] = height(['../results/ecoli_filament_seed' num2str(ii)]);
	% Check of we should append or not
	if ~isempty(tt0)
		tt0c(1:length(tt0),jj) = tt0;
		ht0c(1:length(ht0),jj) = ht0;
	end
	if ~isempty(tt1)
		tt1c(1:length(tt1),jj) = tt1;
		ht1c(1:length(ht1),jj) = ht1;
	end
	if ~isempty(tt2)
		tt2c(1:length(tt2),jj) = tt2;
		ht2c(1:length(ht2),jj) = ht2;
	end
end

% Error check: did anything funny happen to dimenions?
if		any(size(tt0c)~=[Ni Nsim]) || any(size(tt1c)~=[Ni Nsim]) || any(size(tt2c)~=[Ni Nsim])
	error('Dimension mismatch in tt vectors')
elseif	any(size(ht0c)~=[Ni Nsim]) || any(size(ht1c)~=[Ni Nsim]) || any(size(ht2c)~=[Ni Nsim])
	error('Dimension mismatch in ht vectors')
end
	
% Strip full nan rows from collections
tt0c(all(isnan(tt0c),2),:)=[];
tt1c(all(isnan(tt1c),2),:)=[];
tt2c(all(isnan(tt2c),2),:)=[];
ht0c(all(isnan(ht0c),2),:)=[];
ht1c(all(isnan(ht1c),2),:)=[];
ht2c(all(isnan(ht2c),2),:)=[];

% Collect mean and standard deviation data
tt0		= nanmean(tt0c,2);
tt1		= nanmean(tt1c,2);
tt2		= nanmean(tt2c,2);
ht0mu	= nanmean(ht0c,2);
ht1mu	= nanmean(ht1c,2);
ht2mu	= nanmean(ht2c,2);
ht0std	= nanstd(ht0c,[],2);
ht1std	= nanstd(ht1c,[],2);
ht2std	= nanstd(ht2c,[],2);

% Determine confidence interval
Nsam0 = sum(~isnan(ht0c),2);
Nsam1 = sum(~isnan(ht1c),2);
Nsam2 = sum(~isnan(ht2c),2);
ht0ci = tinv(1-0.025,Nsam0-1)  .*  ht0std  ./  sqrt( Nsam0);
ht1ci = tinv(1-0.025,Nsam1-1)  .*  ht1std  ./  sqrt( Nsam1);
ht2ci = tinv(1-0.025,Nsam2-1)  .*  ht2std  ./  sqrt( Nsam2);


% Plot
set(0,'DefaultAxesFontName','Times New Roman')
set(0,'DefaultTextFontName','Times New Roman')

figure
% set(gcf,'PaperPosition',[0.634517 6.34517 20.3046 15.2284])
set(gcf,'PaperPositionMode','manual');
% set(gcf,'PaperSize',[20.984 29.6774]);
set(gcf,'Position', [ 330   548   505   285]);
set(gcf,'DefaultAxesColorOrder',[...
	0.8 0.0 0.0; 0.8 0.0 0.0; 0.8 0.0 0.0;
	0.5 1.0 0.5; 0.5 1.0 0.5; 0.5 1.0 0.5; ...
	0.2 0.8 1.0; 0.2 0.8 1.0; 0.2 0.8 1.0]);

xlabel('Time (h)');
ylabel('Biofilm height (micron)')

set(gcf,'color',[1 1 1])
dt = 4/60;
micron = 1e6;
hold on;
c = 'h = plot(';
nn=0;
for mm=0:2
	nn=nn+1;
	c = [c sprintf('tt%d*dt,(ht%dmu)*micron,''-'',',mm,mm)];
	c = [c sprintf('tt%d*dt,(ht%dmu+ht%dci)*micron,''--'',',mm,mm,mm)];
	c = [c sprintf('tt%d*dt,(ht%dmu-ht%dci)*micron,''--'',',mm,mm,mm)];
end
c = [c '''LineWidth'',2)'];
eval(c);
% h0=plot(tt0*dt,ht0*micron,'k-');
% h1=plot(tt1*dt,ht1*micron,'k--');
% h2=plot(tt2*dt,ht2*micron,'k-.');
legend(h(1:3:Nsim),'without anchoring and filial','anchoring only','filial only','Location','NorthWest')	% Set legends for means only
set(h(1:3:Nsim),'LineWidth',2)	% Set LineWidth for means
