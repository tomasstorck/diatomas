package interactor;

// Linux part stolen from: 
// http://singztechmusings.wordpress.com/2011/06/21/getting-started-with-javas-processbuilder-a-sample-utility-class-to-interact-with-linux-from-java-program/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

public class Interactor {
	public static String executeCommand(String command, boolean waitForResponse, boolean showCommand) {
		String response = "";
		ProcessBuilder pb;
		if(getOS().compareTo("Windows")==0) {
			command = command.replace("/","\\");		// Convert from Unix file separators to Windows (important: note that in the model you should ALWAYS use /). \\ because backslash needs to be escaped.
			pb = new ProcessBuilder("cmd", "/C", command);
		} else {
			pb = new ProcessBuilder("bash", "-c", command);
		}
				
		pb.redirectErrorStream(true);
		if(showCommand) {System.out.println("Command: " + command);}

		try {
			Process shell = pb.start();

			if (waitForResponse) {

				// To capture output from the shell
				InputStream shellIn = shell.getInputStream();

//				// Wait for the shell to finish and get the return code
//				int shellExitStatus = shell.waitFor();
//				System.out.println("Exit status " + shellExitStatus);
				// Wait for the shell to finish
				shell.waitFor();

				response = convertStreamToStr(shellIn);

				shellIn.close();
			}
		}
		catch (IOException e) {
			System.out.println("Error occured while executing command. Error Description: " + e.getMessage());
		}
		catch (InterruptedException e) {
			System.out.println("Error occured while executing command. Error Description: " + e.getMessage());
		}
		return response;
	}

	/*
	 * To convert the InputStream to String we use the Reader.read(char[]
	 * buffer) method. We iterate until the Reader return -1 which means
	 * there's no more data to read. We use the StringWriter class to
	 * produce the string.
	 */

	public static String convertStreamToStr(InputStream is) throws IOException {
		if (is != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(is,
						"UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				is.close();
			}
			return writer.toString();
		}
		else {
			return "";
		}
	}
	
	public static String getOS() {
		if(System.getProperty("os.name").startsWith("Windows",0)) {
			return "Windows";
		} else {
			return "Unix";
		}
		
	}

}