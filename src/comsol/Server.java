package comsol;

import com.comsol.model.util.ModelUtil;

public class Server {
	public static void Connect(int port) {
		ModelUtil.connect("localhost", port);
	}
	
	public static void Disconnect() {
		ModelUtil.disconnect();
	}
	
}
