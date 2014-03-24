package net.amoebaman.wireguidedarrows;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class WireGuidedArrows extends JavaPlugin implements Listener{
	
	private BukkitTask velocityUpdateTask;
	
	public void onEnable(){
		velocityUpdateTask = Bukkit.getScheduler().runTaskTimer(this, new VelocityUpdateTask(this), 0L, 1L);
		Bukkit.getPluginManager().registerEvents(this, this);
	}
	
	public void onDisable(){
		velocityUpdateTask.cancel();
	}
	
}
