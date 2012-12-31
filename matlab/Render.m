%%%%%%%%%

if ~exist('location','var')
	location = uigetdir;
	if isempty(location)
		return
	end
end

% Make output folder if it doesn't exist already
if ~exist([location filesep 'image'],'dir')
	mkdir([location filesep 'image']);
end

loadFileNameList = dir([location filesep 'output' filesep '*.mat']);
loadFileNameList = {loadFileNameList.name};

for iFile=length(loadFileNameList):-1:1
	loadFileName = loadFileNameList{iFile};
	fprintf([loadFileName '\n']);
	load([location filesep 'output' filesep loadFileName]);
	
	RenderFun;
end
