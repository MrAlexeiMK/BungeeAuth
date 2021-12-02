package argento.bungeeauth;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import argento.bungeeauth.actionbar.ActionBar;
import argento.bungeeauth.actionbar.ActionBar_1_12_R1;
import argento.bungeeauth.actionbar.ActionBar_1_16_R1;
import argento.bungeeauth.actionbar.ActionBar_1_16_R2;
import argento.bungeeauth.actionbar.ActionBar_1_16_R3;

public class Main extends JavaPlugin implements Listener {
	private static String url = "jdbc:mysql://localhost:3306/database?useSSL=false&autoReConnect=true";
	private static String user = "root";
    private static String password = "";
    private static String table = "auth";
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	File file = null;
	FileConfiguration config = null;
    private static Connection con;
    private static Statement stmt;
    private static ResultSet rs;
    public ArrayList<Player> reg = new ArrayList<Player>();
    public ArrayList<Player> login = new ArrayList<Player>();
    public HashMap<Player, Long> time = new HashMap<Player, Long>();
    public ArrayList<String> lobbies = new ArrayList<String>();
    public ArrayList<String> blocked_passwords = new ArrayList<String>();
    int login_register_time = 60;
    boolean auth_map = false;
    private ActionBar actionbar;
    String ch1, ch2;
    
    String getVersion() {
        String[] array = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",");
        if (array.length == 4) return array[3];
        return "";
    }
    
    public void setupActionBar() {
    	if(getVersion().equals("v1_12_R1") || getVersion().equals("v1_12_R2") || getVersion().equals("v1_12_R3")) {
    		actionbar = new ActionBar_1_12_R1();
    	}
    	else if(getVersion().equals("v1_16_R1")) {
    		actionbar = new ActionBar_1_16_R1();
    	}
    	else if(getVersion().equals("v1_16_R2")) {
    		actionbar = new ActionBar_1_16_R2();
    	}
    	else {
    		actionbar = new ActionBar_1_16_R3();
    	}
    }
    
    public void toLobby(Player p ) {
	    for(int i = 0; i < 100; ++i) {
			p.sendMessage(" ");
		}
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
	    out.writeUTF("Connect");
	    int ran = new Random().nextInt(lobbies.size());
	    out.writeUTF(lobbies.get(ran));
	    p.sendPluginMessage(this, "BungeeCord", out.toByteArray());
	}
    
    @EventHandler
	public void command(PlayerCommandPreprocessEvent e) {
		String com = e.getMessage().substring(1).toLowerCase().split(" ")[0];
		if(!com.equals("register") && !com.equals("reg") && !com.equals("login") && !com.equals("l") && !com.equals("changepassword") && !com.equals("log")) {
			e.setCancelled(true);
		}
    }
    
    public void createTable() {
    	String query = "CREATE TABLE "+table+"(name VARCHAR(70) NOT NULL UNIQUE, password VARCHAR(256), ip VARCHAR(60));";
		try {
			stmt.executeUpdate(query);
		} catch(SQLException e) {};
    }
	
	public void onEnable() {
		try {
			Class.forName(JDBC_DRIVER);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		
		file = new File(getDataFolder(), "config.yml");
		if(!file.exists()) {
			getConfig().options().copyDefaults(true);
			saveDefaultConfig();
		}
		reloadConfig();
		
		url = getConfig().getString("mysql.url");
		user = getConfig().getString("mysql.user");
		password = getConfig().getString("mysql.password");
		ch1 = getConfig().getString("language.changepassword").replaceAll("&", "§");
		ch2 = getConfig().getString("language.changepassword_click").replaceAll("&", "§");
		lobbies = (ArrayList<String>) getConfig().get("general.lobbies");
		login_register_time = getConfig().getInt("general.login_and_register_time_seconds");
		auth_map = getConfig().getBoolean("general.auth_map_exist");
		blocked_passwords = (ArrayList<String>) getConfig().get("general.blocked_passwords");
		table = getConfig().getString("mysql.table");
		
		setupActionBar();
		
		connect();
		
		try {
			createTable();
		} catch(Exception ee) {};
		
		this.getServer().getPluginManager().registerEvents(this, this);
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				try {
					for(Player p : login) {
						send(p, getConfig().getString("language.login"));
						if((long) System.currentTimeMillis()/1000 - time.get(p) >= login_register_time) {
							login.remove(p);
							p.kickPlayer(getConfig().getString("language.time_up").replaceAll("&", "§"));
						}
					}
					for(Player p : reg) {
						send(p, getConfig().getString("language.reg"));
						if((long) System.currentTimeMillis()/1000 - time.get(p) >= login_register_time) {
							reg.remove(p);
							p.kickPlayer(getConfig().getString("language.time_up").replaceAll("&", "§"));
						}
					}
				} catch (Exception ee) {};
			}
		}, 0, 20);

		
		Bukkit.getLogger().info(getVersion());
	}
	
	public void send(Player p, String msg) {
		p.sendMessage(msg.replaceAll("&", "§"));
	}
	
	boolean inDatabase(Player p) {
		String name = p.getName();
		String query = "SELECT COUNT(*) FROM "+table+" WHERE name = '"+name+"'";
		try {
			rs = stmt.executeQuery(query);
			rs.next();
			if(rs.getInt(1) > 0) return true;
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	int getCountAccounts(Player p) {
		int count = 0;
		String query = "SELECT COUNT(*) FROM "+table+" WHERE ip = '"+p.getAddress().getAddress()+"'";
		try {
			rs = stmt.executeQuery(query);
			rs.next();
			count = rs.getInt(1);
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		return count;
	}
	
	String getPassword(Player p) {
		String name = p.getName();
		String pass = "";
		String query = "SELECT password FROM "+table+" WHERE name = '"+name+"'";
		try {
			rs = stmt.executeQuery(query);
			rs.next();
			pass = rs.getString(1);
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		return pass;
	}
	
	String toHash(String password) {
		String hash = "";
		try {
			MessageDigest md = MessageDigest.getInstance(getConfig().getString("general.hash_passwords_alg"));
			md.update(password.getBytes(StandardCharsets.UTF_8));
			byte[] digest = md.digest();
			hash = String.format("%064x", new BigInteger(1, digest));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return hash;
	}
	
	public void addToDB(Player p, String password) {
		String query = "INSERT INTO "+table+" (name, password, ip) VALUES ('"+p.getName()+"', '"+toHash(password)+"', '"+p.getAddress().getAddress().toString()+"')";
		try {
			stmt.executeUpdate(query);
		} catch(SQLException e) {};
	}
	
	public void remFromDB(Player p) {
		String query = "DELETE FROM "+table+" WHERE name = '"+p.getName()+"'";
		try {
			stmt.executeUpdate(query);
		} catch(SQLException e) {};
	}
	
	public void connect() {
		getLogger().info("Connecting");
		try {
			con = DriverManager.getConnection(url, user, password);;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			stmt = con.createStatement();
		} catch (SQLException e) {}
	}
	
	@EventHandler
	public void move(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		if(!auth_map) {
			if(e.getTo().getBlockX() >= 50 || e.getTo().getBlockZ() >= 50 || e.getTo().getBlockY() <= -100) {
				p.teleport(new Location(p.getWorld(), 0, 0, 0));
			}
		}
	}
	
	@EventHandler
	public void join(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		if(!auth_map) p.teleport(new Location(p.getWorld(), 0, 0, 0));
		for(Player pl : Bukkit.getOnlinePlayers()) {
			if(!p.getName().equals(pl.getName())) {
				p.hidePlayer(pl);
				pl.hidePlayer(p);
			}
		}
		
		time.put(p, (long) System.currentTimeMillis()/1000);
		if(!inDatabase(p)) {
			if(p.getName().length() >= getConfig().getInt("general.max_nick_length")) {
				p.kickPlayer( getConfig().getString("language.long_nickname").replaceAll("&", "§"));
			}
			else if(p.getName().length() <= getConfig().getInt("general.min_nick_length")) {
				p.kickPlayer( getConfig().getString("language.short_nickname").replaceAll("&", "§"));
			}
			else if(p.getName().contains(" ")) {
				p.kickPlayer( getConfig().getString("language.space_not_allowed").replaceAll("&", "§"));
			}
			else if(p.getName().contains("!") || p.getName().contains("@")  || p.getName().contains("#") || p.getName().contains("\"") || p.getName().contains("'") || p.getName().contains("¹") || p.getName().contains("$") || p.getName().contains(";") || p.getName().contains(":") || p.getName().contains("%") || p.getName().contains("^") || p.getName().contains("&") || p.getName().contains("?") || p.getName().contains("/") || p.getName().contains("*") || p.getName().contains("(") || p.getName().contains(")") || p.getName().contains("[") || p.getName().contains("]") || p.getName().contains("{") || p.getName().contains("}") || p.getName().contains("|") || p.getName().contains("`") || p.getName().contains("=") || p.getName().contains("+")) {
				p.kickPlayer( getConfig().getString("language.symbol_not_allowed").replaceAll("&", "§"));
			}
			else reg.add(p);
		}
		else {
			login.add(p);
		}
		e.setJoinMessage(null);
	}
	
	@EventHandler
	public void quit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		if(time.containsKey(p)) time.remove(p);
		if(login.contains(p)) login.remove(p);
		if(reg.contains(p)) reg.remove(p);
		e.setQuitMessage(null);
	}
	
	@EventHandler
	public void damage(EntityDamageEvent e) {
		e.setCancelled(true);
	}
	
	@EventHandler
	public void chat(AsyncPlayerChatEvent e) {
		e.setCancelled(true);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(sender instanceof Player) {
			Player p = (Player) sender;
			if(cmd.getName().toLowerCase().equals("register")) {
				if(reg.contains(p)) {
					if(args.length == 0) {
						send(p, getConfig().getString("language.reg_correct"));
					}
					else {
						String pass = args[0];
						if(pass.length() <= getConfig().getInt("general.min_password_length")) {
							send(p, getConfig().getString("language.short_password"));
						}
						else if(blocked_passwords.contains(pass.toLowerCase())) {
							send(p, getConfig().getString("language.blocked_password"));
						}
						else if(getConfig().getBoolean("general.enable_accounts_per_ip_limiter") && getCountAccounts(p) > getConfig().getInt("general.max_accounts_per_ip")) {
							send(p, getConfig().getString("language.limit_per_ip"));
						}
						else {
							addToDB(p, pass);
							reg.remove(p);
							send(p, getConfig().getString("language.successful_reg"));
							actionbar.sendActionbar(p, ch1, ch2);
							
							Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
								public void run() {
									if(!reg.contains(p)) {
										toLobby(p);
									}
								}
							}, 20*getConfig().getInt("general.delay_before_connect_seconds"));
						}
					}
				}
				else {
					send(p, getConfig().getString("language.registered_already"));
				}
			}
			else if(cmd.getName().toLowerCase().equals("login")) {
				if(login.contains(p)) {
					if(args.length == 0) {
						send(p, getConfig().getString("language.login_correct"));
					}
					else {
						String pass = args[0];
						if(toHash(pass).equals(getPassword(p))) {
							login.remove(p);
							send(p, getConfig().getString("language.successful_login"));
							actionbar.sendActionbar(p, ch1, ch2);
							
							Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
								public void run() {
									if(!reg.contains(p)) {
										toLobby(p);
									}
								}
							}, 20*getConfig().getInt("general.delay_before_connect_seconds"));
						}
						else {
							send(p, getConfig().getString("language.incorrect_password"));
							if(getConfig().getBoolean("general.incorrect_password_kick")) p.kickPlayer( getConfig().getString("language.incorrect_password").replaceAll("&", "§"));
						}
					}
				}
				else {
					send(p, getConfig().getString("language.reg_correct"));
				}
			}
			else if(cmd.getName().toLowerCase().equals("changepassword")) {
				if(!reg.contains(p) && !login.contains(p)) {
					remFromDB(p);
					reg.add(p);
					time.replace(p, (long) System.currentTimeMillis()/1000);
				}
			}
		}
		return false;
	}
}
