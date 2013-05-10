function RenderSketch

% Settings
imageWidth = 1024/2;
imageHeight = 768/2;
aspect = imageWidth/imageHeight;
plane = true;
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
		clear right;
        folderName = folderList{ii};
        disp([datestr(now) '  ' folderName]);
        location = ['../' folderName];
		imageLoc = [location filesep 'image-sketch'];
		% Make output folder if it doesn't exist already
		if ~exist(imageLoc ,'dir') && exist(location,'dir')        % Added second statement so we don't generate the base folder if it was removed
			mkdir(imageLoc );
		end
		loadFileNameList = dir([location filesep 'output' filesep '*.mat']);
		loadFileNameList = {loadFileNameList.name};
		loadFileRange = length(loadFileNameList):-1:1;
		for iFile=loadFileRange
			loadFileName = loadFileNameList{iFile};
			if exist([imageLoc filesep 'pov_' loadFileName(1:end-4) '_00.png'],'file') ||
				exist([location filesep 'image-movie' filesep 'pov_' loadFileName(1:end-4) '_00.png'],'file')
				continue                % Already plotted, skip
			end
			fprintf([loadFileName '\n']);
			try
				load([location filesep 'output' filesep loadFileName]);
				NSave = length(model.ballArray(1).posSave);
				if model.relaxationIter==0 && model.growthIter==0
					NSave = 0;
				end
				for ii=0:2:NSave
					imageName{ii+1} = sprintf('pov_g%04dr%04d_%02d', model.growthIter, model.relaxationIter, ii);
					imagePath{ii+1} = [imageLoc filesep imageName{ii+1} '.png'];
					povName{ii+1} = [location sprintf('/output/pov_g%04dr%04d_%02d.pov', model.growthIter, model.relaxationIter, ii)];
				end
				for ii=0:2:NSave
					fid = fopen(povName{ii+1},'a');
					if rem(iFile,5)==0 || ~exist('right','var')
						right = RenderCalcRight(model, imageWidth, imageHeight, plane, camPosDifference);
					end
					RenderFun(fid, model, ii, right, aspect, camPosDifference);
					% Finalise the file
					fclose(fid);
					systemInput = ['povray ' povName{ii+1} ' +W' num2str(imageWidth) ' +H' num2str(imageHeight) ' +O' imageLoc filesep imageName{ii+1} ' +A +Q4'];		% +A +Q4 instead of +A -J
					remove = ['rm ' povName{ii+1}];
					[~,message] = system(['cd ' location ' ; ' systemInput ' ; cd ..']);
					% Append text for relaxation and growth
					system(['convert -antialias -pointsize 30 -font courier-bold -annotate 0x0+30+50 ''Growth time:     ' sprintf('%5.2f h',model.growthIter*model.growthTimeStep/3600.0) '\nRelaxation time: ' sprintf('%5.2f s'' ',model.relaxationIter*model.relaxationTimeStep+ii*model.relaxationTimeStepdt)  imagePath{ii+1} ' ' imagePath{ii+1}]);
					
					% 	% Append scale bar
					% 	A = camPos;
					% 	C = camView;
					% 	AC = norm(A-C);
					% 	BC = tan(deg2rad(0.5*camAngle))*AC;
					% 	LLine = 1/BC * imageWidth/2;
					% 	system(['convert -antialias -pointsize 30 -font courier-bold -annotate 0x0+880+50 ''1 um'' ' imageLoc{ii+1} ' ' imageLoc{ii+1}]);
					% 	system(['convert -stroke black -strokewidth 3 -draw "line ' num2str(imageWidth-110-LLine/2) ',70 ' num2str(imageWidth-110+LLine/2) ',70" ' imageLoc{ii+1} ' ' imageLoc{ii+1}]);
					
					% Remove POV file if desired
					[~,~] = system(['cd ' location ' ; ' remove ' ; cd ..']);
				end
			catch ME
				continue;
			end
		end
    end
    disp([datestr(now) '  waiting...']);
    pause(10);
end