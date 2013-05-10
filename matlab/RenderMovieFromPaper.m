clear;

while true
	sketch = false;
	text = true;
	
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
		if ~exist(['../' folderName '/image-paper'],'dir')
            remove = true;
        end
        if remove
            folderList(ii)=[];
        end
	end
	
    % Analyse these folders
    for ii=1:length(folderList)
        clear right;
        folderName = folderList{ii};
        disp([datestr(now) '  ' folderName]);
        location = ['../' folderName];
		imageLoadLoc = [location filesep 'image-paper'];
		imageLoc = [location filesep 'image-movie'];
		% Create folder if it doesn't exist
		if ~exist(imageLoc ,'dir')
			mkdir(imageLoc);
		end
		% Figure out where to append
		loadFileNameList = dir([location filesep 'output' filesep '*.mat']);
		loadFileNameList = {loadFileNameList.name};
		for iFile=length(loadFileNameList):-1:1
			loadFileName = loadFileNameList{iFile};
			if ~exist([imageLoadLoc filesep 'pov_' loadFileName(1:end-4) '_00.png'],'file') || exist([imageLoc filesep 'pov_' loadFileName(1:end-4) '_00.png'],'file')
				continue                % Not plotted, skip
			end
			fprintf([loadFileName '\n']);
			try
				load([location filesep 'output' filesep loadFileName]);
				% Append text
				NSave = length(model.ballArray(1).posSave);
				if model.relaxationIter==0 && model.growthIter==0
					NSave = 0;
				end
				for jj=0:NSave
					imageName{jj+1} = sprintf('pov_g%04dr%04d_%02d', model.growthIter, model.relaxationIter, jj);
					imagePath{jj+1} = [imageLoc filesep imageName{jj+1} '.png'];
					imageLoadPath{jj+1} = [imageLoadLoc filesep imageName{jj+1} '.png'];
					povName{jj+1} = [location sprintf('/output/pov_g%04dr%04d_%02d.pov', model.growthIter, model.relaxationIter, jj)];
					system(['convert -antialias -pointsize 30 -font courier-bold -annotate 0x0+30+50 ''Growth time:     ' sprintf('%5.2f h',model.growthIter*model.growthTimeStep/3600.0) '\nRelaxation time: ' sprintf('%5.2f s'' ',model.relaxationIter*model.relaxationTimeStep+jj*model.relaxationTimeStepdt)  imageLoadPath{jj+1} ' ' imagePath{jj+1}]);
				end
			catch ME
				continue;
			end
		end

    end
    disp([datestr(now) '  waiting...']);
    pause(10);
end