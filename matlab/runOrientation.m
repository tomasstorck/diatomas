
range = 1:3;
Nsim = length(range);

Ni = 100;								% Rather too big than too small

tt0c = nan(Ni,Nsim);					% We'll use nan for no data available 
tt1c = nan(Ni,Nsim);
tt2c = nan(Ni,Nsim);
oc0c = nan(Ni,Nsim);
oc1c = nan(Ni,Nsim);
oc2c = nan(Ni,Nsim);

jj=0;
for ii=range
    fprintf('%d... ',ii);
    jj=jj+1;
	[tt0,oc0] = orientation(['../results/ecoli_noanchor_seed' num2str(ii)]);
	[tt1,oc1] = orientation(['../results/ecoli_anchor_seed' num2str(ii)]);
	[tt2,oc2] = orientation(['../results/ecoli_filament_seed' num2str(ii)]);
	% Check of we should append or not
	if ~isempty(tt0)
		tt0c(1:length(tt0),jj) = tt0;
		oc0c(1:length(oc0),jj) = oc0;
	end
	if ~isempty(tt1)
		tt1c(1:length(tt1),jj) = tt1;
		oc1c(1:length(oc1),jj) = oc1;
	end
	if ~isempty(tt2)
		tt2c(1:length(tt2),jj) = tt2;
		oc2c(1:length(oc2),jj) = oc2;
	end
end

% Error check: did anything funny happen to dimenions?
if		any(size(tt0c)~=[Ni Nsim]) || any(size(tt1c)~=[Ni Nsim]) || any(size(tt2c)~=[Ni Nsim])
	error('Dimension mismatch in tt vectors')
elseif	any(size(oc0c)~=[Ni Nsim]) || any(size(oc1c)~=[Ni Nsim]) || any(size(oc2c)~=[Ni Nsim])
	error('Dimension mismatch in oc vectors')
end
	
% Strip full nan rows from collections (different from runHeight because we only want to remove trailing NaN)
tt0c(all(isnan(tt0c),2),:)=[];
tt1c(all(isnan(tt1c),2),:)=[];
tt2c(all(isnan(tt2c),2),:)=[];
oc0c(find(all(~isnan(oc0c),2), 1,'last')+1:end,:)=[];
oc1c(find(all(~isnan(oc0c),2), 1,'last')+1:end,:)=[];
oc2c(find(all(~isnan(oc0c),2), 1,'last')+1:end,:)=[];

% Collect mean and standard deviation data
tt0		= nanmean(tt0c,2);
tt1		= nanmean(tt1c,2);
tt2		= nanmean(tt2c,2);
oc0mu	= nanmean(oc0c,2);
oc1mu	= nanmean(oc1c,2);
oc2mu	= nanmean(oc2c,2);
oc0std	= nanstd(oc0c,[],2);
oc1std	= nanstd(oc1c,[],2);
oc2std	= nanstd(oc2c,[],2);

% Determine confidence interval
Nsam0 = sum(~isnan(oc0c),2);
Nsam1 = sum(~isnan(oc1c),2);
Nsam2 = sum(~isnan(oc2c),2);
oc0ci = tinv(1-0.025,Nsam0-1)  .*  oc0std  ./  sqrt( Nsam0);
oc1ci = tinv(1-0.025,Nsam1-1)  .*  oc1std  ./  sqrt( Nsam1);
oc2ci = tinv(1-0.025,Nsam2-1)  .*  oc2std  ./  sqrt( Nsam2);


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
	0.9 0.9 0.0; 0.9 0.9 0.0; 0.9 0.9 0.0; ...
	0.2 0.8 1.0; 0.2 0.8 1.0; 0.2 0.8 1.0]);
% set(gcf,'DefaultAxesColorOrder',[...
% 	0.1 0.1 1.0; 0.1 0.1 1.0; 0.1 0.1 1.0; ...
% 	0.5 0.5 0.5; 0.5 0.5 0.5; 0.5 0.5 0.5; ...
% 	1.0 0.8 0.8; 1.0 0.8 0.8; 1.0 0.8 0.8]);

xlabel('Time (h)');
ylabel('Orientation correlation (-)')

set(gcf,'color',[1 1 1])
dt = 4/60;
micron = 1e6;
hold on;
c = 'h = plot(';
nn=0;
for mm=0:2
	nn=nn+1;
	c = [c sprintf('tt%d*dt,(oc%dmu),''-'',',mm,mm)];
	c = [c sprintf('tt%d*dt,(oc%dmu+oc%dci),''--'',',mm,mm,mm)];
	c = [c sprintf('tt%d*dt,(oc%dmu-oc%dci),''--'',',mm,mm,mm)];
end
c = [c '''LineWidth'',1);'];
eval(c);
legend(h(1:3:Nsim),'without anchoring and filial','anchoring only','filial only','Location','SouthWest')	% Set legends for means only
set(h(1:3:Nsim),'LineWidth',2)	% Set LineWidth for means
