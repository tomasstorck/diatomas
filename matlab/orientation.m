% Based on Albertas Janulevicius' paper and paper cited therein
function [tt, Ct] = orientation(location)

if ~exist('location','var')
	location = uigetdir;
end

pad = [location '/output/'];

Ct= [];
tt = [];
t = -1;
while true			% Keep going till we run out of files
	t=t+1;
	files=dir([pad sprintf('g%04.0f*.mat',t(end))]);
	if isempty(files)
		% That was he last one, exit
		return
	end
	file = files(1).name;
	load([pad file]);

	v = [];
	for iCell = 1:length(model.cellArray)
		cell = model.cellArray(iCell);
		if cell.type<2
			continue		% This cell won't be added to v
		end
		ball0 = model.ballArray(cell.ballArray(1)+1);
		ball1 = model.ballArray(cell.ballArray(2)+1);

		v(end+1,:) = ball0.pos - ball1.pos;
	end

	theta = [];
	for ii = 1:size(v,1)
		vi = v(ii,:);
		for jj = ii+1:size(v,1)
			vj = v(jj,:);

			theta = [theta real(acosd(dot(vi,vj)/(norm(vi)*norm(vj))))];		% Used real to prevent parallel cells with freak round-off errors

		end
	end

	C = 0;
	for ii = 1:length(theta)
		C = C + 2*cosd(theta(ii))^2-1;
	end
	Ct(end+1) = C/length(theta);
	tt(end+1)  =	t;
end

% hold on; plot(ttN, CtN, 'LineWidth',2,'Color',[0 0 0]+0.7); plot(ttY, CtY, 'LineWidth',2,'Color',[0 0 0]);
% legend('no anchoring','anchoring'); xlabel('growth time (h)'), ylabel('Orientation correlation (-)'); set(gcf,'color','white')
