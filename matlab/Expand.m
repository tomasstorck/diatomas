function a = Expand(a)

% Assign balls in cellArray
for ii=1:length(a.ballArray)
    pBall = a.ballArray(ii);
    if ~isfield(a.cellArray,'ballArray')
        a.cellArray(pBall.cellArrayIndex).ballArray = pBall;
    end
    a.cellArray(pBall.cellArrayIndex).ballArray(pBall.cellBallArrayIndex) = pBall;
end 

% Assign stickCellArray in cellArray
for ii=1:length(a.stickSpringArray)
    pSpring = a.stickSpringArray(ii);
    if ~isfield(a.cellArray,'stickCellArray')
        pCell1 = a.cellArray(a.ballArray(pSpring.ballArrayIndex(1)).cellArrayIndex);
        pCell2 = a.cellArray(a.ballArray(pSpring.ballArrayIndex(2)).cellArrayIndex);
        pCell1.stickCellArrayIndex = [];
        pCell2.stickCellArrayIndex = [];
    end
    pCell1.stickCellArrayIndex(end+1) = pCell2.cellArrayIndex;
    pCell2.stickCellArrayIndex(end+1) = pCell1.cellArrayIndex;
end 
pCell1.stickCellArrayIndex = unique(pCell1.stickCellArrayIndex);
pCell2.stickCellArrayIndex = unique(pCell2.stickCellArrayIndex);