package net.minecraft.game.entity.ai;

import net.minecraft.game.entity.EntityCreature;
import net.minecraft.game.physics.Vec3D;
import util.MathHelper;

/**
 * Picks random nearby blocks for wandering, scored by the creature's
 * {@code getBlockPathWeight}.  Each call samples 10 candidate points in a
 * {@code [-XZ..XZ] x [-Y..Y] x [-XZ..XZ]} box and returns the best one.
 */
public class RandomPositionGenerator {
	/** Scratch vector reused by the "towards" / "away from" helpers. */
	private static Vec3D randomPosition = null;

	/** Returns the best weighted random point in a box, or null. */
	public static Vec3D findRandomTarget(EntityCreature creature, int rangeXZ, int rangeY) {
		return findRandomTargetBlock(creature, rangeXZ, rangeY, null);
	}

	/** Biases the sample toward the given offset. */
	public static Vec3D findRandomTargetBlockTowards(EntityCreature creature, int rangeXZ, int rangeY, Vec3D bias) {
		return findRandomTargetBlock(creature, rangeXZ, rangeY, bias);
	}

	/** Biases the sample away from the given offset. */
	public static Vec3D findRandomTargetBlockAwayFrom(EntityCreature creature, int rangeXZ, int rangeY, Vec3D bias) {
		Vec3D negated = new Vec3D(-bias.xCoord, -bias.yCoord, -bias.zCoord);
		return findRandomTargetBlock(creature, rangeXZ, rangeY, negated);
	}

	private static Vec3D findRandomTargetBlock(EntityCreature creature, int rangeXZ, int rangeY, Vec3D bias) {
		Vec3D found = null;
		int bestX = -1;
		int bestY = -1;
		int bestZ = -1;
		float bestScore = -99999.0F;

		for (int sample = 0; sample < 10; ++sample) {
			int sampleX = MathHelper.floor_double(creature.posX + (double)creature.getRNG().nextInt(rangeXZ * 2 + 1) - (double)rangeXZ);
			int sampleY = MathHelper.floor_double(creature.posY + (double)creature.getRNG().nextInt(rangeY * 2 + 1) - (double)rangeY);
			int sampleZ = MathHelper.floor_double(creature.posZ + (double)creature.getRNG().nextInt(rangeXZ * 2 + 1) - (double)rangeXZ);

			if (bias != null) {
				sampleX += MathHelper.floor_double(bias.xCoord * (double)sample);
				sampleY += MathHelper.floor_double(bias.yCoord * (double)sample);
				sampleZ += MathHelper.floor_double(bias.zCoord * (double)sample);
			}

			float score = creature.getBlockPathWeight(sampleX, sampleY, sampleZ);
			if (score > bestScore) {
				bestScore = score;
				bestX = sampleX;
				bestY = sampleY;
				bestZ = sampleZ;
			}
		}

		if (bestX >= 0) {
			found = new Vec3D((double)((float)bestX + 0.5F), (double)((float)bestY + 0.5F), (double)((float)bestZ + 0.5F));
		}
		setRandomPosition(found);
		return found;
	}

	public static Vec3D getRandomPosition() {
		return randomPosition;
	}

	public static void setRandomPosition(Vec3D randomPosition) {
		RandomPositionGenerator.randomPosition = randomPosition;
	}
}
