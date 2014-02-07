
range = 1:10;
Nsim = length(range);

Ni = 100;								% Rather too big than too small

ttc = cell(3,1);
htc = cell(3,1);
for mm = 1:3
	ttc{mm} = nan(Ni,Nsim);
	htc{mm} = nan(Ni,Nsim);
end

jj=0;
for ii=range
    fprintf('%d... ',ii);
    jj=jj+1;
	[tt{1},ht{1}] = height(['../results/ecoli_noanchor_seed' num2str(ii)]);
	[tt{2},ht{2}] = height(['../results/ecoli_anchor_seed' num2str(ii)]);
	[tt{3},ht{3}] = height(['../results/ecoli_filament_seed' num2str(ii)]);
	% Check of we should append or not
	for mm = 1:3
		if ~isempty(tt{mm})
			ttc{mm}(1:length(tt{mm}),jj) = tt{mm};
			htc{mm}(1:length(ht{mm}),jj) = ht{mm};
		end
	end
end

% Error check: did anything funny happen to dimenions?
for mm = 1:3
	if any(size(ttc{mm})~=[Ni Nsim])
		error(['Dimension mismatch in tt vectors, simulation index ' num2str(mm)]);
	end
end
	
tt =	cell(3,1);
htmu =	cell(3,1);
htstd = cell(3,1);
Nsam =	cell(3,1);
htci =	cell(3,1);

for mm = 1:3
	% Strip full nan rows from collections (different from runHeight because we only want to remove trailing NaN)
	ttc{mm}(find(all(~isnan(ttc{mm}),2), 1,'last')+1:end,:)=[];
	htc{mm}(find(all(~isnan(htc{mm}),2), 1,'last')+1:end,:)=[];

	% Collect mean and standard deviation data
	tt{mm}		= nanmean(ttc{mm},2);
	htmu{mm}	= nanmean(htc{mm},2);
	htstd{mm}	= nanstd(htc{mm},[],2);

	% Determine confidence interval
	Nsam{mm} = sum(~isnan(htc{mm}),2);
	htci{mm} = tinv(1-0.025,Nsam{mm}-1)  .*  htstd{mm}  ./  sqrt( Nsam{mm});
end

% Plot
colour = [0.6 0.0 0.0; 
	0.7 0.9 1.0; 
	0.2 1.0 0.2];
figure
h = struct;
opacity = 0.50;
muBrightness = 1.0;
set(gcf,'renderer','openGL');		% opacity requires bitmap
% set(gcf,'PaperPosition',[0.634517 6.34517 20.3046 15.2284])
set(gcf,'PaperPositionMode','manual');
% set(gcf,'PaperSize',[20.984 29.6774]);
set(gcf,'Position',[300,300,500,400]);
set(gcf,'DefaultAxesColorOrder', colour);

set(gcf,'color',[1 1 1])
dt = 4/60;
micron = 1e6;
hold on;

ttP = cell(3,1);
htP = cell(3,1);
for mm = 1:3
	% Plot means
	h.mu(mm) = plot(dt*tt{mm},htmu{mm}.*micron,'linewidth',2,'color',colour(mm,:).*muBrightness);
	% Plot confidence intervals (parts stolen from Rob Campbell, http://www.mathworks.com.au/matlabcentral/fileexchange/26311-shadederrorbar, November 2009)
	% ci patch
	ttP{mm} = [tt{mm};flipud(tt{mm})];					% P for patch
	htP{mm} = [ htmu{mm} - htci{mm}; flipud(htmu{mm} + htci{mm}) ].*micron;
	ttP{mm}(isnan(htP{mm}))=[];							% Strip NaNs, patch doesn't work with NaNs
	htP{mm}(isnan(htP{mm}))=[];
	h.ci(mm) = patch(dt*ttP{mm},htP{mm},1,'facecolor',colour(mm,:),'edgecolor','none','facealpha',opacity);
	% ci limits
	h.ul(mm) = plot(dt*tt{mm},(htmu{mm}+htci{mm}).*micron,'linewidth',1,'color',colour(mm,:).*muBrightness);
	h.ll(mm) = plot(dt*tt{mm},(htmu{mm}-htci{mm}).*micron,'linewidth',1,'color',colour(mm,:).*muBrightness);
end
% Set patches to foreground (http://www.mathworks.com/matlabcentral/answers/92405)
set(h.ci,'faceoffsetbias',-0.001)

% Set all text
xlabel('Time (h)','FontName','Times New Roman','FontSize',12);
ylabel('Biofilm height (\mum)','FontName','Times New Roman','FontSize',12)
set(gca,'FontName','Times New Roman','FontSize',9);
h.leg = legend(h.mu,'Default case','Anchoring links','Filial links','Location','NorthWest');	% Set legends for means only
legend(h.leg,'boxoff');

% Cut off axis at end of data
a=axis; 
for mm = 1:3
	a(2) = min(max(tt{mm})*dt,a(2));
end
axis(a);

% % Render with MYAA (http://www.mathworks.com.au/matlabcentral/fileexchange/20979-myaa-my-anti-alias-for-matlab)
% myaa([16,8]); % Downsample less than supersample --> higher resolution
% imwrite(getfield(getframe(gca),'cdata'),'height.png');