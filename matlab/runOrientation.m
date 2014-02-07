
range = 1:10;
Nsim = length(range);

Ni = 100;								% Rather too big than too small

ttc = cell(3,1);
occ = cell(3,1);
for mm = 1:3
	ttc{mm} = nan(Ni,Nsim);
	occ{mm} = nan(Ni,Nsim);
end

jj=0;
for ii=range
    fprintf('%d... ',ii);
    jj=jj+1;
	[tt{1},oc{1}] = orientation(['../results/ecoli_noanchor_seed' num2str(ii)]);
	[tt{2},oc{2}] = orientation(['../results/ecoli_anchor_seed' num2str(ii)]);
	[tt{3},oc{3}] = orientation(['../results/ecoli_filament_seed' num2str(ii)]);
	% Check of we should append or not
	for mm = 1:3
		if ~isempty(tt{mm})
			ttc{mm}(1:length(tt{mm}),jj) = tt{mm};
			occ{mm}(1:length(oc{mm}),jj) = oc{mm};
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
ocmu =	cell(3,1);
ocstd = cell(3,1);
Nsam =	cell(3,1);
occi =	cell(3,1);

for mm = 1:3
	% Strip full nan rows from collections (different from runHeight because we only want to remove trailing NaN)
	ttc{mm}(find(all(~isnan(ttc{mm}),2), 1,'last')+1:end,:)=[];
	occ{mm}(find(all(~isnan(occ{mm}),2), 1,'last')+1:end,:)=[];

	% Collect mean and standard deviation data
	tt{mm}		= nanmean(ttc{mm},2);
	ocmu{mm}	= nanmean(occ{mm},2);
	ocstd{mm}	= nanstd(occ{mm},[],2);

	% Determine confidence interval
	Nsam{mm} = sum(~isnan(occ{mm}),2);
	occi{mm} = tinv(1-0.025,Nsam{mm}-1)  .*  ocstd{mm}  ./  sqrt( Nsam{mm});
end

% Plot
colour = [0.6 0.0 0.0; 
	0.5 0.8 1.0; 
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
ocP = cell(3,1);
for mm = 1:3
	% Plot means
	h.mu(mm) = plot(dt*tt{mm},ocmu{mm},'linewidth',2,'color',colour(mm,:).*muBrightness);
	% Plot confidence intervals (parts stolen from Rob Campbell, http://www.mathworks.com.au/matlabcentral/fileexchange/26311-shadederrorbar, November 2009)
	% ci patch
	ttP{mm} = [tt{mm};flipud(tt{mm})];					% P for patch
	ocP{mm} = [ ocmu{mm} - occi{mm}; flipud(ocmu{mm} + occi{mm}) ];
	ttP{mm}(isnan(ocP{mm}))=[];							% Strip NaNs, patch doesn't work with NaNs
	ocP{mm}(isnan(ocP{mm}))=[];
	h.ci(mm) = patch(dt*ttP{mm},ocP{mm},1,'facecolor',colour(mm,:),'edgecolor','none','facealpha',opacity);
	% ci limits
	h.ul(mm) = plot(dt*tt{mm},(ocmu{mm}+occi{mm}),'linewidth',1,'color',colour(mm,:).*muBrightness);
	h.ll(mm) = plot(dt*tt{mm},(ocmu{mm}-occi{mm}),'linewidth',1,'color',colour(mm,:).*muBrightness);
end
% Set patches to foreground (http://www.mathworks.com/matlabcentral/answers/92405)
set(h.ci,'faceoffsetbias',-0.001)

% Set all text
xlabel('Time (h)','FontName','Times New Roman','FontSize',12);
ylabel('Orientation correlation (-)','FontName','Times New Roman','FontSize',12)
set(gca,'FontName','Times New Roman','FontSize',9);
hl = legend(h.mu,'Default case','Anchoring links','Filial links','Location','SouthWest');	% Set legends for means only
legend(hl,'boxoff');
a=axis; 
for mm = 1:3
	a(2) = min(max(tt{mm})*dt,a(2));
end
a(4)=1.01; 
axis(a);

% % Render with MYAA (http://www.mathworks.com.au/matlabcentral/fileexchange/20979-myaa-my-anti-alias-for-matlab)
% myaa([16,8]); % Downsample less than supersample --> higher resolution
% imwrite(getfield(getframe(gca),'cdata'),'orientation.png');