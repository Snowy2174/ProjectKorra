package com.projectkorra.projectkorra.airbending;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.attribute.Attribute;

public class AirBurst extends AirAbility {

	public enum BurstMode {
		SPHERE, CONE, FALL
	}

	private boolean isCharged;
	private BurstMode burstMode;
	private int sneakParticles;
	private float playerFallDistance;
	@Attribute(Attribute.CHARGE_DURATION)
	private long chargeTime;
	private long coneChargeTime;
	@Attribute(Attribute.COOLDOWN)
	private long sphereCooldown;
	@Attribute(Attribute.COOLDOWN)
	private long coneCooldown;
	@Attribute(Attribute.COOLDOWN)
	private long fallCooldown;
	private double fallThreshold;
	@Attribute(Attribute.KNOCKBACK)
	private double spherePushFactor;
	private double conePushFactor;
	private double fallPushFactor;
	@Attribute(Attribute.DAMAGE)
	private double sphereDamage;
	private double coneDamage;
	private double fallDamage;
	@Attribute(Attribute.RANGE)
	private double sphereRange;
	private double coneRange;
	private double fallRange;
	private boolean canFlickLevers;
	private boolean canOpenDoors;
	private boolean canPressButtons;
	private boolean canExtinguishBlocks;
	private boolean separateChargeTimes;

	private double blastAngleTheta;
	private double blastAnglePhi;
	private double particlePercentage;
	private double coneAngle;
	private int coneParticles;
	private ArrayList<AirBlast> blasts;
	private ArrayList<Entity> affectedEntities;

	/**
	 * Constructor for AirBurst with mode parameter
	 *
	 * @param player The player using the ability
	 * @param mode The burst mode (SPHERE, CONE, FALL)
	 */
	public AirBurst(final Player player, final BurstMode mode) {
		super(player);

		this.burstMode = mode;

		if (mode == BurstMode.FALL) {
			if (this.bPlayer.isOnCooldown("AirBurstFall")) {
				this.remove();
				return;
			}
		} else if (mode == BurstMode.CONE) {
			if (this.bPlayer.isOnCooldown("AirBurstCone")) {
				this.remove();
				return;
			}
		} else {
			if (this.bPlayer.isOnCooldown("AirBurst")) {
				this.remove();
				return;
			}
		}

		AirBurst existingBurst = null;
		if (hasAbility(player, AirBurst.class)) {
			existingBurst = getAbility(player, AirBurst.class);
		}

		if (mode == BurstMode.CONE) {
			if (existingBurst != null && existingBurst.isCharged) {
				this.bPlayer.addCooldown("AirBurstCone", existingBurst.coneCooldown);
				existingBurst.burstMode = BurstMode.CONE;
				existingBurst.executeBurst();
				existingBurst.remove();
				this.remove();
				return;
			}
			this.remove();
			return;
		}

		if (mode != BurstMode.CONE && existingBurst != null && !existingBurst.isCharged) {
			this.remove();
			return;
		}

		this.setFields();

		if (mode == BurstMode.FALL) {
			if (this.playerFallDistance >= this.fallThreshold) {
				this.executeBurst();
				this.bPlayer.addCooldown("AirBurstFall", this.fallCooldown);
				this.remove();
			} else {
				this.remove();
			}
			return;
		}

		if (mode == BurstMode.CONE) {
			this.bPlayer.addCooldown("AirBurstCone", this.coneCooldown);
			this.executeBurst();
			this.remove();
			return;
		}
		this.start();
	}

	public void setFields() {
		this.isCharged = this.burstMode == BurstMode.CONE || this.burstMode == BurstMode.FALL;
		this.playerFallDistance = player.getFallDistance();

		this.chargeTime = getConfig().getLong("Abilities.Air.AirBurst.ChargeTime");
		this.coneChargeTime = getConfig().getLong("Abilities.Air.AirBurst.Cone.ChargeTime");
		this.sphereCooldown = getConfig().getLong("Abilities.Air.AirBurst.Cooldown");
		this.coneCooldown = getConfig().getLong("Abilities.Air.AirBurst.Cone.Cooldown");
		this.fallCooldown = getConfig().getLong("Abilities.Air.AirBurst.Fall.Cooldown");
		this.fallThreshold = getConfig().getDouble("Abilities.Air.AirBurst.Fall.Threshold");
		this.spherePushFactor = getConfig().getDouble("Abilities.Air.AirBurst.PushFactor");
		this.conePushFactor = getConfig().getDouble("Abilities.Air.AirBurst.Cone.PushFactor");
		this.fallPushFactor = getConfig().getDouble("Abilities.Air.AirBurst.Fall.PushFactor");
		this.sphereDamage = getConfig().getDouble("Abilities.Air.AirBurst.Damage");
		this.coneDamage = getConfig().getDouble("Abilities.Air.AirBurst.Cone.Damage");
		this.fallDamage = getConfig().getDouble("Abilities.Air.AirBurst.Fall.Damage");
		this.sphereRange = getConfig().getDouble("Abilities.Air.AirBurst.Range");
		this.coneRange = getConfig().getDouble("Abilities.Air.AirBurst.Cone.Range");
		this.fallRange = getConfig().getDouble("Abilities.Air.AirBurst.Fall.Range");
		this.separateChargeTimes = getConfig().getBoolean("Abilities.Air.AirBurst.SeparateChargeTimes");
		this.canFlickLevers = getConfig().getBoolean("Abilities.Air.AirBurst.CanFlickLevers");
		this.canOpenDoors = getConfig().getBoolean("Abilities.Air.AirBurst.CanOpenDoors");
		this.canPressButtons = getConfig().getBoolean("Abilities.Air.AirBurst.CanPressButtons");
		this.canExtinguishBlocks = getConfig().getBoolean("Abilities.Air.AirBurst.CanExtinguishBlocks");
		this.blastAnglePhi = getConfig().getDouble("Abilities.Air.AirBurst.AnglePhi");
		this.blastAngleTheta = getConfig().getDouble("Abilities.Air.AirBurst.AngleTheta");
		this.coneAngle = Math.toRadians(getConfig().getDouble("Abilities.Air.AirBurst.Cone.Angle", 30));
		this.coneParticles = getConfig().getInt("Abilities.Air.AirBurst.Cone.ParticleCount", getConfig().getInt("Abilities.Air.AirBurst.Particles", 8));
		this.sneakParticles = getConfig().getInt("Abilities.Air.AirBurst.SneakParticles");
		this.particlePercentage = getConfig().getDouble("Abilities.Air.AirBurst.ParticlePercentage");

		this.blasts = new ArrayList<>();
		this.affectedEntities = new ArrayList<>();
	}

	/**
	 * Legacy constructor for backward compatibility
	 *
	 * @param player The player using the ability
	 * @param isFallBurst Whether this is a fall burst
	 */
	public AirBurst(final Player player, final boolean isFallBurst) {
		this(player, isFallBurst ? BurstMode.FALL : BurstMode.SPHERE);
	}

	@Override
	public void progress() {
		if (!this.bPlayer.canBend(this)) {
			this.remove();
			return;
		}

		long relevantChargeTime;
		if (this.separateChargeTimes && this.burstMode == BurstMode.CONE) {
			relevantChargeTime = this.coneChargeTime;
		} else {
			relevantChargeTime = this.chargeTime;
		}

		if (System.currentTimeMillis() > this.getStartTime() + relevantChargeTime && !this.isCharged) {
			this.isCharged = true;
		}

		if (!this.player.isSneaking()) {
			if (this.isCharged) {
				this.bPlayer.addCooldown("AirBurst", this.sphereCooldown);
				this.executeBurst();
				this.remove();
				return;
			} else {
				this.remove();
				return;
			}
		} else if (this.isCharged) {
			final Location location = this.player.getEyeLocation();
			playAirbendingParticles(location, this.sneakParticles);
		}
	}

	/**
	 * Executes the appropriate burst based on the current mode
	 */
	private void executeBurst() {
		switch (this.burstMode) {
			case SPHERE:
				this.sphereBurst();
				break;
			case CONE:
				this.coneBurst();
				break;
			case FALL:
				this.fallBurst();
				break;
		}
	}

	private void fallBurst() {
		final Location location = this.player.getLocation();
		double x, y, z;
		final double r = 1;

		for (double theta = 75; theta < 105; theta += this.blastAngleTheta) {
			final double dphi = this.blastAnglePhi / Math.sin(Math.toRadians(theta));
			for (double phi = 0; phi < 360; phi += dphi) {
				final double rphi = Math.toRadians(phi);
				final double rtheta = Math.toRadians(theta);

				x = r * Math.cos(rphi) * Math.sin(rtheta);
				y = r * Math.sin(rphi) * Math.sin(rtheta);
				z = r * Math.cos(rtheta);

				final Vector direction = new Vector(x, z, y);
				final AirBlast blast = new AirBlast(this.player, location, direction.normalize(), this.fallPushFactor, this);
				blast.setDamage(this.fallDamage);
				blast.setRange(this.fallRange);
			}
		}
	}

	private void coneBurst() {
		System.out.println("coneBurst");
		final Location location = this.player.getEyeLocation();
		final Vector vector = location.getDirection();
		double x, y, z;
		final double r = 1;

		for (double theta = 0; theta <= 180; theta += this.blastAngleTheta) {
			final double dphi = this.blastAnglePhi / Math.sin(Math.toRadians(theta));
			for (double phi = 0; phi < 360; phi += dphi) {
				final double rphi = Math.toRadians(phi);
				final double rtheta = Math.toRadians(theta);

				x = r * Math.cos(rphi) * Math.sin(rtheta);
				y = r * Math.sin(rphi) * Math.sin(rtheta);
				z = r * Math.cos(rtheta);

				final Vector direction = new Vector(x, z, y);
				if (direction.angle(vector) <= this.coneAngle) {
					final AirBlast blast = new AirBlast(this.player, location, direction.normalize(), this.conePushFactor, this);
					blast.setDamage(this.coneDamage);
					blast.setParticles(this.coneParticles);
					blast.setShowParticles(false);
					blast.setRange(this.coneRange);
					this.blasts.add(blast);
				}
			}
		}
		this.handleSmoothParticles();
	}

	private void sphereBurst() {
		final Location location = this.player.getEyeLocation();
		double x, y, z;
		final double r = 1;

		for (double theta = 0; theta <= 180; theta += this.blastAngleTheta) {
			final double dphi = this.blastAnglePhi / Math.sin(Math.toRadians(theta));

			for (double phi = 0; phi < 360; phi += dphi) {
				final double rphi = Math.toRadians(phi);
				final double rtheta = Math.toRadians(theta);

				x = r * Math.cos(rphi) * Math.sin(rtheta);
				y = r * Math.sin(rphi) * Math.sin(rtheta);
				z = r * Math.cos(rtheta);

				final Vector direction = new Vector(x, z, y);
				final AirBlast blast = new AirBlast(this.player, location, direction.normalize(), this.spherePushFactor, this);

				blast.setDamage(this.sphereDamage);
				blast.setRange(this.sphereRange);
				blast.setShowParticles(false);
				this.blasts.add(blast);
			}
		}
		this.handleSmoothParticles();
	}

	/**
	 * Static method for initiating cone burst
	 * This ensures backward compatibility with existing code
	 */
	public static void coneBurst(final Player player) {
		new AirBurst(player, BurstMode.CONE);
	}

	public void handleSmoothParticles() {
		for (int i = 0; i < this.blasts.size(); i++) {
			final AirBlast blast = this.blasts.get(i);
			int toggleTime = 0;

			if (i % 4 != 0) {
				toggleTime = (int) (i % (100 / this.particlePercentage)) + 3;
			}
			new BukkitRunnable() {
				@Override
				public void run() {
					blast.setShowParticles(true);
				}
			}.runTaskLater(ProjectKorra.plugin, toggleTime);
		}
	}

	@Override
	public String getName() {
		return "AirBurst";
	}

	@Override
	public Location getLocation() {
		return this.player != null ? this.player.getLocation() : null;
	}

	@Override
	public long getCooldown() {
		switch (this.burstMode) {
			case CONE: return this.coneCooldown;
			case FALL: return this.fallCooldown;
			default: return this.sphereCooldown;
		}
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public List<Location> getLocations() {
		final ArrayList<Location> locations = new ArrayList<>();
		for (final AirBlast blast : this.blasts) {
			locations.add(blast.getLocation());
		}
		return locations;
	}

	public BurstMode getBurstMode() {
		return this.burstMode;
	}

	public void setBurstMode(BurstMode mode) {
		this.burstMode = mode;
	}

	public void addAffectedEntity(final Entity entity) {
		this.affectedEntities.add(entity);
	}

	public boolean isAffectedEntity(final Entity entity) {
		return this.affectedEntities.contains(entity);
	}

	public long getChargeTime() {
		return this.chargeTime;
	}

	public void setChargeTime(final long chargeTime) {
		this.chargeTime = chargeTime;
	}

	public long getConeChargeTime() {
		return this.coneChargeTime;
	}

	public void setConeChargeTime(final long chargeTime) {
		this.coneChargeTime = chargeTime;
	}

	public double getFallThreshold() {
		return this.fallThreshold;
	}

	public void setFallThreshold(final double fallThreshold) {
		this.fallThreshold = fallThreshold;
	}

	public long getSphereCooldown() {
		return this.sphereCooldown;
	}

	public void setSphereCooldown(final long cooldown) {
		this.sphereCooldown = cooldown;
	}

	public long getConeCooldown() {
		return this.coneCooldown;
	}

	public void setConeCooldown(final long cooldown) {
		this.coneCooldown = cooldown;
	}

	public long getFallCooldown() {
		return this.fallCooldown;
	}

	public void setFallCooldown(final long cooldown) {
		this.fallCooldown = cooldown;
	}

	public double getSpherePushFactor() {
		return this.spherePushFactor;
	}

	public void setSpherePushFactor(final double pushFactor) {
		this.spherePushFactor = pushFactor;
	}

	public double getConePushFactor() {
		return this.conePushFactor;
	}

	public void setConePushFactor(final double pushFactor) {
		this.conePushFactor = pushFactor;
	}

	public double getFallPushFactor() {
		return this.fallPushFactor;
	}

	public void setFallPushFactor(final double pushFactor) {
		this.fallPushFactor = pushFactor;
	}

	public double getSphereDamage() {
		return this.sphereDamage;
	}

	public void setSphereDamage(final double damage) {
		this.sphereDamage = damage;
	}

	public double getConeDamage() {
		return this.coneDamage;
	}

	public void setConeDamage(final double damage) {
		this.coneDamage = damage;
	}

	public double getFallDamage() {
		return this.fallDamage;
	}

	public void setFallDamage(final double damage) {
		this.fallDamage = damage;
	}

	public boolean getCanFlickLevers() {
		return this.canFlickLevers;
	}

	public void setCanFlickLevers(final boolean canFlickLevers) {
		this.canFlickLevers = canFlickLevers;
	}

	public boolean getCanOpenDoors() {
		return this.canOpenDoors;
	}

	public void setCanOpenDoors(final boolean canOpenDoors) {
		this.canOpenDoors = canOpenDoors;
	}

	public boolean getCanPressButtons() {
		return this.canPressButtons;
	}

	public void setCanPressButtons(final boolean canPressButtons) {
		this.canPressButtons = canPressButtons;
	}

	public boolean getCanExtinguishBlocks() {
		return this.canExtinguishBlocks;
	}

	public void setCanExtinguishBlocks(final boolean canExtinguishBlocks) {
		this.canExtinguishBlocks = canExtinguishBlocks;
	}

	public int getConeParticles() {
		return this.coneParticles;
	}

	public void setConeParticles(final int particles) {
		this.coneParticles = particles;
	}

	public boolean isCharged() {
		return this.isCharged;
	}

	public void setCharged(final boolean isCharged) {
		this.isCharged = isCharged;
	}

	public ArrayList<AirBlast> getBlasts() {
		return this.blasts;
	}
}