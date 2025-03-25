package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import java.nio.ByteBuffer;
import java.util.Map;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.BasePacket;
import net.floodlightcontroller.packet.MACAddress;

/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device
{	
	/** Routing table for the router */
	private RouteTable routeTable;
	
	/** ARP cache for the router */
	private ArpCache arpCache;
	
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile)
	{
		super(host,logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
	}
	
	/**
	 * @return routing table for the router
	 */
	public RouteTable getRouteTable()
	{ return this.routeTable; }
	
	/**
	 * Load a new routing table from a file.
	 * @param routeTableFile the name of the file containing the routing table
	 */
	public void loadRouteTable(String routeTableFile)
	{
		if (!routeTable.load(routeTableFile, this))
		{
			System.err.println("Error setting up routing table from file "
					+ routeTableFile);
			System.exit(1);
		}
		
		System.out.println("Loaded static route table");
		System.out.println("-------------------------------------------------");
		System.out.print(this.routeTable.toString());
		System.out.println("-------------------------------------------------");
	}
	
	/**
	 * Load a new ARP cache from a file.
	 * @param arpCacheFile the name of the file containing the ARP cache
	 */
	public void loadArpCache(String arpCacheFile)
	{
		if (!arpCache.load(arpCacheFile))
		{
			System.err.println("Error setting up ARP cache from file "
					+ arpCacheFile);
			System.exit(1);
		}
		
		System.out.println("Loaded static ARP cache");
		System.out.println("----------------------------------");
		System.out.print(this.arpCache.toString());
		System.out.println("----------------------------------");
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface the interface on which the packet was received
	 */
	
	public void handlePacket(Ethernet etherPacket, Iface inIface)
	{
		System.out.println("*** -> Received packet: " +
				etherPacket.toString().replace("\n", "\n\t"));
		
		/********************************************************************/
		/* TODO: Handle packets                                             */


		////////// CHECKING PACKET //////////

		// if it contains an ipv4 packet
		if (etherPacket.getEtherType() != etherPacket.TYPE_IPv4) {
			return;
		}

		// cast ipv4 header
		IPv4 currIPv4 = (IPv4)etherPacket.getPayload();
		
		// verify checksum
		short originalChecksum = currIPv4.getChecksum();
		currIPv4.setChecksum((short) 0);
		ByteBuffer byteBuffer = ByteBuffer.wrap(currIPv4.serialize());
		// byte[] serializeResult = currIPv4.serialize();
		// short newChecksum = currIPv4.getChecksum();
		// currIPv4.setChecksum(originalChecksum);
		if (originalChecksum != byteBuffer.getShort(10)) {
			return;
		}

		// decrease TTL
		byte currTTL = currIPv4.getTtl();
		if (currTTL <= 1) {
			return;
		} else {
			currIPv4.setTtl((byte)(currTTL - 1));
			currIPv4.setChecksum((short) 0);
			byte[] currIPv4s = currIPv4.serialize();
			currIPv4.deserialize(currIPv4s, 0, currIPv4s.length);
		}

		// check interface
        int destIpAddr = currIPv4.getDestinationAddress();
		Map<String,Iface> deviceInterfaces = this.getInterfaces();
		for (Iface iface : deviceInterfaces.values()) {
			if (iface.getIpAddress() == destIpAddr) {
				return;
			}
		}

		////////// FORWARDING PACKET //////////
		RouteEntry lpre = this.routeTable.lookup(destIpAddr); // longest prefix route entry
		if (lpre == null) {
			return;
		}

		int nextHopIp = (lpre.getGatewayAddress() != 0)?lpre.getGatewayAddress():destIpAddr;

		ArpEntry arpEntry = this.arpCache.lookup(nextHopIp);
		if (arpEntry == null) {
			return;
		}
		MACAddress macAddress = arpEntry.getMac();

		etherPacket.setSourceMACAddress(lpre.getInterface().getMacAddress().toBytes());
		etherPacket.setDestinationMACAddress(macAddress.toBytes());

		super.sendPacket(etherPacket, lpre.getInterface());

		/********************************************************************/
	}
}
