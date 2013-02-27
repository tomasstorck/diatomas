package backbone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import ser2mat.ser2mat;
import cell.CModel;

public class Interface{

	public static void main(String[] args) throws Exception{
		System.out.println("DIATOMAS Java model");

		// Initialise model, simulation and create an object for a copy
		CModel model = new CModel();
		CModel modelRef;										// modelRef is required for loading command line arguments
		Run instance;
		// Analyse command line arguments, immediately execute some, save rest to Hashtable
		int NArg = args.length;
		Map<String, String> argument = Collections.synchronizedMap(new LinkedHashMap<String, String>());
		for(int ii=0; ii<NArg; ii+=2) {
			String arg = args[ii];
			// Case help file
			if(arg.equalsIgnoreCase("help") || arg.equalsIgnoreCase("--help") || arg.equalsIgnoreCase("?") || arg.equalsIgnoreCase("/?")) {
				System.out.println("Usage: java -jar diatomas.jar [option0] [value0] [option1] [value1] ...");
				System.out.println("where [value] be a number (and 0 == false, 1 == true) or string");
				System.out.println("");
				System.out.println("args\t\t\t\t Shows all possible model input arguments");
				System.out.println("load [path/filename.seg]\t Load the specified file instead of the default parameters. Automatically starts model after loading");
				System.out.println("ser2mat [path] \t\t\t Converts all .ser files found in [path]/output/ to .mat files. Automatically inhibits model starting after loading");
				return;
			// Case argument enumeration
			} else if(arg.equalsIgnoreCase("args")) {
				System.out.println("Possible command line arguments:");
				int counter = 1;
				for(Field field : CModel.class.getFields()) {
					System.out.print(String.format("%-25s",field.getName()));
					if(counter%3==0)	System.out.println("");		
					counter++;
				}
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
			instance = new Run(model);
			model.Write("Loaded " + loadPath, "");
		} else {												// Start from zero
			// Set name to prevent writing things to default folder
			if(argument.containsKey("name"))		model.name = argument.get("name");
			// Set all parameters from command line before we initialise
			SetArgument(model, new CModel(), argument);
			// Initialise parameters
			instance = new Run(model);
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
			
	public static void SetArgument(CModel model, CModel modelRef, Map<String, String> argument) {
		Iterator<Entry<String, String>> argumentKeys = argument.entrySet().iterator();
		args:while(argumentKeys.hasNext()) {
			Entry<String, String> iter = argumentKeys.next(); 
			String keyRaw = iter.getKey();
			String key = keyRaw.contains("[") ? keyRaw.split("\\[")[0] : keyRaw;	// Double escape was necessary. Remove the part at and after "[" if present
			String value = iter.getValue();											
			for(Field field : CModel.class.getFields()) {
				if(key.equalsIgnoreCase(field.getName())) {
					key = field.getName();						// Update key to the correct Capitalisation
					try {
						@SuppressWarnings("rawtypes")
						Class fieldClass = CModel.class.getField(key).get(model).getClass();
						// If the field is any kind of array
						if(field.get(model).getClass().isArray()) {
							// We change only a single index
							if(keyRaw.contains("[")) {
								String[] keySplit = keyRaw.split("\\[");
								String iiString = keySplit[1];
								iiString = iiString.replace("[","");
								iiString = iiString.replace("]","");
								// Array of array (matrix)
								if(keySplit.length>2) {
									String fieldClassName = fieldClass.getComponentType().getComponentType().getName();
									String jjString = keySplit[2];
									jjString = jjString.replace("[","");
									jjString = jjString.replace("]","");
									int ii = Integer.parseInt(iiString);
									int jj = Integer.parseInt(jjString);
									// boolean[][]					// OPTIMISE: We could make this with generics, virtually the same code is used for all these types
									if(fieldClassName.equals("boolean")) {
										boolean[][] modelBool = (boolean[][]) field.get(model);
										boolean[][] bool= new boolean[modelBool.length][modelBool[0].length];
										for(int kk=0; kk<modelBool.length; kk++)	bool[kk] = modelBool[kk].clone();		// We need to create a deep copy
										bool[ii][jj] = Integer.parseInt(value) == 1 ? true : false;
										java.util.Arrays.equals(modelBool, bool);
										if(modelBool[ii][jj] != bool[ii][jj])
											model.Write(field.getName() + "[" + ii + "][" + jj + "] set to " + (bool[ii][jj]?"true":"false"), "");
										field.set(model, bool);
										continue args;
									}
									// double[][]
									if(fieldClassName.equals("double")) {
										double[][] numberRef = (double[][]) field.get(modelRef);
										double[][] number= new double[numberRef.length][numberRef[0].length];
										for(int kk=0; kk<numberRef.length; kk++)	number[kk] = numberRef[kk].clone();		// We need to create a deep copy
										// See if we have a relative (e.g. Ka[0] *10) or absolute (e.g. Ka[0] 1e-10) value
										if(value.startsWith("*")) {	// Relative
											double multiplier = Double.parseDouble(value.substring(1));		// Cut off *
											if(number[ii][jj] != numberRef[ii][jj]*multiplier) { 				// If this were true, the operation would already have been done once
												number[ii][jj] *= multiplier;									// Hasn't been multiplied before, so do it
												model.Write(field.getName() + "[" + ii + "][" + jj + "] set to " + number[ii][jj], "");
											}
										} else {
											number[ii][jj] = Double.parseDouble(value);
											if(number[ii][jj] != numberRef[ii][jj]) 
												model.Write(field.getName() + "[" + ii + "][" + jj + "] set to " + number[ii][jj], "");
										}
										field.set(model, number);
										continue args;
									}
									// int[][]
									if(fieldClassName.equals("int")) {
										int[][] modelNumber = (int[][]) field.get(model);
										int[][] number = new int[modelNumber.length][modelNumber[0].length];
										for(int kk=0; kk<modelNumber.length; kk++)	number[kk] = modelNumber[kk].clone();		// We need to create a deep copy
										number[ii][jj] = Integer.parseInt(value);
										if(modelNumber[ii][jj] != number[ii][jj])
											model.Write(field.getName() + "[" + ii + "][" + jj + "] set to " + number[ii][jj], "");
										field.set(model, number);
										continue args;
									}
									// String[][]
									if(fieldClassName.equals("String")) {
										String[][] modelString = (String[][]) field.get(model);
										String[][] string = new String[modelString.length][modelString[0].length];
										for(int kk=0; kk<modelString.length; kk++)	string[kk] = modelString[kk].clone();		// We need to create a deep copy
										string[ii][jj] = value;
										if(modelString[ii][jj].equals(string[ii][jj]))
											model.Write(field.getName() + "[" + ii + "][" + jj + "] set to " + string[ii][jj], "");
										field.set(model, string);
										continue args;
									}	
								// Array (vector)									
								} else {
									String fieldClassName = fieldClass.getComponentType().getName();
									// boolean[]
									if(fieldClassName.equals("boolean")) {
										boolean[] modelBool = (boolean[]) field.get(model);
										boolean[] bool = modelBool.clone();
										int ii = Integer.parseInt(iiString);
										bool[ii] = Integer.parseInt(value) == 1 ? true : false;
										java.util.Arrays.equals(modelBool, bool);
										if(modelBool[ii] != bool[ii])
											model.Write(field.getName() + "[" + ii + "] set to " + (bool[ii]?"true":"false"), "");
										field.set(model, bool);
										continue args;
									}
									// double[]
									if(fieldClassName.equals("double")) {
										double[] modelNumber = (double[]) field.get(model);
										double[] modelRefNumber = (double[]) field.get(modelRef);
										double[] number = modelNumber.clone();
										int ii = Integer.parseInt(iiString);
										// See if we have a relative (e.g. Ka[0] *10) or absolute (e.g. Ka[0] 1e-10) value
										if(value.startsWith("*")) {	// Relative
											double multiplier = Double.parseDouble(value.substring(1));		// Cut off *
											if(number[ii] != modelRefNumber[ii]*multiplier) { 				// If this were true, the operation would already have been done once
												number[ii] *= multiplier;									// Hasn't been multiplied before, so do it
												model.Write(field.getName() + "[" + ii + "] set to " + number[ii], "");
											}
										} else {
											number[ii] = Double.parseDouble(value);
											if(number[ii] != modelRefNumber[ii]) 
												model.Write(field.getName() + "[" + ii + "] set to " + number[ii], "");
										}
										field.set(model, number);
										continue args;
									}
									// int[]
									if(fieldClassName.equals("int")) {
										int[] modelNumber = (int[]) field.get(model);
										int[] number = modelNumber.clone();
										int ii = Integer.parseInt(iiString);
										number[ii] = Integer.parseInt(value);
										if(modelNumber[ii] != number[ii])
											model.Write(field.getName() + "[" + ii + "] set to " + number[ii], "");
										field.set(model, number);
										continue args;
									}
									// String[]
									if(fieldClassName.equals("String")) {
										String[] modelString = (String[]) field.get(model);
										String[] string = modelString.clone();
										int ii = Integer.parseInt(iiString);
										string[ii] = value;
										if(modelString[ii].equals(string[ii]))
											model.Write(field.getName() + "[" + ii + "] set to " + string[ii], "");
										field.set(model, string);
										continue args;
									}	
								}
							// We make an entirely new array
							} else {
								String fieldClassName = fieldClass.getComponentType().getName();
								String[] splitValue = value.split(",");			// Split at comma
								for(int ii=0; ii<splitValue.length; ii++) {		// Replace all curly braces
									splitValue[ii] = splitValue[ii].replace("{","");
									splitValue[ii] = splitValue[ii].replace("}","");
								}
								// boolean[]
								if(fieldClassName.equals("boolean")) {
									boolean[] bool = new boolean[splitValue.length];
									for(int ii=0; ii<splitValue.length; ii++) {
										boolean modelBool = ((boolean[]) field.get(model))[ii];
										bool[ii] = Integer.parseInt(splitValue[ii]) == 1 ? true : false;
										if(modelBool != bool[ii])
											model.Write(field.getName() + "[" + ii + "] set to " + (bool[ii]?"true":"false"), "");
									}
									field.set(model, bool);
									continue args;
								}
								// double[]
								if(fieldClassName.equals("double")) {
									double[] number = new double[splitValue.length];
									for(int ii=0; ii<splitValue.length; ii++) {
										// See if we have a relative (e.g. Ka[0] *10) or absolute (e.g. Ka[0] 1e-10) value
										if(splitValue[ii].startsWith("*")) {	// Relative
											double modelNumber = ((double[]) field.get(model))[ii];
											double modelRefNumber = ((double[]) field.get(modelRef))[ii];
											double multiplier = Double.parseDouble(splitValue[ii].substring(1));		// Cut off *
											if(modelNumber != modelRefNumber*multiplier) { 								// If this were true, the operation would already have been done once
												number[ii] = modelNumber * multiplier;			// Hasn't been multiplied before, so do it								// Otherwise has been multipliplied before, don't change
												model.Write(field.getName() + "[" + ii + "] set to " + number[ii], "");
											}

										} else {
											double modelNumber = ((double[]) field.get(model))[ii];
											number[ii] = Double.parseDouble(splitValue[ii]);
											if(modelNumber != number[ii]) 
												model.Write(field.getName() + "[" + ii + "] set to " + number[ii], "");
										}
									}
									field.set(model, number);
									continue args;
								}
								// int[]
								if(fieldClassName.equals("int")) {
									int[] number = new int[splitValue.length];
									for(int ii=0; ii<splitValue.length; ii++) {
										int modelNumber = ((int[]) field.get(model))[ii];
										number[ii] = Integer.parseInt(splitValue[ii]);
										if(modelNumber != number[ii]) 
											model.Write(field.getName() + "[" + ii + "] set to " + number[ii], "");
									}
									field.set(model, number);
									continue args;
								}
								// String[]
								if(fieldClassName.equals("String")) {
									String[] string = new String[splitValue.length];
									for(int ii=0; ii<splitValue.length; ii++) {
										String modelString = (String) field.get(model);
										string[ii] = splitValue[ii];
										if(!modelString.equals(string[ii])) 
											model.Write(field.getName() + " set to " + value, "");
									}
									field.set(model, string);
									continue args;
								}
							}
						// The field is NOT an array
						} else {
							String fieldClassName = fieldClass.getSimpleName();
							// boolean
							if(fieldClassName.equals("Boolean")) {
								boolean bool = Integer.parseInt(value) == 1 ? true : false;
								boolean modelBool = field.getBoolean(model);
								if(modelBool != bool) {
									field.setBoolean(model, bool);
									model.Write(field.getName() + " set to " + (bool?"true":"false"), "");
								}
								continue args;
							}
							// double
							if(fieldClassName.equals("Double")) {
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
									}											// Otherwise has been multipliplied before, don't change
								} else {
									number = Double.parseDouble(value);			// Absolute.
									if(modelNumber != number) {
										field.setDouble(model, number);
										model.Write(field.getName() + " set to " + number, "");
									}											
								}
								continue args;									// Check next argument (i.e. continue outer loop)
							}
							// int
							if(fieldClassName.equals("Integer")) {
								int number = Integer.parseInt(value);
								int modelNumber = field.getInt(model);
								if(modelNumber != number) {
									field.setInt(model, number);
									model.Write(field.getName() + " set to " + number, "");	
								}
								continue args;
							}
							// String
							if(fieldClassName.equals("String")) {
								String modelString = (String) field.get(model);
								if(!modelString.equals(value)) {
									field.set(model, value);
									model.Write(field.getName() + " set to " + value, "");
								}												// Has been set before, don't change
								continue args;
							}
							// Throw an error
							throw new RuntimeException("Unknown class type");
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
			// Are you still here?
			if(!key.equalsIgnoreCase("load"))		throw new RuntimeException("Unknown argument: " + key);
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