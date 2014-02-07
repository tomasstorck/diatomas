% area = FITAREA(model) 
% Determines the area covered by the cell centres (not actual positions), covered by balls in model.ballArray
function area = fitArea(model)


pos = [model.ballArray.pos];
proj = pos; proj(2,:) = [];			% Project positions onto plane (we already know it's a flat biofilm)

% Fit ellipse via Nima Moshtagh's function: the bulky way
[A, c] = MinVolEllipse(proj, 1e-3);

% % Fit ellipse via Nima Moshtagh's function: the smart way (reducing proj to boundary points only; see help file)
% K = convhulln(proj');				% Facet of the convex hull. Boils down to take outer points
% K = unique(K(:));  
% Q = proj(:,K);
% [A, c] = MinVolEllipse(Q, 1e-3);

% Visualise (should be commented out for speed)
clf;
hold on;
scatter(proj(1,:),proj(2,:));
Ellipse_plot(A,c)
drawnow
pause(1)

% Determine radii (from http://www.mathworks.com.au/matlabcentral/fileexchange/9542-minimum-volume-enclosing-ellipsoid description)
[U Q] = svd(A);
r1 = 1/sqrt(Q(1,1)); 
r2 = 1/sqrt(Q(2,2)); 
area = r1*r2*pi;