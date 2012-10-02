package backbone;

import java.io.File;
import java.io.FilenameFilter;

import cell.CModel;

public class Interface{

	public static void main(String[] args) throws Exception{
		System.out.println("DIATOMAS Java model");

		CModel model = new CModel("default");

		int NArg = args.length;
		for(int ii=0; ii<NArg; ii++) {
			String arg = args[ii];
			//
			if(arg.equalsIgnoreCase("help") || arg.equalsIgnoreCase("--help") || arg.equalsIgnoreCase("?") || arg.equalsIgnoreCase("/?")) {
				System.out.println("Usage: java -jar diatomas.jar [arguments]");
				System.out.println("where arguments can be any of the following:");
				System.out.println("help || --help || ? || /?\t\t Show this help text");
				System.out.println("enableEchoCommand || disableEchoCommand\t\t Echoes or silences the command line functions ran from the model");
				System.out.println("enablePlot || disablePlot\t\t Enable or disable plotting");
				System.out.println("enablePlotIntermediate || disablePlotIntermediate\t Enable or disable plotting of intermediate movement steps");
				System.out.println("enablePostPlot || disablePostPlot\t\t Runs or does not run a postPlot after starting the model. Can be combined with *IntermediatePlot. Be sure to disableStart if you just want to postPlot");
				System.out.println("enableStart || disableStart\t\t Starts the model automatically after looping through the arguments, or not");
				System.out.println("enableWaitForFinish || disableWaitForFinish\t When calling command line arguments from the model, waits for them to finish running or continues with the model");
				System.out.println("withComsol || withoutComsol\t\t Enable or disable the use of COMSOL. Enable: use backbone file WithComsol.java, otherwise use WithoutComsol.java");
				System.out.println("load [path/filename.extension]\t Load the speficied file instead of the default parameters");
				System.out.println("port [comsol server port]\t\t\t\t Use port [comsol server port] to start COMSOL server");
				System.out.println("*\t\t\t\t Any unrecognised arguments are assumed to be model names");
				return;
			}
			//
			if(arg.equalsIgnoreCase("enableplot")) 	{setting.plot = true;} else
			if(arg.equalsIgnoreCase("disableplot")) {setting.plot = false;} else
			//
			if(arg.equalsIgnoreCase("enableplotintermediate") || arg.equalsIgnoreCase("enableintermediateplot")) {setting.plotIntermediate = true;} else
			if(arg.equalsIgnoreCase("disableplotintermediate") || arg.equalsIgnoreCase("disableintermediateplot")) {setting.plotIntermediate = false;} else
			//
			if(arg.equalsIgnoreCase("disablestart")) {setting.start = false;} else
			if(arg.equalsIgnoreCase("enablestart")) {setting.start = true;} else
			//
			if(arg.equalsIgnoreCase("enablewaitforfinish")) {setting.waitForFinish = true;} else
			if(arg.equalsIgnoreCase("disablewaitforfinish")) {setting.waitForFinish = false;} else
			//
			if(arg.equalsIgnoreCase("enableechocommand")) {setting.echoCommand = true;} else
			if(arg.equalsIgnoreCase("disableechocommand")) {setting.echoCommand = false;} else
			//
			if(arg.equalsIgnoreCase("withcomsol")) {setting.withComsol = true;} else
			if(arg.equalsIgnoreCase("withoutcomsol")) {setting.withComsol = false;} else
			//
			if(arg.equalsIgnoreCase("load")) {
				ii++;			// Look at the next argument
				model.Write("Loading " + args[ii], "iter");
				model.Load(args[ii]);} else
			//
			if(arg.equalsIgnoreCase("port")) {
				ii++;			// Look at the next argument
				setting.port = Integer.parseInt(args[ii]);} else
			//
			if(arg.equalsIgnoreCase("enablepostplot")) {setting.postPlot = true;} else
			if(arg.equalsIgnoreCase("disablepostplot")) {setting.postPlot = false;}
			else {model.name=arg;}	// If not any of the above, it must be the name
			//
		}
		
		// Done analysing input arguments
		
		// Start model if requested
		if(setting.start) {
			System.out.print("Loading w/ arguments: ");
			for(int ii=0; ii<args.length; ii++) 	System.out.print(args[ii] + " ");
			System.out.println();
			if(setting.withComsol) 				WithComsol.Run(model);
			else								WithoutComsol.Run(model);
		}
		// Render POV things
		if(setting.postPlot) {
			// Open directory
			String name = model.name;
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
				model.Write("Loading " + fileName,"",true);
				model = null;
				model = new CModel(name);
				model.Load(name + "/output/" + fileName);
				// Fix name if it was run from another folder
				model.name = name;
				model.POV_Write(setting.plotIntermediate);
				model.POV_Plot(setting.plotIntermediate);	
			}
		}
	}
}