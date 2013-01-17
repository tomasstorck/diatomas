if ~exist('location','var')
	location = uigetdir;
	if isempty(location)
		return
	end
end

pad = [location '/output/'];

files=dir([pad '*.mat']);

c=[];
for iFile = 1:length(files)
	load([pad files(iFile).name]);

	v = [];
	for iCell = 1:length(model.cellArray)
		cell = model.cellArray(iCell);
		ball0 = model.ballArray(cell.ballArray(1)+1);
		ball1 = model.ballArray(cell.ballArray(2)+1);

		v(end+1,:) = ball0.pos - ball1.pos;
	end

	theta = [];
	for ii = 1:length(v)
		vi = v(ii,:);
		for jj = ii+1:length(v)
			vj = v(jj,:);

			theta = [theta acosd(dot(vi,vj)/(norm(vi)*norm(vj)))];

		end
	end

	C = 0;
	for ii = 1:length(theta)
		C = C + 2*cosd(theta(ii))^2-1;
	end
	c(end+1) = C/length(theta);
end