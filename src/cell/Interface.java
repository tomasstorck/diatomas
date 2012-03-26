package cell;

import java.io.*;

public class Interface {

	public static void main(String[] args){
		System.out.println("////// DIATOMAS Java model //////");
		boolean simSet = false;
		boolean menuSet = false;
		String name = "default";

		int Narg = args.length;
		// Find out if the simulation name is specified, ALSO if the menu has been called  
		for(int ii=0; ii<Narg; ii++) {
			if((!args[ii].equalsIgnoreCase("menu"))) {
				// A name has been specified
				simSet = true;
				name = args[ii];
			} else {
				// Menu has been called
				menuSet = true;
			}
		}
		System.out.println("Simulation loaded:\t" + name);

		// Display menu OR go with default Run model choice
		String input = "1";
		if(menuSet) {	// Don't forget to add new options below
			System.out.println("[1] Run model");
			System.out.println("[2] Render POV files");
			System.out.println("[3] Clean files/Reset simulation");

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
				if(	input.equalsIgnoreCase("1") ||
						input.equalsIgnoreCase("2") ||
						input.equalsIgnoreCase("3")) {
					break;
				} 	
				else{System.out.println("Invalid choice");}	
			}
		}

		if(input.equalsIgnoreCase("1")) {
			// Start the model
			new main(name);
		} else if(input.equalsIgnoreCase("2")) {
			// Render POV
			// TODO
		} else if(input.equalsIgnoreCase("3")) {
			// Reset model
			// TODO

		}
	}
}