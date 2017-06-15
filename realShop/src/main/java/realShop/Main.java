package realShop;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

public class Main extends JavaPlugin implements Listener {
	public HashMap<Player, Object[]> selectedShop = new HashMap<Player, Object[]>();
	public HashMap<Player, Object[]> createShop = new HashMap<Player, Object[]>();
	public Vector<Player> removeShop = new Vector<Player>();
	private static Economy econ = null;

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	public Double getPrice(String name, Integer trade) {
		if (!this.getConfig().getList("name").contains(name))
			return null;
		Integer index = this.getConfig().getList("name").indexOf(name);
		Double worth = (Double) this.getConfig().getList("worth").get(index);
		Integer amount = (Integer) this.getConfig().getList("amount").get(index);
		if (trade > 0) {
			if (trade > amount)
				return null;
			Double price = 0.0;
			for (int i = 0; i < trade; i++) {
				if (amount == 1)
					price += worth;
				else
					price += (worth / (double) amount + worth / ((double) amount - 1)) / 2;
				amount--;
			}
			return price;
		} else if (trade < 0) {
			Double price = 0.0;
			for (int i = 0; i < -trade; i++) {
				if (amount == 0)
					price += worth;
				else
					price += (worth / (double) amount + worth / ((double) amount + 1)) / 2;
				amount++;
			}
			return price;
		} else
			return 0.0;
	}

	public void loadConfiguration() {
		this.getConfig().options().copyDefaults(true);
		// Field[] items = Material.class.getDeclaredFields();
		this.getConfig().addDefault("server_language",
				Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry());
		/*
		 * for (int i = 0; i < items.length - 8; i++) { Material material =
		 * Material.matchMaterial(items[i].getName());
		 * this.getConfig().addDefault(material.toString() + ".worth", 100000);
		 * this.getConfig().addDefault(material.toString() + ".amount", 1); }
		 */

		this.saveConfig();
	}

	private Method getMethod(String name, Class<?> clazz) {
		for (Method m : clazz.getDeclaredMethods()) {
			if (m.getName().equals(name))
				return m;
		}
		return null;
	}
	
	public String getVersion(Server server) {
	    final String packageName = server.getClass().getPackage().getName();
	    return packageName.substring(packageName.lastIndexOf('.') + 1);
	}
	
	public void updateSign(){
		List<org.bukkit.util.Vector> vectors = (List<org.bukkit.util.Vector>) this.getConfig().getList("vector");
		List<String> worlds = (List<String>) this.getConfig().getList("world");
		if (vectors == null || worlds == null) {
			vectors = new ArrayList<org.bukkit.util.Vector>();
			worlds = new ArrayList<String>();
			this.getConfig().set("vector", vectors);
			this.getConfig().set("world", worlds);
		}
		for (int i = 0; i < vectors.size(); i++) {
			org.bukkit.util.Vector v = vectors.get(i);
			String worldname = worlds.get(i);
			World w = this.getServer().getWorld(worldname);
			if (w == null) continue;
			Block block = w.getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ());
			if (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST) {
				Sign sign = (Sign) block.getState();
				Boolean buy = true;
				Boolean sell = true;
				String name = sign.getLine(1);
				if (this.getConfig().getList("name") == null || !this.getConfig().getList("name").contains(name)) {
					continue;
				}
				if (sign.getLine(2).split("B: ")[1].equals("X"))
					buy = false;
				if (sign.getLine(3).split("S: ")[1].equals("X"))
					sell = false;
				Double buyprice = this.getPrice(name, 1);
				Double sellprice = this.getPrice(name, -1);
				String buystring, sellstring;
				if (buyprice == null)
					buystring = "No item";
				else
					buystring = econ.format(buyprice.intValue());
				if (sellprice == null)
					sellstring = "No item";
				else
					sellstring = econ.format(sellprice.intValue());
				if (buy && !sell) {
					sign.setLine(2, ChatColor.WHITE + "B: " + buystring);
					sign.setLine(3, "S: X");
				} else if (!buy && sell) {
					sign.setLine(2, "B: X");
					sign.setLine(3, ChatColor.WHITE + "S: " + sellstring);
				} else if (!buy && !sell) {
					sign.setLine(2, "B: X");
					sign.setLine(3, "S: X");
				} else {
					sign.setLine(2, ChatColor.WHITE + "B: " + buystring);
					sign.setLine(3, ChatColor.WHITE + "S: " + sellstring);
				}
				sign.update();
			}
		}
	}

	public String getLanguage(Player p) {
		String language = "en_US";
		try {
			Object ep = getMethod("getHandle", p.getClass()).invoke(p, (Object[]) null);
			Field f = ep.getClass().getDeclaredField("locale");
			f.setAccessible(true);
			language = (String) f.get(ep);
		} catch (Exception e) {

		}
		return language;
	}

	@Override
	public void onDisable() {

	}

	@Override
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(this, this);
		this.loadConfiguration();
		this.setupEconomy();
		this.updateSign();
		
	}

	@Override
	public void onLoad() {
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Integer start = null;
		Vector<String> newargs = new Vector<String>();
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("`"))
				start = i;
			else if (args[i].endsWith("`") && start != null) {
				String join = "";
				for (int j = start; j <= i; j++) {
					if (j == start) {
						join += args[j].substring(1, args[j].length());
					} else if (j == i) {
						join += args[j].substring(0, args[j].length() - 1);
					} else {
						join += args[j];
					}
					if (j != i)
						join += " ";
				}
				newargs.add(join);
				start = null;
				continue;
			}
			if (start == null)
				newargs.add(args[i]);
		}
		args = newargs.toArray(new String[newargs.size()]);
		String cmd = command.getName();
		Player p = null;
		if (sender instanceof Player) {
			p = (Player) sender;
		}
		if (cmd.equals("rs")) {
			if (args.length == 0) {
				String language = this.getConfig().getString("server_language");
				if (p != null)
					language = this.getLanguage(p);
				if (language.equals("ko_KR")) {
					sender.sendMessage(ChatColor.WHITE + "=====[ " + ChatColor.AQUA + "realShop 도움말" + ChatColor.WHITE
							+ " ]=====");
					int count = 0;
					if (sender.hasPermission("realshop.commands.buy"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs 구매 <수량>" + ChatColor.WHITE + " - 아이템을 <수량> 만큼 구매합니다.");
					if (sender.hasPermission("realshop.commands.sell"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs 판매 <수량>" + ChatColor.WHITE + " - 아이템을 <수량> 만큼 판매합니다.");
					if (sender.hasPermission("realshop.commands.instantbuy"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs 즉시구매 <이름> <수량>" + ChatColor.WHITE
								+ " - <이름>을 <수량> 만큼 즉시 구매합니다.");
					if (sender.hasPermission("realshop.commands.instantsell"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs 즉시판매 [이름] <수량>" + ChatColor.WHITE
								+ " - <이름>을 <수량> 만큼 즉시 판매합니다.");
					if (sender.hasPermission("realshop.commands.getprice"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs 가격 <이름> <수량>" + ChatColor.WHITE
								+ " - <이름>을 <수량> 만큼 구매할 경우의 가격을 알려줍니다. (수량 < 0인 경우 판매)");
					if (sender.hasPermission("realshop.commands.getworth"))
						sender.sendMessage(
								ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". " + ChatColor.AQUA
										+ "/rs 가치 <이름>" + ChatColor.WHITE + " - <이름>의 가치를 알려줍니다.");
					if (sender.hasPermission("realshop.commands.getamount"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs 수량 <이름>" + ChatColor.WHITE
								+ " - <이름>의 현재 수량을 알려줍니다.");
					if (sender.hasPermission("realshop.commands.setworth"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs 가치설정 <이름> <가치>" + ChatColor.WHITE
								+ " - <이름>의 가치를 설정합니다.");
					if (sender.hasPermission("realshop.commands.setamount"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs 수량설정 <이름> <수량>" + ChatColor.WHITE
								+ " - <이름>의 수량을 설정합니다.");
					if (sender.hasPermission("realshop.commands.createshop"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs 상점 생성 <이름> [구매] [판매]" + ChatColor.WHITE
								+ " - 터치로 <이름>을 파는 상점을 생성합니다. (활성/비활성, 구매/판매 - 구매/판매 가능?, true/false)");
					if (sender.hasPermission("realshop.commands.removeshop"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs 상점 제거" + ChatColor.WHITE + " - 터치로 상점을 제거합니다. (활성/비활성)");
					if (sender.hasPermission("realshop.commands.additem"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs 아이템추가 <이름>" + ChatColor.WHITE
								+ " - 손에 든 아이템을 <이름> 으로 등록합니다.");
					if (sender.hasPermission("realshop.commands.removeitem"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs 아이템제거 <이름>" + ChatColor.WHITE
								+ " - <이름> 을 제거합니다.");
					if (sender.hasPermission("realshop.commands.itemlist"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs 아이템목록" + ChatColor.WHITE
								+ " - 아이템 목록을 보여줍니다.");
					if (count == 0)
						sender.sendMessage(ChatColor.WHITE + "사용 가능한 명령어가 없습니다.");
				} else {
					sender.sendMessage(ChatColor.WHITE + "=====[ " + ChatColor.AQUA + "realShop Help" + ChatColor.WHITE
							+ " ]=====");
					int count = 0;
					// if (sender.hasPermission("realshop.commands.buy"))
					// sender.sendMessage(ChatColor.AQUA +
					// Integer.toString(++count) + ChatColor.WHITE + ". " +
					// ChatColor.AQUA + "/rs 구매 [수량]" + ChatColor.WHITE + " -
					// ");
					if (sender.hasPermission("realshop.commands.buy"))
						sender.sendMessage(
								ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". " + ChatColor.AQUA
										+ "/rs buy <amount>" + ChatColor.WHITE + " - buys item for <amount>.");
					if (sender.hasPermission("realshop.commands.sell"))
						sender.sendMessage(
								ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". " + ChatColor.AQUA
										+ "/rs sell <amount>" + ChatColor.WHITE + " - sells item for <amount>.");
					if (sender.hasPermission("realshop.commands.instantbuy"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs instantbuy <name> <amount>" + ChatColor.WHITE
								+ " - buys <name> for <amount> instantly.");
					if (sender.hasPermission("realshop.commands.instantsell"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs instantsell <name> <amount>" + ChatColor.WHITE
								+ " - sells <name> for <amount> instantly.");
					if (sender.hasPermission("realshop.commands.getprice"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs price <name> <amount>" + ChatColor.WHITE
								+ " - tells you the price when you buy <name> by <amount>. (amount < 0 -> sell)");
					if (sender.hasPermission("realshop.commands.getworth"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs worth <name>" + ChatColor.WHITE
								+ " - tells you the worth of <name>.");
					if (sender.hasPermission("realshop.commands.getamount"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs amount <name>" + ChatColor.WHITE
								+ " - tells you the amount of <name>.");
					if (sender.hasPermission("realshop.commands.setworth"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs setworth <name> <worth>" + ChatColor.WHITE
								+ " - sets the worth of <name>.");
					if (sender.hasPermission("realshop.commands.setamount"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs setamount <name> <amount>" + ChatColor.WHITE
								+ " - sets the amount of <name>.");
					if (sender.hasPermission("realshop.commands.createshop"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs shop create <name> [buy] [sell]" + ChatColor.WHITE
								+ " - creates a realShop that sells <name> on a touched place. (On/Off, buy, sell - can buy/sell?, true/false)");
					if (sender.hasPermission("realshop.commands.removeshop"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs shop remove" + ChatColor.WHITE
								+ " - removes touched realShop. (On/Off, buy, sell - can buy/sell?, true/false)");
					if (sender.hasPermission("realshop.commands.additem"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs additem <name>" + ChatColor.WHITE
								+ " - register item on hand as <name>.");
					if (sender.hasPermission("realshop.commands.removeitem"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs additem <name>" + ChatColor.WHITE
								+ " - removes <name>.");
					if (sender.hasPermission("realshop.commands.itemlist"))
						sender.sendMessage(ChatColor.AQUA + Integer.toString(++count) + ChatColor.WHITE + ". "
								+ ChatColor.AQUA + "/rs itemlist" + ChatColor.WHITE
								+ " - shows item list.");
					if (count == 0)
						sender.sendMessage(ChatColor.WHITE + "No available commands.");
				}
			} else if (args[0].equals("구매") || args[0].equals("buy")) {
				if (p == null) {
					if (this.getConfig().getString("server_language").equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "이 명령어는 게임 내에서만 사용 가능합니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "This command only can be used in game.");
					}
					return true;
				}
				if (!p.hasPermission("realshop.commands.buy")) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "명령어를 사용할 권한이 없습니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
					}
					return true;
				}
				if (!selectedShop.containsKey(p)) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "선택된 상점이 없습니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You didn't select shop.");
					}
					return true;
				}
				Object[] data = selectedShop.get(p);
				String name = (String) data[0];
				ItemStack item = (ItemStack) data[1];
				Boolean buy = (Boolean) data[2];
				if (!buy) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "구매가 불가능한 상점입니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "This shop cannot be bought.");
					}
					return true;
				}
				Double price = 0.0;
				Integer buyamount = 1;
				if (args.length > 1) {
					try {
						buyamount = Integer.parseInt(args[1]);
					} catch (Exception e) {
						buyamount = 1;
					}
				}
				Double worth = (Double) this.getConfig().getList("worth")
						.get(this.getConfig().getList("name").indexOf(name));
				Integer amount = (Integer) this.getConfig().getList("amount")
						.get(this.getConfig().getList("name").indexOf(name));
				if (buyamount > amount) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(
								ChatColor.RED + "아이템이 부족합니다. (" + amount + " 존재)");
					} else {
						sender.sendMessage(ChatColor.RED + "Item is not enough. (" + amount + " exists.)");
					}
					return true;
				}
				for (int i = 0; i < buyamount; i++) {
					if (amount == 1)
						price += worth;
					else
						price += (worth / (double) amount + worth / ((double) amount - 1)) / 2;
					amount--;
				}
				if (econ.getBalance(p) < price) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(
								ChatColor.RED + "돈이 부족합니다. (" + econ.getBalance(p) + " 존재, " + price + " 필요)");
					} else {
						sender.sendMessage(ChatColor.RED + "Your Money is not enough. (" + econ.getBalance(p)
								+ " exists, needs " + price + ")");
					}
					return true;
				}
				item.setAmount(buyamount);
				p.getInventory().addItem(item);
				econ.withdrawPlayer(p, price);
				String language = this.getLanguage(p);
				if (language.equals("ko_KR")) {
					sender.sendMessage(ChatColor.AQUA + name + ChatColor.WHITE + "을(를) " + ChatColor.AQUA + buyamount
							+ ChatColor.WHITE + "개 구매했습니다. (돈 " + ChatColor.AQUA
							+ econ.format(econ.getBalance(p) + price) + ChatColor.WHITE + " -> " + ChatColor.AQUA
							+ econ.format(econ.getBalance(p)) + ChatColor.WHITE + ")");
				} else {
					sender.sendMessage(ChatColor.WHITE + "You bought " + ChatColor.AQUA + name + ChatColor.WHITE
							+ " for " + ChatColor.AQUA + buyamount + ChatColor.WHITE + ". (Money " + ChatColor.AQUA
							+ econ.format(econ.getBalance(p) + price) + ChatColor.WHITE + " -> " + ChatColor.AQUA
							+ econ.format(econ.getBalance(p)) + ChatColor.WHITE + ")");
				}
				List<Integer> amountList = (List<Integer>) this.getConfig().getList("amount");
				amountList.set(this.getConfig().getList("name").indexOf(name), amount);
				this.getConfig().set("amount", amountList);
				this.saveConfig();
				this.updateSign();
			} else if (args[0].equals("판매") || args[0].equals("sell")) {
				if (p == null) {
					if (this.getConfig().getString("server_language").equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "이 명령어는 게임 내에서만 사용 가능합니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "This command only can be used in game.");
					}
					return true;
				}
				if (!p.hasPermission("realshop.commands.sell")) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "명령어를 사용할 권한이 없습니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
					}
					return true;
				}
				Object[] data = selectedShop.get(p);
				String name = (String) data[0];
				ItemStack item = (ItemStack) data[1];
				Boolean sell = (Boolean) data[3];
				if (!sell) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "판매가 불가능한 상점입니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "This shop cannot be sold.");
					}
					return true;
				}
				Double price = 0.0;
				Integer sellamount = 1;
				if (args.length > 1) {
					try {
						sellamount = Integer.parseInt(args[1]);
					} catch (Exception e) {
						sellamount = 1;
					}
				}
				ItemStack[] items = p.getInventory().getContents();
				Integer itemcount = 0;
				for (ItemStack i : items) {
					if (i != null && i.isSimilar(item))
						itemcount += i.getAmount();
				}
				if (itemcount < sellamount) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "아이템이 부족합니다. (" + itemcount + " 존재)");
					} else {
						sender.sendMessage(ChatColor.RED + "Item is not enough. (" + itemcount + " exists.)");
					}
					return true;
				}
				Double worth = (Double) this.getConfig().getList("worth")
						.get(this.getConfig().getList("name").indexOf(name));
				Integer amount = (Integer) this.getConfig().getList("amount")
						.get(this.getConfig().getList("name").indexOf(name));
				for (int i = 0; i < sellamount; i++) {
					if (amount == 0)
						price += worth;
					else
						price += (worth / (double) amount + worth / ((double) amount + 1)) / 2;
					amount++;
				}
				item.setAmount(sellamount);
				item.setAmount(sellamount);
				p.getInventory().removeItem(item);
				econ.depositPlayer(p, price);
				String language = this.getLanguage(p);
				if (language.equals("ko_KR")) {
					sender.sendMessage(ChatColor.AQUA + name + ChatColor.WHITE + "을(를) " + ChatColor.AQUA + sellamount
							+ ChatColor.WHITE + "개 판매했습니다. (돈 " + ChatColor.AQUA
							+ econ.format(econ.getBalance(p) - price) + ChatColor.WHITE + " -> " + ChatColor.AQUA
							+ econ.format(econ.getBalance(p)) + ChatColor.WHITE + ")");
				} else {
					sender.sendMessage(ChatColor.WHITE + "You sold " + ChatColor.AQUA + name + ChatColor.WHITE + " for "
							+ ChatColor.AQUA + sellamount + ChatColor.WHITE + ". (Money " + ChatColor.AQUA
							+ econ.format(econ.getBalance(p) - price) + ChatColor.WHITE + " -> " + ChatColor.AQUA
							+ econ.format(econ.getBalance(p)) + ChatColor.WHITE + ")");
				}
				List<Integer> amountList = (List<Integer>) this.getConfig().getList("amount");
				amountList.set(this.getConfig().getList("name").indexOf(name), amount);
				this.getConfig().set("amount", amountList);
				this.saveConfig();
				this.updateSign();
			} else if (args[0].equals("즉시구매") || args[0].equals("instantbuy")) {
				if (p == null) {
					if (this.getConfig().getString("server_language").equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "이 명령어는 게임 내에서만 사용 가능합니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "This command only can be used in game.");
					}
					return true;
				}
				if (!p.hasPermission("realshop.commands.instantbuy")) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "명령어를 사용할 권한이 없습니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
					}
					return true;
				}
				if (args.length < 2) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "아이템 이름을 작성해야 합니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You should fill out item name.");
					}
					return true;
				}
				String name = args[1];
				if (this.getConfig().getList("name") == null || !this.getConfig().getList("name").contains(name)) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "존재하지 않는 아이템입니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "the item does not exists.");
					}
					return true;
				}
				ItemStack item = (ItemStack) this.getConfig().getList("item").get(this.getConfig().getList("name").indexOf(name));
				Integer buyamount = 1;
				if (args.length > 2){
					try{
						buyamount = Integer.parseInt(args[2]);
					} catch(Exception e){
						
					}
				}
				Double price = 0.0;
				Double worth = (Double) this.getConfig().getList("worth")
						.get(this.getConfig().getList("name").indexOf(name));
				Integer amount = (Integer) this.getConfig().getList("amount")
						.get(this.getConfig().getList("name").indexOf(name));
				if (buyamount > amount) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(
								ChatColor.RED + "아이템이 부족합니다. (" + amount + " 존재)");
					} else {
						sender.sendMessage(ChatColor.RED + "Item is not enough. (" + amount + " exists.)");
					}
					return true;
				}
				for (int i = 0; i < buyamount; i++) {
					if (amount == 1)
						price += worth;
					else
						price += (worth / (double) amount + worth / ((double) amount - 1)) / 2;
					amount--;
				}
				if (econ.getBalance(p) < price) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(
								ChatColor.RED + "돈이 부족합니다. (" + econ.getBalance(p) + " 존재, " + price + " 필요)");
					} else {
						sender.sendMessage(ChatColor.RED + "Your Money is not enough. (" + econ.getBalance(p)
								+ " exists, needs " + price + ")");
					}
					return true;
				}
				item.setAmount(buyamount);
				p.getInventory().addItem(item);
				econ.withdrawPlayer(p, price);
				String language = this.getLanguage(p);
				if (language.equals("ko_KR")) {
					sender.sendMessage(ChatColor.AQUA + name + ChatColor.WHITE + "을(를) " + ChatColor.AQUA + buyamount
							+ ChatColor.WHITE + "개 구매했습니다. (돈 " + ChatColor.AQUA
							+ econ.format(econ.getBalance(p) + price) + ChatColor.WHITE + " -> " + ChatColor.AQUA
							+ econ.format(econ.getBalance(p)) + ChatColor.WHITE + ")");
				} else {
					sender.sendMessage(ChatColor.WHITE + "You bought " + ChatColor.AQUA + name + ChatColor.WHITE
							+ " for " + ChatColor.AQUA + buyamount + ChatColor.WHITE + ". (Money " + ChatColor.AQUA
							+ econ.format(econ.getBalance(p) + price) + ChatColor.WHITE + " -> " + ChatColor.AQUA
							+ econ.format(econ.getBalance(p)) + ChatColor.WHITE + ")");
				}
				List<Integer> amountList = (List<Integer>) this.getConfig().getList("amount");
				amountList.set(this.getConfig().getList("name").indexOf(name), amount);
				this.getConfig().set("amount", amountList);
				this.saveConfig();
			} else if (args[0].equals("즉시판매") || args[0].equals("instantsell")) {
				if (p == null) {
					if (this.getConfig().getString("server_language").equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "이 명령어는 게임 내에서만 사용 가능합니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "This command only can be used in game.");
					}
					return true;
				}
				if (!p.hasPermission("realshop.commands.instantsell")) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "명령어를 사용할 권한이 없습니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
					}
					return true;
				}
				if (args.length < 2) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "아이템 이름을 작성해야 합니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You should fill out item name.");
					}
					return true;
				}
				String name = args[1];
				if (this.getConfig().getList("name") == null || !this.getConfig().getList("name").contains(name)) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "존재하지 않는 아이템입니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "the item does not exists.");
					}
					return true;
				}
				ItemStack item = (ItemStack) this.getConfig().getList("item").get(this.getConfig().getList("name").indexOf(name));
				Double price = 0.0;
				Integer sellamount = 1;
				if (args.length > 2) {
					try {
						sellamount = Integer.parseInt(args[2]);
					} catch (Exception e) {
					}
				}
				ItemStack[] items = p.getInventory().getContents();
				Integer itemcount = 0;
				for (ItemStack i : items) {
					if (i != null && i.isSimilar(item))
						itemcount += i.getAmount();
				}
				if (itemcount < sellamount) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "아이템이 부족합니다. (" + itemcount + " 존재)");
					} else {
						sender.sendMessage(ChatColor.RED + "Item is not enough. (" + itemcount + " exists.)");
					}
					return true;
				}
				Double worth = (Double) this.getConfig().getList("worth")
						.get(this.getConfig().getList("name").indexOf(name));
				Integer amount = (Integer) this.getConfig().getList("amount")
						.get(this.getConfig().getList("name").indexOf(name));
				for (int i = 0; i < sellamount; i++) {
					if (amount == 0)
						price += worth;
					else
						price += (worth / (double) amount + worth / ((double) amount + 1)) / 2;
					amount++;
				}
				item.setAmount(sellamount);
				item.setAmount(sellamount);
				p.getInventory().removeItem(item);
				econ.depositPlayer(p, price);
				String language = this.getLanguage(p);
				if (language.equals("ko_KR")) {
					sender.sendMessage(ChatColor.AQUA + name + ChatColor.WHITE + "을(를) " + ChatColor.AQUA + sellamount
							+ ChatColor.WHITE + "개 판매했습니다. (돈 " + ChatColor.AQUA
							+ econ.format(econ.getBalance(p) - price) + ChatColor.WHITE + " -> " + ChatColor.AQUA
							+ econ.format(econ.getBalance(p)) + ChatColor.WHITE + ")");
				} else {
					sender.sendMessage(ChatColor.WHITE + "You sold " + ChatColor.AQUA + name + ChatColor.WHITE + " for "
							+ ChatColor.AQUA + sellamount + ChatColor.WHITE + ". (Money " + ChatColor.AQUA
							+ econ.format(econ.getBalance(p) - price) + ChatColor.WHITE + " -> " + ChatColor.AQUA
							+ econ.format(econ.getBalance(p)) + ChatColor.WHITE + ")");
				}
				List<Integer> amountList = (List<Integer>) this.getConfig().getList("amount");
				amountList.set(this.getConfig().getList("name").indexOf(name), amount);
				this.getConfig().set("amount", amountList);
				this.saveConfig();
			} else if (args[0].equals("가격") || args[0].equals("price")) {
				String language;
				if (p == null) language = this.getConfig().getString("server_language");
				else language = this.getLanguage(p);
				if (!sender.hasPermission("realshop.commands.getprice")) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "명령어를 사용할 권한이 없습니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
					}
					return true;
				}
				if (args.length < 2) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "아이템 이름을 작성해야 합니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You should fill out item name.");
					}
					return true;
				}
				String name = args[1];
				Integer amount = 1;
				if (args.length > 2) {
					try {
						amount = Integer.parseInt(args[2]);
					} catch (Exception e) {
						
					}
				}
				if (Math.abs(amount) > 10000) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "10000개 이하까지만 계산 가능합니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "Only up to 10,000 can be calculated.");
					}
					return true;
				}
				if (this.getConfig().getList("name") == null || !this.getConfig().getList("name").contains(name)) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "존재하지 않는 아이템입니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "the item does not exists.");
					}
					return true;
				}
				Double price = this.getPrice(name, amount);
				if (price == null) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "아이템이 부족합니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "Item is not enough.");
					}
					return true;
				}
				if (language.equals("ko_KR")) {
					if (amount >= 0) {
						sender.sendMessage(ChatColor.AQUA + name + ChatColor.WHITE + " " + ChatColor.AQUA + amount + ChatColor.WHITE + "개의 구매가: " + ChatColor.AQUA + econ.format(price) + ChatColor.WHITE + ".");
					} else {
						sender.sendMessage(ChatColor.AQUA + name + ChatColor.WHITE + " " + ChatColor.AQUA + (-amount) + ChatColor.WHITE + "개의 판매가: " + ChatColor.AQUA + econ.format(price) + ChatColor.WHITE + ".");
					}
				} else {
					if (amount >= 0) {
						sender.sendMessage(ChatColor.AQUA + "" + amount + ChatColor.WHITE + " " + ChatColor.AQUA + name + ChatColor.WHITE + " buys: " + ChatColor.AQUA + econ.format(price) + ChatColor.WHITE + ".");
					} else {
						sender.sendMessage(ChatColor.AQUA + "" + (-amount) + ChatColor.WHITE + " " + ChatColor.AQUA + name + ChatColor.WHITE + " sells: " + ChatColor.AQUA + econ.format(price) + ChatColor.WHITE + ".");
					}
				}
			} else if (args[0].equals("가치") || args[0].equals("worth")) {
				String language;
				if (p == null) language = this.getConfig().getString("server_language");
				else language = this.getLanguage(p);
				if (!sender.hasPermission("realshop.commands.getworth")) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "명령어를 사용할 권한이 없습니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
					}
					return true;
				}
				if (args.length < 2) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "아이템 이름을 작성해야 합니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You should fill out item name.");
					}
					return true;
				}
				String name = args[1];
				if (this.getConfig().getList("name") == null || !this.getConfig().getList("name").contains(name)) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "존재하지 않는 아이템입니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "the item does not exists.");
					}
					return true;
				}
				Double worth = (Double) this.getConfig().getList("worth").get(this.getConfig().getList("name").indexOf(name));
				if (language.equals("ko_KR")) {
					sender.sendMessage(ChatColor.AQUA + name + ChatColor.WHITE + "의 가치: " + ChatColor.AQUA + worth + ChatColor.WHITE + ".");
				} else {
					sender.sendMessage(ChatColor.AQUA + "worth of " + ChatColor.AQUA + name + ChatColor.WHITE + ": " + ChatColor.AQUA + worth + ChatColor.WHITE + ".");
				}
			} else if (args[0].equals("수량") || args[0].equals("amount")) {
				String language;
				if (p == null) language = this.getConfig().getString("server_language");
				else language = this.getLanguage(p);
				if (!sender.hasPermission("realshop.commands.getamount")) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "명령어를 사용할 권한이 없습니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
					}
					return true;
				}
				if (args.length < 2) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "아이템 이름을 작성해야 합니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You should fill out item name.");
					}
					return true;
				}
				String name = args[1];
				if (this.getConfig().getList("name") == null || !this.getConfig().getList("name").contains(name)) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "존재하지 않는 아이템입니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "the item does not exists.");
					}
					return true;
				}
				Integer amount = (Integer) this.getConfig().getList("amount").get(this.getConfig().getList("name").indexOf(name));
				if (language.equals("ko_KR")) {
					sender.sendMessage(ChatColor.AQUA + name + ChatColor.WHITE + "의 수량: " + ChatColor.AQUA + amount + ChatColor.WHITE + ".");
				} else {
					sender.sendMessage(ChatColor.AQUA + "amount of " + ChatColor.AQUA + name + ChatColor.WHITE + ": " + ChatColor.AQUA + amount + ChatColor.WHITE + ".");
				}
			} else if (args[0].equals("가치설정") || args[0].equals("setworth")) {
				String language;
				if (p == null) language = this.getConfig().getString("server_language");
				else language = this.getLanguage(p);
				if (!sender.hasPermission("realshop.commands.setworth")) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "명령어를 사용할 권한이 없습니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
					}
					return true;
				}
				if (args.length < 3) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "아이템 이름과 가치를 작성해야 합니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You should fill out item name and worth.");
					}
					return true;
				}
				Double worth;
					try {
						worth = Double.parseDouble(args[2]);
					} catch (Exception e) {
						if (language.equals("ko_KR")) {
							sender.sendMessage(ChatColor.RED + "가치는 유리수여야 합니다.");
						} else {
							sender.sendMessage(ChatColor.RED + "worth must be rational.");
						}
						return true;
					}
				String name = args[1];
				if (this.getConfig().getList("name") == null || !this.getConfig().getList("name").contains(name)) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "존재하지 않는 아이템입니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "the item does not exists.");
					}
					return true;
				}
				int index = this.getConfig().getList("name").indexOf(name);
				Double lastworth = (Double) this.getConfig().getList("worth").get(this.getConfig().getList("name").indexOf(name));
				((List<Double>)this.getConfig().getList("worth")).set(index, worth);
				this.saveConfig();
				if (language.equals("ko_KR")) {
					sender.sendMessage(ChatColor.AQUA + name + ChatColor.WHITE + "의 가치: " + ChatColor.AQUA + lastworth + ChatColor.WHITE + " -> " + ChatColor.AQUA + worth + ChatColor.WHITE + ".");
				} else {
					sender.sendMessage(ChatColor.AQUA + "worth of " + ChatColor.AQUA + name + ChatColor.WHITE + ": " + ChatColor.AQUA + lastworth + ChatColor.WHITE + " -> " + ChatColor.AQUA + worth + ChatColor.WHITE + ".");
				}
				this.updateSign();
			} else if (args[0].equals("수량설정") || args[0].equals("setamount")) {
				String language;
				if (p == null) language = this.getConfig().getString("server_language");
				else language = this.getLanguage(p);
				if (!sender.hasPermission("realshop.commands.setamount")) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "명령어를 사용할 권한이 없습니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
					}
					return true;
				}
				if (args.length < 3) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "아이템 이름과 수량을 작성해야 합니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You should fill out item name and amount.");
					}
					return true;
				}
				Integer amount;
					try {
						amount = Integer.parseInt(args[2]);
					} catch (Exception e) {
						if (language.equals("ko_KR")) {
							sender.sendMessage(ChatColor.RED + "수량은 0 이상의 정수여야 합니다.");
						} else {
							sender.sendMessage(ChatColor.RED + "amount must be an integer (amount >= 0).");
						}
						return true;
					}
				String name = args[1];
				if (this.getConfig().getList("name") == null || !this.getConfig().getList("name").contains(name)) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "존재하지 않는 아이템입니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "the item does not exists.");
					}
					return true;
				}
				int index = this.getConfig().getList("name").indexOf(name);
				Integer lastamount = (Integer) this.getConfig().getList("amount").get(this.getConfig().getList("name").indexOf(name));
				((List<Integer>)this.getConfig().getList("amount")).set(index, amount);
				this.saveConfig();
				if (language.equals("ko_KR")) {
					sender.sendMessage(ChatColor.AQUA + name + ChatColor.WHITE + "의 수량: " + ChatColor.AQUA + lastamount + ChatColor.WHITE + " -> " + ChatColor.AQUA + amount + ChatColor.WHITE + ".");
				} else {
					sender.sendMessage(ChatColor.AQUA + "amount of " + ChatColor.AQUA + name + ChatColor.WHITE + ": " + ChatColor.AQUA + lastamount + ChatColor.WHITE + " -> " + ChatColor.AQUA + amount + ChatColor.WHITE + ".");
				}
				this.updateSign();
			} else if (args[0].equals("상점") || args[0].equals("shop")) {
				if (args[1].equals("생성") || args[1].equals("create")) {
					if (p == null) {
						if (this.getConfig().getString("server_language").equals("ko_KR")) {
							sender.sendMessage(ChatColor.RED + "이 명령어는 게임 내에서만 사용 가능합니다.");
						} else {
							sender.sendMessage(ChatColor.RED + "This command only can be used in game.");
						}
						return true;
					}
					if (!p.hasPermission("realshop.commands.createshop")) {
						String language = this.getLanguage(p);
						if (language.equals("ko_KR")) {
							sender.sendMessage(ChatColor.RED + "명령어를 사용할 권한이 없습니다.");
						} else {
							sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
						}
						return true;
					}
					if (createShop.containsKey(p)) {
						String language = this.getLanguage(p);
						if (language.equals("ko_KR")) {
							sender.sendMessage(ChatColor.WHITE + "상점 생성 모드를 비활성화합니다.");
						} else {
							sender.sendMessage(ChatColor.WHITE + "DIsable shop create mode.");
						}
						createShop.remove(p);
						return true;
					}
					if (args.length < 3) {
						String language = this.getLanguage(p);
						if (language.equals("ko_KR")) {
							sender.sendMessage(ChatColor.RED + "아이템 이름을 작성하세요.");
						} else {
							sender.sendMessage(ChatColor.RED + "Please fill out item name.");
						}
						return true;
					}
					ItemStack item;
					if (this.getConfig().getList("name").contains(args[2])) {
						item = (ItemStack) this.getConfig().getList("item")
								.get(this.getConfig().getList("name").indexOf(args[2]));
					} else {
						Double worth = 100000.0;
						Integer amount = 1;
						if (args.length > 3) {
							try {
								worth = Double.parseDouble(args[3]);
							} catch (Exception e) {

							}
						}
						if (args.length > 4) {
							try {
								amount = Integer.parseInt(args[4]);
							} catch (Exception e) {

							}
						}
						String version = this.getVersion(this.getServer());
						Integer second = Integer.parseInt(version.split("_")[1]);
						if (second >= 9) {
							item = p.getInventory().getItemInMainHand();
						} else {
							item = p.getItemInHand();
						}
						List<ItemStack> itemList = (List<ItemStack>) this.getConfig().getList("item");
						if (itemList == null)
							itemList = new ArrayList<ItemStack>();
						itemList.add(item);
						this.getConfig().set("item", itemList);
						List<String> nameList = (List<String>) this.getConfig().getList("name");
						if (nameList == null)
							nameList = new ArrayList<String>();
						nameList.add(args[2]);
						this.getConfig().set("name", nameList);
						List<Double> worthList = (List<Double>) this.getConfig().getList("worth");
						if (worthList == null)
							worthList = new ArrayList<Double>();
						worthList.add(worth);
						this.getConfig().set("worth", worthList);
						List<Integer> amountList = (List<Integer>) this.getConfig().getList("amount");
						if (amountList == null)
							amountList = new ArrayList<Integer>();
						amountList.add(amount);
						this.getConfig().set("amount", amountList);
						this.saveConfig();
					}
					Boolean buy = true;
					Boolean sell = true;
					if (args.length > 3)
						buy = Boolean.parseBoolean(args[3]);
					if (args.length > 4)
						sell = Boolean.parseBoolean(args[4]);
					if (removeShop.contains(p))
						removeShop.remove(p);
					createShop.put(p, new Object[] { args[2], item, buy, sell });
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						String buysell;
						if (buy && !sell)
							buysell = "구매 전용 ";
						else if (!buy && sell)
							buysell = "판매 전용 ";
						else if (!buy && !sell)
							buysell = "거래 불가 ";
						else
							buysell = "";
						sender.sendMessage(ChatColor.WHITE + "표지판을 터치하여 " + buysell + ChatColor.AQUA + args[2]
								+ ChatColor.WHITE + " 상점을 생성하세요.");
					} else {
						String buysell;
						if (buy && !sell)
							buysell = " that only can be bought";
						else if (!buy && sell)
							buysell = " that only can be sold";
						else if (!buy && !sell)
							buysell = " that cannot be traded";
						else
							buysell = "";
						sender.sendMessage(ChatColor.WHITE + "touch a sign to create a " + ChatColor.AQUA + args[2]
								+ ChatColor.WHITE + " shop" + buysell + ".");
					}
				} else if (args[1].equals("제거") || args[1].equals("remove")) {
					if (p == null) {
						if (this.getConfig().getString("server_language").equals("ko_KR")) {
							sender.sendMessage(ChatColor.RED + "이 명령어는 게임 내에서만 사용 가능합니다.");
						} else {
							sender.sendMessage(ChatColor.RED + "This command only can be used in game.");
						}
						return true;
					}
					if (!p.hasPermission("realshop.commands.removeshop")) {
						String language = this.getLanguage(p);
						if (language.equals("ko_KR")) {
							sender.sendMessage(ChatColor.RED + "명령어를 사용할 권한이 없습니다.");
						} else {
							sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
						}
						return true;
					}
					if (removeShop.contains(p)) {
						String language = this.getLanguage(p);
						if (language.equals("ko_KR")) {
							sender.sendMessage(ChatColor.WHITE + "상점 제거 모드를 비활성화합니다.");
						} else {
							sender.sendMessage(ChatColor.WHITE + "DIsable shop remove mode.");
						}
						removeShop.remove(p);
						return true;
					}
					if (createShop.containsKey(p))
						createShop.remove(p);
					removeShop.add(p);
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.WHITE + "표지판을 터치하여 상점을 제거하세요.");
					} else {
						sender.sendMessage(ChatColor.WHITE + "touch a sign to remove a shop.");
					}
				}
			} else if (args[0].equals("아이템추가") || args[0].equals("additem")) {
				if (p == null) {
					if (this.getConfig().getString("server_language").equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "이 명령어는 게임 내에서만 사용 가능합니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "This command only can be used in game.");
					}
					return true;
				}
				if (!p.hasPermission("realshop.commands.additem")) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "명령어를 사용할 권한이 없습니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
					}
					return true;
				}
				if (args.length < 2) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "아이템 이름을 작성해야 합니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You should fill out item name.");
					}
					return true;
				}
				if (this.getConfig().getList("name") != null && this.getConfig().getList("name").contains(args[1])) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "이미 존재하는 아이템 이름입니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "that item name already exists.");
					}
					return true;
				}
				Double worth = 100000.0;
				Integer amount = 1;
				if (args.length > 1) {
					try {
						worth = Double.parseDouble(args[2]);
					} catch (Exception e) {

					}
				}
				if (args.length > 2) {
					try {
						amount = Integer.parseInt(args[3]);
					} catch (Exception e) {

					}
				}
				ItemStack item;
				String version = this.getVersion(this.getServer());
				Integer second = Integer.parseInt(version.split("_")[1]);
				if (second >= 9) {
					item = p.getInventory().getItemInMainHand();
				} else {
					item = p.getItemInHand();
				}
				List<ItemStack> itemList = (List<ItemStack>) this.getConfig().getList("item");
				if (itemList == null)
					itemList = new ArrayList<ItemStack>();
				itemList.add(item);
				this.getConfig().set("item", itemList);
				List<String> nameList = (List<String>) this.getConfig().getList("name");
				if (nameList == null)
					nameList = new ArrayList<String>();
				nameList.add(args[1]);
				this.getConfig().set("name", nameList);
				List<Double> worthList = (List<Double>) this.getConfig().getList("worth");
				if (worthList == null)
					worthList = new ArrayList<Double>();
				worthList.add(worth);
				this.getConfig().set("worth", worthList);
				List<Integer> amountList = (List<Integer>) this.getConfig().getList("amount");
				if (amountList == null)
					amountList = new ArrayList<Integer>();
				amountList.add(amount);
				this.getConfig().set("amount", amountList);
				this.saveConfig();
				String language = this.getLanguage(p);
				if (language.equals("ko_KR")) {
					if (worth == 1) {
						sender.sendMessage(ChatColor.AQUA + args[1] + ChatColor.WHITE + "(가치 " + ChatColor.AQUA + worth
								+ ChatColor.WHITE + ", 개수 " + ChatColor.AQUA + amount + ChatColor.WHITE
								+ ")을(를) 추가했습니다.");
					} else {
						sender.sendMessage(ChatColor.AQUA + args[1] + ChatColor.WHITE + "(가치 " + ChatColor.AQUA + worth
								+ ChatColor.WHITE + ", 개수 " + ChatColor.AQUA + amount + ChatColor.WHITE
								+ ")을(를) 추가했습니다.");
					}
				} else {
					if (worth == 1) {
						sender.sendMessage(ChatColor.WHITE + "created " + ChatColor.AQUA + args[1] + ChatColor.WHITE
								+ ". (worth " + ChatColor.AQUA + worth + ChatColor.WHITE + ", amount "
								+ ChatColor.AQUA + amount + ChatColor.WHITE + ")");
					} else {
						sender.sendMessage(ChatColor.WHITE + "created " + ChatColor.AQUA + args[1] + ChatColor.WHITE
								+ ". (worth " + ChatColor.AQUA + worth + ChatColor.WHITE + ", amount "
								+ ChatColor.AQUA + amount + ChatColor.WHITE + ")");
					}
				}
			} else if (args[0].equals("아이템제거") || args[0].equals("removeitem")) {
				if (p == null) {
					if (this.getConfig().getString("server_language").equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "이 명령어는 게임 내에서만 사용 가능합니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "This command only can be used in game.");
					}
					return true;
				}
				if (!p.hasPermission("realshop.commands.removeitem")) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "명령어를 사용할 권한이 없습니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
					}
					return true;
				}
				if (args.length < 2) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "아이템 이름을 작성해야 합니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You should fill out item name.");
					}
					return true;
				}
				List<String> nameList = (List<String>) this.getConfig().getList("name");
				if (nameList == null || !nameList.contains(args[1])) {
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "존재하지 않는 아이템입니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "the item does not exists.");
					}
					return true;
				}
				int index = nameList.indexOf(args[1]);
				nameList.remove(index);
				this.getConfig().set("name", nameList);
				List<ItemStack> itemList = (List<ItemStack>) this.getConfig().getList("item");
				itemList.remove(index);
				this.getConfig().set("item", itemList);
				List<Double> worthList = (List<Double>) this.getConfig().getList("worth");
				worthList.remove(index);
				this.getConfig().set("worth", worthList);
				List<Integer> amountList = (List<Integer>) this.getConfig().getList("amount");
				amountList.remove(index);
				this.getConfig().set("amount", amountList);
				this.saveConfig();
				String language = this.getLanguage(p);
				if (language.equals("ko_KR")) {
					sender.sendMessage(ChatColor.AQUA + args[1] + ChatColor.WHITE + "을(를) 제거했습니다.");
				} else {
					sender.sendMessage(ChatColor.WHITE + "removed " + ChatColor.AQUA + args[1] + ChatColor.WHITE + ".");
				}
			} else if (args[0].equals("아이템목록") || args[0].equals("itemlist")) {
				String language;
				if (p == null) language = this.getConfig().getString("server_language");
				else language = this.getLanguage(p);
				if (!sender.hasPermission("realshop.commands.itemlist")) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "명령어를 사용할 권한이 없습니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
					}
					return true;
				}
				if (this.getConfig().getList("name") == null) {
					if (language.equals("ko_KR")) {
						sender.sendMessage(ChatColor.RED + "아이템이 존재하지 않습니다.");
					} else {
						sender.sendMessage(ChatColor.RED + "item doesn't exist.");
					}
					return true;
				}
				String result = "";
				for (String name : (List<String>)this.getConfig().getList("name")) {
					result += ChatColor.AQUA + name + ChatColor.WHITE + ", ";
				}
				result = result.substring(0, result.length() - 2);
				if (language.equals("ko_KR")) {
					sender.sendMessage(ChatColor.WHITE + "아이템 목록: " + result + ".");
				} else {
					sender.sendMessage(ChatColor.WHITE + "item list: " + result + ".");
				}
			}
			return true;
		}
		return false;
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
			Block b = event.getClickedBlock();
			if (b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST) {
				Sign sign = (Sign) b.getState();
				Player p = event.getPlayer();
				if (createShop.containsKey(p)) {
					if (this.getConfig().getList("vector") != null && this.getConfig().getList("vector").contains(sign.getLocation().toVector())) {
						Integer index = this.getConfig().getList("vector").indexOf(sign.getLocation().toVector());
						List<org.bukkit.util.Vector> vectorlist = (List<org.bukkit.util.Vector>) this.getConfig().getList("vector");
						vectorlist.remove((int)index);
						this.getConfig().set("vector", vectorlist);
						List<String> shoplist = (List<String>) this.getConfig().getList("shop");
						shoplist.remove((int)index);
						this.getConfig().set("shop", shoplist);
						List<String> worldlist = (List<String>) this.getConfig().getList("world");
						worldlist.remove((int)index);
						this.getConfig().set("world", worldlist);
						this.saveConfig();
					}
					Object[] data = createShop.get(p);
					String name = (String) data[0];
					// ItemStack item = (ItemStack) data[1];
					Boolean buy = (Boolean) data[2];
					Boolean sell = (Boolean) data[3];
					sign.setLine(0, ChatColor.AQUA + "[ realShop ]");
					sign.setLine(1, name);
					Integer index = this.getConfig().getList("name").indexOf(name);
					String buysell;
					if (buy && !sell) {
						sign.setLine(2, ChatColor.WHITE + "B: " + econ.format(this.getPrice(name, 1).intValue()));
						sign.setLine(3, "S: X");
					} else if (!buy && sell) {
						sign.setLine(2, "B: X");
						sign.setLine(3,
								ChatColor.WHITE + "S: " + econ.format(this.getPrice(name, -1).intValue()));
					} else if (!buy && !sell) {
						sign.setLine(2, "B: X");
						sign.setLine(3, "S: X");
					} else {
						sign.setLine(2, ChatColor.WHITE + "B: " + econ.format(this.getPrice(name, 1).intValue()));
						sign.setLine(3,
								ChatColor.WHITE + "S: " + econ.format(this.getPrice(name, -1).intValue()));
					}
					String language = this.getLanguage(p);
					if (language.equals("ko_KR")) {
						if (buy && !sell)
							buysell = "구매 전용 ";
						else if (!buy && sell)
							buysell = "판매 전용 ";
						else if (!buy && !sell)
							buysell = "거래 불가 ";
						else
							buysell = "";
						p.sendMessage(
								ChatColor.WHITE + buysell + ChatColor.AQUA + name + ChatColor.WHITE + " 상점을 생성했습니다.");
					} else {
						if (buy && !sell)
							buysell = " that only can be bought";
						else if (!buy && sell)
							buysell = " that only can be sold";
						else if (!buy && !sell)
							buysell = " that cannot be traded";
						else
							buysell = "";
						p.sendMessage(ChatColor.WHITE + "created " + ChatColor.AQUA + name + ChatColor.WHITE + " shop"
								+ buysell + ".");
					}
					sign.update();
					List<String> shopList = (List<String>) this.getConfig().getList("shop");
					if (shopList == null)
						shopList = new ArrayList<String>();
					shopList.add(name);
					this.getConfig().set("shop", shopList);
					List<org.bukkit.util.Vector> vectorList = (List<org.bukkit.util.Vector>) this.getConfig()
							.getList("vector");
					if (vectorList == null)
						vectorList = new ArrayList<org.bukkit.util.Vector>();
					vectorList.add(sign.getLocation().toVector());
					List<String> worldList = (List<String>) this.getConfig().getList("world");
					if (worldList == null)
						worldList = new ArrayList<String>();
					worldList.add(sign.getLocation().getWorld().getName());
					this.getConfig().set("world", worldList);
					this.saveConfig();
					return;
				}
				if (sign.getLine(0).equals(ChatColor.AQUA + "[ realShop ]")) {
					if (removeShop.contains(p)) {
						if (this.getConfig().getList("vector") == null
								|| !this.getConfig().getList("vector").contains(sign.getLocation().toVector())) {
							String language = this.getLanguage(p);
							if (language.equals("ko_KR")) {
								p.sendMessage(ChatColor.RED + "상점이 아닙니다.");
							} else {
								p.sendMessage(ChatColor.RED + "It isn't a shop.");
							}
							return;
						}
						String name = sign.getLine(1);
						sign.setLine(0, "");
						sign.setLine(1, "");
						sign.setLine(2, "");
						sign.setLine(3, "");
						String language = this.getLanguage(p);
						if (language.equals("ko_KR")) {
							p.sendMessage(ChatColor.AQUA + name + ChatColor.WHITE + " 상점을 제거했습니다.");
						} else {
							p.sendMessage(ChatColor.WHITE + "removed " + ChatColor.AQUA + name + ChatColor.WHITE + " shop.");
						}
						sign.update();
						Integer index = this.getConfig().getList("vector").indexOf(sign.getLocation().toVector());
						List<org.bukkit.util.Vector> vectorlist = (List<org.bukkit.util.Vector>) this.getConfig().getList("vector");
						vectorlist.remove((int)index);
						this.getConfig().set("vector", vectorlist);
						List<String> shoplist = (List<String>) this.getConfig().getList("shop");
						shoplist.remove((int)index);
						this.getConfig().set("shop", shoplist);
						List<String> worldlist = (List<String>) this.getConfig().getList("world");
						worldlist.remove((int)index);
						this.getConfig().set("world", worldlist);
						this.saveConfig();
						return;
					}
					if (selectedShop.containsKey(p))
						selectedShop.remove(p);
					String name = (String) sign.getLine(1);
					ItemStack item;
					Boolean buy = true;
					Boolean sell = true;
					if (this.getConfig().getList("name") == null || !this.getConfig().getList("name").contains(name)) {
						String language = this.getLanguage(p);
						if (language.equals("ko_KR")) {
							p.sendMessage(ChatColor.RED + "잘못된 상점입니다.");
						} else {
							p.sendMessage(ChatColor.RED + "Invalid shop.");
						}
						return;
					}
					if (this.getConfig().getList("vector") == null
							|| !this.getConfig().getList("vector").contains(sign.getLocation().toVector())) {
						String language = this.getLanguage(p);
						if (language.equals("ko_KR")) {
							p.sendMessage(ChatColor.RED + "잘못된 상점입니다.");
						} else {
							p.sendMessage(ChatColor.RED + "Invalid shop.");
						}
						return;
					}
					item = (ItemStack) this.getConfig().getList("item")
							.get(this.getConfig().getList("name").indexOf(name));
					if (sign.getLine(2).split("B: ")[1].equals("X"))
						buy = false;
					if (sign.getLine(3).split("S: ")[1].equals("X"))
						sell = false;
					String buysell;
					String usage;
					String language = this.getLanguage(p);
					Double worth = (Double) this.getConfig().getList("worth")
							.get(this.getConfig().getList("name").indexOf(name));
					Integer amount = (Integer) this.getConfig().getList("amount")
							.get(this.getConfig().getList("name").indexOf(name));
					if (language.equals("ko_KR")) {
						if (buy && !sell) {
							buysell = "구매 전용 ";
							usage = ChatColor.AQUA + " /rs 구매" + ChatColor.WHITE + "로 거래하세요.";
						} else if (!buy && sell) {
							buysell = "판매 전용 ";
							usage = ChatColor.AQUA + " /rs 판매" + ChatColor.WHITE + "로 거래하세요.";
						} else if (!buy && !sell) {
							buysell = "거래 불가 ";
							usage = "";
						} else {
							buysell = "";
							usage = ChatColor.AQUA + " /rs 구매" + ChatColor.WHITE + "," + ChatColor.AQUA + " /rs 판매"
									+ ChatColor.WHITE + "로 거래하세요.";
						}
						p.sendMessage(ChatColor.WHITE + buysell + ChatColor.AQUA + name + ChatColor.WHITE + " 상점입니다."
								+ usage + " (가치 " + ChatColor.AQUA + worth + ChatColor.WHITE + ", 개수 " + ChatColor.AQUA
								+ amount + ChatColor.WHITE + ")");
					} else {
						if (buy && !sell) {
							buysell = " that only can be bought";
							usage = " trade with " + ChatColor.AQUA + "/rs buy" + ChatColor.WHITE + ".";
						} else if (!buy && sell) {
							buysell = " that only can be sold";
							usage = " trade with " + ChatColor.AQUA + "/rs sell" + ChatColor.WHITE + ".";
						} else if (!buy && !sell) {
							buysell = " that cannot be traded";
							usage = "";
						} else {
							buysell = "";
							usage = " trade with " + ChatColor.AQUA + "/rs buy" + ChatColor.WHITE + "," + ChatColor.AQUA
									+ " /rs sell" + ChatColor.WHITE + ".";
						}
						p.sendMessage(ChatColor.WHITE + "created " + ChatColor.AQUA + name + ChatColor.WHITE + " shop"
								+ buysell + "." + usage + " (worth " + ChatColor.AQUA + worth + ChatColor.WHITE
								+ ", amount " + ChatColor.AQUA + amount + ChatColor.WHITE + ")");
					}
					selectedShop.put(p, new Object[] { name, item, buy, sell });
				}
			}
		}
	}
	
	@EventHandler(priority=EventPriority.NORMAL)
	public void onBlockBreak(BlockBreakEvent event) {
		List<org.bukkit.util.Vector> vectors = (List<org.bukkit.util.Vector>) this.getConfig().getList("vector");
		List<String> worlds = (List<String>) this.getConfig().getList("world");
		for (int i = 0; i < vectors.size(); i++) {
			org.bukkit.util.Vector v = vectors.get(i);
			String worldname = worlds.get(i);
			if (event.getBlock().getLocation().getWorld().getName().equals(worldname) && event.getBlock().getLocation().toVector().equals(v)) {
				event.setCancelled(true);
				Player p = event.getPlayer();
				String language = this.getLanguage(p);
				if (language.equals("ko_KR")) {
					p.sendMessage(ChatColor.RED + "상점을 파괴할 수 없습니다.");
				} else {
					p.sendMessage(ChatColor.RED + "You can't break the shop.");
				}
			}
		}
	}

}
