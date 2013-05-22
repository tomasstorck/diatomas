package comsol;

import interactor.Interactor;

import backbone.Assistant;

import com.comsol.model.util.ModelUtil;


public class Server {
	public static void Start(int port) throws Exception {
		String architecture = (Assistant.bit64) ? "-64 " : "-32 ";
		int NProcAvailable = Runtime.getRuntime().availableProcessors();
		String NProcessor;
		if(NProcAvailable<2)
			NProcessor = "-np 1";
		else if(NProcAvailable<8)
			NProcessor = "-np 2";
		else
			NProcessor = "-np 8";
		Interactor.executeCommand("comsol " + architecture + " server " + NProcessor + " -user tomas -port " + port, false, false);		// Can't waitForFinish, process remains open
	}
	
	public static void Stop(boolean waitForFinish) throws Exception{											// Note that this is a scary function
		Interactor.executeCommand("pkill comsollauncher", waitForFinish, false);
	}
	
	public static void Connect(int port) {
		ModelUtil.connect("localhost", port);
	}
	
	public static void Disconnect() {
		ModelUtil.disconnect();
	}
	
}
