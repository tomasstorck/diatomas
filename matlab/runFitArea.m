range = 1:10;							% seed range
Nsim = length(range);
locationFilter = '../results/ecoli_filament_seed';

area = zeros(Nsim,1);
for ii=range							% for loop over different seeds
	success = false;					% becomes true if we found stacking. If not, error
    fprintf('seed %d...   ',ii);
	fileList = dir([locationFilter num2str(ii) '/output/*.mat']);
	for jj=1:length(fileList)			% for loop over files in output folder
		load([locationFilter num2str(ii) '/output/' fileList(jj).name]);
		pos = [model.ballArray.pos];
		rMax = model.radiusCellMax(5);
		if max(pos(2,:)) > 2*rMax		% Only do something if a ball is > rMax off the substratum
			fprintf('Stacking occurs after %g iterations\n',jj);
			area(ii) = fitArea(model);	% Append area for this simulation
			success = true;				% Yup, got stacking
			break						% Now go try the next simulation
		end
	end
	if ~success
		error('No stacking detected for simulation: %s%g',locationFilter,ii)
	end
end

% Do error analysis
if any(area==0)
	error('Something is wrong with results, at least one area not assigned')
end

% Analyse stats
disp('mean: ')
mean(area)
disp('95% CI upper/lower boundary size: ')
tinv(1-0.025,Nsim-1) * std(area) / sqrt(Nsim)
