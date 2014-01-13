function a = arodfixed(n,R)

MW=24.6e-3;
rho = 1010;

a = n*MW/(2*pi*rho*R^3)-2/3;

end