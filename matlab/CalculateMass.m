function CalculateMass(Rinit,a,type)			% Note: Mass is in Cmol

% Good value for desired radius --> initial radius is *0.9

format compact

if type==0
	Minit = Msphere(Rinit)
	Mdiv = 2*Minit
	Rinit = Rsphere(Minit)
	Rdiv = Rsphere(Mdiv)
end
if type==1
	Minit = Mrodvar(Rinit,a)
	Mdiv = 2*Minit
	Rinit = Rrodvar(Minit,a)
	Rdiv = Rrodvar(Mdiv,a)
end
if type==2
	
	MW = 24.6e-3;
	rho = 1100;
	
	Mdiv = Mrodvar(Rinit,a)
	adiv = Mdiv*MW/(2*pi*rho*Rinit^3)-2/3
	Minit = Mdiv/2
	ainit = Minit*MW/(2*pi*rho*Rinit^3)-2/3
	Rinit = Rrodvar(Minit,ainit)
	Rdiv = Rrodvar(Mdiv,adiv)
end


end

function R = Rsphere(n)

MW = 24.6e-3;
rho = 1100;

R = (3*n*MW/(4*pi*rho))^(1/3);
end

function n = Msphere(R)

MW = 24.6e-3;
rho = 1100;

n = 4/3*pi*R^3*rho/MW;
end

function R = Rrodvar(n,a)

MW = 24.6e-3;
rho = 1100;

R = (n*MW/(rho*pi*(4/3+a*2)))^(1/3);
end

function n = Mrodvar(R,a)

MW = 24.6e-3;
rho = 1100;

n = (4/3*pi*R^3+a*2*R*pi*R^2)*rho/MW;

end