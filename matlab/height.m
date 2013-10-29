function [tt, ht] = height(location)

if ~exist('location','var')
	location = uigetdir;
end

pad = [location '/output/'];

ht= [];
tt = [];
t = -1;
tmax = 83;

while t<tmax			% Keep going till we run out of files
	maxHeight=0;
	t=t+1;
	files=dir([pad sprintf('g%04.0f*.mat',t(end))]);
	if isempty(files)
		% That was he last one, exit
		return
	end
	file = files(1).name;
	load([pad file]);

	for iBall = 1:length(model.ballArray)
		ball = model.ballArray(iBall);
		maxHeight = max(maxHeight, ball.pos(2));
	end
	
	ht(end+1) = maxHeight;
	tt(end+1) = t;
end