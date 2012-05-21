function ballArray = BallArray(model)
ii=0;
for iCell=1:length(model.cellArray)
    pCell = model.cellArray(iCell);
    for iBall=1:length(pCell.ballArray)
        pBall = pCell.ballArray(iBall);
        ii=ii+1;
        ballArray(ii) = pBall;
    end
end