package orm;

public class Logger {

	public static final int DEBUG = 3;
	public static final int INFO = 2;
	public static final int WARN = 1;
	public static final int ERROR = 0;
	public static final int NO_LOG = -1;
	
	static final String LINE_PLACEHOLDER = "--  ";
	
	private boolean enabled = true;
	
	protected int loglevel = DEBUG;
	
	public int tab = 0;
	
	protected boolean debug(Object message) {
		return log(message, DEBUG);
	}
	
	protected boolean info(Object message) {
		return log(message, INFO);
	}
	
	protected boolean warn(Object message) {
		return log(message, WARN);
	}
	
	protected boolean error(Object message) {
		return log(message, ERROR);
	}
	
	protected boolean log(Object message, int logLevel) {
		if(logLevel > this.loglevel || message.toString().length() == 0 || !enabled) {
			return false;
		}
		switch(logLevel) {
		case DEBUG: System.out.println(	"[Debug] " + OrmUtils.getNStrings(LINE_PLACEHOLDER, tab) + message); break;
		case INFO: System.out.println(	"[Info]  " + OrmUtils.getNStrings(LINE_PLACEHOLDER, tab) + message); break;
		case WARN: System.err.println(	"[Warn]  " + OrmUtils.getNStrings(LINE_PLACEHOLDER, tab) + message); break;
		case ERROR: System.err.println(	"[Error] " + OrmUtils.getNStrings(LINE_PLACEHOLDER, tab) + message); break;
		default: return false;
		}
		return true;
	}

	public int getLoglevel() {
		return loglevel;
	}

	public void setLoglevel(int loglevel) {
		this.loglevel = loglevel;
	}
	
	public boolean incrementTab() {
		return incrementTab(null, 0);
	}
	
	public boolean incrementTab(String message, int logLevel) {
		boolean succsess = true;
		if(message != null) {
			succsess = log(message, logLevel);
		}
		tab++;
		return succsess;
	}
	
	public boolean decrementTab(String message, int logLevel) {
		boolean succsess = true;
		tab--;
		if(message != null) {
			succsess = log(message, logLevel);
		}
		return succsess;
	}
	
	public boolean decrementTab() {
		return decrementTab(null, 0);
	}
	
	public void enable() {
		this.enabled = true;
	}
	
	public void disable() {
		this.enabled = false;
	}
}
