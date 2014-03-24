package net.amoebaman.wireguidedarrows;

import java.util.*;

import org.bukkit.*;
import org.bukkit.Note.Tone;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

@SuppressWarnings("deprecation")
public class VelocityUpdateTask implements Runnable{
	
	/*
	 * This is a set of all the transparent blocks
	 * We fill this based on the properties of enumerated materials
	 */
	private final static HashSet<Byte> trans = new HashSet<Byte>();
	static{
		for(Material mat : Material.values())
			if(mat.isBlock() && (!mat.isSolid() || mat.isTransparent()) && mat.getId() < Byte.MAX_VALUE)
				trans.add((byte) mat.getId());
		
	}
	
	/*
	 * We use this in conjunction with modulus division to do things
	 * only every certain number of ticks
	 */
	private long tick = 0;
	
	private WireGuidedArrows plugin;
	
	protected VelocityUpdateTask(WireGuidedArrows plugin){
		this.plugin = plugin;
	}
	
	public void run(){
		/*
		 * Increment the ticker
		 */
		tick++;
		/*
		 * Set up a map of targeted locations
		 * This can save LOTS of time if players have multiple airborne arrows
		 */
		Map<Player, Location> targets = new HashMap<Player, Location>();
		/*
		 * For each and every world...
		 */
		for(World world : Bukkit.getWorlds())
			/*
			 * For every arrow in the world...
			 */
			for(Arrow arrow : world.getEntitiesByClass(Arrow.class))
				/*
				 * If it's still in flight and hasn't expired and was shot by a player...
				 */
				if(!arrow.isOnGround() && arrow.getTicksLived() < 100 && arrow.getShooter() instanceof Player){
					
					Player shooter = (Player) arrow.getShooter();
					Location target = targets.get(shooter);
					/*
					 * If there's not a stored target for the shooter, calculate it now...
					 */
					if(!targets.containsKey(shooter)){
						/*
						 * Start by grabbing the nearest targeted block
						 */
						target = shooter.getTargetBlock(trans, 100).getLocation().add(0.5, 0.5, 0.5);
						/*
						 * If it's air, set the target to null
						 */
						if(target.getBlock().getType() == Material.AIR)
							target = null;
						/*
						 * Loop through the player's line of sight (LoS)...
						 */
						losLoop: for(Block los : shooter.getLineOfSight(trans, 100))
							/*
							 * For every block, check the entities that might be near it...
							 */
							for(Entity e : los.getChunk().getEntities())
								/*
								 * Only check living entities, and don't grab the shooter...
								 */
								if(e instanceof LivingEntity && !e.equals(shooter))
									/*
									 * Make sure they're close enough to our line of sight...
									 */
									if(e.getLocation().distance(los.getLocation()) < 2){
										/*
										 * Make their location the target
										 */
										target = e.getLocation();
										/*
										 * Play some fancy missile-lock sounds
										 */
										if(tick % 3 == 0){
											shooter.playNote(shooter.getLocation(), Instrument.PIANO, Note.natural(1, Tone.G));
											if(e instanceof Player)
												((Player) e).playNote(e.getLocation(), Instrument.PIANO, Note.natural(1, Tone.G));
										}
										/*
										 * We've got a target, so no more need to check the reset of the LoS
										 */
										break losLoop;
										
									}
						Vector newVel = arrow.getVelocity().clone();
						/*
						 * If we've got a valid target...
						 */
						if(target != null){
							/*
							 * Send the arrow to it
							 */
							Vector diff = target.toVector().subtract(arrow.getLocation().toVector());
							newVel = arrow.getVelocity().clone();
							newVel.add(diff).normalize().multiply(2);
						}
						/*
						 * Otherwise...
						 */
						else{
							/*
							 * Continue the arrow's current trajectory
							 */
							newVel = arrow.hasMetadata("last-vel")
										? (Vector) arrow.getMetadata("last-vel").get(0).value()
										: arrow.getVelocity().clone().normalize().multiply(2);
						}
						/*
						 * Update the arrows velocity and last-vel metadata
						 */
						arrow.setVelocity(newVel);
						arrow.setMetadata("last-vel", new FixedMetadataValue(plugin, newVel));
					}
					
				}
	}
}
