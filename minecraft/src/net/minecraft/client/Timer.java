package net.minecraft.client;

/**
 * Converts wall-clock time into the number of game ticks that should elapse
 * each frame, along with the partial-tick fraction used to interpolate
 * animation between ticks.
 *
 * It reads two clocks -- the coarse system clock and the high-resolution
 * monotonic clock -- to keep the game speed stable even when the coarse clock
 * is adjusted, and it dampens those adjustments to avoid visible jumps.
 */
public final class Timer {
	float ticksPerSecond = 20.0F;
	private double lastHRTime;
	public int elapsedTicks;
	public float renderPartialTicks;
	private float timerSpeed = 1.0F;
	private float elapsedPartialTicks = 0.0F;
	private long lastSyncSysClock = System.currentTimeMillis();
	private long lastSyncHRClock = System.nanoTime() / 1000000L;
	private double timeSyncAdjustment = 1.0D;

	// The original leftover parameter is not used; the tick rate stays fixed.
	public Timer(float ticksPerSecondParam) {
	}

	public final void updateTimer() {
		long sysClockNow = System.currentTimeMillis();
		long sysClockDelta = sysClockNow - this.lastSyncSysClock;
		long hrClockNow = System.nanoTime() / 1000000L;
		double timeScale;
		if(sysClockDelta > 1000L) {
			// Recalibrate the ratio between the coarse and HR clocks once a second.
			long hrClockDelta = hrClockNow - this.lastSyncHRClock;
			timeScale = (double)sysClockDelta / (double)hrClockDelta;
			this.timeSyncAdjustment += (timeScale - this.timeSyncAdjustment) * (double)0.2F;
			this.lastSyncSysClock = sysClockNow;
			this.lastSyncHRClock = hrClockNow;
		}

		if(sysClockDelta < 0L) {
			// The system clock moved backwards; resync both clocks.
			this.lastSyncSysClock = sysClockNow;
			this.lastSyncHRClock = hrClockNow;
		}

		// Time slice in seconds since the previous frame, scaled to keep speed stable.
		double hrNow = (double)hrClockNow / 1000.0D;
		timeScale = (hrNow - this.lastHRTime) * this.timeSyncAdjustment;
		this.lastHRTime = hrNow;
		if(timeScale < 0.0D) {
			timeScale = 0.0D;
		}

		if(timeScale > 1.0D) {
			timeScale = 1.0D;
		}

		this.elapsedPartialTicks = (float)((double)this.elapsedPartialTicks + timeScale * (double)this.timerSpeed * (double)this.ticksPerSecond);
		this.elapsedTicks = (int)this.elapsedPartialTicks;
		this.elapsedPartialTicks -= (float)this.elapsedTicks;
		if(this.elapsedTicks > 10) {
			this.elapsedTicks = 10;
		}

		this.renderPartialTicks = this.elapsedPartialTicks;
	}
}
