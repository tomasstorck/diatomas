package backbone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
				System.out.println("sticking\t\t\t Enable or disable cell-cell EPS links (sticking springs)");
				System.out.println("filament\t\t\t Enable or disable filial links between mother and daughter cells");
				System.out.println("gravity\t\t\t Enable or disable gravity");
				System.out.println("anchoring\t\t\t Enable or disable cell-substratum EPS links (anchoring springs)");
				
				System.out.println("  OS communication");
				System.out.println("waitForFinish || disableWaitForFinish\t When calling command line arguments from the model, waits for them to finish running or continues with the model");
				System.out.println("echoCommand \t\t Echoes or silences the command line functions ran from the model");
								
				System.out.println("  Comsol:");
				System.out.println("comsol \t\t\t Enable or disable the use of COMSOL. Enable: use backbone file WithComsol.java, otherwise use WithoutComsol.java");
				System.out.println("port [comsol server port]\t\t\t\t Use port [comsol server port] to start COMSOL server");
				System.out.println("64bit \t\t\t Enable (use 64 bit) or disable (use 32 bit) the use of a 32 bit architecture (can eliminate memory issues, can cause memory leak)");
				
				System.out.println("  Other:");
				System.out.println("start \t\t\t Starts the model automatically after looping through the arguments, or not");
				System.out.println("load [path/filename.extension]\t Load the specified file instead of the default parameters. Automatically starts model after loading");
				System.out.println("makemat [path] \t\t\t Converts all .ser files found in [path]/output/ to .mat files. Automatically inhibits model starting after loading");
								
				System.out.println("*\t\t\t\t Any unrecognised argument is assumed to be simulation the name");
				return;
			}
			//
			if(arg.equalsIgnoreCase("64bit") || arg.equalsIgnoreCase("bit64")) 
															{Assistant.withComsol = (Integer.parseInt(args[ii+1])==1)?true:false;			continue;}
			if(arg.equalsIgnoreCase("anchoring") || arg.equalsIgnoreCase("anchor"))
															{model.anchoring = (Integer.parseInt(args[ii+1])==1)?true:false;				continue;}
			if(arg.equalsIgnoreCase("comsol")) 				{Assistant.withComsol = (Integer.parseInt(args[ii+1])==1)?true:false;			continue;}
			if(arg.equalsIgnoreCase("echocommand")) 		{Assistant.echoCommand = (Integer.parseInt(args[ii+1])==1)?true:false;			continue;}
			if(arg.equalsIgnoreCase("filament")) 			{model.filament = (Integer.parseInt(args[ii+1])==1)?true:false;				continue;}
			if(arg.equalsIgnoreCase("gravity")) 			{model.gravity = (Integer.parseInt(args[ii+1])==1)?true:false;					continue;}
			if(arg.equalsIgnoreCase("gravityz")) 			{model.gravityZ = (Integer.parseInt(args[ii+1])==1)?true:false;				continue;}
			if(arg.equalsIgnoreCase("port")) 				{Assistant.port = Integer.parseInt(args[ii+1]);									continue;}
			if(arg.equalsIgnoreCase("start"))				{Assistant.start = (Integer.parseInt(args[ii+1])==1)?true:false;				continue;}
			if(arg.equalsIgnoreCase("sticking")) 			{model.sticking = (Integer.parseInt(args[ii+1])==1)?true:false;				continue;}
			if(arg.equalsIgnoreCase("waitforfinish")) 		{Assistant.waitForFinish = (Integer.parseInt(args[ii+1])==1)?true:false;		continue;}
			if(arg.equalsIgnoreCase("load")){
				String loadPath = args[ii+1];
				model.Write("Loading " + loadPath, "");
				model = Load(loadPath);
				
				Assistant.start = true;
				continue;}
			if(arg.equalsIgnoreCase("makemat")){
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
			model.name=arg;
			ii--;				// subtract 1 from ii because we don't want to ignore the argument after this name
		}
		
		// Done analysing input arguments
		// Start model if requested
		if(Assistant.start) {
			System.out.print("Starting simulation '" + model.name + "' w/ arguments: ");
			for(int jj=0; jj<args.length; jj++) 	System.out.print(args[jj] + " ");
			System.out.println();
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