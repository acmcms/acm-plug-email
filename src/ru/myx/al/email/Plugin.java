package ru.myx.al.email;

import java.util.Deque;
import java.util.LinkedList;

import ru.myx.ae1.AbstractPluginInstance;
import ru.myx.ae1.BaseRT3;
import ru.myx.ae3.Engine;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.status.StatusInfo;
import ru.myx.sapi.RuntimeEnvironment;

/**
 * 
 * @author Alexander I. Kharitchev
 * @version 1.0
 */

public class Plugin extends AbstractPluginInstance {
	
	final SenderTask[] tasks = new SenderTask[4];
	
	String smtpHost;
	
	int smtpPort;
	
	boolean plainDefault;
	
	int maxAttempts;
	
	String leastSender;
	
	private final Deque<BaseObject> queuePending = new LinkedList<>();
	
	private final Deque<BaseObject> queueDelay = new LinkedList<>();
	
	@Override
	public void destroy() {
		
		for (int i = this.tasks.length - 1; i >= 0; --i) {
			this.tasks[i] = null;
		}
	}
	
	final synchronized void enqueueEmail(final BaseObject email) {
		
		this.queuePending.addLast(email);
	}
	
	/**
	 * @return
	 */
	protected final synchronized BaseObject getNext() {
		
		return this.queuePending.pollFirst();
	}
	
	@Override
	public void register() {
		
		final RuntimeEnvironment rt3 = BaseRT3.runtime(this.getServer().getRootContext());
		rt3.registerEmailSender(new SenderSimple(this));
		this.getServer().getControlRoot().bind(new NodeRoot(this));
		rt3.registerPersonalActor(new ActorPersonal());
	}
	
	@Override
	public void start() {
		
		this.update(null);
	}
	
	void statusFill(final StatusInfo data) {
		
		data.put("Pending Queue Size", this.queuePending.size());
		data.put("Delay Queue size", this.queueDelay.size());
	}
	
	@Override
	public String toString() {
		
		return "Email Sender (" + this.smtpHost + ":" + this.smtpPort + ")";
	}
	
	void update(final BaseObject dataNew) {
		
		final BaseObject data = this.getSettingsProtected();
		if (dataNew != null) {
			data.baseDefineImportAllEnumerable(dataNew);
		}
		this.smtpHost = Base.getString(data, "host", "localhost");
		this.smtpPort = Convert.MapEntry.toInt(data, "port", 25);
		this.plainDefault = Convert.MapEntry.toBoolean(data, "plain", false);
		this.maxAttempts = Convert.MapEntry.toInt(data, "max_attempts", 3);
		this.leastSender = Base.getString(data, "least_sender", "invalid@" + Engine.HOST_NAME);
		
		for (int i = this.tasks.length - 1; i >= 0; --i) {
			final SenderTask task = new SenderTask(this, i);
			this.tasks[i] = task;
			task.start();
		}
		
		if (dataNew != null) {
			this.commitProtectedSettings();
		}
	}
	
}
