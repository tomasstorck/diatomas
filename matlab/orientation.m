if ~exist('location','var')
	location = uigetdir;
	if isempty(location)
		return
	end
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
	for ii = 1:length(v)
		vi = v(ii,:);
		for jj = ii+1:length(v)
			vj = v(jj,:);

			theta = [theta acosd(dot(vi,vj)/(norm(vi)*norm(vj)))];

		end
	end

	C = 0;
	for ii = 1:length(theta)
		C = C + 2*cosd(theta(ii))^2-1;
	end
	Ct(end+1) = C/length(theta);
	tt(end+1)  =	t;
end