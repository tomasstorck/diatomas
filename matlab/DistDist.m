pos=[model.ballArray.pos]';

mn=mean(pos);
middle=(min(pos)+max(pos))/2;

dist = zeros(size(pos,1),1);
for ii=1:size(pos,1)
	dist(ii) = sqrt(sum((pos(ii,:)-mn).^2));
end

stdev = std(dist);