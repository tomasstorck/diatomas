package backbone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.zip.GZIPInputStream;

import ser2mat.ser2mat;
import cell.CModel;

public class Interface{

	public static void main(String[] args) throws Exception{
		System.out.println("DIATOMAS Java model");

		CModel model = new CModel("default");
		
		int NArg = args.length;
		args:for(int ii=0; ii<NArg; ii+=2) {
			String arg = args[ii];
			//
			if(arg.equalsIgnoreCase("help") || arg.equalsIgnoreCase("--help") || arg.equalsIgnoreCase("?") || arg.equalsIgnoreCase("/?")) {
				System.out.println("Usage: java -jar diatomas.jar [option0] [value0] [option1] [value1] ...");
				System.out.println("where [value] be a number (0 == false, 1 == true) or string");
				System.out.println("  Other:");
				System.out.println("start \t\t\t Starts the model automatically after looping through the arguments, or not");
				System.out.println("load [path/filename.seg]\t Load the specified file instead of the default parameters. Automatically starts model after loading. Be sure to specify argument comsol");
				System.out.println("ser2mat [path] \t\t\t Converts all .ser files found in [path]/output/ to .mat files. Automatically inhibits model starting after loading");
								
				System.out.println("*\t\t\t\t Any unrecognised argument is assumed to be simulation the name");
				return;
			}
			if(arg.equalsIgnoreCase("load")){
				String loadPath = args[ii+1];
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
				model.Write("Loaded " + loadPath, "");
				Assistant.start = true;
				continue;
			}
			// Convert ser to mat files
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
				
				Assistant.start = false;
				continue;
			}
			// See if we can set a field value to this
			for(Field field : CModel.class.getFields()) {
				if(arg.equalsIgnoreCase(field.getName())) {
					@SuppressWarnings("rawtypes")
					Class fieldClass = CModel.class.getField(arg).get(model).getClass();
					String value = args[ii+1];
					if(fieldClass.equals(Double.class)) {				// Does the field contain a double?
						double number;
						if(value.contains("*")) number = field.getDouble(model) * Double.parseDouble(value.substring(1));	// Cut off * and multiply
						else number = Double.parseDouble(value);
						field.setDouble(model, number);
						model.Write(field.getName() + " set to " + number, "");
						continue args;									// Check next argument (i.e. continue outer loop)
					} else if(fieldClass.equals(Integer.class)) {		// An int?
						int number = Integer.parseInt(value);
						field.setInt(model, number);
						model.Write(field.getName() + " set to " + number, "");
						continue args;
					} else if(fieldClass.equals(String.class)) {		// A String?
						field.set(model, value);
						model.Write(field.getName() + " set to " + value, "");
						continue args;
					} else {
						throw new RuntimeException("Unknown class type");
					}
				}
			}
			// Are you still here?
			throw new RuntimeException("Unknown argument/field name: " + arg);
		}
		
		// Done analysing input arguments
		// Start model if requested
		if(Assistant.start) {
			try {
				model.Write("=====================================", "");
				String message = "Starting simulation '" + model.name + "' w/ arguments: ";
				for(int jj=0; jj<args.length; jj++) 	message += args[jj] + " ";
				model.Write(message,"");
				model.Write("=====================================", "");
				new Run(model);
			} catch (RuntimeException E) {
				StringWriter sw = new StringWriter();				// We need this line and below to get the exception as a string for the log
				PrintWriter pw = new PrintWriter(sw);
				E.printStackTrace(pw);								// Write the exception stack trace to a print
				model.Write(sw.toString(), "error", false, true);	// Convert stack trace to string, print to log
				E.printStackTrace();								// Throw the error so the simulation stops running
			}
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