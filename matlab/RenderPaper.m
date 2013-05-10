function RenderPaper

% Settings
imageWidth = 1024;
imageHeight = 768;
aspect = imageWidth/imageHeight;
%%%%%%%%%%%%%%%%%%
camPosDifference = [0.0; 40; 0];
% camPosDifference = [0.0; 40; -80];
%%%%%%%%%%%%%%%%%%

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
        folderName = folderList{ii};
        disp([datestr(now) '  ' folderName]);
        location = ['../' folderName];
		imageLoc = [location filesep 'image-paper'];
		% Make output folder if it doesn't exist already
		if ~exist(imageLoc ,'dir') && exist(location,'dir')        % Added second statement so we don't generate the base folder if it was removed
			mkdir(imageLoc );
		end
		loadFileNameList = dir([location filesep 'output' filesep '*.mat']);
		loadFileNameList = {loadFileNameList.name};
		%%%%%%%%%%%%%%%%%%%%%
		% Use whole range
% 		loadFileRange = length(loadFileNameList):-1:1;
		% OR use selected numbers
		loadFileRange = [131 96 80 60 48 47 0];
		loadFileRange = loadFileRange(loadFileRange<length(loadFileNameList))+1;		% Kick out ones we don't have, adjust index
		%%%%%%%%%%%%%%%%%%%%%
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
				for ii=0:NSave
					if exist(povName{ii+1},'file'),	 delete(povName{ii+1}); end
					fid = fopen(povName{ii+1},'a');
					%%%%%%%%%%%%%%%%%%
% 					right = RenderCalcRight(model, imageWidth, imageHeight, camPosDifference);
% 					right = 80;		% AS
% 					right = 45;		% E coli, persp
					right = 56.5;	% E coli, top down
					%%%%%%%%%%%%%%%%%%
					RenderFun(fid, model, ii, right, aspect, plane, camPosDifference);
					% Finalise the file
					fclose(fid);
					systemInput = ['povray ' povName{ii+1} ' +W' num2str(imageWidth) ' +H' num2str(imageHeight) ' +O' imageLoc filesep imageName{ii+1} ' +A +J +Q11'];
					remove = ['rm ' povName{ii+1}];
					[~,message] = system(['cd ' location ' ; ' systemInput ' ; cd ..']);
% 					% Append text for relaxation and growth
% 					system(['convert -antialias -pointsize 30 -font courier-bold -annotate 0x0+30+50 ''Growth time:     ' sprintf('%5.2f h',model.growthIter*model.growthTimeStep/3600.0) '\nRelaxation time: ' sprintf('%5.2f s'' ',model.relaxationIter*model.relaxationTimeStep+ii*model.relaxationTimeStepdt)  imagePath{ii+1} ' ' imagePath{ii+1}]);
					% Append scale bar, if no place is used for scale
					if ~plane
						LLine = 5*imageWidth/right;
						system(['convert -antialias -pointsize 30 -font courier-bold -annotate 0x0+880+50 ''5 um'' ' imagePath{ii+1} ' ' imagePath{ii+1}]);
						system(['convert -stroke black -strokewidth 3 -draw "line ' num2str(imageWidth-110-LLine/2) ',70 ' num2str(imageWidth-110+LLine/2) ',70" ' imagePath{ii+1} ' ' imagePath{ii+1}]);
					end
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