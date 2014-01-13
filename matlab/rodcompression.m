if ~exist('location','var')
	location = uigetdir;
	if isempty(location)
		return
	end
end

pad = [location '/output/'];

files=dir([pad '*.mat']);

meanDL = [];
sigmaDL = [];
for iFile = 1:length(files)
	load([pad files(iFile).name]);
    
    DL = [];
    for ii=1:length(model.rodSpringArray)
        spring = model.rodSpringArray(ii);
         L = norm(model.ballArray(spring.ballArray(2)+1).pos - model.ballArray(spring.ballArray(1)+1).pos);
         Lr = spring.restLength;
         DL(end+1) = L-Lr;
    end
    meanDL(end+1) = mean(DL);
    sigmaDL(end+1) = std(DL);
end

plot([1:length(meanDL)], [meanDL; meanDL+2*sigmaDL; meanDL-2*sigmaDL])