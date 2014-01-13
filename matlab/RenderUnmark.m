function RenderMovie

% Make list of folders with an output subfolder
folderList = dir('../results/');
folderList = {folderList.name};
for ii=length(folderList):-1:1
	remove = false;
	folderName = folderList{ii};
	if folderName(1)=='.';
		remove = true;
	end
	if ~exist(['../results/' folderName '/output'],'dir')
		remove = true;
	end
	if remove
		folderList(ii)=[];
	end
end

% Analyse these folders
for ii=1:length(folderList)
	if exist(['../results/' folderList{ii} filesep 'rendering'],'file')
		delete(['../results/' folderList{ii} filesep 'rendering']);
	end
end