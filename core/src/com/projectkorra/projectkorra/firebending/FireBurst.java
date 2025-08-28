package com.projectkorra.projectkorra.firebending;

import java.util.ArrayList;
import java.util.List;

import com.projectkorra.projectkorra.attribute.markers.DayNightFactor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.Element.SubElement;
import com.projectkorra.projectkorra.ability.BlueFireAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.attribute.Attribute;

public class FireBurst extends FireAbility {

	public enum BurstMode {
		SPHERE, CONE
	}

	@Attribute("Charged")
	private boolean charged;
	private BurstMode burstMode;

	@Attribute(Attribute.DAMAGE) @DayNightFactor
	private double sphereDamage;
	private double coneDamage;

	@Attribute(Attribute.CHARGE_DURATION) @DayNightFactor(invert = true)
	private long chargeTime;
	private long coneChargeTime;

	@Attribute(Attribute.RANGE) @DayNightFactor
	private double sphereRange;
	private double coneRange;

	@Attribute(Attribute.COOLDOWN) @DayNightFactor(invert = true)
	private long sphereCooldown;
	private long coneCooldown;

	private double angleTheta;
	private double anglePhi;
	private double coneAngle;
	private int coneParticles;
	private int sphereParticles;
	private double particlesPercentage;
	private boolean separateChargeTimes;
	private ArrayList<FireBlast> blasts;

	/**
	 * Constructor for FireBurst with mode parameter
	 *
	 * @param player The player using the ability
	 * @param mode The burst mode (SPHERE, CONE)
	 */
	public FireBurst(final Player player, final BurstMode mode) {
		super(player);

		this.burstMode = mode;

		if (mode == BurstMode.CONE) {
			if (this.bPlayer.isOnCooldown("FireBurstCone")) {
				this.remove();
				return;
			}
		} else {
			if (this.bPlayer.isOnCooldown("FireBurst")) {
				this.remove();
				return;
			}
		}

		FireBurst existingBurst = null;
		if (hasAbility(player, FireBurst.class)) {
			existingBurst = getAbility(player, FireBurst.class);
		}

		if (mode == BurstMode.CONE) {
			if (existingBurst != null && existingBurst.charged) {
				this.bPlayer.addCooldown("FireBurstCone", existingBurst.coneCooldown);
				existingBurst.burstMode = BurstMode.CONE;
				existingBurst.executeBurst();
				existingBurst.remove();
				this.remove();
				return;
			}
			this.remove();
			return;
		}

		if (mode != BurstMode.CONE && existingBurst != null) {
			this.remove();
			return;
		}

		this.charged = mode == BurstMode.CONE;
		this.sphereDamage = getConfig().getDouble("Abilities.Fire.FireBurst.Damage");
		this.coneDamage = getConfig().getDouble("Abilities.Fire.FireBurst.Cone.Damage", this.sphereDamage);
		this.chargeTime = getConfig().getLong("Abilities.Fire.FireBurst.ChargeTime");
		this.coneChargeTime = getConfig().getLong("Abilities.Fire.FireBurst.Cone.ChargeTime", this.chargeTime);
		this.sphereRange = getConfig().getDouble("Abilities.Fire.FireBurst.Range");
		this.coneRange = getConfig().getDouble("Abilities.Fire.FireBurst.Cone.Range", this.sphereRange);
		this.sphereCooldown = getConfig().getLong("Abilities.Fire.FireBurst.Cooldown");
		this.coneCooldown = getConfig().getLong("Abilities.Fire.FireBurst.Cone.Cooldown", this.sphereCooldown);
		this.angleTheta = getConfig().getDouble("Abilities.Fire.FireBurst.AngleTheta");
		this.anglePhi = getConfig().getDouble("Abilities.Fire.FireBurst.AnglePhi");
		this.coneAngle = Math.toRadians(getConfig().getDouble("Abilities.Fire.FireBurst.Cone.Angle", 30));
		this.coneParticles = getConfig().getInt("Abilities.Fire.FireBurst.Cone.ParticleCount", 1);
		this.sphereParticles = getConfig().getInt("Abilities.Fire.FireBurst.ParticleCount", 1);
		this.particlesPercentage = getConfig().getDouble("Abilities.Fire.FireBurst.ParticlesPercentage");
		this.separateChargeTimes = getConfig().getBoolean("Abilities.Fire.FireBurst.SeparateChargeTimes", true);
		this.blasts = new ArrayList<>();

		if (mode == BurstMode.CONE) {
			this.bPlayer.addCooldown("FireBurstCone", this.coneCooldown);
			this.executeBurst();
			this.remove();
			return;
		}

		this.start();
	}

	/**
	 * Legacy constructor for backward compatibility
	 *
	 * @param player The player using the ability
	 */
	public FireBurst(final Player player) {
		this(player, BurstMode.SPHERE);
	}

	public static void coneBurst(final Player player) {
		new FireBurst(player, BurstMode.CONE);
	}

	@Override
	public void progress() {
		if (!this.bPlayer.canBendIgnoreCooldowns(this)) {
			this.remove();
			return;
		}

		long relevantChargeTime;
		if (this.separateChargeTimes && this.burstMode == BurstMode.CONE) {
			relevantChargeTime = this.coneChargeTime;
		} else {
			relevantChargeTime = this.chargeTime;
		}

		if (System.currentTimeMillis() > this.getStartTime() + relevantChargeTime && !this.charged) {
			this.charged = true;
		}

		if (!this.player.isSneaking()) {
			if (this.charged) {
				this.bPlayer.addCooldown("FireBurst", this.sphereCooldown);
				this.executeBurst();
				this.remove();
			} else {
				this.remove();
			}
		} else if (this.charged) {
			final Location location = this.player.getEyeLocation();
			location.add(location.getDirection());
			playFirebendingParticles(location, 1, .01, .01, .01);
			emitFirebendingLight(location);
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
		}
	}

	private void coneBurst() {
		final Location location = this.player.getEyeLocation();
		final List<Block> safeBlocks = GeneralMethods.getBlocksAroundPoint(this.player.getLocation(), 2);
		final Vector vector = location.getDirection();
		double x, y, z;
		final double r = 1;

		for (double theta = 0; theta <= 180; theta += this.angleTheta) {
			final double dphi = this.anglePhi / Math.sin(Math.toRadians(theta));
			for (double phi = 0; phi < 360; phi += dphi) {
				final double rphi = Math.toRadians(phi);
				final double rtheta = Math.toRadians(theta);

				x = r * Math.cos(rphi) * Math.sin(rtheta);
				y = r * Math.sin(rphi) * Math.sin(rtheta);
				z = r * Math.cos(rtheta);
				final Vector direction = new Vector(x, z, y);

				if (direction.angle(vector) <= this.coneAngle) {
					final FireBlast fblast = new FireBlast(location, direction.normalize(), this.player, this.coneDamage, safeBlocks);
					fblast.setRange(this.coneRange);
					fblast.setFireBurst(true);
					this.blasts.add(fblast);
				}
			}
		}
		this.handleSmoothParticles();
	}

	private void sphereBurst() {
		final Location location = this.player.getEyeLocation();
		final List<Block> safeblocks = GeneralMethods.getBlocksAroundPoint(this.player.getLocation(), 2);
		double x, y, z;
		final double r = 1;

		for (double theta = 0; theta <= 180; theta += this.angleTheta) {
			final double dphi = this.anglePhi / Math.sin(Math.toRadians(theta));
			for (double phi = 0; phi < 360; phi += dphi) {
				final double rphi = Math.toRadians(phi);
				final double rtheta = Math.toRadians(theta);

				x = r * Math.cos(rphi) * Math.sin(rtheta);
				y = r * Math.sin(rphi) * Math.sin(rtheta);
				z = r * Math.cos(rtheta);

				final Vector direction = new Vector(x, z, y);
				final FireBlast fblast = new FireBlast(location, direction.normalize(), this.player, this.sphereDamage, safeblocks);

				fblast.setRange(this.sphereRange);
				fblast.setShowParticles(false);
				fblast.setFireBurst(true);
				this.blasts.add(fblast);
			}
		}
		this.handleSmoothParticles();
	}

	public void handleSmoothParticles() {
		for (int i = 0; i < this.blasts.size(); i++) {
			final FireBlast fblast = this.blasts.get(i);
			final int toggleTime = (int) (i % (100.0 / this.particlesPercentage));
			new BukkitRunnable() {
				@Override
				public void run() {
					fblast.setShowParticles(true);
				}
			}.runTaskLater(ProjectKorra.plugin, toggleTime);
		}
	}

	@Override
	public String getName() {
		return "FireBurst";
	}

	@Override
	public Location getLocation() {
		return this.player != null ? this.player.getLocation() : null;
	}

	@Override
	public long getCooldown() {
		return this.burstMode == BurstMode.CONE ? this.coneCooldown : this.sphereCooldown;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	public BurstMode getBurstMode() {
		return this.burstMode;
	}

	public void setBurstMode(BurstMode mode) {
		this.burstMode = mode;
	}

	public boolean isCharged() {
		return this.charged;
	}

	public void setCharged(final boolean charged) {
		this.charged = charged;
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

	public double getSphereRange() {
		return this.sphereRange;
	}

	public void setSphereRange(final double range) {
		this.sphereRange = range;
	}

	public double getConeRange() {
		return this.coneRange;
	}

	public void setConeRange(final double range) {
		this.coneRange = range;
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

	public double getAngleTheta() {
		return this.angleTheta;
	}

	public void setAngleTheta(final double angleTheta) {
		this.angleTheta = angleTheta;
	}

	public double getAnglePhi() {
		return this.anglePhi;
	}

	public void setAnglePhi(final double anglePhi) {
		this.anglePhi = anglePhi;
	}

	public double getConeAngle() {
		return this.coneAngle;
	}

	public void setConeAngle(final double angle) {
		this.coneAngle = angle;
	}

	public int getConeParticles() {
		return this.coneParticles;
	}

	public void setConeParticles(final int particles) {
		this.coneParticles = particles;
	}

	public int getSphereParticles() {
		return this.sphereParticles;
	}

	public void setSphereParticles(final int particles) {
		this.sphereParticles = particles;
	}

	public double getParticlesPercentage() {
		return this.particlesPercentage;
	}

	public void setParticlesPercentage(final double particlesPercentage) {
		this.particlesPercentage = particlesPercentage;
	}

	public ArrayList<FireBlast> getBlasts() {
		return this.blasts;
	}
}