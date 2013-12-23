function right = RenderCalcRight(model, imageWidth, imageHeight, camPosDifference)

% Create camera, background and lighting based on L
%%%%%%%%
%
%   ^ |  /^
%   | | //
% O y |/z   OB
% ____C__O_________
%  VD/ x->  &
% A /
%  /
%
%%%%%%%%

L = [20,20,20];
minPos = min([model.ballArray.pos],[],2)*1e6;		% *1e6 to convert to POVRay coordinates. 2 is for dimension 2
maxPos = max([model.ballArray.pos],[],2)*1e6;
aspect = imageWidth/imageHeight;
C = (maxPos+minPos)/2;	% Camera view point is at the plane, right in the middle, height == radius
% Find vector perpendicular to x axis AND camera axis
camView = C;
camPos = C+camPosDifference;
vertAxis = cross(camView-camPos, [1; 0; 0]);
% Reset ranges
horRange = 0.0;
vertRange = 0.0;

for ii=1:length(model.ballArray)
	ball = model.ballArray(ii);
	B = ball.pos*1e6;
	% Find horizontal range (easy)
	horRangeNew = abs(C(1)-ball.pos(1)*1e6 + ball.radius*1e6);
	% Find vertical range (harder)
	% Project position onto vertAxis, convert to right
	BC = C-B;
	projBC = dot(vertAxis, BC)/norm(vertAxis);
	vertRangeNew = abs(projBC)+ball.radius*1e6;
	if horRangeNew>horRange || vertRangeNew>vertRange
		if horRangeNew > vertRangeNew*aspect
			horRange = horRangeNew;
			vertRange = horRangeNew/aspect;
		else
			horRange = vertRangeNew*aspect;
			vertRange = vertRangeNew;
		end
		right = 2*horRange+0.5;						% From which we'll derive up vector
	end
end