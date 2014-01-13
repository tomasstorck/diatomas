function n = nrodvar(R,a)

MW = 24.6e-3;
rho = 1010;

n = (4/3*pi*R^3+a*2*R*pi*R^2)*rho/MW;

end