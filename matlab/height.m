if ~exist('location','var')
	location = uigetdir;
	if isempty(location)
		return
	end
end

pad = [location '/output/'];

files=dir([pad '*.mat']);

maxHeight=0;
maxHeight_time = [];
for iFile = 1:length(files)
	load([pad files(iFile).name]);

	for iBall = 1:length(model.ballArray)
		ball = model.ballArray(iBall);
		maxHeight = max(maxHeight, ball.pos(2));
	end
	
	maxHeight_time(end+1) = maxHeight;
end