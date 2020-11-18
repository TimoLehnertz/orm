package orm;

import java.util.ArrayList;
import java.util.List;

public class Logger {

	public static final int DEBUG = 3;
	public static final int NOTICE = 2;
	public static final int WARN = 1;
	public static final int ERROR = 0;
	
	protected int loglevel = DEBUG;
	
	public int tab = 0;
	
	List<String> logs = new ArrayList<>();
	
	protected boolean debug(String message) {
		return log(message, DEBUG);
	}
	
	protected boolean notice(String message) {
		return log(message, NOTICE);
	}
	
	protected boolean warn(String message) {
		return log(message, WARN);
	}
	
	protected boolean error(String message) {
		return log(message, ERROR);
	}
	
	protected boolean log(String message, int logLevel) {
		if(logLevel > this.loglevel) {
			System.out.println("skipped log");
			return false;
		}
		for (String log : logs) {
			if(message.contentEquals(log)) {
				return false;
			}
		}
		if(logs.size() > 4) {
			logs.remove(0);
		}
		logs.add(message);
		switch(logLevel) {
		case DEBUG: System.out.println("Debug -> " + OrmUtils.getnTabs(tab) + message); break;
		case NOTICE: System.out.println("Notice -> " + OrmUtils.getnTabs(tab) + message); break;
		case WARN: System.err.println("Warning -> " + OrmUtils.getnTabs(tab) + message); break;
		case ERROR: System.err.println("Error -> " + OrmUtils.getnTabs(tab) + message); break;
		}
		return true;
	}

	public int getLoglevel() {
		return loglevel;
	}

	public void setLoglevel(int loglevel) {
		this.loglevel = loglevel;
	}
	
	public void incrementTab() {
		tab++;
	}
	
	public void decrementTab() {
		tab--;
	}
}
