package comsol;

import interactor.Interactor;

import com.comsol.model.util.ModelUtil;


public class Server {
//	public static boolean started;
	
	public static void Start() throws Exception {
		Interactor.executeCommand("comsol -64 -3drend sw server -user tomas -port 2036", false, false);		// Can't waitForFinish, process remains open
//		Runtime.getRuntime().exec("comsol -64 -3drend sw server -user tomas -port 2036");
//		started = true;
	}
	
	public static void Stop(boolean waitForFinish) throws Exception{	// Note that this is a scary function
		Interactor.executeCommand("pkill comsollauncher", waitForFinish, false);
//		Runtime.getRuntime().exec("pkill comsollauncher");
//		started = false;
	}
	
	public static void Connect() {
		ModelUtil.connect("localhost", 2036);
	}
	
	public static void Disconnect() {
		ModelUtil.disconnect();
	}
	
}
