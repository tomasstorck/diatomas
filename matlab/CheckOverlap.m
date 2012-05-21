function out=CheckOverlap(ballArray)

out = [];
for iBall=1:length(ballArray);
    pBall = ballArray(iBall);
    out(end+1,:) = pBall.pos;
end