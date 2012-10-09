package backbone;

import java.io.File;
import java.io.FilenameFilter;

import cell.CModel;

public class Interface{

	public static void main(String[] args) throws Exception{
		System.out.println("DIATOMAS Java model");

		int NArg = args.length;
		for(int ii=0; ii<NArg; ii+=2) {
			String arg = args[ii];
			//
			if(arg.equalsIgnoreCase("help") || arg.equalsIgnoreCase("--help") || arg.equalsIgnoreCase("?") || arg.equalsIgnoreCase("/?")) {
				System.out.println("Usage: java -jar diatomas.jar [option0] [value0] [option1] [value1] ...");
				System.out.println("where [value] be a number (where 0 == false, 1 == true) or sometimes string and [option] can be any of the following:");
				System.out.println("help || --help || ? || /?\t\t Show this help text");
				System.out.println("comsol \t\t\t Enable or disable the use of COMSOL. Enable: use backbone file WithComsol.java, otherwise use WithoutComsol.java");
				System.out.println("echoCommand \t\t Echoes or silences the command line functions ran from the model");
				System.out.println("plot \t\t\t Enable or disable plotting");
				System.out.println("plotIntermediate \t\t Enable or disable plotting of intermediate movement steps");
				System.out.println("postPlot \t\t\t Runs or does not run a postPlot after starting the model. Can be combined with *IntermediatePlot. Be sure to disable start if you just want to postPlot");
				System.out.println("start \t\t\t Starts the model automatically after looping through the arguments, or not");
				System.out.println("waitForFinish || disableWaitForFinish\t When calling command line arguments from the model, waits for them to finish running or continues with the model");
				System.out.println("load [path/filename.extension]\t Load the speficied file instead of the default parameters");
				System.out.println("port [comsol server port]\t\t\t\t Use port [comsol server port] to start COMSOL server");
				System.out.println("*\t\t\t\t Any unrecognised arguments are assumed to be model names");
				return;
			}
			//
			if(arg.equalsIgnoreCase("comsol")) 				{Assistant.withComsol = (Integer.parseInt(args[ii+1])==1)?true:false;			continue;} 
			if(arg.equalsIgnoreCase("plot")) 				{Assistant.plot = (Integer.parseInt(args[ii+1])==1)?true:false;				continue;}
			if(arg.equalsIgnoreCase("plotintermediate")) 	{Assistant.plotIntermediate = (Integer.parseInt(args[ii+1])==1)?true:false;	continue;}
			if(arg.equalsIgnoreCase("start"))				{Assistant.start = (Integer.parseInt(args[ii+1])==1)?true:false;				continue;}
			if(arg.equalsIgnoreCase("waitforfinish")) 		{Assistant.waitForFinish = (Integer.parseInt(args[ii+1])==1)?true:false;		continue;}
			if(arg.equalsIgnoreCase("echocommand")) 		{Assistant.echoCommand = (Integer.parseInt(args[ii+1])==1)?true:false;			continue;}
			if(arg.equalsIgnoreCase("load")){
				CModel.Write("Loading " + args[ii], "iter");
				CModel.Load(args[ii+1]);
				continue;}
			if(arg.equalsIgnoreCase("port")) 				{Assistant.port = Integer.parseInt(args[ii+1]);									continue;}
			if(arg.equalsIgnoreCase("postplot")) 			{Assistant.postPlot = (Integer.parseInt(args[ii+1])==1)?true:false;			continue;}
			// If not any of the above, it must be the name
			CModel.name=arg;
			ii--;				// subtract 1 from ii because we don't want to ignore the argument after this name
		}
		
		// Done analysing input arguments
		// Start model if requested
		if(Assistant.start) {
			System.out.print("Loading w/ arguments: ");
			for(int ii=0; ii<args.length; ii++) 	System.out.print(args[ii] + " ");
			System.out.println();
			if(Assistant.withComsol) 				WithComsol.Run();
			else									WithoutComsol.Run();
		}
		// Render POV things
		if(Assistant.postPlot) {
			// Open directory
			String name = CModel.name;
			File dir = new File(name + "/output/");
			// Construct filter
			FilenameFilter filter = new FilenameFilter() {
			    public boolean accept(File dir, String name) {
			    	return name.endsWith(".mat");
			    }
			};
			// List filtered files
			String[] files = dir.list(filter);
			if(files==null) throw new Exception("No files found in directory " + name + "/output/");
			for(String fileName : files) { 
				CModel.Write("Loading " + fileName,"",true);
				CModel.Load(name + "/output/" + fileName);			// Note that not included variables will still be defined as default, could lead to issues
				// Fix name if it was run from another folder
				CModel.name = name;
				CModel.POV_Write(Assistant.plotIntermediate);
				CModel.POV_Plot(Assistant.plotIntermediate);	
			}
		}
	}
}