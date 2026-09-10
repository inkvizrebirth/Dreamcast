package com.dreamcast.client.automation;

import com.dreamcast.client.baritone.BaritoneBridge;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tick-based parkour correction layer used while Baritone executes a planned
 * route. Baritone remains responsible for path search; this controller handles
 * the timing-sensitive part of a jump that a static settings profile cannot
 * express (head-hitter windows, diagonal corrections and landing alignment).
 *
 * <p>The controller never teleports the player and never changes pitch. If a
 * movement hint is unavailable it falls back to ordinary forward/sprint/jump
 * input, which also makes the node useful with compatible Baritone forks whose
 * path executor API does not expose movement details.</p>
 */
@Environment(EnvType.CLIENT)
public final class ParkourController {
	private static final int MAX_TICKS = 20 * 90;
	private static final int REPLAN_AFTER_STALL_TICKS = 36;
	private static final int FAIL_AFTER_STALL_TICKS = 180;
	private static final double ARRIVAL_DISTANCE = 1.45D;
	private static final double EPSILON = 0.015D;
	private static final double LOOK_AHEAD = 1.15D;
	private static final float MAX_TURN_PER_TICK = 18.0F;
	private static final float HEAD_HITTER_TURN = 45.0F;

	private boolean active;
	private boolean complete;
	private boolean failed;
	private boolean replanRequested;
	private BlockPos target;
	private String profile = "universal";
	private int ticks;
	private int airborneTicks;
	private int stalledTicks;
	private double previousDistance = Double.MAX_VALUE;
	private double originX;
	private double originZ;
	private boolean originCaptured;
	private float launchYaw;
	private boolean h2hTurn;

	private boolean forwardOverride;
	private boolean sprintOverride;
	private boolean jumpOverride;
	private boolean leftOverride;
	private boolean rightOverride;

	/** Creates an idle controller. */
	public ParkourController() { }

	/**
	 * Starts an adaptive controller for a new Baritone goal.
	 *
	 * @param destination final block position to reach
	 * @param requestedProfile selected parkour profile
	 */
	public void start(BlockPos destination, String requestedProfile) {
		stop();
		target = destination == null ? null : new BlockPos(destination.getX(), destination.getY(), destination.getZ());
		profile = normalizeProfile(requestedProfile);
		active = target != null;
		complete = false;
		failed = false;
		replanRequested = false;
		ticks = 0;
		airborneTicks = 0;
		stalledTicks = 0;
		previousDistance = Double.MAX_VALUE;
		originX = originZ = 0.0D;
		originCaptured = false;
	}

	/**
	 * Advances the controller by one client tick.
	 *
	 * @param client current Minecraft client
	 * @param hint movement selected by Baritone, or {@code null} when unavailable
	 */
	public void tick(Minecraft client, BaritoneBridge.MovementHint hint) {
		if (!active || client == null || client.player == null || client.level == null || target == null) return;
		ticks++;
		if (ticks > MAX_TICKS) {
			failed = true;
			stopKeys(client);
			return;
		}

		double targetX = hint == null ? target.getX() + 0.5D : hint.destination().getX() + 0.5D;
		double targetZ = hint == null ? target.getZ() + 0.5D : hint.destination().getZ() + 0.5D;
		double dx = targetX - client.player.getX();
		double dz = targetZ - client.player.getZ();
		double distance = Math.hypot(dx, dz);
		if (Math.hypot(target.getX() + 0.5D - client.player.getX(), target.getZ() + 0.5D - client.player.getZ()) <= ARRIVAL_DISTANCE
				&& Math.abs(target.getY() - client.player.getY()) <= 2.0D) {
			complete = true;
			stopKeys(client);
			return;
		}

		if (distance + EPSILON < previousDistance) stalledTicks = 0;
		else stalledTicks++;
		previousDistance = distance;
		if (stalledTicks >= REPLAN_AFTER_STALL_TICKS) replanRequested = true;
		if (stalledTicks >= FAIL_AFTER_STALL_TICKS) {
			failed = true;
			stopKeys(client);
			return;
		}

		if (client.player.onGround()) airborneTicks = 0;
		else airborneTicks++;
		boolean h2h = "h2h".equals(profile);
		boolean plannerActive = hint != null;
		float desiredYaw = yawTo(dx, dz);
		if (h2h && airborneTicks > 0 && airborneTicks <= 3 && h2hTurn) {
			desiredYaw = launchYaw + Math.copySign(HEAD_HITTER_TURN, wrap(desiredYaw - launchYaw));
		}
		steerYaw(client, desiredYaw, h2h && airborneTicks > 0 && airborneTicks <= 3);

		BlockPos feet = BlockPos.containing(client.player.getX(), client.player.getY(), client.player.getZ());
		boolean obstacle = obstacleAhead(client, dx, dz);
		boolean gap = gapAhead(client, dx, dz);
		BlockPos headAhead = BlockPos.containing(client.player.getX() + dx / Math.max(0.001D, distance) * LOOK_AHEAD,
				client.player.getY() + 2.0D, client.player.getZ() + dz / Math.max(0.001D, distance) * LOOK_AHEAD);
		boolean headHitter = solid(client, feet.above(2)) || solid(client, feet.above(3))
				|| solid(client, headAhead) || solid(client, headAhead.above());
		boolean verticalStep = target.getY() > client.player.getY() + 0.35D;
		boolean adaptive = !"balanced".equals(profile);
		boolean jumpWindow = client.player.onGround() && (obstacle || gap || verticalStep || (adaptive && distance > 1.8D));
		if (jumpWindow) {
			if (airborneTicks == 0) {
				launchYaw = client.player.getYRot();
				h2hTurn = h2h && (headHitter || Math.abs(wrap(desiredYaw - launchYaw)) >= 22.5F);
			}
			setOverride(client.options.keyJump, true, Key.JUMP);
		} else if (airborneTicks > 5 || client.player.onGround()) {
			setOverride(client.options.keyJump, false, Key.JUMP);
		}

		// A Baritone path already owns W and sprint. Only the fallback path drives
		// those keys; this is what keeps the correction layer from fighting it.
		if (!plannerActive) {
			setOverride(client.options.keyUp, true, Key.FORWARD);
			setOverride(client.options.keySprint, true, Key.SPRINT);
		}
		boolean correctLeft = crossTrackCorrection(hint, client.player.getX(), client.player.getZ(), dx, dz) < -0.35D;
		boolean correctRight = crossTrackCorrection(hint, client.player.getX(), client.player.getZ(), dx, dz) > 0.35D;
		if (h2h && airborneTicks > 0 && airborneTicks <= 3 && h2hTurn) {
			float turn = wrap(desiredYaw - client.player.getYRot());
			correctLeft = turn > 2.0F;
			correctRight = turn < -2.0F;
		}
		setOverride(client.options.keyLeft, correctLeft, Key.LEFT);
		setOverride(client.options.keyRight, correctRight, Key.RIGHT);
	}

	/** Requests a fresh Baritone path after a stationary movement window. */
	public boolean consumeReplanRequest() {
		if (!replanRequested) return false;
		replanRequested = false;
		stalledTicks = 0;
		return true;
	}

	/** @return whether the current parkour segment reached its destination */
	public boolean isComplete() { return complete; }

	/** @return whether the controller gave up after a timeout or stall */
	public boolean isFailed() { return failed; }

	/** @return whether an adaptive segment is currently active */
	public boolean isActive() { return active; }

	/** Stops the controller and releases only keys it owns. */
	public void stop() {
		Minecraft client = Minecraft.getInstance();
		stopKeys(client);
		active = false;
	}

	private void steerYaw(Minecraft client, float desiredYaw, boolean exactTurnWindow) {
		float delta = wrap(desiredYaw - client.player.getYRot());
		if (exactTurnWindow) client.player.setYRot(client.player.getYRot() + clamp(delta, -HEAD_HITTER_TURN, HEAD_HITTER_TURN));
		else client.player.setYRot(client.player.getYRot() + clamp(delta, -MAX_TURN_PER_TICK, MAX_TURN_PER_TICK));
	}

	private boolean obstacleAhead(Minecraft client, double dx, double dz) {
		double length = Math.max(0.001D, Math.hypot(dx, dz));
		BlockPos ahead = BlockPos.containing(client.player.getX() + dx / length * LOOK_AHEAD,
				client.player.getY(), client.player.getZ() + dz / length * LOOK_AHEAD);
		return solid(client, ahead) || solid(client, ahead.above());
	}

	private boolean gapAhead(Minecraft client, double dx, double dz) {
		double length = Math.max(0.001D, Math.hypot(dx, dz));
		BlockPos ahead = BlockPos.containing(client.player.getX() + dx / length * LOOK_AHEAD,
				client.player.getY() - 1.0D, client.player.getZ() + dz / length * LOOK_AHEAD);
		return !solid(client, ahead) && !solid(client, ahead.below());
	}

	private boolean solid(Minecraft client, BlockPos pos) {
		BlockState state = client.level.getBlockState(pos);
		return !state.isAir() && !state.getCollisionShape(client.level, pos).isEmpty();
	}

	private double crossTrackCorrection(BaritoneBridge.MovementHint hint, double x, double z, double dx, double dz) {
		double sourceX = hint == null ? originX : hint.source().getX() + 0.5D;
		double sourceZ = hint == null ? originZ : hint.source().getZ() + 0.5D;
		if (hint == null && !originCaptured) {
			originX = x;
			originZ = z;
			originCaptured = true;
			sourceX = x;
			sourceZ = z;
		}
		double length = Math.max(0.001D, Math.hypot(dx, dz));
		return (dx / length) * (z - sourceZ) - (dz / length) * (x - sourceX);
	}

	private void setOverride(KeyMapping mapping, boolean pressed, Key key) {
		boolean owned = switch (key) {
			case FORWARD -> forwardOverride;
			case SPRINT -> sprintOverride;
			case JUMP -> jumpOverride;
			case LEFT -> leftOverride;
			case RIGHT -> rightOverride;
		};
		if (pressed) {
			mapping.setDown(true);
			setOwned(key, true);
		} else if (owned) {
			mapping.setDown(false);
			setOwned(key, false);
		}
	}

	private void setOwned(Key key, boolean value) {
		switch (key) {
			case FORWARD -> forwardOverride = value;
			case SPRINT -> sprintOverride = value;
			case JUMP -> jumpOverride = value;
			case LEFT -> leftOverride = value;
			case RIGHT -> rightOverride = value;
		}
	}

	private void stopKeys(Minecraft client) {
		if (client != null && client.options != null) {
			if (forwardOverride) client.options.keyUp.setDown(false);
			if (sprintOverride) client.options.keySprint.setDown(false);
			if (jumpOverride) client.options.keyJump.setDown(false);
			if (leftOverride) client.options.keyLeft.setDown(false);
			if (rightOverride) client.options.keyRight.setDown(false);
		}
		forwardOverride = sprintOverride = jumpOverride = leftOverride = rightOverride = false;
	}

	private static String normalizeProfile(String value) {
		String normalized = value == null ? "universal" : value.trim().toLowerCase(java.util.Locale.ROOT);
		if (normalized.equals("neo") || normalized.equals("head_to_head") || normalized.equals("head-to-head")) return "h2h";
		if (normalized.equals("balanced")) return "balanced";
		return "universal";
	}

	private static float yawTo(double dx, double dz) { return (float) Math.toDegrees(Math.atan2(-dx, dz)); }
	private static float wrap(float value) {
		while (value > 180.0F) value -= 360.0F;
		while (value < -180.0F) value += 360.0F;
		return value;
	}
	private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }

	private enum Key { FORWARD, SPRINT, JUMP, LEFT, RIGHT }
}
