package cell;

import java.io.*;

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
			if(arg.equalsIgnoreCase("enableplot")) 	{setting.enablePlot = true;}
			if(arg.equalsIgnoreCase("disableplot")) {setting.enablePlot = false;}
			//
			if(arg.equalsIgnoreCase("defaultparameter")) {setting.defaultParameter = true;}
			if(arg.equalsIgnoreCase("load")) {
				setting.defaultParameter = false;
				ii++;			// Look at the next argument
				model.Load(args[ii]);
			}
			//

//			if(arg.equalsIgnoreCase("enableplot")) 	{enablePlot = true;} else
//			if(arg.equalsIgnoreCase("enableplot")) 	{enablePlot = true;} else
													{model.name = arg;}	// If not any of the above, it must be the name
		}

		// Set defaults if not called
		if(setting.defaultParameter == true) {
			model.LoadDefaultParameters();
		}
	}
}