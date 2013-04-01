package echowand.net;

import java.io.IOException;
import java.net.*;

/**
 * IPv4ネットワークのサブネット
 * @author Yoshiki Makino
 */
public class Inet4Subnet implements Subnet {
    
    /**
     * ECHONET Liteが利用するIPv4マルチキャストアドレス
     */
    public static final String MULTICAST_ADDRESS=  "224.0.23.0";
    
    /**
     * ECHONET Liteが利用するポート番号
     */
    public static final short  DEFAULT_PORT = 3610;
    
    /**
     * 受信データ用バッファの最大長のデフォルト
     */
    public static final short  DEFAULT_BUFSIZE = 1500;
    
    private MulticastSocket multicastSocket;
    private NetworkInterface networkInterface;
    private Inet4Address groupAddress;
    private Inet4Address localAddress;
    private Inet4Node groupNode;
    private Inet4Node localNode;
    private int bufferSize = DEFAULT_BUFSIZE;
    private boolean enable = false;
    
    /**
     * Inet4Subnetを生成する。
     * ソケットの初期化も同時に行い、このInet4Subnetを有効にする。
     * @throws SubnetException 生成に失敗した場合
     */
    public Inet4Subnet() throws SubnetException {
        initInet4Subnet(true);
    }
    
    /**
     * Inet4Subnetを生成する。
     * 与えられたdoInitがtrueであればソケットの初期化も行い、このInet4Subnetを有効にする。
     * doInitがfalseであればソケットの初期化は行わず、enableが呼ばれるまで無効状態になる。
     * @param doInit ソケットの初期化処理の有無
     * @throws SubnetException 生成に失敗した場合
     */
    public Inet4Subnet(boolean doInit) throws SubnetException {
        initInet4Subnet(doInit);
    }
    
    /**
     * Inet4Subnetを生成する。
     * 与えられたdoInitがtrueであればソケットの初期化も行い、このInet4Subnetを有効にする。
     * doInitがfalseであればソケットの初期化は行わず、enableが呼ばれるまで無効状態になる。
     * addressにより利用するネットワークインタフェースの指定を行う。
     * @param address 利用するネットワークインタフェースにつけられたアドレス
     * @param doInit 初期化処理を行うかどうかの指定
     * @throws SubnetException 生成に失敗した場合
     */
    public Inet4Subnet(Inet4Address address, boolean doInit) throws SubnetException {
        if (address == null) {
            throw new SubnetException("invalid address: " + address);
        }

        try {
            localAddress = address;
            networkInterface = NetworkInterface.getByInetAddress(address);

            if (networkInterface == null) {
                throw new SubnetException("invalid address: " + address);
            }
            
            initInet4Subnet(doInit);
        } catch (SocketException e) {
            throw new SubnetException("catched exception", e);
        }
    }
    
    /**
     * Inet4Subnetを生成する。
     * 与えられたdoInitがtrueであればソケットの初期化も行い、このInet4Subnetを有効にする。
     * doInitがfalseであればソケットの初期化は行わず、enableが呼ばれるまで無効状態になる。
     * networkInterfaceにより利用するネットワークインタフェースの指定を行う。
     * @param networkInterface 利用するネットワークインタフェース
     * @param doInit ソケットの初期化処理の有無
     * @throws SubnetException 生成に失敗した場合
     */
    public Inet4Subnet(NetworkInterface networkInterface, boolean doInit) throws SubnetException {
        if (networkInterface == null) {
            throw new SubnetException("invalid network interface: " + networkInterface);
        }
        
        this.networkInterface = networkInterface;
        initInet4Subnet(doInit);
    }
    
    private void initInet4Subnet(boolean doInit) throws SubnetException {
        
        try {
            groupAddress = (Inet4Address)Inet4Address.getByName(MULTICAST_ADDRESS);
            if (localAddress == null) {
                localAddress = (Inet4Address)Inet4Address.getLocalHost();
            }
        } catch (UnknownHostException e) {
            throw new SubnetException("catched exception", e);
        }
        
        if (doInit) {
            initSocket();
        }
    }
    
    private void initSocket() throws SubnetException {
        try {
            closeSocket();
            multicastSocket = new MulticastSocket(DEFAULT_PORT);
            
            if (networkInterface != null) {
                multicastSocket.setNetworkInterface(networkInterface);
            }
            
            multicastSocket.joinGroup(groupAddress);
            multicastSocket.setLoopbackMode(false);
            multicastSocket.setReuseAddress(false);
            
            enable = true;
        } catch (BindException e) {
            throw new SubnetException("catched exception", e);
        } catch (IOException e) {
            closeSocket();
            throw new SubnetException("catched exception", e);
        }
    }
    
    private void closeSocket() {
        if (multicastSocket != null) {
            multicastSocket.close();
            multicastSocket = null;
            enable = false;
        }
    }
    
    /**
     * 設定されたネットワークインタフェースを返す。
     * @return 設定されたネットワークインタフェース
     */
    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }
    
    /**
     * バッファの最大長を返す。
     * @return バッファの最大長
     */
    public int getBufferSize() {
        return bufferSize;
    }
    
    
    /**
     * バッファの最大長を設定する。
     * @param bufferSize 
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }
    
    /**
     * このInet4Subnetが有効であるかどうか返す。
     * @return 有効であればtrue、そうでなければfalse
     */
    public synchronized boolean isEnabled() {
        return enable;
    }
    
    /**
     * このInet4Subnetを無効にする。
     * @return 有効から無効に変更した場合はtrue、そうでなければfalse
     */
    public synchronized boolean disable() {
        if (enable) {
            closeSocket();
            return !enable;
        } else {
            return false;
        }
    }
    
    /**
     * このInet4Subnetを有効にする。
     * @return 無効から有効に変更した場合はtrue、そうでなければfalse
     * @throws SubnetException 有効にするのに失敗した場合
     */
    public synchronized boolean enable() throws SubnetException {
        if (enable) {
            return false;
        } else {
            initSocket();
            return enable;
        }
    }
    
    /**
     * このInet4Subnetのサブネットにフレームを転送する。
     * フレームの送信ノードや受信ノードがこのInet4Subnetに含まれない場合には例外が発生する。
     * @param frame 送信するフレーム
     * @return 常にtrue
     * @throws SubnetException 送信に失敗した場合
     */
    @Override
    public boolean send(Frame frame) throws SubnetException {
        if (!enable) {
            throw new SubnetException("not enabled");
        }

        CommonFrame cf = frame.getCommonFrame();
        byte[] data = cf.toBytes();

        if (!frame.getSender().isMemberOf(this)) {
            throw new SubnetException("invalid sender");
        }

        if (!frame.getReceiver().isMemberOf(this)) {
            throw new SubnetException("invalid receiver");
        }

        try {
            Inet4Node node = (Inet4Node) frame.getReceiver();
            Inet4Address addr = node.getAddress();
            int port = node.getPort();
            DatagramPacket packet = new DatagramPacket(data, data.length, addr, port);
            
            multicastSocket.send(packet);

            return true;
        } catch (IOException e) {
            throw new SubnetException("catched exception", e);
        }
    }
    
    /**
     * このInet4Subnetのサブネットからフレームを受信する。
     * 受信を行うまで待機する。
     * @return 受信したFrame
     * @throws SubnetException 無効なフレームを受信、あるいは受信に失敗した場合
     */
    @Override
    public Frame recv()  throws SubnetException {
        if (!enable) {
            throw new SubnetException("not enabled");
        }
        
        try {
            byte[] packetData = new byte[this.bufferSize];

            DatagramPacket packet = new DatagramPacket(packetData, packetData.length);
            multicastSocket.receive(packet);
            int len = packet.getLength();
            byte[] data = new byte[len];
            System.arraycopy(packetData, 0, data, 0, len);

            CommonFrame cf = new CommonFrame(data);
            Inet4Address addr = (Inet4Address)packet.getAddress();
            int port = packet.getPort();

            Node node = getRemoteNode(addr, port);
            Frame frame = new Frame(node, getLocalNode(), cf);
            return frame;
        } catch (IOException e) {
            throw new SubnetException("catched exception", e);
        } catch (InvalidDataException e) {
            throw new SubnetException("invalid frame", e);
        }
    }
    
    /**
     * リモートノードを表すNodeを生成する。
     * @param addr リモートノードのIPv4アドレス
     * @param port リモートノードのポート番号
     * @return リモートノードのNode
     */
    public Node getRemoteNode(Inet4Address addr, int port) {
        return new Inet4Node(this, addr, port);
    }
    
    /**
     * リモートノードを表すNodeを生成する。
     * @param addr リモートノードのIPv4アドレス
     * @return リモートノードのNode
     */
    public Node getRemoteNode(Inet4Address addr) {
        return new Inet4Node(this, addr, DEFAULT_PORT);
    }
    
    /**
     * ローカルノードを表すNodeを返す。
     * @return ローカルノードのNode
     */
    @Override
    public synchronized Node getLocalNode() {
        if (localNode == null) {
            localNode = new Inet4Node(this, localAddress, DEFAULT_PORT);
        }
        return localNode;
    }
    
    /**
     * グループを表すNodeを返す。
     * このノード宛にフレームを転送するとマルチキャスト転送になる。
     * @return グループのNode
     */
    @Override
    public synchronized Node getGroupNode() {
        if (groupNode == null) {
            groupNode = new Inet4Node(this, groupAddress, DEFAULT_PORT);
        }
        return groupNode;
    }
}
