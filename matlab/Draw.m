opengl NeverSelect		% in case you don't have opengl. Disable if you do.

n = 10;		% resolution of the sphere (lower = faster)

[x,y,z] = sphere(n);		
yshadow = zeros(size(y));
% figure;
hold on;

overlapArray = CheckOverlap(model.ballArray);

box;

for iCell = 0:length(model.cellArray)-1;
	C0 = zeros(n+1);
	C1 = zeros(n+1);

	cell = model.cellArray(iCell+1);
	iBall0 = cell.ballArray(1);
	ball0 = model.ballArray(iBall0+1);
	if cell.type>1
		iBall1 = cell.ballArray(2);
		ball1 = model.ballArray(iBall1+1);
	end
	
	if ~exist('colourMode','var')	colourMode = 0; end
	
	if colourMode==0
		% colour balls based on cellIndex
		C0=zeros(n+1)+ball0.cellIndex;
		C1=zeros(n+1)+ball1.cellIndex;
	end
	
	if colourMode==1
		% colour balls based on ancestor's cellIndex
		cii=-1;
		for jCell=1:model.NInitCell
			if all(cell.colour == model.cellArray(jCell).colour);
				cii = jCell;
				break;
			end
		end
		C0=zeros(n+1)+cii;
		C1=C0;
	end

	if colourMode==2
		% colour balls based on collision or not
		if any(any(iBall0==overlapArray))
				C0 = zeros(n+1)+1;
		end
		if cell.type>1 && any(any(iBall1==overlapArray))
				C1 = zeros(n+1)+1;
		end
	end
	%
	if cell.type>1
		surf(ball0.radius*x+ball0.pos(1),ball0.radius*z+ball0.pos(3),ball0.radius*y+ball0.pos(2),C0);
		surf(ball1.radius*x+ball1.pos(1),ball1.radius*z+ball1.pos(3),ball1.radius*y+ball1.pos(2),C1);
		plot3([ball1.pos(1) ball0.pos(1)],[ball1.pos(3) ball0.pos(3)], [ball1.pos(2) ball0.pos(2)],'k');
	else
		surf(ball0.radius*x+ball0.pos(1),ball0.radius*z+ball0.pos(3),ball0.radius*y+ball0.pos(2),C0);
	end
end

% for iFil=1:length(model.filSpringArray)
%     fil = model.filSpringArray(iFil);
%     ball0 = model.ballArray(fil.ballArray(1)+1);
%     ball1 = model.ballArray(fil.ballArray(2)+1);
%     plot3([ball1.pos(1) ball0.pos(1)],[ball1.pos(3) ball0.pos(3)], [ball1.pos(2) ball0.pos(2)],'b');
% end

for iStick=1:length(model.stickSpringArray)
    stick = model.stickSpringArray(iStick);
    ball0 = model.ballArray(stick.ballArray(1)+1);
    ball1 = model.ballArray(stick.ballArray(2)+1);
    plot3([ball1.pos(1) ball0.pos(1)],[ball1.pos(3) ball0.pos(3)], [ball1.pos(2) ball0.pos(2)],'r');
end

if model.normalForce
	
	% Draw plane
	Vpos = [model.ballArray.pos];
	Vmax = max(Vpos,[],2);
	Vmin = min(Vpos,[],2);

	patch([Vmin(1) Vmax(1) Vmax(1) Vmin(1)], [Vmin(3) Vmin(3) Vmax(3) Vmax(3)], [-0.01e-6 -0.01e-6 -0.01e-6 -0.01e-6], [0.7 0.7 0.7])

	% Shadows
	for ii = 1:length(model.ballArray);
		ball = model.ballArray(ii);
		C=zeros(n+1);
		surf(ball.radius*x+ball.pos(1),ball.radius*z+ball.pos(3),ball.radius*yshadow,C);
	end
end

colorbar
xlabel('x')
ylabel('z')
zlabel('y')


%axis tight
axis equal
box

% % set view to correspond more or less to POVRay
% AZ = -35.197696406078933;
% EL = 17.875288325852580;
% view(AZ,EL);
							