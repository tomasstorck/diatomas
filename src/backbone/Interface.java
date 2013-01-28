package backbone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;

import ser2mat.ser2mat;
import cell.CModel;

public class Interface{

	public static void main(String[] args) throws Exception{
		System.out.println("DIATOMAS Java model");

		// Initialise model, simulation and create an object for a copy
		CModel model = new CModel();
		CModel modelRef;										// modelRef is required for loading command line arguments
		Run instance = new Run(model);
		// Analyse command line arguments, immediately execute some, save rest to Hashtable
		int NArg = args.length;
		Hashtable<String, String> argument = new Hashtable<String, String>();
		for(int ii=0; ii<NArg; ii+=2) {
			String arg = args[ii];
			// Case help file
			if(arg.equalsIgnoreCase("help") || arg.equalsIgnoreCase("--help") || arg.equalsIgnoreCase("?") || arg.equalsIgnoreCase("/?")) {
				System.out.println("Usage: java -jar diatomas.jar [option0] [value0] [option1] [value1] ...");
				System.out.println("where [value] be a number (0 == false, 1 == true) or string");
				System.out.println("");
				System.out.println("load [path/filename.seg]\t Load the specified file instead of the default parameters. Automatically starts model after loading");
				System.out.println("ser2mat [path] \t\t\t Converts all .ser files found in [path]/output/ to .mat files. Automatically inhibits model starting after loading");
				return;
			// Case convert serialised files to MATLAB files
			} else if(arg.equalsIgnoreCase("ser2mat")) {
				// Convert ser to mat files
				String modelPath = args[ii+1];
				// Open directory
				File dir = new File(modelPath + "/output/");
				// Construct filter
				FilenameFilter filter = new FilenameFilter() {
				    public boolean accept(File dir, String name) {
				    	return name.endsWith(".ser");
				    }
				};
				// List filtered files and convert
				String[] files = dir.list(filter);
				if(files==null) throw new Exception("No .ser files found in directory " + modelPath + "/output/");
				java.util.Arrays.sort(files);
				for(String fileName : files) { 
					model.Write("Loading " + fileName,"", true, false);
					String loadPath = modelPath + "/output/" + fileName;
					model = Load(loadPath);
					ser2mat.Convert(model);
				}
				return;
			// Case argument needs to be set in the model
			} else {
				// Save all other arguments in the hashtable
				argument.put(arg.toLowerCase(), args[ii+1]);
			}
		}
		
		//
		
		if(argument.containsKey("load")){						// Iterations > 0
			String loadPath = argument.get("load");
			if(!loadPath.contains("/")) {
				// loadPath doesn't state which simulation to load --> load the most recent one
				// Open directory
				File dir = new File(loadPath + "/output/");
				// Construct filter
				FilenameFilter filter = new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.endsWith(".ser");
					}
				};
				// List filtered files
				String[] files = dir.list(filter);
				if(files==null) throw new Exception("No .ser files found in directory " + loadPath + "/output/");
				// Update loadPath based on found .ser files
				java.util.Arrays.sort(files);
				loadPath = loadPath + "/output/" + files[files.length-1];
			}
			model = Load(loadPath);
			modelRef = Load(loadPath); 
			model.Write("Loaded " + loadPath, "");
		} else {												// Start from zero
			// Set all parameters from command line before we initialise
			SetArgument(model, new CModel(), argument);
			// Initialise parameters
			instance.Initialise();
			modelRef = new CModel();
		}
		// Set all parameters from command line, perhaps again, to overwrite whatever was set in the initialiser or load method. SetArgument won't do arguments already set and not changed during intialisation again
		SetArgument(model, modelRef, argument);
		modelRef = null;										// We don't need modelRef anymore, mark it for garbage collection
		// Done analysing input arguments. Start model
		try {
			model.Write("=====================================", "");
			String message = "Starting simulation '" + model.name + "' w/ arguments: ";
			for(int jj=0; jj<args.length; jj++) 	message += args[jj] + " ";
			model.Write(message,"");
			model.Write("=====================================", "");
			// Commence the simulation
			instance.Start();
		} catch (RuntimeException E) {
			StringWriter sw = new StringWriter();				// We need this line and below to get the exception as a string for the log
			PrintWriter pw = new PrintWriter(sw);
			E.printStackTrace(pw);								// Write the exception stack trace to a print
			model.Write(sw.toString(), "error", false, true);	// Convert stack trace to string, print to log
			E.printStackTrace();								// Throw the error so the simulation stops running
		}
	}
			
	public static void SetArgument(CModel model, CModel modelRef, Hashtable<String, String> argument) {
		Enumeration<String> argumentKeys = argument.keys();
		args:while(argumentKeys.hasMoreElements()) {
			String key = argumentKeys.nextElement();
			String value = argument.get(key);
			for(Field field : CModel.class.getFields()) {
				if(key.equalsIgnoreCase(field.getName())) {
					key = field.getName();									// Update key to the correct Capitalisation
					try {
						@SuppressWarnings("rawtypes")
						Class fieldClass = CModel.class.getField(key).get(model).getClass();
						if(fieldClass.equals(Double.class)) {				// Does the field contain a double?
							double number;
							double modelNumber = field.getDouble(model);
							double modelRefNumber = field.getDouble(modelRef);
							// See if we have a relative (e.g. Kan *10) or absolute (e.g. Kan 1e-10) value
							if(value.startsWith("*")) {						// Relative
								double multiplier = Double.parseDouble(value.substring(1));		// Cut off *
								if(modelNumber != modelRefNumber*multiplier) {					// If this were true, the operation would already have been done once
									number = field.getDouble(model) * multiplier;				// Hasn't been multiplied before, so do it
									field.setDouble(model, number);
									model.Write(field.getName() + " set to " + number, "");
								}											// Has been multipliplied before, don't change
							} else {
								number = Double.parseDouble(value);			// Absolute.
								if(modelNumber != number) {
									field.setDouble(model, number);
									model.Write(field.getName() + " set to " + number, "");
								}											
							}
							continue args;									// Check next argument (i.e. continue outer loop)
						} else if(fieldClass.equals(Integer.class)) {		// An int?
							int number = Integer.parseInt(value);
							int modelNumber = field.getInt(model);
							if(modelNumber != number) {
								field.setInt(model, number);
								model.Write(field.getName() + " set to " + number, "");	
							}
							continue args;
						} else if(fieldClass.equals(String.class)) {		// A String?
							String modelString = (String) field.get(model);
							if(!modelString.equals(value)) {
								field.set(model, value);
								model.Write(field.getName() + " set to " + value, "");
							}												// Has been set before, don't change
							continue args;
						} else {
							throw new RuntimeException("Unknown class type");
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
			// Are you still here?
			throw new RuntimeException("Unknown argument: " + key);
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
		
		// Update model name
		String[] splitLoadPath = loadPath.split("/");
		model.name = splitLoadPath[0];
		
		return model;
	}
}