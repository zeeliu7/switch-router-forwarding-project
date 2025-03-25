package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.MACAddress;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device
{	

    private class InterfaceEntry {
        Iface iface;
        long timestamp;

        InterfaceEntry(Iface iface, long timestamp) {
            this.iface = iface;
            this.timestamp = timestamp;
        }
    }

    private HashMap<MACAddress, InterfaceEntry> addressTable = new HashMap<>();

	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Switch(String host, DumpFile logfile)
	{
		super(host,logfile);
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * @param etherPacket the Ethernet packet that was received
	 * @param incomingIface the interface on which the addressTable.put(sourceMacAddr, new InterfaceEntry(incomingIface, currTime));packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface incomingIface)
	{
		System.out.println("*** -> Received packet: " +
				etherPacket.toString().replace("\n", "\n\t"));
		
		/********************************************************************/
		/* TODO: Handle packets                                             */
		MACAddress sourceMacAddr = etherPacket.getSourceMAC();
        MACAddress destMacAddr = etherPacket.getDestinationMAC();

        long currTime = System.currentTimeMillis();

        // Remove expired MAC addresses
        Iterator<Map.Entry<MACAddress, InterfaceEntry>> iterator = addressTable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<MACAddress, InterfaceEntry> entry = iterator.next();
            if (currTime - entry.getValue().timestamp > 15000) { // 15-second timeout
                iterator.remove();
            }
        }

        // Learn the source MAC address
        addressTable.put(sourceMacAddr, new InterfaceEntry(incomingIface, currTime));

        // Forward the packet
        InterfaceEntry outEntry = addressTable.get(destMacAddr);
        if (outEntry != null) {
            // Unicast forwarding if destination MAC is known
            super.sendPacket(etherPacket, outEntry.iface);
        } else {
            // Flood if destination MAC is unknown
            for (Iface iface : this.interfaces.values()) {
                if (!iface.equals(incomingIface)) {
                    super.sendPacket(etherPacket, iface);
                }
            }
        }
		/********************************************************************/
	}
}
