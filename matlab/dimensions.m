if ~exist('location','var')
	location = uigetdir;
	if isempty(location)
		return
	end
end

pad = [location '/output/'];

files=dir([pad '*.mat']);

meanL = [];
sigmaL = [];
meanr = [];
sigmar = [];
for iFile = 1:length(files)
	load([pad files(iFile).name]);
    
    L = []; r = [];
	for iCell = 1:length(model.cellArray)
		cell = model.cellArray(iCell);
        ball0 = model.ballArray(cell.ballArray(1)+1);
        ball1 = model.ballArray(cell.ballArray(2)+1);
        L(end+1) = norm(ball1.pos - ball0.pos);
		r(end+1) = ball0.radius;
    end
    meanL(end+1) = mean(L);
    sigmaL(end+1) = std(L);
    meanr(end+1) = mean(r);
    sigmar(end+1) = std(r);
    
end

