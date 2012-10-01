opengl NeverSelect		% in case you don't have opengl

n = 10;		% resolution of the sphere (lower = faster)

[x,y,z] = sphere(n);		
yshadow = zeros(size(y));

% figure;
hold on;

% Draw plane
Vpos = [model.ballArray.pos];
Vmax = max(Vpos,[],2);
Vmin = min(Vpos,[],2);

patch([Vmin(1) Vmax(1) Vmax(1) Vmin(1)], [Vmin(3) Vmin(3) Vmax(3) Vmax(3)], [-0.01e-6 -0.01e-6 -0.01e-6 -0.01e-6], [0.7 0.7 0.7])

for ii = 1:length(model.ballArray);
	ball = model.ballArray(ii);
	
	% CONDITION
%  	if ball.cellIndex== 10 || ball.cellIndex==16
	%%%%%%%%%

	% colour balls based on cellIndex
	C=zeros(n+1)+ball.cellIndex;
	
	%	  scale ball*x+x pos ball , ... , ...
	surf(ball.radius*x+ball.pos(1),ball.radius*z+ball.pos(3),ball.radius*y+ball.pos(2),C);
	
	%%%%%%%%%
%  	end
	%%%%%%%%%
end

% Shadows
for ii = 1:length(model.ballArray);
	ball = model.ballArray(ii);
	
	% CONDITION
%  	if ball.cellIndex== 10 || ball.cellIndex==16
	%%%%%%%%%

	% colour balls based on cellIndex
	C=zeros(n+1);
	
	%	  scale ball*x+x pos ball , ... , ...
	surf(ball.radius*x+ball.pos(1),ball.radius*z+ball.pos(3),ball.radius*yshadow,C);
	
	%%%%%%%%%
%  	end
	%%%%%%%%%
end

colorbar
xlabel('x')
ylabel('y')
zlabel('z')


%axis tight
axis equal
box