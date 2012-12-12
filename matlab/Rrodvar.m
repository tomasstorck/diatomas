function R = Rrodvar(n,a)

MW = 24.6e-3;
rho = 1010;

R = (n*MW/(rho*pi*(4/3+a*2)))^(1/3);
end