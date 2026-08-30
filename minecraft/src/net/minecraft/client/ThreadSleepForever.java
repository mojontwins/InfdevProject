package net.minecraft.client;

/**
 * A daemon thread that sleeps essentially forever (about 24.8 days) and refuses
 * to be interrupted. It is used to keep the client's class loader / resources
 * alive when the applet would otherwise tear them down.
 */
final class ThreadSleepForever extends Thread {
	ThreadSleepForever(Minecraft minecraft, String threadName) {
		super(threadName);
		this.setDaemon(true);
		this.start();
	}

	public final void run() {
		while(true) {
			try {
				Thread.sleep(2147483647L);
			} catch (InterruptedException e) {
			}
		}
	}
}
