package ser2mat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;

import cell.*;
import jmatio.*;

public class ser2mat {
	
	public MLChar GetString(String i) {
		MLChar o = new MLChar(null, new String[] {i}, i.length()); 
		return o;
	}
	
	public MLDouble GetDouble(double i) {
		MLDouble o = new MLDouble(null, new double[] {i}, 1);
		return o;
	}
	
	public MLDouble GetDouble(double[] i) {
		MLDouble o = new MLDouble(null, i, i.length);
		return o;
	}
	
	public MLDouble GetBoolean(boolean i) {
		MLDouble o = new MLDouble(null, new double[] {i?1:0}, 1);
		return o;
	}
	
	@SuppressWarnings("unchecked")
	public static void Convert(CModel model) {
		// Loop over classes, starting with model
		ArrayList<Object> oTodoArray = new ArrayList<Object>();			// Array to-do array - yes, can be an "ArrayList< ArrayList<?> >"
		ArrayList<MLStructure> mlObjectArray = new ArrayList<MLStructure>();
		oTodoArray.add(model);
		mlObjectArray.add(new MLStructure("model", new int[] {1,1}));
		int it = 0; 													// it for Todo
		while(it<oTodoArray.size()) {
			Object oRaw = oTodoArray.get(it); 							// Perhaps still to be wrapped
			MLStructure mlO = mlObjectArray.get(it);
			// Wrap oRaw into an ArrayList oArray if we haven't already
			ArrayList<Object> oArray;
			if(oRaw instanceof ArrayList<?>) {
				oArray = (ArrayList<Object>) oRaw;						// This causes a "unchecked" warning we don't need to worry about. Will always be an ArrayList<Object>. Suppressed
			} else if(oRaw instanceof Object[]) {
				oArray = new ArrayList<Object>(Arrays.asList(oRaw));
			} else {
				oArray = new ArrayList<Object>();
				oArray.add(oRaw);
			}
			// And loop over the different elements in ArrayList<o.class> oArray
			for(int io=0; io<oArray.size(); io++) {
				Object o = oArray.get(io);
				Field[] fields = GetAllClassFields(o.getClass()); 		// Should remain the same for oArray
				// Loop over the fields in o
				for(Field f : fields) {
					try {
						// Don't want to try private fields
						if(Modifier.isPrivate(f.getModifiers()))						 
							continue;
						/* Simple types (where we can use the .class field) */
						if(f.getGenericType() 		== long.class) {
							String fname = f.getName();
							long val = f.getLong(o);
							mlO.setField(fname,                    new MLDouble(null, new double[] {val}, 1), io);
						}
						else if (f.getGenericType() == String.class) {
							String fname = f.getName();
							String val = String.valueOf(f.get(o));
							mlO.setField(fname,                    new MLChar(null, new String[] {val}, val.length()), io);
						}
						else if(f.getGenericType() 	== int.class) {
							String fname = f.getName();
							int val = f.getInt(o);
							mlO.setField(fname,                    new MLDouble(null, new double[] {val}, 1), io);
						}
						else if(f.getGenericType() 	== double.class) {
							String fname = f.getName();
							double val = f.getDouble(o);
							mlO.setField(fname,                    new MLDouble(null, new double[] {val}, 1), io);
						}
						else if(f.getGenericType() 	== boolean.class) {
							String fname = f.getName();
							boolean val = f.getBoolean(o);
							mlO.setField(fname,                    new MLDouble(null, new double[] {val?1:0}, 1), io);
						}
						else if(f.getGenericType() 	== cell.Vector3d.class) {
							String fname = f.getName();
							Vector3d val = (Vector3d) f.get(o);
							mlO.setField(fname,                    new MLDouble(null, new double[] {val.x, val.y, val.z}, 3), io);
						}
						else if(f.getGenericType() 	== CCell.class) {
							String fname = f.getName();
							int val;
							if(f.get(o) != null)
								val = ((CCell) f.get(o)).Index();
							else
								val = -1;
							mlO.setField(fname,                    new MLDouble(null, new double[] {val}, 1), io);
						}
						else if(f.getGenericType() 	== CModel.class) {
							continue; 									// Don't save CModel
						}
						else if(f.getType() == int[].class) {
							String fname = f.getName();
							int[] val = (int[]) f.get(o);
							// Convert int[] to double[] (there is no other way than this)
							double[] valDouble = new double[val.length];
							for(int ii=0; ii<val.length; ii++) {
								valDouble[ii] = (int) val[ii];
							}
							mlO.setField(fname,                    new MLDouble(null, valDouble, valDouble.length), io);
						}
						else if(f.getType() == int[][].class) {
							String fname = f.getName();
							int[][] val = (int[][]) f.get(o);
							// Convert int[][] to double[][] (there is no other way than this)
							double[][] valDouble = new double[val.length][val[0].length];
							for(int ii=0; ii<val.length; ii++) {
								for(int jj=0; jj<val[0].length; jj++) {
									valDouble[ii][jj] = (int) val[ii][jj];
								}
							}
							mlO.setField(fname,                    new MLDouble(null, valDouble), io);
						}
						else if(f.getType() == double[].class) {
							String fname = f.getName();
							double[] val = (double[]) f.get(o);
							mlO.setField(fname,                    new MLDouble(null, val, val.length), io);
						}
						else if(f.getType() == double[][].class) {
							String fname = f.getName();
							double[][] val = (double[][]) f.get(o);
							mlO.setField(fname,                    new MLDouble(null, val), io);
						}
						else if(f.getType() == boolean[].class) { 		// Alternatively: f.get(model).getClass().getComponentType()
							String fname = f.getName();
							boolean[] val = (boolean[]) f.get(o);
							// Convert boolean[] to double[]
							double[] valDouble = new double[val.length]; 
							for(int ii=0; ii<val.length; ii++)
								valDouble[ii] = val[ii] ? 1.0 : 0.0;
							mlO.setField(fname,                    new MLDouble(null, valDouble, valDouble.length), io);
						}
						else if(f.getType() == boolean[][].class) {
							String fname = f.getName();
							boolean[][] val = (boolean[][]) f.get(o);
							// Convert boolean[][] to double[][]
							double[][] valDouble = new double[val.length][val[0].length]; 
							for(int ii=0; ii<val.length; ii++)
								for(int jj=0; jj<val[0].length; jj++)
									valDouble[ii][jj] = val[ii][jj] ? 1.0 : 0.0;
							mlO.setField(fname,                    new MLDouble(null, valDouble), io);
						}
						else if(f.getType() == CBall[].class) {
							String fname = f.getName();
							CBall[] val = (CBall[]) f.get(o);
							// Convert CBall[] to double[] by looking at indices
							double[] valDouble = new double[val.length]; 
							for(int ii=0; ii<val.length; ii++)
								valDouble[ii] = val[ii].Index();
							mlO.setField(fname,                    new MLDouble(null, valDouble, valDouble.length), io);
						}
						else if(f.getType() == Vector3d[].class) {
							String fname = f.getName();
							Vector3d[] val = (Vector3d[]) f.get(o);
							// Convert Vector3d[] to double[][]
							double[][] valDouble = new double[val.length][3]; 
							for(int ii=0; ii<val.length; ii++) {
								valDouble[ii][0] = val[ii].x;
								valDouble[ii][1] = val[ii].y;
								valDouble[ii][2] = val[ii].z;
							}
							mlO.setField(fname,                    new MLDouble(null, valDouble), io);
						}
						// Get the really tricky ones (cellArray, ballArray, ...). Luckily we can generalise these
						else if(f.get(o) instanceof ArrayList) { 			// It's an ArrayList
							String fname = f.getName();
							ArrayList<?> fArrayList = (ArrayList<?>) f.get(o); 
							Class<?> c = GetParClass(f);					// It's an ArrayList<Class c>
								if(o == model) {							// This ArrayList is nested directly under CModel
									MLStructure mlONew = new MLStructure(fname, new int[] {((ArrayList<?>) f.get(o)).size() ,1}); 		
									oTodoArray.add(f.get(o));
									mlObjectArray.add(mlONew);
								} else {									// NOT nested directly under CModel --> find indices now
									Method IndexMethod = c.getMethod("Index");
									double[] fIndexArray = new double[fArrayList.size()]; 		// Though it is int, we'll save it as double
									for(int ie=0; ie<fArrayList.size(); ie++) {
										Object e1 = fArrayList.get(ie);
										fIndexArray[ie] = (int) IndexMethod.invoke(e1);
									}
									mlO.setField(fname,            new MLDouble(null, fIndexArray, fIndexArray.length), io);
								}
						}
						else {
							throw new RuntimeException("Unknown class: "+f.getGenericType().toString()+" for variable "+f.getName()); 
						}
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					} catch (SecurityException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			}
			it++;
		}
		// populate mlModel and save
		ArrayList<MLArray> list = new ArrayList<MLArray>(1);
		MLStructure mlModel = mlObjectArray.get(0);
		for(int is=1; is<mlObjectArray.size(); is++) { 			// Don't start at 0, that's mlModel
			MLStructure mlO = mlObjectArray.get(is);
			mlModel.setField(mlO.name, 		   mlO);
		}
		list.add(mlModel);
		
		try {
			new MatFileWriter("results/" + model.name + "/output/" + String.format("g%04dr%04d", model.growthIter, model.relaxationIter) + ".mat",list);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Class<?> GetParClass(Field f) {
		return (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
	}
	
	private static Field[] GetAllClassFields(Class<?> c) {
		if(c.getSuperclass() == null || c.getSuperclass() == Object.class) {
			return c.getDeclaredFields();
		} else {
			ArrayList<Field> allFields = new ArrayList<Field>();
			// Add this class' fields
			for( Field f : c.getDeclaredFields() )					allFields.add(f);
			// Analyse superclass in the same way as this class (might have another super class)
			for( Field f : GetAllClassFields(c.getSuperclass()))	allFields.add(f);
			return  Arrays.copyOf(allFields.toArray(), allFields.size(), Field[].class);
		}
	}
}