package NR;


public class StepperDopr853 extends StepperBase {
	
	public StepperDopr853(Vector yy, Vector dydxx, double xx, double hh, double atoll, double rtoll, boolean dens) {
		super(yy,dydxx,xx,hh,atoll,rtoll,dens);	// Construct super class
		yerr2 = new Vector(n);
		k2 = new Vector(n);
		k3 = new Vector(n);
		k4 = new Vector(n);
		k5 = new Vector(n);
		k6 = new Vector(n);
		k7 = new Vector(n);
		k8 = new Vector(n);
		k9 = new Vector(n);
		k10 = new Vector(n);
		
		rcont1 = new Vector(n);
		rcont2 = new Vector(n);
		rcont3 = new Vector(n);
		rcont4 = new Vector(n);
		rcont5 = new Vector(n);
		rcont6 = new Vector(n);
		rcont7 = new Vector(n);
		rcont8 = new Vector(n);
		
		EPS 	= Double.MIN_VALUE; // correct? FIXME
	}
	
	Vector yerr2;
	Vector k2,k3,k4,k5,k6,k7,k8,k9,k10;
	Vector rcont1,rcont2,rcont3,rcont4,rcont5,rcont6,rcont7,rcont8;
	Controller con = new Controller();					// Initiate the controller we just made
	
	class Controller {
		double hnext,errold;
		boolean reject;

		public Controller() {
			reject = false;
			errold = 1e-4;
		}
		
		boolean success(double err) {			// h is no longer passed as a reference: we need to change the stepper's h
			//static const double beta=0.2,alpha=1.0/8.0-beta*0.2,safe=0.9,minscale=0.333,maxscale=6.0;			// Set beta alpha safe minscale maxscale, more stable
			//fast
			double beta=0.0;
			double alpha=1.0/8.0-beta*0.2;
			double safe=0.9;
			double minscale=0.333;
			double maxscale=6.0;				
			double scale;

			if (err <= 1.0) {
				if (err == 0.0)
					scale=maxscale;
				else {
					scale=safe*Math.pow(err,-alpha)*Math.pow(errold,beta);
					if (scale<minscale) scale=minscale;
					if (scale>maxscale) scale=maxscale;
				}
				if (reject)
					hnext=h*Math.min(scale,1.0);
				else
					hnext=h*scale;
				errold=Math.min(err,1.0e-4);
				reject=false;
				return true;
			} else {
				if (err==err){										// means something as in !NaN or !inf
					scale=Math.max(safe*Math.pow(err,-alpha),minscale);
					h *= scale;
				}else{
					h = 0.5*h;										// We don't know what to scale with --> scale by 0.5.		// THIS IS THE PROBLEM: H IS NOT RETURNED BUT CHANGED LOCALLY FIXME
				}
				reject=true;
				return false;
			}
		}
	}

	public void step(double htry,feval derivs) throws Exception {
		Vector dydxnew = new Vector(n);
		h=htry;

		for (;;) {
			dy(h,derivs);
			double err=error(h);

			if (con.success(err)) 
				break;
			if (Math.abs(h) <= Math.abs(x)*EPS) throw new Exception("stepsize underflow in StepperDopr853");
		}
		derivs.Calculate(x+h,yout,dydxnew);
		if (dense)
			prepare_dense(h,dydxnew,derivs);
		dydx=dydxnew;
		y.set(yout);										// This is the one that took me a while: in C++ the pointed value is copied, in Java the value to the reference
		xold=x;
		x += (hdid=h);
		hnext=con.hnext;
	}

	public void dy(double h,feval derivs) {
		Vector ytemp= new Vector(n);
		int i;

		for (i=0;i<n;i++)															// Step 1
			ytemp.set(i,y.get(i)+h*c.a21*dydx.get(i));
		derivs.Calculate(x+c.c2*h,ytemp,k2);
		for (i=0;i<n;i++)															// Step 2
			ytemp.set(i,y.get(i)+h*(c.a31*dydx.get(i)+c.a32*k2.get(i)));
		derivs.Calculate(x+c.c3*h,ytemp,k3);
		for (i=0;i<n;i++)															// Step 3
			ytemp.set(i,y.get(i)+h*(c.a41*dydx.get(i)+c.a43*k3.get(i)));
		derivs.Calculate(x+c.c4*h,ytemp,k4);
		for (i=0;i<n;i++)															// Step 4
			ytemp.set(i,y.get(i)+h*(c.a51*dydx.get(i)+c.a53*k3.get(i)+c.a54*k4.get(i)));
		derivs.Calculate(x+c.c5*h,ytemp,k5);
		for (i=0;i<n;i++)															// Step 5
			ytemp.set(i,y.get(i)+h*(c.a61*dydx.get(i)+c.a64*k4.get(i)+c.a65*k5.get(i)));
		derivs.Calculate(x+c.c6*h,ytemp,k6);
		for (i=0;i<n;i++)															// Step 6
			ytemp.set(i,y.get(i)+h*(c.a71*dydx.get(i)+c.a74*k4.get(i)+c.a75*k5.get(i)+c.a76*k6.get(i)));
		derivs.Calculate(x+c.c7*h,ytemp,k7);
		for (i=0;i<n;i++)															// Step 7
			ytemp.set(i,y.get(i)+h*(c.a81*dydx.get(i)+c.a84*k4.get(i)+c.a85*k5.get(i)+c.a86*k6.get(i)+c.a87*k7.get(i)));
		derivs.Calculate(x+c.c8*h,ytemp,k8);
		for (i=0;i<n;i++)															// Step 8
			ytemp.set(i,y.get(i)+h*(c.a91*dydx.get(i)+c.a94*k4.get(i)+c.a95*k5.get(i)+c.a96*k6.get(i)+c.a97*k7.get(i)+c.a98*k8.get(i)));
		derivs.Calculate(x+c.c9*h,ytemp,k9);
		for (i=0;i<n;i++)															// Step 9
			ytemp.set(i,y.get(i)+h*(c.a101*dydx.get(i)+c.a104*k4.get(i)+c.a105*k5.get(i)+c.a106*k6.get(i)+c.a107*k7.get(i)+c.a108*k8.get(i)+c.a109*k9.get(i)));
		derivs.Calculate(x+c.c10*h,ytemp,k10);
		for (i=0;i<n;i++)															// Step 10
			ytemp.set(i,y.get(i)+h*(c.a111*dydx.get(i)+c.a114*k4.get(i)+c.a115*k5.get(i)+c.a116*k6.get(i)+c.a117*k7.get(i)+c.a118*k8.get(i)+c.a119*k9.get(i)+c.a1110*k10.get(i)));
		derivs.Calculate(x+c.c11*h,ytemp,k2);
		double xph=x+h;
		for (i=0;i<n;i++)															// Step 11
			ytemp.set(i,y.get(i)+h*(c.a121*dydx.get(i)+c.a124*k4.get(i)+c.a125*k5.get(i)+c.a126*k6.get(i)+c.a127*k7.get(i)+c.a128*k8.get(i)+c.a129*k9.get(i)+c.a1210*k10.get(i)+c.a1211*k2.get(i)));
		derivs.Calculate(xph,ytemp,k3);
		for (i=0;i<n;i++) {															// Step 12
			k4.set(i,c.b1*dydx.get(i)+c.b6*k6.get(i)+c.b7*k7.get(i)+c.b8*k8.get(i)+c.b9*k9.get(i)+c.b10*k10.get(i)+c.b11*k2.get(i)+c.b12*k3.get(i));
			yout.set(i,y.get(i)+h*k4.get(i));
		}
		for (i=0;i<n;i++) {															// Step 13
			yerr.set(i,k4.get(i)-c.bhh1*dydx.get(i)-c.bhh2*k9.get(i)-c.bhh3*k3.get(i));
			yerr2.set(i,c.er1*dydx.get(i)+c.er6*k6.get(i)+c.er7*k7.get(i)+c.er8*k8.get(i)+c.er9*k9.get(i)+c.er10*k10.get(i)+c.er11*k2.get(i)+c.er12*k3.get(i));
		}
	}
	
	public void prepare_dense(double h,Vector dydxnew,	feval derivs) {
		int i;
		double ydiff,bspl;
		Vector ytemp = new Vector(n);
		for (i=0;i<n;i++) {
			rcont1.set(i,y.get(i));
			ydiff=yout.get(i)-y.get(i);
			rcont2.set(i,ydiff);
			bspl=h*dydx.get(i)-ydiff;
			rcont3.set(i,bspl);
			rcont4.set(i,ydiff-h*dydxnew.get(i)-bspl);
			rcont5.set(i,c.d41*dydx.get(i)+c.d46*k6.get(i)+c.d47*k7.get(i)+c.d48*k8.get(i)+c.d49*k9.get(i)+c.d410*k10.get(i)+c.d411*k2.get(i)+c.d412*k3.get(i));
			rcont6.set(i,c.d51*dydx.get(i)+c.d56*k6.get(i)+c.d57*k7.get(i)+c.d58*k8.get(i)+c.d59*k9.get(i)+c.d510*k10.get(i)+c.d511*k2.get(i)+c.d512*k3.get(i));
			rcont7.set(i,c.d61*dydx.get(i)+c.d66*k6.get(i)+c.d67*k7.get(i)+c.d68*k8.get(i)+c.d69*k9.get(i)+c.d610*k10.get(i)+c.d611*k2.get(i)+c.d612*k3.get(i));
			rcont8.set(i,c.d71*dydx.get(i)+c.d76*k6.get(i)+c.d77*k7.get(i)+c.d78*k8.get(i)+c.d79*k9.get(i)+c.d710*k10.get(i)+c.d711*k2.get(i)+c.d712*k3.get(i));
		}
		for (i=0;i<n;i++)
			ytemp.set(i,y.get(i)+h*(c.a141*dydx.get(i)+c.a147*k7.get(i)+c.a148*k8.get(i)+c.a149*k9.get(i)+c.a1410*k10.get(i)+c.a1411*k2.get(i)+c.a1412*k3.get(i)+c.a1413*dydxnew.get(i)));
			derivs.Calculate(x+c.c14*h,ytemp,k10);
			for (i=0;i<n;i++)
				ytemp.set(i,y.get(i)+h*(c.a151*dydx.get(i)+c.a156*k6.get(i)+c.a157*k7.get(i)+c.a158*k8.get(i)+c.a1511*k2.get(i)+c.a1512*k3.get(i)+c.a1513*dydxnew.get(i)+c.a1514*k10.get(i)));
				derivs.Calculate(x+c.c15*h,ytemp,k2);
				for (i=0;i<n;i++)
					ytemp.set(i,y.get(i)+h*(c.a161*dydx.get(i)+c.a166*k6.get(i)+c.a167*k7.get(i)+c.a168*k8.get(i)+c.a169*k9.get(i)+c.a1613*dydxnew.get(i)+c.a1614*k10.get(i)+c.a1615*k2.get(i)));
					derivs.Calculate(x+c.c16*h,ytemp,k3);
					for (i=0;i<n;i++)
					{
						rcont5.set(i,h*(rcont5.get(i)+c.d413*dydxnew.get(i)+c.d414*k10.get(i)+c.d415*k2.get(i)+c.d416*k3.get(i)));
						rcont6.set(i,h*(rcont6.get(i)+c.d513*dydxnew.get(i)+c.d514*k10.get(i)+c.d515*k2.get(i)+c.d516*k3.get(i)));
						rcont7.set(i,h*(rcont7.get(i)+c.d613*dydxnew.get(i)+c.d614*k10.get(i)+c.d615*k2.get(i)+c.d616*k3.get(i)));
						rcont8.set(i,h*(rcont8.get(i)+c.d713*dydxnew.get(i)+c.d714*k10.get(i)+c.d715*k2.get(i)+c.d716*k3.get(i)));
					}
	}
	

	public double dense_out(int i, double x, double h) {
		double s=(x-xold)/h;
		double s1=1.0-s;
		return rcont1.get(i)+s*(rcont2.get(i)+s1*(rcont3.get(i)+s*(rcont4.get(i)+s1*(rcont5.get(i)+	s*(rcont6.get(i)+s1*(rcont7.get(i)+s*rcont8.get(i)))))));
	}

	public double error(double h) {
		double err=0.0;
		double err2=0.0;
		double sk,deno;
		for (int i=0;i<n;i++) {
			sk = atol+rtol*Math.max(Math.abs(y.get(i)),Math.abs(yout.get(i)));
			err2 += Math.pow(yerr.get(i)/sk,2);	// Optimise? TODO
			err += Math.pow(yerr2.get(i)/sk,2);
		}
		deno=err+0.01*err2;
		if (deno <= 0.0)
			deno=1.0;

		return Math.abs(h)*err*Math.sqrt(1.0/(n*deno));
	}
	
	private static class c {			//Dopr853_constants
		static double c2  = 0.526001519587677318785587544488e-01;
		static double c3  = 0.789002279381515978178381316732e-01;
		static double c4  = 0.118350341907227396726757197510e+00;
		static double c5  = 0.281649658092772603273242802490e+00;
		static double c6  = 0.333333333333333333333333333333e+00;
		static double c7  = 0.25e+00;
		static double c8  = 0.307692307692307692307692307692e+00;
		static double c9  = 0.651282051282051282051282051282e+00;
		static double c10 = 0.6e+00;
		static double c11 = 0.857142857142857142857142857142e+00;
		static double c14 = 0.1e+00;
		static double c15 = 0.2e+00;
		static double c16 = 0.777777777777777777777777777778e+00;

		static double b1 =   5.42937341165687622380535766363e-2;
		static double b6 =   4.45031289275240888144113950566e0;
		static double b7 =   1.89151789931450038304281599044e0;
		static double b8 =  -5.8012039600105847814672114227e0;
		static double b9 =   3.1116436695781989440891606237e-1;
		static double b10 = -1.52160949662516078556178806805e-1;
		static double b11 =  2.01365400804030348374776537501e-1;
		static double b12 =  4.47106157277725905176885569043e-2;

		static double bhh1 = 0.244094488188976377952755905512e+00;
		static double bhh2 = 0.733846688281611857341361741547e+00;
		static double bhh3 = 0.220588235294117647058823529412e-01;

		static double er1  =  0.1312004499419488073250102996e-01;
		static double er6  = -0.1225156446376204440720569753e+01;
		static double er7  = -0.4957589496572501915214079952e+00;
		static double er8  =  0.1664377182454986536961530415e+01;
		static double er9  = -0.3503288487499736816886487290e+00;
		static double er10 =  0.3341791187130174790297318841e+00;
		static double er11 =  0.8192320648511571246570742613e-01;
		static double er12 = -0.2235530786388629525884427845e-01;

		static double a21 =    5.26001519587677318785587544488e-2;
		static double a31 =    1.97250569845378994544595329183e-2;
		static double a32 =    5.91751709536136983633785987549e-2;
		static double a41 =    2.95875854768068491816892993775e-2;
		static double a43 =    8.87627564304205475450678981324e-2;
		static double a51 =    2.41365134159266685502369798665e-1;
		static double a53 =   -8.84549479328286085344864962717e-1;
		static double a54 =    9.24834003261792003115737966543e-1;
		static double a61 =    3.7037037037037037037037037037e-2;
		static double a64 =    1.70828608729473871279604482173e-1;
		static double a65 =    1.25467687566822425016691814123e-1;
		static double a71 =    3.7109375e-2;
		static double a74 =    1.70252211019544039314978060272e-1;
		static double a75 =    6.02165389804559606850219397283e-2;
		static double a76 =   -1.7578125e-2;

		static double a81 =    3.70920001185047927108779319836e-2;
		static double a84 =    1.70383925712239993810214054705e-1;
		static double a85 =    1.07262030446373284651809199168e-1;
		static double a86 =   -1.53194377486244017527936158236e-2;
		static double a87 =    8.27378916381402288758473766002e-3;
		static double a91 =    6.24110958716075717114429577812e-1;
		static double a94 =   -3.36089262944694129406857109825e0;
		static double a95 =   -8.68219346841726006818189891453e-1;
		static double a96 =    2.75920996994467083049415600797e1;
		static double a97 =    2.01540675504778934086186788979e1;
		static double a98 =   -4.34898841810699588477366255144e1;
		static double a101 =   4.77662536438264365890433908527e-1;
		static double a104 =  -2.48811461997166764192642586468e0;
		static double a105 =  -5.90290826836842996371446475743e-1;
		static double a106 =   2.12300514481811942347288949897e1;
		static double a107 =   1.52792336328824235832596922938e1;
		static double a108 =  -3.32882109689848629194453265587e1;
		static double a109 =  -2.03312017085086261358222928593e-2;

		static double a111 =  -9.3714243008598732571704021658e-1;
		static double a114 =   5.18637242884406370830023853209e0;
		static double a115 =   1.09143734899672957818500254654e0;
		static double a116 =  -8.14978701074692612513997267357e0;
		static double a117 =  -1.85200656599969598641566180701e1;
		static double a118 =   2.27394870993505042818970056734e1;
		static double a119 =   2.49360555267965238987089396762e0;
		static double a1110 = -3.0467644718982195003823669022e0;
		static double a121 =   2.27331014751653820792359768449e0;
		static double a124 =  -1.05344954667372501984066689879e1;
		static double a125 =  -2.00087205822486249909675718444e0;
		static double a126 =  -1.79589318631187989172765950534e1;
		static double a127 =   2.79488845294199600508499808837e1;
		static double a128 =  -2.85899827713502369474065508674e0;
		static double a129 =  -8.87285693353062954433549289258e0;
		static double a1210 =  1.23605671757943030647266201528e1;
		static double a1211 =  6.43392746015763530355970484046e-1;

		static double a141 =  5.61675022830479523392909219681e-2;
		static double a147 =  2.53500210216624811088794765333e-1;
		static double a148 = -2.46239037470802489917441475441e-1;
		static double a149 = -1.24191423263816360469010140626e-1;
		static double a1410 =  1.5329179827876569731206322685e-1;
		static double a1411 =  8.20105229563468988491666602057e-3;
		static double a1412 =  7.56789766054569976138603589584e-3;
		static double a1413 = -8.298e-3;

		static double a151 =  3.18346481635021405060768473261e-2;
		static double a156 =  2.83009096723667755288322961402e-2;
		static double a157 =  5.35419883074385676223797384372e-2;
		static double a158 = -5.49237485713909884646569340306e-2;
		static double a1511 = -1.08347328697249322858509316994e-4;
		static double a1512 =  3.82571090835658412954920192323e-4;
		static double a1513 = -3.40465008687404560802977114492e-4;
		static double a1514 =  1.41312443674632500278074618366e-1;
		static double a161 = -4.28896301583791923408573538692e-1;
		static double a166 = -4.69762141536116384314449447206e0;
		static double a167 =  7.68342119606259904184240953878e0;
		static double a168 =  4.06898981839711007970213554331e0;
		static double a169 =  3.56727187455281109270669543021e-1;
		static double a1613 = -1.39902416515901462129418009734e-3;
		static double a1614 =  2.9475147891527723389556272149e0;
		static double a1615 = -9.15095847217987001081870187138e0;

		static double d41  = -0.84289382761090128651353491142e+01;
		static double d46  =  0.56671495351937776962531783590e+00;
		static double d47  = -0.30689499459498916912797304727e+01;
		static double d48  =  0.23846676565120698287728149680e+01;
		static double d49  =  0.21170345824450282767155149946e+01;
		static double d410 = -0.87139158377797299206789907490e+00;
		static double d411 =  0.22404374302607882758541771650e+01;
		static double d412 =  0.63157877876946881815570249290e+00;
		static double d413 = -0.88990336451333310820698117400e-01;
		static double d414 =  0.18148505520854727256656404962e+02;
		static double d415 = -0.91946323924783554000451984436e+01;
		static double d416 = -0.44360363875948939664310572000e+01;

		static double d51  =  0.10427508642579134603413151009e+02;
		static double d56  =  0.24228349177525818288430175319e+03;
		static double d57  =  0.16520045171727028198505394887e+03;
		static double d58  = -0.37454675472269020279518312152e+03;
		static double d59  = -0.22113666853125306036270938578e+02;
		static double d510 =  0.77334326684722638389603898808e+01;
		static double d511 = -0.30674084731089398182061213626e+02;
		static double d512 = -0.93321305264302278729567221706e+01;
		static double d513 =  0.15697238121770843886131091075e+02;
		static double d514 = -0.31139403219565177677282850411e+02;
		static double d515 = -0.93529243588444783865713862664e+01;
		static double d516 =  0.35816841486394083752465898540e+02;

		static double d61 =  0.19985053242002433820987653617e+02;
		static double d66 = -0.38703730874935176555105901742e+03;
		static double d67 = -0.18917813819516756882830838328e+03;
		static double d68 =  0.52780815920542364900561016686e+03;
		static double d69 = -0.11573902539959630126141871134e+02;
		static double d610 =  0.68812326946963000169666922661e+01;
		static double d611 = -0.10006050966910838403183860980e+01;
		static double d612 =  0.77771377980534432092869265740e+00;
		static double d613 = -0.27782057523535084065932004339e+01;
		static double d614 = -0.60196695231264120758267380846e+02;
		static double d615 =  0.84320405506677161018159903784e+02;
		static double d616 =  0.11992291136182789328035130030e+02;

		static double d71  = -0.25693933462703749003312586129e+02;
		static double d76  = -0.15418974869023643374053993627e+03;
		static double d77  = -0.23152937917604549567536039109e+03;
		static double d78  =  0.35763911791061412378285349910e+03;
		static double d79  =  0.93405324183624310003907691704e+02;
		static double d710 = -0.37458323136451633156875139351e+02;
		static double d711 =  0.10409964950896230045147246184e+03;
		static double d712 =  0.29840293426660503123344363579e+02;
		static double d713 = -0.43533456590011143754432175058e+02;
		static double d714 =  0.96324553959188282948394950600e+02;
		static double d715 = -0.39177261675615439165231486172e+02;
		static double d716 = -0.14972683625798562581422125276e+03;
	}
}