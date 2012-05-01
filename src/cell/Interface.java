package cell;

import java.io.*;

public class Interface {

	public static void main(String[] args) throws Exception{
		System.out.println("////// DIATOMAS Java model //////");
		// Defaults
		boolean enablePlot = true;
		boolean enableMenu = false;
		boolean simSet = false;
		
		String name = "default";

		int NArg = args.length;
		// Find out if the simulation name is specified, ALSO if the menu has been called  
		for(int ii=0; ii<NArg; ii++) {
			String arg = args[ii];
			if(arg.equalsIgnoreCase("enableplot")) 	{enablePlot = true;} else
			if(arg.equalsIgnoreCase("disableplot")) {enablePlot = false;} else
			if(arg.equalsIgnoreCase("menu")) 		{enableMenu = true;} else
//			if(arg.equalsIgnoreCase("enableplot")) 	{enablePlot = true;} else
//			if(arg.equalsIgnoreCase("enableplot")) 	{enablePlot = true;} else
													{name = arg;}
		}
		System.out.println("Simulation loaded:\t" + name);

		// Display menu OR go with default Run model choice
		String input = "1";
		CModel model = new CModel(name);
		if(enableMenu) {	// Don't forget to add new options below
			System.out.println("[1] Run model from current time");
			System.out.println("[2] Set t0");
			System.out.println("[3] Render POV files");
			System.out.println("[4] Clean files/Reset simulation");
			while(true) {
				System.out.print("\n choose: ");
				if(!simSet) {
					BufferedReader reader;
					reader = new BufferedReader(new InputStreamReader(System.in));
					try {
						input = reader.readLine();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				System.out.println();
				if(input.equalsIgnoreCase("1") ||
						input.equalsIgnoreCase("3") ||
						input.equalsIgnoreCase("4")) {
					break;
				} else if(input.equalsIgnoreCase("2")) {
					BufferedReader reader;
					reader = new BufferedReader(new InputStreamReader(System.in));
					try {
						String input2 = reader.readLine();
						int model.movementIter = Integer.valueOf(input2);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else System.out.println("Invalid choice");	
			}
		}

		if(input.equalsIgnoreCase("1")) {
			// Start the model
			model.LoadDefaultParameters();
			new Run(model,enablePlot);
		}
		if(input.equalsIgnoreCase("2")) {
			// Start the model
			model.Load();
			new Run(model,enablePlot);
		} else if(input.equalsIgnoreCase("3")) {
			// Render POV
			model.POV_Plot();
		} else if(input.equalsIgnoreCase("4")) {
			// Reset model
			// TODO

		}
	}
}