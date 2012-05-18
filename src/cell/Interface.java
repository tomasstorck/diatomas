package cell;

public class Interface {

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
				System.out.println("enablePlot || disablePlot\t\t Enable or disable plotting");
				System.out.println("defaultParameter\t\t\t Load default parameters for the model");
				System.out.println("load [path/filename.extension]\t Load the speficied file instead of the default parameters");
				System.out.println("*\t\t\t\t Any unrecognised arguments are assumed to be model names");
				return;
			}
			//
			if(arg.equalsIgnoreCase("enableplot")) 	{setting.plot = true;} else
			if(arg.equalsIgnoreCase("disableplot")) {setting.plot = false;} else
			//
			if(arg.equalsIgnoreCase("enableplotintermediate")) {setting.plotIntermediate = true;} else
			if(arg.equalsIgnoreCase("disableplotintermediate")) {setting.plotIntermediate = false;} else
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
			if(arg.equalsIgnoreCase("defaultparameter")) {setting.defaultParameter = true;} else
			if(arg.equalsIgnoreCase("load")) {
				setting.defaultParameter = false;
				ii++;			// Look at the next argument
				model.Write("Loading " + args[ii], "iter");
				model.Load(args[ii]);
			//
			} else {model.name = arg;}	// If not any of the above, it must be the name
			//
		}

		// Set defaults if not called
		if(setting.defaultParameter == true) {
			model.LoadDefaultParameters();
		}

		// Start model if requested
		if(setting.start)	new Run(model);
		
		// Render POV things
		if(setting.postPlot) {
			// TODO
		}
	}
}