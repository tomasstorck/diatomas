function n = nsphere(R)

MW = 24.6e-3;
rho = 1010;

n = 4/3*pi*R^3*rho/MW;
end