package comsol;

import java.io.IOException;
import java.util.ArrayList;

import cell.CBall;
import cell.CCell;
import cell.CModel;
import cell.Vector3d;

import com.comsol.model.Model;
import com.comsol.model.util.ModelUtil;
import com.comsol.util.exceptions.FlException;

public class Comsol {
	Model comsol;				// The COMSOL model
	CModel java;
	
	final double dimensionFactor = 1.00;	// FIXME, was 0.75
	
	ArrayList<String> cellList = new ArrayList<String>();   
	ArrayList<String> sphList = new ArrayList<String>();
	ArrayList<String> rodList = new ArrayList<String>();
	
	// Settings for model
	static int meshSize = 8;
	
	public Comsol(CModel java) {
		this.java = java;  
	}
	
	//////////////////////////////////
	
	public void Initialise() throws FlException {
		// Create model, initialise geometry, mesh, study and physics
		ModelUtil.initStandalone(false);
		ModelUtil.showProgress(false);								// enabling this causes COMSOL to run something SWT/graphical --> crash
		ModelUtil.showProgress(java.name + "/logfile_comsol.txt");
		comsol = ModelUtil.create("Model");
	    comsol.modelPath("/home/tomas/Desktop");					// UPDATE
	    comsol.modelNode().create("mod1");
	    comsol.geom().create("geom1", 3);
	    comsol.geom("geom1").geomRep("comsol");						// Use COMSOL geometry, prevents license issues

	    // Parameter list. These are all hardcoded.
	    comsol.param().set("D_hac", "1.21e-9[m^2/s]*D_factor", "[Cussler 1997], 25 C");
	    comsol.param().set("D_ac", "1.089e-9[m^2/s]*D_factor", "[Vanysek 2012], 25 C. Said to increase by 2-3% per degree C");
	    comsol.param().set("D_hpro", "1.06e-9[m^2/s]*D_factor", "[Cussler 1997], 25 C");
	    comsol.param().set("D_pro", "1.2e-9[m^2/s]*D_factor", "[Vanysek 2012], 25 C");
	    comsol.param().set("D_h2", "4.500e-9[m^2/s]*D_factor", "[Cussler 1997], 25 C");
	    comsol.param().set("D_co2", "1.92e-9[m^2/s]*D_factor", "[Cussler 1997], 25 C");
	    comsol.param().set("D_hco3", "1.185e-9[m^2/s]*D_factor", "[Vanysek 2012], 25 C");
	    comsol.param().set("D_co3", "2*0.923e-9[m^2/s]*D_factor", "[Vanysek 2012], 25 C, as \"1/2CO32-\" --> x2");
	    comsol.param().set("D_ch4", "1.49e-9[m^2/s]*D_factor", "[Cussler 1997], 25 C");
	    comsol.param().set("D_h", "9.311e-9[m^2/s]*D_factor", "[Vanysek 2012], 25 C");
	    comsol.param().set("D_oh", "5.273e-9[m^2/s]*D_factor", "[Vanysek 2012], 25 C");
	    comsol.param().set("D_k", "1.957e-9[m^2/s]*D_factor", "[Vanysek 2012], 25 C");
	    comsol.param().set("D_cl", "2.032e-9[m^2/s]*D_factor", "[Vanysek 2012], 25 C");
	    comsol.param().set("D_factor", "1");
	    comsol.param().set("R", "8.314[J/mol/K]");
	    comsol.param().set("T", "(273+20)[K]");
	    comsol.param().set("c0_h", "1e-4[mol/m^3]", "Fixed concentration BC. This is pH = 7");
	    comsol.param().set("c0_ac_tot", "0.049[mol/m^3]", "From ADM1_single_0_5.xls");
	    comsol.param().set("c0_ox_tot", "0.018[mol/m^3]", "From ADM1_single_0_5.xls");
	    comsol.param().set("c0_co2_tot", "0.075[mol/m^3]", "From ADM1_single_0_5.xls");
	    comsol.param().set("c0_h2", "0[mol/m^3]", "0 or 2.6e-7[mol/m^3] (ADM1)");
	    comsol.param().set("c0_ch4", "0.054[mol/m^3]", "From ADM1_single_0_5.xls");
	    comsol.param().set("c0_cl", "40[mol/m^3]");
	    comsol.param().set("c0_ac", "c0_ac_tot*Ka_hac/(c0_h+Ka_hac)");
	    comsol.param().set("c0_hac", "c0_ac_tot*c0_h/(c0_h+Ka_hac)");
	    comsol.param().set("c0_pro", "c0_ox_tot*Ka_hpro/(c0_h+Ka_hpro)");
	    comsol.param().set("c0_hpro", "c0_ox_tot*c0_h/(c0_h+Ka_hpro)");
	    comsol.param().set("c0_co2", "c0_co2_tot/(1+Ka_co2/c0_h*(1+Ka_hco3/c0_h))");
	    comsol.param().set("c0_hco3", "c0_co2_tot/(c0_h/Ka_co2+1+Ka_hco3/c0_h)");
	    comsol.param().set("c0_co3", "c0_co2_tot/((c0_h/Ka_co2+1)*c0_h/Ka_hco3+1)");
	    comsol.param().set("c0_oh", "Ka_hoh/c0_h");
	    comsol.param().set("c0_k", "-(-c0_cl+c0_h-c0_oh-c0_ac-c0_hco3-2*c0_co3-c0_pro)");
	    comsol.param().set("V0", "0[V]");
	    comsol.param().set("Ka_hpro", "10^(-4.87)[mol/L]", "[Vanysek 2012], 25 C");
	    comsol.param().set("Ka_hac", "10^(-4.76)[mol/L]", "[Vanysek 2012], 25 C");
	    comsol.param().set("Ka_co2", "10^(-6.35)[mol/L]", "[Vanysek 2012], 25 C, assuming this is apparent acid dissociation constant!");
	    comsol.param().set("Ka_hco3", "10^(-10.33)[mol/L]", "[Vanysek 2012], 25 C");
	    comsol.param().set("Ka_hoh", "10^(-13.995)[mol^2/L^2]", "[Vanysek 2012], 25 C");
	    comsol.param().set("ka_co2", "1e6[m^3/mol]", "Accelerator for forward rate. 1 = rate constant 1/s. Reverse reaction automatically calculated though Ka.  Arbitrary for fast reactions");
	    comsol.param().set("ka_hpro", "1e6[m^3/mol]");
	    comsol.param().set("ka_hac", "1e6[m^3/mol]");
	    comsol.param().set("ka_hco3", "1e6[m^3/mol]");
	    comsol.param().set("ka_hoh", "1e6");
	    comsol.param().set("q_max_pro", "1e3*13[mol/mol/day]", "[Batstone 2002, table 6.2]. !!! Note factor !!!");
	    comsol.param().set("q_max_hyd", "1e3*35[mol/mol/day]", "[Batstone 2002, table 6.2]. !!! Note factor !!!");
	    comsol.param().set("K_xpro_pro", "2.68[mol/m^3]", "[Batstone 2002, table 6.2]");
	    comsol.param().set("Ki_xpro_h2", "2.19e-4[mol/m^3]", "[Batstone 2002, table 6.2]");
	    comsol.param().set("K_xhyd_h2", "1.56e-3[mol/m^3]", "[Batstone 2002, table 6.2]");
	    comsol.param().set("Ki_xpro_h", "2e-3[mol/m^3]");
	    comsol.param().set("K_xhyd_h", "5e-4[mol/m^3]");
	    comsol.param().set("MW_x", "24.6[g/mol]");
	    comsol.param().set("n_x", "200[g/L]/MW_x");
	    comsol.param().set("F", "96485.3415[C/mol]");
	    comsol.param().set("pHul_xpro", "5.5", "[Batstone 2002, Excel sheet] from bac");
	    comsol.param().set("pHll_xpro", "4", "[Batstone 2002, Excel sheet] from bac");
	    comsol.param().set("pHul_xhyd", "6", "[Batstone 2002, table 6.2]");
	    comsol.param().set("pHll_xhyd", "5", "[Batstone 2002, table 6.2]");
	    
	    // Variables list. This will be appended later on.
	    comsol.variable().create("var1");
	    comsol.variable("var1").model("mod1");
	    comsol.variable("var1").set("rx_ox_iiet", "q_max_pro*cx_pro*S_ox_xpro*I_pH_xpro_form1*I_h2_xpro", "IIET");
	    comsol.variable("var1").set("rx_red_iiet", "q_max_hyd*cx_hyd*S_h2_xhyd*I_pH_xhyd_form1", "IIET");
	    comsol.variable("var1").set("rx_ox_diet", "q_max_pro*cx_pro*S_ox_xpro*I_pH_xpro_form1", "DIET. Unlimited (see _lim)");
	    comsol.variable("var1").set("rx_red_diet", "q_max_hyd*cx_hyd*I_pH_xhyd_form1", "DIET. Unlimited (see _lim)");
	    comsol.variable("var1").set("ra_hoh", "ka_hoh*(1[mol/(m^3*s)]-c_oh*c_h[mol/(m^3*s)]/Ka_hoh)");
	    comsol.variable("var1").set("ra_hac", "ka_hac*(1[mol/(m^3*s)]*c_hac-c_ac*c_h[mol/(m^3*s)]/Ka_hac)");
	    comsol.variable("var1").set("ra_hpro", "ka_hpro*(1[mol/(m^3*s)]*c_hpro-c_pro*c_h[mol/(m^3*s)]/Ka_hpro)");
	    comsol.variable("var1").set("ra_co2", "ka_co2*(1[mol/(m^3*s)]*c_co2-c_hco3*c_h[mol/(m^3*s)]/Ka_co2)");
	    comsol.variable("var1").set("ra_hco3", "ka_hco3*(1[mol/(m^3*s)]*c_hco3-c_co3*c_h[mol/(m^3*s)]/Ka_hco3)");
	    comsol.variable("var1").set("cx_pro", "n_x");
	    comsol.variable("var1").set("cx_hyd", "n_x");
	    comsol.variable("var1").set("phie_lim", "min(phie_oxT_diet,phie_redT_diet)");
	    comsol.variable("var1").set("c_co2_tot", "c_co2+c_hco3+c_co3");
	    comsol.variable("var1").set("c_hpro_tot", "c_hpro+c_pro");
	    comsol.variable("var1").set("c_hac_tot", "c_hac+c_ac");
	    comsol.variable("var1").set("I_h2_xpro", "Ki_xpro_h2/(Ki_xpro_h2+c_h2)");
	    comsol.variable("var1").set("I_pH_xpro_form2", "if(pH<pHul_xpro,exp(-3*((pH-pHul_xpro)/(pHul_xpro-pHll_xpro))^2),1)", "[Batstone 2002, table 3.5] Empirical lower only pH inhibition");
	    comsol.variable("var1").set("I_pH_xhyd_form2", "if(pH<pHul_xhyd,exp(-3*((pH-pHul_xhyd)/(pHul_xhyd-pHll_xhyd))^2),1)", "[Batstone 2002, table 3.5] Empirical lower only pH inhibition");
	    comsol.variable("var1").set("pH", "-log10(c_h*1e-3[m^3/mol])", "*1e-3 is to go to mol/L. Units needed due to warnings");
	    comsol.variable("var1").set("I_pH_xpro_form1", "(1+2*10^(0.5*(pHll_xpro-pHul_xpro)))/(1+10^(pH-pHul_xpro)+10^(pHll_xpro-pH))", "[Batstone 2002, table 3.5] Empirical lower and upper pH inhibition");
	    comsol.variable("var1").set("I_pH_xhyd_form1", "(1+2*10^(0.5*(pHll_xhyd-pHul_xhyd)))/(1+10^(pH-pHul_xhyd)+10^(pHll_xhyd-pH))", "[Batstone 2002, table 3.5] Empirical lower and upper pH inhibition");
	    comsol.variable("var1").set("S_ox_xpro", "c_hpro_tot/(K_xpro_pro+c_hpro_tot)");
	    comsol.variable("var1").set("S_h2_xhyd", "c_h2/(c_h2+K_xhyd_h2)");
	    comsol.variable("var1").set("rx_lim_ox_diet", "f_rx_ox*rx_ox_diet");
	    comsol.variable("var1").set("rx_lim_red_diet", "f_rx_red*rx_red_diet");
	    comsol.variable("var1").set("f_rx_ox", "min(1/ratio_diet,1)");
	    comsol.variable("var1").set("f_rx_red", "min(ratio_diet,1)");

	    // Create mesh
	    comsol.mesh().create("mesh1", "geom1");
	    comsol.mesh("mesh1").automatic(true);
		comsol.mesh("mesh1").autoMeshSize(meshSize);		// 4 == fine, 5 == normal, 7 == coarser, 9 == extremely coarse (max)
		comsol.mesh("mesh1").run();	
		
		// Define physics
		comsol.physics().create("chnp", "NernstPlanck", "geom1");
		comsol.physics("chnp").field("concentration").component(new String[]{"c_hpro", "c_pro", "c_hac", "c_ac", "c_co2", "c_hco3", "c_co3", "c_h2", "c_ch4", "c_h", "c_oh", "c_k", "c_cl"});
		comsol.physics("chnp").prop("SpeciesProperties").set("FromElectroneutrality", "12");
		comsol.physics("chnp").feature("cdm1").set("D_0", new String[][]{{"D_hpro"}, {"0"}, {"0"}, {"0"}, {"D_hpro"}, {"0"}, {"0"}, {"0"}, {"D_hpro"}});
		comsol.physics("chnp").feature("cdm1").set("D_1", new String[][]{{"D_pro"}, {"0"}, {"0"}, {"0"}, {"D_pro"}, {"0"}, {"0"}, {"0"}, {"D_pro"}});
		comsol.physics("chnp").feature("cdm1").set("D_2", new String[][]{{"D_hac"}, {"0"}, {"0"}, {"0"}, {"D_hac"}, {"0"}, {"0"}, {"0"}, {"D_hac"}});
		comsol.physics("chnp").feature("cdm1").set("D_3", new String[][]{{"D_ac"}, {"0"}, {"0"}, {"0"}, {"D_ac"}, {"0"}, {"0"}, {"0"}, {"D_ac"}});
		comsol.physics("chnp").feature("cdm1").set("D_4", new String[][]{{"D_co2"}, {"0"}, {"0"}, {"0"}, {"D_co2"}, {"0"}, {"0"}, {"0"}, {"D_co2"}});
		comsol.physics("chnp").feature("cdm1").set("D_5", new String[][]{{"D_hco3"}, {"0"}, {"0"}, {"0"}, {"D_hco3"}, {"0"}, {"0"}, {"0"}, {"D_hco3"}});
		comsol.physics("chnp").feature("cdm1").set("D_6", new String[][]{{"D_co3"}, {"0"}, {"0"}, {"0"}, {"D_co3"}, {"0"}, {"0"}, {"0"}, {"D_co3"}});
		comsol.physics("chnp").feature("cdm1").set("D_7", new String[][]{{"D_h2"}, {"0"}, {"0"}, {"0"}, {"D_h2"}, {"0"}, {"0"}, {"0"}, {"D_h2"}});
		comsol.physics("chnp").feature("cdm1").set("D_8", new String[][]{{"D_ch4"}, {"0"}, {"0"}, {"0"}, {"D_ch4"}, {"0"}, {"0"}, {"0"}, {"D_ch4"}});
		comsol.physics("chnp").feature("cdm1").set("D_9", new String[][]{{"D_h"}, {"0"}, {"0"}, {"0"}, {"D_h"}, {"0"}, {"0"}, {"0"}, {"D_h"}});
		comsol.physics("chnp").feature("cdm1").set("D_10", new String[][]{{"D_oh"}, {"0"}, {"0"}, {"0"}, {"D_oh"}, {"0"}, {"0"}, {"0"}, {"D_oh"}});
		comsol.physics("chnp").feature("cdm1").set("D_11", new String[][]{{"D_k"}, {"0"}, {"0"}, {"0"}, {"D_k"}, {"0"}, {"0"}, {"0"}, {"D_k"}});
		comsol.physics("chnp").feature("cdm1").set("D_12", new String[][]{{"D_cl"}, {"0"}, {"0"}, {"0"}, {"D_cl"}, {"0"}, {"0"}, {"0"}, {"D_cl"}});
		comsol.physics("chnp").feature("cdm1").set("z", new String[][]{{"0"}, {"-1"}, {"0"}, {"-1"}, {"0"}, {"-1"}, {"-2"}, {"0"}, {"0"}, {"1"}, {"-1"}, {"1"}, {"-1"}});
	    comsol.physics("chnp").feature("cdm1").set("MobilityModel", 1, "NernstEinstein");
		comsol.physics("chnp").feature("init1").set("c_hpro", "c0_hpro");
		comsol.physics("chnp").feature("init1").set("c_pro", "c0_pro");
		comsol.physics("chnp").feature("init1").set("c_hac", "c0_hac");
		comsol.physics("chnp").feature("init1").set("c_ac", "c0_ac");
		comsol.physics("chnp").feature("init1").set("c_co2", "c0_co2");
		comsol.physics("chnp").feature("init1").set("c_hco3", "c0_hco3");
		comsol.physics("chnp").feature("init1").set("c_co3", "c0_co3");
		comsol.physics("chnp").feature("init1").set("c_h2", "c0_h2");
		comsol.physics("chnp").feature("init1").set("c_ch4", "c0_ch4");
		comsol.physics("chnp").feature("init1").set("c_h", "c0_h");
		comsol.physics("chnp").feature("init1").set("c_oh", "c0_oh");
		comsol.physics("chnp").feature("init1").set("c_cl", "c0_cl");
		
	    comsol.study().create("std1");
	    comsol.study("std1").feature().create("stat", "Stationary");

	    // Create solution
	    comsol.sol().create("sol1");
	    comsol.sol("sol1").study("std1");
	    comsol.sol("sol1").feature().create("st1", "StudyStep");
	    comsol.sol("sol1").feature("st1").set("study", "std1");
	    comsol.sol("sol1").feature("st1").set("studystep", "stat");
	    comsol.sol("sol1").feature().create("v1", "Variables");
	    comsol.sol("sol1").feature().create("s1", "Stationary");
	    comsol.sol("sol1").feature("s1").feature().create("fc1", "FullyCoupled");
	    comsol.sol("sol1").feature("s1").feature("fc1").set("dtech", "auto");
	    comsol.sol("sol1").feature("s1").feature("fc1").set("initstep", 0.01);
	    comsol.sol("sol1").feature("s1").feature("fc1").set("minstep", 1.0E-6);
	    comsol.sol("sol1").feature("s1").feature("fc1").set("maxiter", 50);
	    comsol.sol("sol1").feature("s1").feature().create("i1", "Iterative");
	    comsol.sol("sol1").feature("s1").feature("i1").set("linsolver", "gmres");
	    comsol.sol("sol1").feature("s1").feature("i1").set("prefuntype", "left");
	    comsol.sol("sol1").feature("s1").feature("i1").set("rhob", 20);
	    comsol.sol("sol1").feature("s1").feature("i1").set("itrestart", 50);
	    comsol.sol("sol1").feature("s1").feature("fc1").set("linsolver", "i1");
	    comsol.sol("sol1").feature("s1").feature("i1").feature().create("mg1", "Multigrid");
	    comsol.sol("sol1").feature("s1").feature("i1").feature("mg1").set("prefun", "gmg");
	    comsol.sol("sol1").feature("s1").feature("i1").feature("mg1").set("mcasegen", "any");
	    comsol.sol("sol1").feature("s1").feature("i1").feature("mg1").feature("pr").feature().create("sl1", "SORLine");
	    comsol.sol("sol1").feature("s1").feature("i1").feature("mg1").feature("pr").feature("sl1").set("iter", 2);
	    comsol.sol("sol1").feature("s1").feature("i1").feature("mg1").feature("pr").feature("sl1").set("linerelax", 0.4);
	    comsol.sol("sol1").feature("s1").feature("i1").feature("mg1").feature("pr").feature("sl1").set("seconditer", 1);
	    comsol.sol("sol1").feature("s1").feature("i1").feature("mg1").feature("pr").feature("sl1").set("relax", 0.3);
	    comsol.sol("sol1").feature("s1").feature("i1").feature("mg1").feature("po").feature().create("sl1", "SORLine");
	    comsol.sol("sol1").feature("s1").feature("i1").feature("mg1").feature("po").feature("sl1").set("iter", 2);
	    comsol.sol("sol1").feature("s1").feature("i1").feature("mg1").feature("po").feature("sl1").set("linerelax", 0.4);
	    comsol.sol("sol1").feature("s1").feature("i1").feature("mg1").feature("po").feature("sl1").set("seconditer", 2);
	    comsol.sol("sol1").feature("s1").feature("i1").feature("mg1").feature("po").feature("sl1").set("relax", 0.5);
	    comsol.sol("sol1").feature("s1").feature("i1").feature("mg1").feature("cs").feature().create("d1", "Direct");
	    comsol.sol("sol1").feature("s1").feature("i1").feature("mg1").feature("cs").feature("d1").set("linsolver", "pardiso");
	    comsol.sol("sol1").feature("s1").feature("fc1").set("dtech", "auto");
	    comsol.sol("sol1").feature("s1").feature("fc1").set("initstep", 0.01);
	    comsol.sol("sol1").feature("s1").feature("fc1").set("minstep", 1.0E-6);
	    comsol.sol("sol1").feature("s1").feature("fc1").set("maxiter", 50);
	    comsol.sol("sol1").feature("s1").feature().remove("fcDef");
	    comsol.sol("sol1").attach("std1");
	    comsol.sol("sol1").feature("s1").feature("dDef").active(true);
    }
	
	public void CreateSphere(CCell cell) throws FlException {
		// Pure geometry
		String name = GetCellName(cell);
	    comsol.geom("geom1").feature().create(name, "Sphere");
	    comsol.geom("geom1").feature(name).set("r", Double.toString(cell.ballArray[0].radius*dimensionFactor));
	    comsol.geom("geom1").feature(name).set("createselection", "on");		// Allows us to add something by selection name
	    comsol.geom("geom1").feature(name).set("pos", new String[]{Double.toString(cell.ballArray[0].pos.x), Double.toString(cell.ballArray[0].pos.y), Double.toString(cell.ballArray[0].pos.z)});

	    // Update the model information
	    cellList.add(name);
	    sphList.add(name);
	}
	
	public void CreateRod(CCell cell) throws FlException {
//		double cellHT = ( (cell.ballArray[1].pos.minus(cell.ballArray[0].pos)).norm() + 2.0*cell.ballArray[0].radius )*dimensionFactor;		// HT = Head-Tail
		double cellL = cell.ballArray[1].pos.minus(cell.ballArray[0].pos).norm()*dimensionFactor;
		Vector3d pos0 = cell.ballArray[0].pos.plus(cell.ballArray[1].pos.minus(cell.ballArray[0].pos).times((1.0-dimensionFactor)*0.5));
		Vector3d pos1 = cell.ballArray[1].pos.minus(cell.ballArray[1].pos.minus(cell.ballArray[0].pos).times((1.0-dimensionFactor)*0.5));

		// Create and name geometry for rod cell
		String sph0 = "sph" + cell.Index() + "_" + "0";
		String sph1 = "sph" + cell.Index() + "_" + "1";
		String cyl = "cyl" + cell.Index();
		String uni = "uni" + cell.Index();
		String rot = "rot" + cell.Index() + "y";
		String mov = GetCellName(cell);
	    comsol.geom("geom1").feature().create(sph0, "Sphere");
	    comsol.geom("geom1").feature().create(sph1, "Sphere");
	    comsol.geom("geom1").feature().create(cyl, "Cylinder");
	    comsol.geom("geom1").feature().create(uni, "Union");
	    comsol.geom("geom1").feature().create(rot, "Rotate");
	    comsol.geom("geom1").feature().create(mov, "Move");

	    // Define spheres at arbitrary locations
	    comsol.geom("geom1").feature(sph0).set("r", Double.toString(cell.ballArray[0].radius));
	    comsol.geom("geom1").feature(sph0).set("pos", new String[]{-1.0*cellL+"/2", "0", "0"});
	    comsol.geom("geom1").feature(sph1).set("r", Double.toString(cell.ballArray[0].radius));
	    comsol.geom("geom1").feature(sph1).set("pos", new String[]{     cellL+"/2", "0", "0"});
		// Define cylinder
	    comsol.geom("geom1").feature(cyl).set("axis", new String[]{"1", "0", "0"});
	    comsol.geom("geom1").feature(cyl).set("pos", new String[]{-1.0*cellL+"/2", "0", "0"});
	    comsol.geom("geom1").feature(cyl).set("h", Double.toString(cellL));
	    comsol.geom("geom1").feature(cyl).set("r", Double.toString(cell.ballArray[0].radius));
	    // Create unification of the two spheres and cylinder
	    comsol.geom("geom1").feature(uni).set("face", "all");
	    comsol.geom("geom1").feature(uni).set("intbnd", false);		// Don't keep internal boundaries
	    comsol.geom("geom1").feature(uni).selection("input").set(new String[]{cyl, sph0, sph1});
	    
	    // Rotate the unified object around the axis perpendicular to both the unified object AND the desired cell orientation (pos of ball 1 minus ball 0)
	    Vector3d orientation = cell.ballArray[1].pos.minus(cell.ballArray[0].pos).normalise();		// Normalised vector pointing from ball 0 to ball 1
	    Vector3d xAxis = new Vector3d(1, 0, 0);														// The x axis is always used for the unified object
	    Vector3d axis = orientation.cross(xAxis);													// Cross product so perpendicular to both
	    double angle = Math.toDegrees(Math.acos(orientation.dot(xAxis)));							// The angle between the two axi can be determined with dot product
	    comsol.geom("geom1").feature(rot).set("axis", new String[]{Double.toString(axis.x), Double.toString(axis.y), Double.toString(axis.z)});
	    comsol.geom("geom1").feature(rot).set("rot", Double.toString(-1.0*angle));					// COMSOL seems to swap the direction of the angle compared to my logic. See journal 130523
	    comsol.geom("geom1").feature(rot).selection("input").set(new String[]{uni});
	
	    // Move the rotated cell into position
	    comsol.geom("geom1").feature(mov).set("displx", Double.toString(0.5*(pos0.x+pos1.x)));
	    comsol.geom("geom1").feature(mov).set("disply", Double.toString(0.5*(pos0.y+pos1.y)));
	    comsol.geom("geom1").feature(mov).set("displz", Double.toString(0.5*(pos0.z+pos1.z)));
	    comsol.geom("geom1").feature(mov).selection("input").set(new String[]{rot});
	    
	    // Make the final cell selectable
	    comsol.geom("geom1").feature(mov).set("createselection", "on");
	    
	    // Update model information
	    cellList.add(mov);
	    rodList.add(mov);
	}
	
	public void CreateRodRevolve(CCell cell) throws FlException {
		double cellHT = ( (cell.ballArray[1].pos.minus(cell.ballArray[0].pos)).norm() + 2.0*cell.ballArray[0].radius )*dimensionFactor;		// HT = Head-Tail
		Vector3d pos0 = cell.ballArray[0].pos.plus(cell.ballArray[1].pos.minus(cell.ballArray[0].pos).times((1.0-dimensionFactor)*0.5));
		Vector3d pos1 = cell.ballArray[1].pos.minus(cell.ballArray[1].pos.minus(cell.ballArray[0].pos).times((1.0-dimensionFactor)*0.5));
		
	    // Create WP
	    String wpName = "wp" + Integer.toString(cell.Index());
	    comsol.geom("geom1").feature().create(wpName, "WorkPlane");
	    comsol.geom("geom1").feature(wpName).set("planetype", "general");
	    comsol.geom("geom1").feature(wpName).setIndex("genpoints", String.format("%.4g", pos0.x), 0, 0);		// First vertex, x coordinate
	    comsol.geom("geom1").feature(wpName).setIndex("genpoints", String.format("%.4g", pos0.y), 0, 1);		// y coordinate
	    comsol.geom("geom1").feature(wpName).setIndex("genpoints", String.format("%.4g", pos0.z), 0, 2);		// z coordinate
	    comsol.geom("geom1").feature(wpName).setIndex("genpoints", String.format("%.4g", pos1.x), 1, 0);		// Second vertex, x coordinate
	    comsol.geom("geom1").feature(wpName).setIndex("genpoints", String.format("%.4g", pos1.y), 1, 1);
	    comsol.geom("geom1").feature(wpName).setIndex("genpoints", String.format("%.4g", pos1.z), 1, 2);
	    comsol.geom("geom1").feature(wpName).setIndex("genpoints", String.format("%.4g", pos1.x), 2, 0);
	    comsol.geom("geom1").feature(wpName).setIndex("genpoints", String.format("%.4g", pos1.y), 2, 1);
	    comsol.geom("geom1").feature(wpName).setIndex("genpoints", String.format("%.4g", pos1.z+cell.ballArray[0].radius*dimensionFactor), 2, 2);
	    
	    // Create rectangle in WP
	    String rectName = "rect" + Integer.toString(cell.Index());
	    comsol.geom("geom1").feature(wpName).geom().feature().create(rectName, "Rectangle");
	    comsol.geom("geom1").feature(wpName).geom().feature(rectName).setIndex("size", String.format("%.4g", cellHT), 0);
	    comsol.geom("geom1").feature(wpName).geom().feature(rectName).setIndex("size", String.format("%.4g", cell.ballArray[0].radius*dimensionFactor), 1);			// We're revolving --> half the actual height
	    comsol.geom("geom1").feature(wpName).geom().feature(rectName).setIndex("pos", String.format("%.4g", -cell.ballArray[0].radius*dimensionFactor), 0);			// Move the cell on the x axis, so that the centre of the ball is aligned with the origin 

	    // Fillet the rectangle
	    String filName = "fil" + Integer.toString(cell.Index());
	    comsol.geom("geom1").feature(wpName).geom().feature().create(filName, "Fillet");
	    comsol.geom("geom1").feature(wpName).geom().feature(filName).selection("point").set(rectName + "(1)", new int[]{3, 4});
	    comsol.geom("geom1").feature(wpName).geom().feature(filName).set("radius", String.format("%.4g", cell.ballArray[0].radius*dimensionFactor));

	    // Revolve WP around X axis
	    String name = GetCellName(cell);
	    comsol.geom("geom1").feature().create(name, "Revolve");
	    comsol.geom("geom1").feature(name).set("angtype", "full");
	    comsol.geom("geom1").feature(name).setIndex("axis", "1", 0); 	// Revolve around axis x==1
	    comsol.geom("geom1").feature(name).setIndex("axis", "0", 1);		// ... and y==0
	    comsol.geom("geom1").feature(name).selection("input").set(new String[]{wpName});
	    comsol.geom("geom1").feature(name).set("createselection", "on");	// Make sure we can select this object later on
	
	    // Update model information
	    cellList.add(name);
	    rodList.add(name);
	}
	
	public void CreateBCBox() throws FlException {
		double BCMultiplier = 2.0;
		
	    comsol.geom("geom1").feature().create("blk1", "Block");

	    // Find extremes
	    double minX = 10;
	    double maxX = 0;
	    double minY = 10;
	    double maxY = 0;
	    double minZ = 10;
	    double maxZ = 0;
	    for(CBall ball : java.ballArray) {
	    	if(ball.pos.x < minX) 	minX = ball.pos.x - ball.radius;		// Using radius because initially balls might be in the same plane
	    	if(ball.pos.x > maxX) 	maxX = ball.pos.x + ball.radius;
	    	if(ball.pos.y < minY) 	minY = ball.pos.y - ball.radius;
	    	if(ball.pos.y > maxY) 	maxY = ball.pos.y + ball.radius;
	    	if(ball.pos.z < minZ) 	minZ = ball.pos.z - ball.radius;
	    	if(ball.pos.z > maxZ) 	maxZ = ball.pos.z + ball.radius;
	    }
	    comsol.geom("geom1").feature("blk1").setIndex("pos", "(" + Double.toString(maxX) + "+" + Double.toString(minX) + ")" + "/2.0", 0);
	    comsol.geom("geom1").feature("blk1").setIndex("pos", "(" + Double.toString(maxY) + "+" + Double.toString(minY) + ")" + "/2.0", 1);
	    comsol.geom("geom1").feature("blk1").setIndex("pos", "(" + Double.toString(maxZ) + "+" + Double.toString(minZ) + ")" + "/2.0", 2);
	    
//	    // Additional code to take the max of the max (i.e. box of Lmax x Lmax x Lmax instead of Lxmax x Lymax x Lzmax), remove if you want to undo
//	    if(maxX - minX < maxY - minY)		minX = minY; maxX = maxY;
//	    if(maxX - minX < maxZ - minZ)		minX = minZ; maxX = maxZ;
//	    if(maxY - minY < maxX - minX)		minY = minX; maxY = maxX;
//	    if(maxY - minY < maxZ - minZ)		minY = minZ; maxY = maxZ;
//	    if(maxZ - minZ < maxX - minX)		minZ = minX; maxZ = maxX;
//	    if(maxZ - minZ < maxY - minY)		minZ = minY; maxZ = maxY;
	    
	    // Define block geometry
	    comsol.geom("geom1").feature("blk1").setIndex("size", Double.toString(BCMultiplier) + "*(" + Double.toString(maxX) + "-" + Double.toString(minX) + ") + 10.0e-6", 0);
	    comsol.geom("geom1").feature("blk1").setIndex("size", Double.toString(BCMultiplier) + "*(" + Double.toString(maxY) + "-" + Double.toString(minY) + ") + 10.0e-6", 1);
	    comsol.geom("geom1").feature("blk1").setIndex("size", Double.toString(BCMultiplier) + "*(" + Double.toString(maxZ) + "-" + Double.toString(minZ) + ") + 10.0e-6", 2);
	    comsol.geom("geom1").feature("blk1").set("base", "center");
	    comsol.geom("geom1").feature("blk1").set("createselection", "on");

	    // Set boundary concentrations
	    comsol.physics("chnp").feature().create("conc1", "Concentration", 2);
	    comsol.physics("chnp").feature("conc1").selection().named("geom1_blk1_bnd");
	    comsol.physics("chnp").feature("conc1").set("c0", new String[][]{{"c0_hpro"}, {"c0_pro"}, {"c0_hac"}, {"c0_ac"}, {"c0_co2"}, {"c0_hco3"}, {"c0_co3"}, {"c0_h2"}, {"c0_ch4"}, {"c0_h"}, {"c0_oh"}, {"0"}, {"c0_cl"}});
	    comsol.physics("chnp").feature("conc1").set("species", new String[][]{{"1"}, {"1"}, {"1"}, {"1"}, {"1"}, {"1"}, {"1"}, {"1"}, {"1"}, {"1"}, {"1"}, {"0"}, {"1"}});			// Note that species 12 (K+) is disabled as this is the balancing species
	    
//	    // Subtract cells from this block
//	    String[] cellArray = Arrays.copyOf(cellList.toArray(), cellList.size(), String[].class);	// Convert cellList from ArrayList<String> to String[]. Typecast doesn't work for some reason
//	    comsol.geom("geom1").feature().create("dif1", "Difference");		// Subtract the cells from the block:
//	    comsol.geom("geom1").feature("dif1").selection("input")			// Add this block
//	         .set(new String[]{"blk1"});
//	    comsol.geom("geom1").feature("dif1").selection("input2")			// Subtract all cells from cellList
//	    	 .set(cellArray);	         //.set(new String[]{"sph0","sph2","sph6"});
//	    comsol.geom("geom1").feature("dif1").set("createselection", "on");
	}
	
	public void BuildGeometry() throws FlException {
		comsol.geom("geom1").run();
	}
	
	//////////////////////////////////
	
	public void CreateAcidDissociation() {
	    comsol.physics("chnp").feature().create("reacAD", "Reactions", 3);
	    comsol.physics("chnp").feature("reacAD").name("Reactions acid dissociation");
	    comsol.physics("chnp").feature("reacAD").selection().all();
	    comsol.physics("chnp").feature("reacAD").set("R", new String[][]{{"-ra_hpro"}, {"ra_hpro"}, {"-ra_hac"}, {"ra_hac"}, {"-ra_co2"}, {"ra_co2-ra_hco3"}, {"ra_hco3"}, {"0"}, {"0"}, {"ra_hpro+ra_hac+ra_co2+ra_hco3+ra_hoh"}, {"ra_hoh"}, {"0"}, {"0"}});
	}

	public void CreateElectricPotential(CCell cell) {		// Would rather make this a single physics with a larger selection, but couldn't get COMSOL to select multiple named domains. We might be able to convert names to int[] (which should work) using selection.entities()  
//		String[] cellNameArray = Arrays.copyOf(cellList.toArray(), cellList.size(), String[].class);	// Convert cellList from ArrayList<String> to String[]. Typecast doesn't work for some reason
//		int[] test = comsol.geom("geom1").feature("rod0").selection().entities(2);
		String potName = "pot" + cell.Index();
		comsol.physics("chnp").feature().create(potName, "ElectricPotential", 2);
		comsol.physics("chnp").feature(potName).selection().named("geom1_" + GetCellName(cell) + "_bnd");

	}
	
	public void CreateBiomassReaction(CCell cell, String type) {
		String reacName = "reac" + cell.Index(); 
		String cellName = GetCellName(cell);
	    comsol.physics("chnp").feature().create(reacName, "Reactions", 3);
	    comsol.physics("chnp").feature(reacName).selection().named("geom1_" + cellName + "_dom");
	    if(type.equalsIgnoreCase("ox")) {
	    	comsol.physics("chnp").feature(reacName).set("R", new String[][]{{"0"}, {"-rx_lim_ox_diet"}, {"0"}, {"rx_lim_ox_diet"}, {"rx_lim_ox_diet"}, {"0"}, {"0"}, {"0"}, {"0"}, {"6*rx_lim_ox_diet"}, {"0"}, {"0"}, {"0"}});
	    } else if(type.equalsIgnoreCase("red")) {
	        comsol.physics("chnp").feature(reacName).set("R", new String[][]{{"0"}, {"0"}, {"0"}, {"0"}, {"-rx_lim_red_diet"}, {"0"}, {"0"}, {"0"}, {"rx_lim_red_diet"}, {"-8*rx_lim_red_diet"}, {"0"}, {"0"}, {"0"}});
	    } else {
	    	throw new IndexOutOfBoundsException("Unknown biomass conversion reaction type: " + type);
	    }
	    // Determine total rate for this cell (mol/s)
	    comsol.variable("var1").set("Rx_" + type + cell.Index() + "_diet", "X_CELL" + cell.Index() + "(rx_" + type + "_diet)*" + cell.Volume() + "[m^3]");
	}
	
	public void CreateCurrentDiscontinuity(CCell cell, String type) {
		String cellName = GetCellName(cell);
		String cdName = "cdisc" + cell.Index();
		comsol.physics("chnp").feature().create(cdName, "CurrentDiscontinuity", 2);
		comsol.physics("chnp").feature(cdName).selection().named("geom1_" + cellName + "_bnd");
	    if(type.equalsIgnoreCase("ox")) {
	    	comsol.physics("chnp").feature(cdName).set("i0",  "6*F*Rx_ox" + cell.Index() + "_diet/(" + cell.SurfaceArea() + ")[m^2]");	
	    } else if(type.equalsIgnoreCase("red")) {
	    	comsol.physics("chnp").feature(cdName).set("i0", "-8*F*Rx_red" +cell.Index() + "_diet/(" + cell.SurfaceArea() + ")[m^2]");
	    } else {
	    	throw new IndexOutOfBoundsException("Unknown current discontinuity type: " + type);
	    }
	}
	
	public void CreateAverageOp(CCell cell) {	    // Create domain average function
		String avName = "aveop" + cell.Index();
	    comsol.cpl().create(avName, "Average", "geom1");
	    comsol.cpl(avName).set("opname", "X_CELL" + cell.Index());
	    comsol.cpl(avName).selection().named("geom1_" + GetCellName(cell) + "_bnd");
	}
	
	public void CreateRatioDiet(ArrayList<CCell> oxCellArray, ArrayList<CCell> redCellArray) {
		String stringOx = "";
		for(CCell cell : oxCellArray) {
			 stringOx = stringOx + "+Rx_ox" + cell.Index() + "_diet";
		}
		String stringRed = "";
		for(CCell cell : redCellArray) {
			 stringRed = stringRed + "+Rx_red" + cell.Index() + "_diet";
		}
		comsol.variable("var1").set("ratio_diet", "(6*(" + stringOx + "))/(8*(" + stringRed + "))");
	}
	
	public void CreateRepair(ArrayList<CCell> cellArray) {				// Most likely requires CAD toolbox/license
	    comsol.geom("geom1").feature().create("rep1", "Repair");		// Default is 1e-5, yet is different from setting value in Geometry node to this 
	    for(CCell cell : cellArray) {
	    	comsol.geom("geom1").feature("rep1").selection("input").add(new String[]{GetCellName(cell)});
	    }
	}
	
	//////////////////////////////////

	public String GetCellName(CCell cell) {
		if(cell.type<2)
			return "sph" + cell.Index();
		else if(cell.type <6)
			return "rod" + cell.Index();
		else
			throw new IndexOutOfBoundsException("Cell type: " + cell.type);
	}
	
	public double GetRx(CCell cell, String type, String iet) throws FlException {
		String gevName = "gev" + cell.Index();
	    comsol.result().numerical().create(gevName, "EvalGlobal");
		comsol.result().numerical(gevName).set("expr", "Rx_" + type + cell.Index() + "_" + iet);
		return comsol.result().numerical(gevName).getReal()[0][0];								// Return the value's [0][0] (getReal returns a double[][])
	}
	
	public double GetParameter(CCell cell, String parameter, String name) throws FlException {
		String avName = "av" + Integer.toString(cell.Index()) + "_" + name;						// e.g. av0_c0
		String cellName = (cell.type<2 ? "sph" : "rod") + cell.Index();							// We named it either sphere or rod + the cell's number  
		comsol.result().numerical().create(avName,"AvSurface");									// Determine the average surface value...
		comsol.result().numerical(avName).selection().named("geom1_" + cellName + "_bnd");		// ... of the cell's area's... (if a selection was made, this last part allows us to select its boundaries)
		comsol.result().numerical(avName).set("expr", parameter);								// ... parameter (e.g. concentration 1, "c0") 
		return comsol.result().numerical(avName).getReal()[0][0];								// Return the value's [0][0] (getReal returns a double[][])
	}
	
	//////////////////////////////////
	
	public void Run(){																			// Includes detailed error handling using try/catch
		boolean solved = false;
		while(!solved) {
			try {
				comsol.sol("sol1").runAll();			
				// No problems? Continue then
				solved = true;
			} catch(FlException E) {
				String message = E.toString();
				if(message.contains("Out of memory LU factorization")) {
					java.Write("\tOut of memory during LU factorisation","warning");
					if(meshSize<9) {
						java.Write("\tIncreasing mesh size by 1 to " + ++meshSize + " and re-running", "iter");
						comsol.mesh("mesh1").autoMeshSize(meshSize);	// Add 1 to meshSize, enter that value		
						comsol.mesh("mesh1").run();					// Run mesh again
						continue;									// Try solving again
					} else {
						java.Write("\tCannot increase mesh size any further", "warning");
					}
				} else if(message.contains("Failed to respect boundary element edge on geometry face")) {
					java.Write("\tBoundary element edge meshing problem","warning");
					if(meshSize>1) {
						java.Write("Decreasing mesh size by 1 to " + --meshSize + " and re-running", "iter");
						comsol.mesh("mesh1").autoMeshSize(meshSize);	// Add 1 to meshSize, enter that value		
						comsol.mesh("mesh1").run();						// Run mesh again
						continue;
					} else 		java.Write("\tCannot increase mesh size any further", "warning");
				} else if(message.contains("Mean operator requires an adjacent domain of higher dimension")) {
					java.Write("\tMean operator domain size problem","warning");
					if(meshSize>1) {
						java.Write("Decreasing mesh size by 1 to " + --meshSize + " and re-running", "iter");
						comsol.mesh("mesh1").autoMeshSize(meshSize);	// Add 1 to meshSize, enter that value		
						comsol.mesh("mesh1").run();						// Run mesh again
						continue;
					} else 		java.Write("\tCannot increase mesh size any further", "warning");
				}
				// If we're still here, throw error
				java.Write(message,"");
				throw new RuntimeException("Don't know how to deal with error above, exiting");
			}
		}
				
	}
	
	public void Save() throws IOException {
		comsol.save(System.getProperty("user.dir") + "/" + java.name + "/output/" + String.format("g%04dm%04d", java.growthIter, java.relaxationIter));		// No 2nd arguments --> save as .mph
	}
	
	//////////////////////////////////
	
	public void RemoveModel() {
		ModelUtil.remove("model1");
	}
}
