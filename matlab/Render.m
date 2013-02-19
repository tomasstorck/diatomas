%%%%%%%%%

sketch = true;

%%%%%%%%%

if ~exist('location','var')
	location = uigetdir('../');
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
    if exist([location filesep 'image' filesep 'pov_' loadFileName(1:end-4) '_00.png'],'file')
        continue                % Already plotted, skip
    end
	fprintf([loadFileName '\n']);
% 	try
		load([location filesep 'output' filesep loadFileName]);
		if rem(model.relaxationIter,10)==0
			clear right;
		end
		RenderFun;
% 	catch ME
% 		continue;
% 	end
end
