function R = Rsphere(n)

MW = 24.6e-3;
rho = 1010;

R = (3*n*MW/(4*pi*rho))^(1/3);
end