function RenderMovie

% Make list of folders with an output subfolder
folderList = dir('../');
folderList = {folderList.name};
for ii=length(folderList):-1:1
	remove = false;
	folderName = folderList{ii};
	if folderName(1)=='.';
		remove = true;
	end
	if ~exist(['../' folderName '/output'],'dir')
		remove = true;
	end
	if remove
		folderList(ii)=[];
	end
end

% Analyse these folders
for ii=1:length(folderList)
	if exist(['..' filesep folderList{ii} filesep 'rendering'],'file')
		delete(['..' filesep folderList{ii} filesep 'rendering']);
	end
end