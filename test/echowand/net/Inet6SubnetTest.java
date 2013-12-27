package echowand.net;

import echowand.common.EOJ;
import echowand.common.EPC;
import echowand.common.ESV;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author Yoshiki Makino
 */
public class Inet6SubnetTest {
    private Inet6Subnet subnet;
    
    @Before
    public void setUp() throws SubnetException {
        subnet = new Inet6Subnet();
    }
    
    @After
    public void tearDown() {
        subnet.stopService();
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    
    public CommonFrame createFrame() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(14);
            buffer.put((byte) 0x10);
            buffer.put((byte) 0x81);
            buffer.putShort((short) 0x01);
            buffer.put(new EOJ("001101").toBytes());
            buffer.put(new EOJ("001101").toBytes());
            buffer.put(ESV.Get.toByte());
            buffer.put((byte) 0x01);
            buffer.put(new Property(EPC.x80).toBytes());
            return new CommonFrame(buffer.array());
        } catch (InvalidDataException e) {
            e.printStackTrace();
            fail();
            return null;
        }
    }
    
    public void sendTest(Node target, boolean success) {
        Frame sendFrame = new Frame(subnet.getLocalNode(), target, createFrame());

        try {
            subnet.send(sendFrame);
        } catch (SubnetException e) {
            e.printStackTrace();
        }

        FrameReceiver receiver = new FrameReceiver(subnet);
        receiver.start();
        
        Frame recvFrame = receiver.getRecvFrame();
        if (success) {
            assertFalse(recvFrame == null);
            assertTrue(Arrays.equals(sendFrame.getCommonFrame().toBytes(), recvFrame.getCommonFrame().toBytes()));
        } else {
            assertTrue(recvFrame == null);
        }
    }

    @Test
    public void testSendAndRecv() throws SubnetException, UnknownHostException {
        subnet.startService();

        sendTest(subnet.getGroupNode(), true);
        sendTest(subnet.getLocalNode(), true);

        Node node = subnet.getRemoteNode((Inet6Address) Inet6Address.getByName("::1"));
        sendTest(node, true);
        Node invalidAddr = subnet.getRemoteNode((Inet6Address) Inet6Address.getByName("FD00::fe"));
        sendTest(invalidAddr, false);
    }

    @Test
    public void testCreation() throws SubnetException {
        assertFalse(subnet.stopService());
        assertFalse(subnet.stopService());
        
        assertTrue(subnet.startService());
        assertFalse(subnet.startService());
        assertTrue(subnet.isWorking());
        
        assertTrue(subnet.stopService());
        assertFalse(subnet.stopService());
        assertFalse(subnet.isWorking());
    }
    
    private LinkedList<Inet6Address> getInet6Addresses() throws SocketException {
        LinkedList<Inet6Address> inet6addrs = new LinkedList<Inet6Address>();
        
        Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
        while (nifs.hasMoreElements()) {
            NetworkInterface nif = nifs.nextElement();
            Enumeration<InetAddress> addrs = nif.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress addr = addrs.nextElement();
                if (addr instanceof Inet6Address) {
                    inet6addrs.add((Inet6Address)addr);
                }
            }
        }
        
        return inet6addrs;
    }
    
    private LinkedList<NetworkInterface> getInet6Interfaces() throws SocketException {
        LinkedList<NetworkInterface> inet6ifs = new LinkedList<NetworkInterface>();
        
        for (Inet6Address addr : getInet6Addresses()) {
            NetworkInterface nif = NetworkInterface.getByInetAddress(addr);
            if (!inet6ifs.contains(nif)) {
                inet6ifs.add(nif);
            }
        }
        
        return inet6ifs;
    }
    
    @Test
    public void testCreationWithNetworkInterface() throws SocketException, SubnetException {
        subnet.stopService();

        for (NetworkInterface nif : getInet6Interfaces()) {
            subnet = new Inet6Subnet(nif);
            assertFalse(subnet.isWorking());
            assertEquals(subnet.getNetworkInterface(), nif);
            subnet.stopService();
        }
    }

    @Test(expected = SubnetException.class)
    public void testCreationWithNullNetworkInterface() throws SubnetException {
        subnet.stopService();
        subnet = new Inet6Subnet((NetworkInterface) null);
    }

    @Test
    public void testCreationWithAddress() throws SocketException, SubnetException {
        subnet.stopService();

        for (Inet6Address addr : getInet6Addresses()) {
            NetworkInterface nif = NetworkInterface.getByInetAddress(addr);
            subnet = new Inet6Subnet(addr);
            assertFalse(subnet.isWorking());
            assertEquals(nif, subnet.getNetworkInterface());
            subnet.stopService();
        }
    }
    
    @Test(expected=SubnetException.class)
    public void testCreationWithNullAddress() throws SubnetException {
        subnet.stopService();
        subnet = new Inet6Subnet((Inet6Address)null);
    }
    
    @Test
    public void testStartAndStopService() throws SubnetException {
        assertFalse(subnet.isWorking());

        assertTrue(subnet.startService());
        assertTrue(subnet.isWorking());
        assertFalse(subnet.startService());
        assertTrue(subnet.isWorking());

        assertTrue(subnet.stopService());
        assertFalse(subnet.isWorking());
        assertFalse(subnet.stopService());
        assertFalse(subnet.isWorking());
    }
    
    @Test(expected=SubnetException.class)
    public void testInvalidSend() throws SubnetException {
        subnet.stopService();
        subnet.send(new Frame(subnet.getLocalNode(), subnet.getLocalNode(), createFrame()));
    }
    
    @Test(expected=SubnetException.class)
    public void testInvalidRecv() throws SubnetException {  
        subnet.stopService();
        subnet.receive();
    }
    
    @Test(expected= SubnetException.class)
    public void testSendAfterStopService() throws SubnetException {
        subnet.stopService();
        subnet.send(new Frame(subnet.getLocalNode(), subnet.getLocalNode(), createFrame()));
    }

    @Test
    public void testStartServiceAfterStopService() throws SubnetException {
        try {
            subnet.startService();
            assertTrue(subnet.send(new Frame(subnet.getLocalNode(), subnet.getLocalNode(), createFrame())));
        } catch (SubnetException e) {
            fail();
        }    
        
        subnet.stopService();

        try {
            subnet.send(new Frame(subnet.getLocalNode(), subnet.getLocalNode(), createFrame()));
        } catch (SubnetException e) {
        }

        subnet.startService();

        try {
            assertTrue(subnet.send(new Frame(subnet.getLocalNode(), subnet.getLocalNode(), createFrame())));
        } catch (SubnetException e) {
            e.printStackTrace();
            fail();
        }
    }
    
    /*
    @Test
    public void setBufferSize() {
        assertEquals(1500, subnet.getBufferSize());
        subnet.setBufferSize(3000);
        assertEquals(3000, subnet.getBufferSize());
    }*/
    
    @Test
    public void testNodeEquals() throws SubnetException {
        try {
            Node node1 = subnet.getRemoteNode(Inet6Address.getByName("FD00::1"));
            Node node2 = subnet.getRemoteNode(Inet6Address.getByName("FD00::1"));
            assertEquals(node1, node2);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            fail();
        }
    }
}
