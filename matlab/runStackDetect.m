locationFilter = '../results/ecoli_anchor_anchorstretchlim0.5_*';

folderList = dir(locationFilter);

stackList = zeros(length(folderList),1);
for ii=1:length(folderList)				% for loop over different seeds
	location = folderList(ii).name;
	success = false;					% becomes true if we found stacking. If not, error
    fprintf(' %s...   ',location);
	fileList = dir(['../results/' location '/output/*.mat']);
	for jj=1:length(fileList)			% for loop over files in output folder
		load(['../results/' location '/output/' fileList(jj).name]);
		pos = [model.ballArray.pos];
		rMax = model.radiusCellMax(5);
		if max(pos(2,:)) > 2*rMax		% Only do something if a ball is > rMax off the substratum
			fprintf('Stacking occurs after %g iterations\n',jj);
			success = true;				% Yup, got stacking
			break						% Now go try the next simulation
		end
		stackList(ii) = jj;
	end
	if ~success
		warning('No stacking detected for simulation: %s%g',locationFilter,ii)
	end
end