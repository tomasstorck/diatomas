function RenderSketch(root)

if ~exist('root','var')
	root = '';
end

% Settings for what to plot
imageFolderName = 'sketch';
renderIter = 10;		% Which results to render, as in 1:renderIter:end
loadFileMax = 500;		% Maximum number of files to load per folder before moving on to the next

% Plot only a select range or all
fixLoadFileNameList = false;
% fixLoadFileNameList = true;
% loadFileNameList = {'g0073r0073.mat'};
% loadFileNameList = {'g0112r0112.mat'};

% % AS
% folderFilter = 'as_low*';
% plane = false;
% ceilLightColour = [0.8,0.8,0.8];
% camLightColour = [0.6 0.6 0.6];
% E coli
folderFilter = 'ecoli_*';
% plane = true;
plane = false;
ceilLightColour = [0.65,0.65,0.65];
camLightColour = [0.45 0.45 0.45];

% Resolution
resolutionFactor = 0.5;	% 0.5 is sketch, 1.0 is paper
imageWidth = 1024*resolutionFactor;
imageHeight = 768*resolutionFactor;
aspect = imageWidth/imageHeight;

% Various
% plane = true;
% plane = false;
removePOV = true;
% removePOV = false;
appendText = true;
% appendText = false;
appendScaleBar = true;
LScale = 1;		% micron
% appendScaleBar = false;

% Zooming
rightIter = 10;			% How often to realign the zoom factor ("right")
fixRight = false;		% Set our own right value
% Be sure to comment out below when fixRight = false
% fixRight = true;
% right =	148.923;	% AS paper
% right = 108;		% E. coli paper (with 90 degree rotation)

% Where the camera will hover compared to camView
camPosDifference = [0.0; 40; -80];  	% Perspective
% camPosDifference = [0.0; 40; 0];		% Top
% camPosDifference = [0.0; 0; -80];		% Side
camRotate = [0; 0; 0];
% camRotate = [0; 90; 0];					% Useful for E. coli fil in IbM paper

% Colours
cellColour = [0.60 0.00 0.00;		% Cell colours: 
	1.00 1.00 0.85;
	0.50 0.88 1.00;
	0.00 1.00 0.20];
filColour = [.10 .10 .10];			% Filament spring is black
stickColour = [0.80 .80 0.80];		% Sticking spring is white
anchorColour = [.50 .50 .50];		% Anchoring spring is grey

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

while true
    % Make list of folders with an output subfolder
    folderList = dir(['../' root '/results/' folderFilter]);
    folderList = {folderList.name};
    for ii=length(folderList):-1:1
        remove = false;
        simulationFolderName = folderList{ii};
        if simulationFolderName(1)=='.';
            remove = true;
        end
        if ~exist(['../' root '/results/' simulationFolderName '/output'],'dir')
            remove = true;
        end
        if remove
            folderList(ii)=[];
        end
    end
    
    % Analyse these folders
    for ii=1:length(folderList)
        % New folder. Clean up old data
		if ~fixRight
			clear right;
		end
		if ~fixLoadFileNameList
			clear loadFileNameList
		end
        % Get folder name, mark as being rendered and start rendering
        simulationFolderName = folderList{ii};
        disp([datestr(now) '  ' simulationFolderName]);
        location = ['../' root '/results/' simulationFolderName];
% 		imageLoc = [location filesep imageFolderName];
        % See if this is already being rendered
		if exist([location filesep 'rendering'],'file')
			disp([datestr(now) '  ' '  already being rendered, skipping']);
			continue
        elseif exist(location,'dir')
			frendering = fopen([location filesep 'rendering'],'w');
			fclose(frendering);
        else 
            continue
		end
		% Make image output folder if it doesn't exist already
		if ~exist([location '/' imageFolderName],'dir') && exist(location,'dir')        % Added second statement so we don't generate the base folder if it was removed
			mkdir([location '/' imageFolderName]);
		end
		if ~exist('loadFileNameList','var')
			loadFileNameList = dir([location filesep 'output' filesep '*.mat']);
			loadFileNameList = {loadFileNameList.name};
		end
		% Remove all that are already plotted from loadFileNameList
		removeFromFileNameList = [];
		pngNameList = dir([location '/' imageFolderName '/pov_*.png']);
		for iMat = 1:length(loadFileNameList)
			matName = loadFileNameList{iMat};
			for iPng = 1:length(pngNameList)
				pngRange = strfind(pngNameList(iPng).name,'_');
				if length(pngRange)~=2
					error(['Don''t know how to deal with file name ' pngNameExt.name]);
				end
				pngName = pngNameList(iPng).name(pngRange(1)+1:pngRange(2)-1);
				if strcmp(...
					matName(1:strfind(matName,'.')-1),...
					pngName)
					removeFromFileNameList(end+1) = iMat; %#ok<AGROW>
				end
			end
		end
		loadFileRange = length(loadFileNameList):-1:1;
		for iFile=loadFileRange
			% See if we want to break the for loop, so we can start rendering the other folders
			if find(loadFileRange==iFile) > loadFileMax
				break;
			end
			% See if we want to skip this file, if it's in removeFromFileNameList
			if any(iFile==removeFromFileNameList)
				continue;
			end
			% Continue rendering
			loadFileName = loadFileNameList{iFile};
			if exist([location '/' imageFolderName '/pov_' loadFileName(1:end-4) '_00.png'],'file')
				continue                % Already plotted, skip
			end
			fprintf([loadFileName '\n']);
% 			try
				load([location filesep 'output' filesep loadFileName]);
				NSave = length(model.ballArray(1).posSave);
				if model.relaxationIter==0 && model.growthIter==0
					NSave = 0;
				end
				for ii=0:renderIter:NSave
					imageName{ii+1} = sprintf('pov_g%04dr%04d_%02d', model.growthIter, model.relaxationIter, ii);
					imagePath{ii+1} = [location '/' imageFolderName '/' imageName{ii+1} '.png'];
					povName{ii+1} = sprintf(['pov_g%04dr%04d_%02d.pov'], model.growthIter, model.relaxationIter, ii);
				end
				for ii=0:renderIter:NSave
					if exist([location '/' imageFolderName '/' povName{ii+1}],'file')
						delete([location '/' imageFolderName '/' povName{ii+1}]);  % remove old .pov file
					end
					fid = fopen([location '/' imageFolderName '/' povName{ii+1}],'a');
					% See if we need to find right, camView, or both
					if (rem(iFile,rightIter)==0 && ~fixRight)  || ~exist('right','var') || ~exist('camView','var')
						[newRight, newCamView] = RenderCalcRight(model, imageWidth, imageHeight, camPosDifference);
						if rem(iFile,rightIter)==0 || ~exist('right','var')
							right = newRight;
% 							%%%%%%%%%%
% 							% Horribly ugly fix for rotation (FIXME)
% 							right = right*imageWidth/imageHeight;
% 							%%%%%%%%%%
						end
						if rem(iFile,rightIter)==0 || ~exist('camView','var')
							camView = newCamView;
						end
					end
					RenderBuildPov(fid, model, ii, right, aspect, plane, camPosDifference, ceilLightColour,camLightColour, cellColour, filColour, stickColour, anchorColour, camView, camRotate);
					% Finalise the file
					fclose(fid);
					systemInput = ['povray ' imageFolderName '/' povName{ii+1} ' +W' num2str(imageWidth) ' +H' num2str(imageHeight) ' +O' imageFolderName '/' imageName{ii+1} ' +A +Q4'];		% +A +Q4 instead of +A -J
					[~,message] = system(['cd ' location ' ; ' systemInput]);
					if any(strfind(message,'Render failed'))
						error(['Render failed: ...' message(end-300:end)]);
					end
					% Append text for relaxation and growth
					if appendText
						system(['convert -antialias -pointsize 30 -font courier-bold -annotate 0x0+30+50 ''Growth time:     ' sprintf('%5.2f h',model.growthIter*model.growthTimeStep/3600.0) '\nRelaxation time: ' sprintf('%5.2f s'' ',model.relaxationIter*model.relaxationTimeStep+ii*model.relaxationTimeStepdt)  imagePath{ii+1} ' ' imagePath{ii+1}]);
					end
					
					if appendScaleBar
						% Append scale bar
						LLine = LScale * 1/right * imageWidth;
						system(['convert -antialias -pointsize 30 -font courier-bold -annotate 0x0+880+50 ''' num2str(LScale) ' um'' ' imagePath{ii+1} ' ' imagePath{ii+1}]);
						system(['convert -stroke black -strokewidth 3 -draw "line ' num2str(imageWidth-110-LLine/2) ',70 ' num2str(imageWidth-110+LLine/2) ',70" ' imagePath{ii+1} ' ' imagePath{ii+1}]);
					end
					
					% Remove POV file if desired
					if removePOV
						remove = ['rm ' povName{ii+1}];
						[~,~] = system(['cd ' location '/' imageFolderName  '/ ; ' remove]);
					end
				end
% 			catch ME
%                 if exist([location filesep 'rendering'],'file')
%                     % Done with this folder, delete "mark as rendered"
% 					delete([location filesep 'rendering']);
% 					continue
% 				end
% 				warning(['Encountered error: ' ME.message])
% 				continue;
% 			end
        end
        % Done with this folder, delete "mark as rendered"
		delete([location filesep 'rendering']);
    end
    disp([datestr(now) '  waiting...']);
    pause(10);
end