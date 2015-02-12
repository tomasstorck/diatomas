function RenderSketch(root)

if ~exist('root','var')
	root = '';
end

% Do not change here, but change below
ECOLI = false;
AS = false;
PERSPECTIVE = false;
TOP = false;
SIDE = false;


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% %%%%%%%%%%%%%%%
% % E. COLI
% %%%%%%%%%%%%%%%
% ECOLI = true;
% % loadFileNameList = {'g0070r0070.mat'};		% E. coli
% 
% %%
% % Perspective
% PERSPECTIVE = true;
% imageFolderName = 'perspective';
% %%
% % % Top
% % TOP = true;
% % imageFolderName = 'top';
% % %%
% % % Side
% % SIDE = true;
% % imageFolderName = 'side';
% % %%
% 
% folderFilter = 'ecoli*';		% <================
% plane = true;
% ceilLightColour = [0.65,0.65,0.65];
% camLightColour = [0.45 0.45 0.45];

%%%%%%%%%%%%%%%
% AS
%%%%%%%%%%%%%%%
AS = true;
PERSPECTIVE = true;
%%%
% AS low
% loadFileNameList = {'g0131r0131.mat','g0112r0112.mat','g0000r0000.mat'};		% AS low
folderFilter = 'as_low*';
% %%%
% % % AS high
% % loadFileNameList = {'g0092r0092.mat'};		% AS high
% % folderFilter = 'as_high*';
% % %%%
% 
imageFolderName = 'perspective';
plane = false;
ceilLightColour = [0.8,0.8,0.8];
camLightColour = [0.6 0.6 0.6];

% % SWAPCOLOURS - for cocci simulations
% swapColours = true;
% %%%


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


%%%%%%%%%%%%%%%
% SKETCH
%%%%%%%%%%%%%%%
imageFolderName = ['sketch-' imageFolderName];
renderIter = 10;		% Which results to render, as in 1:renderIter:end
fixLoadFileNameList = false;
imageWidth = 1024*0.5;
imageHeight = 768*0.5;
aspect = imageWidth/imageHeight;
appendScaleBar = true;
LScale = 1;		% micron
scaleBarPos = [imageWidth-110 50];		% Could use some tuning
appendText = true;
fixRight = false;	

if PERSPECTIVE
	camPosDifference = [0.0; 40; -80];
elseif TOP
	camPosDifference = [0.0; 40; 0];		% Top
elseif SIDE
	camPosDifference = [0.0; 0; -80];		% Side
else
	error('Cannot determine camPosDifference')
end

% %%%%%%%%%%%%%%%
% % MOVIE
% %%%%%%%%%%%%%%%
% imageFolderName = ['movie-' imageFolderName];
% renderIter = 1;			% Which results to render, as in 1:renderIter:end
% fixLoadFileNameList = false;
% imageWidth = 1024;
% imageHeight = 768;
% aspect = imageWidth/imageHeight;
% appendScaleBar = true;
% LScale = 1;		% micron
% scaleBarPos = [imageWidth-110 70];
% appendText = true;
% fixRight = false;	
% 
% if PERSPECTIVE
% 	camPosDifference = [0.0; 40; -80];
% elseif TOP
% 	camPosDifference = [0.0; 40; 0];		% Top
% elseif SIDE
% 	camPosDifference = [0.0; 0; -80];		% Side
% else
% 	error('Cannot determine camPosDifference')
% end

% %%%%%%%%%%%%%%%
% % PAPER
% %%%%%%%%%%%%%%%
% imageFolderName = ['paper-' imageFolderName];
% renderIter = 10;		% Which results to render, as in 1:renderIter:end
% fixLoadFileNameList = true;
% imageWidth = 1024*8;
% imageHeight = 768*8;
% aspect = imageWidth/imageHeight;
% appendScaleBar = false;
% LScale = 10;		% micron
% scaleBarPos = [imageWidth-1100 imageHeight-70];			% [posX posY]
% appendText = false;
% 
% fixRight = true;		% Set our own right value
% if PERSPECTIVE && AS
% 	camPosDifference = [0.0; 40; -80];
% 	right =	148.923;						% AS paper (fixed perspective)
% elseif ECOLI && TOP
% 	camPosDifference = [0.0; 40; 0];		% Top
% 	right = 67.5245;						% E. coli paper (fixed top-down)
% elseif ECOLI && SIDE
% 	camPosDifference = [0.0; 0; -80];		% Side
% 	right = 67.5245;						% E. coli paper (fixed side view)
% else
% 	error('Cannot determine camPosDifference')
% end
% 
% % SCALEBAR - sometimes we DO want a scalebar
% appendScaleBar = true
% %%%


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%%%%%%%%%%%%%%
% GENERAL
%%%%%%%%%%%%%%%
loadFileMax = 500;		% Maximum number of files to load per folder before moving on to the next
removePOV = true;
rightIter = 10;			% How often to realign the zoom factor ("right")
camRotate = [0; 0; 0];

% Colours
cellColour = [0.60 0.00 0.00;		% Cell colours: 
	1.00 1.00 0.85;
	0.50 0.88 1.00;
	0.00 1.00 0.20];
filColour = [.10 .10 .10];			% Filament spring is black
stickColour = [0.80 .80 0.80];		% Sticking spring is white
anchorColour = [.50 .50 .50];		% Anchoring spring is grey

if exist('swapColours','var') && swapColours
	tempColour = cellColour(1,:);
	cellColour(1,:) = cellColour(2,:);
	cellColour(2,:) = tempColour;
end

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
				if (model.relaxationIter==0 && model.growthIter==0) || ~isfield(model.ballArray(1),'posSave')
					NSave = 0;
				else
					NSave = size(model.ballArray(1).posSave,1);
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
					systemInput = ['povray ' imageFolderName '/' povName{ii+1} ' +W' num2str(imageWidth) ' +H' num2str(imageHeight) ' +O' imageFolderName '/' imageName{ii+1} ' +A +Q11'];		% +A +Q11 instead of +A -J
					[~,message] = system(['cd ' location ' ; ' systemInput]);
					if any(strfind(message,'Render failed'))
						error(['Render failed: ...' message(end-300:end)]);
					end
					% Append text for relaxation and growth
					if appendText
						system(['convert -antialias -pointsize 30 -font courier-bold -annotate 0x0+30+50 ''Growth time:     ' sprintf('%5.2f h',model.growthTime/3600.0) '\nRelaxation time: ' sprintf('%5.2f s'' ',model.relaxationTime+ii*model.relaxationTimeStepdt)  imagePath{ii+1} ' ' imagePath{ii+1}]);
					end
					
					if appendScaleBar
						% Append scale bar
						LLine = LScale * 1/right * imageWidth;
						system(['convert -antialias -pointsize 30 -font courier-bold -annotate 0x0+' num2str(scaleBarPos(1)-30) '+' num2str(scaleBarPos(2)-20) ' ''' num2str(LScale) ' um'' ' imagePath{ii+1} ' ' imagePath{ii+1}]);
						system(['convert -stroke black -strokewidth 3 -draw "line ' num2str(scaleBarPos(1)-LLine/2) ',' num2str(scaleBarPos(2))  ' ' num2str(scaleBarPos(1)+LLine/2) ',' num2str(scaleBarPos(2)) '" ' imagePath{ii+1} ' ' imagePath{ii+1}]);
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
