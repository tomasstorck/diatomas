function RenderMovie

% Settings
imageWidth = 1024;
imageHeight = 768;
aspect = imageWidth/imageHeight;
camPosDifference = [0.0; 40; -80];		% Where the camera will hover compared to camView

while true
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
		% Clear camera zoom
		clear right;
		% Get folder name, mark as being rendered and start rendering
        folderName = folderList{ii};
        disp([datestr(now) '  ' folderName]);
        location = ['../' folderName];
		imageLoc = [location filesep 'image-movie'];
		% See if this is already being rendered
		if exist([location filesep 'rendering'],'file')
			disp([datestr(now) '  ' '  already being rendered, skipping']);
			continue
		else 
			frendering = fopen([location filesep 'rendering'],'w');
			fclose(frendering);
		end
		% Make output folder if it doesn't exist already
		if ~exist(imageLoc ,'dir') && exist(location,'dir')        % Added second statement so we don't generate the base folder if it was removed
			mkdir(imageLoc);
		end
		loadFileNameList = dir([location filesep 'output' filesep '*.mat']);
		loadFileNameList = {loadFileNameList.name};
		loadFileRange = length(loadFileNameList):-1:1;
		for iFile=loadFileRange
			loadFileName = loadFileNameList{iFile};
			if exist([imageLoc filesep 'pov_' loadFileName(1:end-4) '_00.png'],'file')
				continue                % Already plotted, skip
			end
			fprintf([loadFileName '\n']);
			try
				load([location filesep 'output' filesep loadFileName]);
				plane = model.normalForce;
				NSave = length(model.ballArray(1).posSave);
				if model.relaxationIter==0 && model.growthIter==0
					NSave = 0;
				end
				for ii=0:NSave
					imageName{ii+1} = sprintf('pov_g%04dr%04d_%02d', model.growthIter, model.relaxationIter, ii);
					imagePath{ii+1} = [imageLoc filesep imageName{ii+1} '.png'];
					povName{ii+1} = [location sprintf('/output/pov_g%04dr%04d_%02d.pov', model.growthIter, model.relaxationIter, ii)];
				end
				if rem(iFile,5)==0 || ~exist('right','var')
					right = RenderCalcRight(model, imageWidth, imageHeight, camPosDifference);
				end
				for ii=0:NSave
					fid = fopen(povName{ii+1},'a');
					RenderFun(fid, model, ii, right, aspect, plane, camPosDifference);
					% Finalise the file
					fclose(fid);
					systemInput = ['povray ' povName{ii+1} ' +W' num2str(imageWidth) ' +H' num2str(imageHeight) ' +O' imageLoc filesep imageName{ii+1} ' +A -J'];
					remove = ['rm ' povName{ii+1}];
					[~,message] = system(['cd ' location ' ; ' systemInput ' ; cd ..']);
					% Append text for relaxation and growth
					system(['convert -antialias -pointsize 30 -font courier-bold -annotate 0x0+30+50 ''Growth time:     ' sprintf('%5.2f h',model.growthIter*model.growthTimeStep/3600.0) '\nRelaxation time: ' sprintf('%5.2f s'' ',model.relaxationIter*model.relaxationTimeStep+ii*model.relaxationTimeStepdt)  imagePath{ii+1} ' ' imagePath{ii+1}]);
					% Append scale bar, if no place is used for scale
					if ~plane
						LLine = imageWidth/right;
						system(['convert -antialias -pointsize 30 -font courier-bold -annotate 0x0+880+50 ''1 um'' ' imagePath{ii+1} ' ' imagePath{ii+1}]);
						system(['convert -stroke black -strokewidth 3 -draw "line ' num2str(imageWidth-110-LLine/2) ',70 ' num2str(imageWidth-110+LLine/2) ',70" ' imagePath{ii+1} ' ' imagePath{ii+1}]);
					end
					% Remove POV file if desired
					[~,~] = system(['cd ' location ' ; ' remove ' ; cd ..']);
				end
			catch ME
				if exist([location filesep 'rendering'],'file')
                    % Done with this folder, delete "mark as rendered"
					delete([location filesep 'rendering']);
					continue
				end
				continue;
			end
		end
		% Done with this folder
		delete([location filesep 'rendering']);
    end
    disp([datestr(now) '  waiting...']);
    pause(10);
end