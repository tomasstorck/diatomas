if ~exist('location','var')
	location = uigetdir;
	if isempty(location)
		return
	end
end

loadFileNameList = dir([location filesep 'output' filesep '*.mat']);
loadFileNameList = {loadFileNameList.name};

% set view to correspond more or less to POVRay
AZ = -35.197696406078933;
EL = 17.875288325852580;
view(AZ,EL);

colourMode=0;

for iFile=1:length(loadFileNameList)
	cla;
	
	loadFileName = loadFileNameList{iFile};
	fprintf([loadFileName '\n']);
	load([location filesep 'output' filesep loadFileName]);
	
	Draw;
	drawnow;
end
