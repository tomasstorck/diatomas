function RenderUnmark(root)

if ~exist('root','var')
    root = '';
end

% Make list of folders with an output subfolder
folderList = dir(['../' root '/results/']);
folderList = {folderList.name};
for ii=length(folderList):-1:1
	remove = false;
	folderName = folderList{ii};
	if folderName(1)=='.';
		remove = true;
	end
	if ~exist(['../' root '/results/' folderName '/output'],'dir')
		remove = true;
	end
	if remove
		folderList(ii)=[];
	end
end

% Analyse these folders
for ii=1:length(folderList)
	if exist(['../' root '/results/' folderList{ii} filesep 'rendering'],'file')
		delete(['../' root '/results/' folderList{ii} filesep 'rendering']);
	end
end