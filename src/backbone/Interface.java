package backbone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.zip.GZIPInputStream;

import ser2mat.ser2mat;

import cell.CModel;

public class Interface{

	public static void main(String[] args) throws Exception{
		System.out.println("DIATOMAS Java model");

		CModel model = new CModel("default");
		
		int NArg = args.length;
		for(int ii=0; ii<NArg; ii+=2) {
			String arg = args[ii];
			//
			if(arg.equalsIgnoreCase("help") || arg.equalsIgnoreCase("--help") || arg.equalsIgnoreCase("?") || arg.equalsIgnoreCase("/?")) {
				System.out.println("Usage: java -jar diatomas.jar [option0] [value0] [option1] [value1] ...");
				System.out.println("where [value] be a number (where 0 == false, 1 == true) or sometimes string and [option] can be any of the following:");
				System.out.println("help || --help || ? || /?\t\t Show this help text");
				
				System.out.println("  Model:");
				System.out.println("seed\t\t\t Set random seed");
				System.out.println("sticking\t\t\t Enable or disable cell-cell EPS links (sticking springs)");
				System.out.println("filament\t\t\t Enable or disable filial links between mother and daughter cells");
				System.out.println("gravity\t\t\t Enable or disable gravity");
				System.out.println("anchoring\t\t\t Enable or disable cell-substratum EPS links (anchoring springs)");
				System.out.println("gravityZ\t\t\t Gravity parallel to the plane instead of perpendicular (needs gravity 1)");
				System.out.println("sphereStraightFil\t\t Sphere-sphere filial links are straight (like streptococci, not staphyllococci)");
				System.out.println("initialAtSubstratum\t\t Initial cells start at substratum (y==ball.radius)");
				System.out.println("syntrophyFactor\t\t Growth acceleration due to cells being stuck to a cell of a different type (1.0 is no acceleration)");
				System.out.println("growthTimeStep\t\t Time passed in seconds per growth step");
				System.out.println("relaxationTimeStep\t\t Time passed in seconds per relaxation step");
				System.out.println("normalForce\t\t\t Use normal force acting at the substratum, y=0");
				
				System.out.println("  OS communication");
				System.out.println("waitForFinish || disableWaitForFinish\t When calling command line arguments from the model, waits for them to finish running or continues with the model");
				System.out.println("echoCommand \t\t Echoes or silences the command line functions ran from the model");
								
				System.out.println("  Comsol:");
				System.out.println("comsol \t\t\t Enable or disable the use of COMSOL. Enable: use backbone file WithComsol.java, otherwise use WithoutComsol.java");
				System.out.println("port [comsol server port]\t\t\t\t Use port [comsol server port] to start COMSOL server");
				System.out.println("64bit \t\t\t Enable (use 64 bit) or disable (use 32 bit) the use of a 32 bit architecture (can eliminate memory issues, can cause memory leak)");
				
				System.out.println("  Other:");
				System.out.println("start \t\t\t Starts the model automatically after looping through the arguments, or not");
				System.out.println("load [path/filename.seg]\t Load the specified file instead of the default parameters. Automatically starts model after loading. Be sure to specify argument comsol");
				System.out.println("ser2mat [path] \t\t\t Converts all .ser files found in [path]/output/ to .mat files. Automatically inhibits model starting after loading");
								
				System.out.println("*\t\t\t\t Any unrecognised argument is assumed to be simulation the name");
				return;
			}
			//
			boolean boolNameSet = false;
			if(arg.equalsIgnoreCase("64bit") || arg.equalsIgnoreCase("bit64")) 
															{Assistant.withComsol = (Integer.parseInt(args[ii+1])==1)?true:false;			continue;}
			if(arg.equalsIgnoreCase("anchoring") || arg.equalsIgnoreCase("anchor"))
															{model.anchoring = (Integer.parseInt(args[ii+1])==1)?true:false;				continue;}
			if(arg.equalsIgnoreCase("comsol")) 				{Assistant.withComsol = (Integer.parseInt(args[ii+1])==1)?true:false;			continue;}
			if(arg.equalsIgnoreCase("echocommand")) 		{Assistant.echoCommand = (Integer.parseInt(args[ii+1])==1)?true:false;			continue;}
			if(arg.equalsIgnoreCase("filament")) 			{model.filament = (Integer.parseInt(args[ii+1])==1)?true:false;					continue;}
			if(arg.equalsIgnoreCase("gravity")) 			{model.gravity = (Integer.parseInt(args[ii+1])==1)?true:false;					continue;}
			if(arg.equalsIgnoreCase("gravityz")) 			{model.gravityZ = (Integer.parseInt(args[ii+1])==1)?true:false;					continue;}
			if(arg.equalsIgnoreCase("growthTimeStep"))		{model.growthTimeStep = Double.parseDouble(args[ii+1]);							continue;}
			if(arg.equalsIgnoreCase("initialatsubstratum")) {model.initialAtSubstratum = (Integer.parseInt(args[ii+1])==1)?true:false;		continue;}
			if(arg.equalsIgnoreCase("relaxationtimeStep"))	{model.relaxationTimeStep = Double.parseDouble(args[ii+1]);						continue;}
			if(arg.equalsIgnoreCase("normalforce"))			{model.normalForce = (Integer.parseInt(args[ii+1])==1)?true:false;				continue;}			
			if(arg.equalsIgnoreCase("port")) 				{Assistant.port = Integer.parseInt(args[ii+1]);									continue;}
			if(arg.equalsIgnoreCase("seed")) 				{model.randomSeed = Integer.parseInt(args[ii+1]);								continue;}
			if(arg.equalsIgnoreCase("start"))				{Assistant.start = (Integer.parseInt(args[ii+1])==1)?true:false;				continue;}
			if(arg.equalsIgnoreCase("spherestraightfil")) 	{model.sphereStraightFil = (Integer.parseInt(args[ii+1])==1)?true:false;		continue;}
			if(arg.equalsIgnoreCase("sticking")) 			{model.sticking = (Integer.parseInt(args[ii+1])==1)?true:false;					continue;}
			if(arg.equalsIgnoreCase("syntrophyFactor"))		{model.syntrophyFactor = Double.parseDouble(args[ii+1]);					continue;}
			if(arg.equalsIgnoreCase("waitforfinish")) 		{Assistant.waitForFinish = (Integer.parseInt(args[ii+1])==1)?true:false;		continue;}
			if(arg.equalsIgnoreCase("load")){
				String loadPath = args[ii+1];
				model.Write("Loading " + loadPath, "");
				model = Load(loadPath);
				Assistant.start = true;
				continue;}
			if(arg.equalsIgnoreCase("ser2mat")){
				String modelPath = args[ii+1];
				// Open directory
				File dir = new File(modelPath + "/output/");
				// Construct filter
				FilenameFilter filter = new FilenameFilter() {
				    public boolean accept(File dir, String name) {
				    	return name.endsWith(".ser");
				    }
				};
				// List filtered files
				String[] files = dir.list(filter);
				if(files==null) throw new Exception("No .ser files found in directory " + modelPath + "/output/");
				for(String fileName : files) { 
					model.Write("Loading " + fileName,"",true);
					String loadPath = modelPath + "/output/" + fileName;
					model = Load(loadPath);
					ser2mat.Convert(model);
				}
				
				Assistant.start = false;
			}
			// If not any of the above, it must be the name
			if(boolNameSet)	throw new Exception("Name already set to '" + model.name + "': cannot set to '" + arg + "'");
			model.name=arg;
			boolNameSet = true;
			ii--;				// subtract 1 from ii because we don't want to ignore the argument after this name
		}
		
		// Done analysing input arguments
		// Start model if requested
		if(Assistant.start) {
			model.Write("=====================================", "");
			String message = "Starting simulation '" + model.name + "' w/ arguments: ";
			for(int jj=0; jj<args.length; jj++) 	message += args[jj] + " ";
			model.Write(message,"");
			model.Write("=====================================", "");
			if(Assistant.withComsol) 				WithComsol.Run(model);
			else									WithoutComsol.Run(model);
		}
	}
	
	public static CModel Load(String loadPath) {
		FileInputStream fis = null;
		GZIPInputStream gz = null;
		ObjectInputStream ois = null;
		CModel model = null;
		
		try {
			fis = new FileInputStream(loadPath);
			gz = new GZIPInputStream(fis);
			ois = new ObjectInputStream(gz);
			model = (CModel) ois.readObject();
			ois.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		
		return model;
	}
}