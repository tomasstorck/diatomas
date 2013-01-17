function out=CheckOverlap(ballArray)

out = [];
for iBall=0:length(ballArray)-1;
    ball0 = ballArray(iBall+1);
	for jBall=iBall+1:length(ballArray)-1;
		ball1 = ballArray(jBall+1);
		if norm(ball1.pos-ball0.pos) < (ball0.radius+ball1.radius)
			out(end+1,:) = [iBall jBall];
		end
	end
    
	
end