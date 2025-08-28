package com.projectkorra.projectkorra.earthbending;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.ParticleEffect;

public class Shockwave extends EarthAbility {

	public static enum ShockwaveMode {
		AREA, CONE, FALL
	}
	private boolean charged;
	private ShockwaveMode shockwaveMode;
	@Attribute(Attribute.CHARGE_DURATION)
	private long chargeTime;
	private long coneChargeTime;
	@Attribute(Attribute.COOLDOWN)
	private long areaCooldown;
	private long coneCooldown;
	private long fallCooldown;
	@Attribute(Attribute.RANGE)
	private double areaRange;
	private double coneRange;
	private double fallRange;
	@Attribute(Attribute.DAMAGE)
	private double areaDamage;
	private double coneDamage;
	private double fallDamage;
	@Attribute(Attribute.KNOCKBACK)
	private double areaKnockback;
	private double coneKnockback;
	private double fallKnockback;
	private double coneAngle;
	private double fallThreshold;
	private boolean separateChargeTimes;

	/**
	 * Constructor for Shockwave with mode parameter
	 *
	 * @param player The player using the ability
	 * @param mode The shockwave mode (AREA, CONE, FALL)
	 */
	public Shockwave(final Player player, final ShockwaveMode mode) {
		super(player);

		this.shockwaveMode = mode;

		if (mode == ShockwaveMode.FALL) {
			if (this.bPlayer.isOnCooldown("ShockwaveFall")) {
				this.remove();
				return;
			}
		} else if (mode == ShockwaveMode.CONE) {
			if (this.bPlayer.isOnCooldown("ShockwaveCone")) {
				this.remove();
				return;
			}
		} else {
			if (this.bPlayer.isOnCooldown("Shockwave")) {
				this.remove();
				return;
			}
		}

		Shockwave existingShockwave = null;
		if (hasAbility(player, Shockwave.class)) {
			existingShockwave = getAbility(player, Shockwave.class);
		}

		if (mode == ShockwaveMode.CONE) {
			if (existingShockwave != null && existingShockwave.isCharged()) {
				this.bPlayer.addCooldown("ShockwaveCone", existingShockwave.coneCooldown);
				existingShockwave.shockwaveMode = ShockwaveMode.CONE;
				existingShockwave.executeShockwave();
				existingShockwave.remove();
				this.remove();
				return;
			}
			this.remove();
			return;
		}

		if (mode != ShockwaveMode.CONE && existingShockwave != null) {
			this.remove();
			return;
		}

		this.charged = mode == ShockwaveMode.CONE || mode == ShockwaveMode.FALL;
		this.setFields();

		if (mode == ShockwaveMode.FALL) {
			if (!this.bPlayer.canBendIgnoreCooldowns(this) ||
					this.player.getFallDistance() < this.fallThreshold ||
					!this.isEarthbendable(this.player.getLocation().clone().subtract(0, 1, 0).getBlock())) {
				this.remove();
				return;
			}

			this.bPlayer.addCooldown("ShockwaveFall", this.fallCooldown);
			this.executeShockwave();
			this.remove();
			return;
		}

		this.start();
	}

	public void setFields() {
		this.chargeTime = getConfig().getLong("Abilities.Earth.Shockwave.ChargeTime");
		this.coneChargeTime = getConfig().getLong("Abilities.Earth.Shockwave.Cone.ChargeTime", this.chargeTime);
		this.areaCooldown = getConfig().getLong("Abilities.Earth.Shockwave.Cooldown");
		this.coneCooldown = getConfig().getLong("Abilities.Earth.Shockwave.Cone.Cooldown", this.areaCooldown);
		this.fallCooldown = getConfig().getLong("Abilities.Earth.Shockwave.Fall.Cooldown", this.areaCooldown);
		this.fallThreshold = getConfig().getDouble("Abilities.Earth.Shockwave.FallThreshold");
		this.areaRange = getConfig().getDouble("Abilities.Earth.Shockwave.Range");
		this.coneRange = getConfig().getDouble("Abilities.Earth.Shockwave.Cone.Range", this.areaRange);
		this.fallRange = getConfig().getDouble("Abilities.Earth.Shockwave.Fall.Range", this.areaRange);
		this.areaDamage = getConfig().getDouble("Abilities.Earth.Shockwave.Damage");
		this.coneDamage = getConfig().getDouble("Abilities.Earth.Shockwave.Cone.Damage", this.areaDamage);
		this.fallDamage = getConfig().getDouble("Abilities.Earth.Shockwave.Fall.Damage", this.areaDamage);
		this.areaKnockback = getConfig().getDouble("Abilities.Earth.Shockwave.Knockback");
		this.coneKnockback = getConfig().getDouble("Abilities.Earth.Shockwave.Cone.Knockback", this.areaKnockback);
		this.fallKnockback = getConfig().getDouble("Abilities.Earth.Shockwave.Fall.Knockback", this.areaKnockback);
		this.coneAngle = Math.toRadians(getConfig().getDouble("Abilities.Earth.Shockwave.Angle"));
		this.separateChargeTimes = getConfig().getBoolean("Abilities.Earth.Shockwave.SeparateChargeTimes", true);
	}

	/**
	 * Legacy constructor for backward compatibility
	 *
	 * @param player The player using the ability
	 * @param fall Whether this is a fall shockwave
	 */
	public Shockwave(final Player player, final boolean fall) {
		this(player, fall ? ShockwaveMode.FALL : ShockwaveMode.AREA);
	}

	@Override
	public void progress() {
		if (!this.bPlayer.canBendIgnoreCooldowns(this)) {
			this.remove();
			return;
		}

		long relevantChargeTime;
		if (this.separateChargeTimes && this.shockwaveMode == ShockwaveMode.CONE) {
			relevantChargeTime = this.coneChargeTime;
		} else {
			relevantChargeTime = this.chargeTime;
		}

		if (System.currentTimeMillis() > this.getStartTime() + relevantChargeTime && !this.charged) {
			this.charged = true;
		}

		if (!this.player.isSneaking()) {
			if (this.charged) {
				this.bPlayer.addCooldown("Shockwave", this.areaCooldown);
				this.executeShockwave();
				this.remove();
				return;
			} else {
				this.remove();
				return;
			}
		} else if (this.charged) {
			final Location location = this.player.getEyeLocation().add(this.player.getEyeLocation().getDirection());
			ParticleEffect.SMOKE_NORMAL.display(location, 1);
		}
	}

	/**
	 * Executes the appropriate shockwave based on the current mode
	 */
	private void executeShockwave() {
		switch (this.shockwaveMode) {
			case AREA:
				this.areaShockwave();
				break;
			case CONE:
				this.coneShockwave();
				break;
			case FALL:
				this.fallShockwave();
				break;
		}
	}

	public static void progressAll() {
		Ripple.progressAllCleanup();
	}

	private void areaShockwave() {
		final double dtheta = 360.0 / (2 * Math.PI * this.areaRange) - 1;

		for (double theta = 0; theta < 360; theta += dtheta) {
			final double rtheta = Math.toRadians(theta);
			final Vector vector = new Vector(Math.cos(rtheta), 0, Math.sin(rtheta));
			new Ripple(this.player, vector.normalize(), ShockwaveMode.AREA,
					this.areaRange, this.areaDamage, this.areaKnockback);
		}
	}

	private void coneShockwave() {
		final double dtheta = 360.0 / (2 * Math.PI * this.coneRange) - 1;

		for (double theta = 0; theta < 360; theta += dtheta) {
			final double rtheta = Math.toRadians(theta);
			final Vector vector = new Vector(Math.cos(rtheta), 0, Math.sin(rtheta));
			if (vector.angle(player.getEyeLocation().getDirection()) < this.coneAngle) {
				new Ripple(player, vector.normalize(), ShockwaveMode.CONE,
						this.coneRange, this.coneDamage, this.coneKnockback);
			}
		}
	}

	private void fallShockwave() {
		final double dtheta = 360.0 / (2 * Math.PI * this.fallRange) - 1;

		for (double theta = 0; theta < 360; theta += dtheta) {
			final double rtheta = Math.toRadians(theta);
			final Vector vector = new Vector(Math.cos(rtheta), 0, Math.sin(rtheta));
			new Ripple(this.player, vector.normalize(), ShockwaveMode.FALL,
					this.fallRange, this.fallDamage, this.fallKnockback);
		}
	}

	/**
	 * Static method for initiating cone shockwave
	 * This ensures backward compatibility with existing code
	 */
	public static void coneShockwave(final Player player) {
		new Shockwave(player, ShockwaveMode.CONE);
	}

	@Override
	public String getName() {
		return "Shockwave";
	}

	@Override
	public Location getLocation() {
		return this.player != null ? this.player.getLocation() : null;
	}

	@Override
	public long getCooldown() {
		switch (this.shockwaveMode) {
			case CONE: return this.coneCooldown;
			case FALL: return this.fallCooldown;
			default: return this.areaCooldown;
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

	public boolean isCharged() {
		return this.charged;
	}

	public void setCharged(final boolean charged) {
		this.charged = charged;
	}

	public ShockwaveMode getShockwaveMode() {
		return this.shockwaveMode;
	}

	public void setShockwaveMode(ShockwaveMode mode) {
		this.shockwaveMode = mode;
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

	public long getAreaCooldown() {
		return this.areaCooldown;
	}

	public void setAreaCooldown(final long cooldown) {
		this.areaCooldown = cooldown;
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

	public double getAreaRange() {
		return this.areaRange;
	}

	public void setAreaRange(final double range) {
		this.areaRange = range;
	}

	public double getConeRange() {
		return this.coneRange;
	}

	public void setConeRange(final double range) {
		this.coneRange = range;
	}

	public double getFallRange() {
		return this.fallRange;
	}

	public void setFallRange(final double range) {
		this.fallRange = range;
	}

	public double getAreaDamage() {
		return this.areaDamage;
	}

	public void setAreaDamage(final double damage) {
		this.areaDamage = damage;
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

	public double getAreaKnockback() {
		return this.areaKnockback;
	}

	public void setAreaKnockback(final double knockback) {
		this.areaKnockback = knockback;
	}

	public double getConeKnockback() {
		return this.coneKnockback;
	}

	public void setConeKnockback(final double knockback) {
		this.coneKnockback = knockback;
	}

	public double getFallKnockback() {
		return this.fallKnockback;
	}

	public void setFallKnockback(final double knockback) {
		this.fallKnockback = knockback;
	}

	public double getConeAngle() {
		return this.coneAngle;
	}

	public void setConeAngle(final double angle) {
		this.coneAngle = angle;
	}

	public double getFallThreshold() {
		return this.fallThreshold;
	}

	public void setFallThreshold(final double threshold) {
		this.fallThreshold = threshold;
	}
}