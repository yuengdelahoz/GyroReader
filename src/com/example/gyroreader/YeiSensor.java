package com.example.gyroreader;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class YeiSensor{
	private BluetoothSocket btSocket = null;
	private OutputStream BTOutStream = null;
	private InputStream BTInStream = null;
	private ReentrantLock call_lock;
	
	private static YeiSensor instance;
    
    public class AxisDirectionStruct {
    	public String axis_order = "XYZ";
    	public boolean neg_x = false;
    	public boolean neg_y = false;
    	public boolean neg_z = false;
    }
	
	protected YeiSensor() throws Exception{
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		UUID MY_UUID =
	            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
		String server_mac = null;

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		Log.e("YEI", "aojogjanosfpg");

        // If there are paired devices, set the device mac address string as needed
        //(for now we assume the only paired device is the 3-Space sensor)
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
            	Log.e("pairedDevices", device.getName());
            	if(device.getName().contains("YEI_3SpaceBT"))
            	{
            		server_mac = device.getAddress();
            		break;
            	}
            }
        }
        
        if (server_mac != null)
        {
        	
        	//Get a reference to the remote device
        	BluetoothDevice remote_device = mBluetoothAdapter.getRemoteDevice(server_mac);
        	//Create a socket
            btSocket = remote_device.createRfcommSocketToServiceRecord(MY_UUID);
        	//Stop discovery if it is enabled
        	mBluetoothAdapter.cancelDiscovery();
        	//Try to connect to the remote device.
            btSocket.connect();
        	//Now lets create the in/out streams
            BTOutStream = btSocket.getOutputStream();
        	BTInStream = btSocket.getInputStream();
        	call_lock = new ReentrantLock();
        }
        else
        {
        	throw new Exception();
        }
	}
	
	public static YeiSensor getInstance() throws Exception
	{
		if(instance == null)
		{
			instance = new YeiSensor();
		}
		return instance;
	}
        
    public byte createChecksum(byte[] data)
    {
    	byte checksum = 0;
    	for(int i = 0; i < data.length; i++)
    	{
    		checksum += data[i] % 256;
    	}
    	return checksum;
    }
	
	public void write(byte[] data)
    {
        byte[] msgBuffer = new byte[data.length + 2];
        System.arraycopy(data, 0, msgBuffer, 1, data.length);
        msgBuffer[0] = (byte)0xf7;
        msgBuffer[data.length + 1] = createChecksum(data);
        try {
        		BTOutStream.write(msgBuffer);
        		BTOutStream.flush();
        }
        catch (IOException e) {
        }
    }
    
    public byte[] read(int amnt)
    {
    	byte[] response = new byte[amnt];
    	int amnt_read = 0;
    	while (amnt_read < amnt)
    	{
	        try {
	        	amnt_read += BTInStream.read(response, amnt_read, amnt - amnt_read);
	        }
	        catch (IOException e) {
	        }
    	}
        return response;
    }
    
    public void close()
    {
    	//We are done, so lets close the connection
    	try {
    		btSocket.close();
    	}
    	catch (IOException e) {
    	}
        instance = null;
    }

    public float[] binToFloat(byte[] b)
    {
    	if (b.length % 4 != 0)
    	{
    		return new float[0];
    	}
    	float[] return_array = new float[b.length / 4];
    	for (int i = 0; i < b.length; i += 4)
    	{
    		//We account for endieness here
		    int asInt = (b[i + 3] & 0xFF) 
		            | ((b[i + 2] & 0xFF) << 8) 
		            | ((b[i + 1] & 0xFF) << 16) 
		            | ((b[i] & 0xFF) << 24);
	
		    return_array[i / 4] = Float.intBitsToFloat(asInt);
    	}
    	return return_array;
    }
    
    public int[] binToInt(byte[] b)
    {
    	if (b.length % 4 != 0)
    	{
    		return new int[0];
    	}
    	int[] return_array = new int[b.length / 4];
    	for (int i = 0; i < b.length; i += 4)
    	{
    		//We account for endieness here
    		return_array[i / 4] = (b[i + 3] & 0xFF) 
		            | ((b[i + 2] & 0xFF) << 8) 
		            | ((b[i + 1] & 0xFF) << 16) 
		            | ((b[i] & 0xFF) << 24);
	
    	}
    	return return_array;
    }
    
    public short[] binToShort(byte[] b)
    {
    	if (b.length % 2 != 0)
    	{
    		return new short[0];
    	}
    	short[] return_array = new short[b.length / 2];
    	for (int i = 0; i < b.length; i += 2)
    	{
    		//We account for endieness here
    		return_array[i / 2] = (short)((b[i + 1] & 0xFF) 
		            | ((b[i] & 0xFF) << 8));
	
    	}
    	return return_array;
    }
    
    public byte[] floatToBin(float[] f)
    {
    	ByteBuffer byteBuf = ByteBuffer.allocate(4 * f.length);
    	FloatBuffer floatBuf = byteBuf.asFloatBuffer();
    	floatBuf.put(f);
    	return byteBuf.array();
    }

    public void setLEDColor(float red, float green, float blue)
    {
    	call_lock.lock();
    	byte[] float_data = floatToBin(new float[]{red,green,blue});
    	byte[] send_data = new byte[]{(byte)0xee, float_data[0], float_data[1], float_data[2], float_data[3],
    											  float_data[4], float_data[5], float_data[6], float_data[7],
    											  float_data[8], float_data[9], float_data[10], float_data[11]};
    	write(send_data);
    	call_lock.unlock();
    }
    
    public float[] getLEDColor()
    {
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0xef};
    	write(send_data);
    	byte[] float_data = read(12);
    	call_lock.unlock();
    	return binToFloat(float_data);
    }

    public float[] getFiltTaredEulerAngles() {
        call_lock.lock();
        byte[] send_data = new byte[]{(byte)0x01};
        write(send_data);
        byte[] response = read(12);
        call_lock.unlock();
        return binToFloat(response);
    }

    public float[] getFiltTaredOrientMat()
    {
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0x00};
    	write(send_data);
    	byte[] response = read(16);
    	call_lock.unlock();
    	return binToFloat(response);
    }
    
    public void setTareCurrentOrient()
    {
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0x60};
    	write(send_data);
    	call_lock.unlock();
    }
    
    public void setAxisDirections(String axis_order, boolean neg_x, boolean neg_y, boolean neg_z)
    {
    	byte val = 0;
    	if(axis_order.compareTo("XYZ") == 0)
    	{
    		val = 0x0;
    	}
    	else if(axis_order.compareTo("XZY") == 0)
    	{
    		val = 0x1;
    	}
    	else if(axis_order.compareTo("YXZ") == 0)
    	{
    		val = 0x2;
    	}
    	else if(axis_order.compareTo("YZX") == 0)
    	{
    		val = 0x3;
    	}
    	else if(axis_order.compareTo("ZXY") == 0)
    	{
    		val = 0x4;
    	}
    	else if (axis_order.compareTo("ZYX") == 0)
    	{
    		val = 0x5;
    	}
    	else
    	{
    		return;
    	}
    	if (neg_x)
    	{
    		val = (byte)(val | 0x20);
    	}
    	if (neg_y)
    	{
    		val = (byte)(val | 0x10);
    	}
    	if (neg_z)
    	{
    		val = (byte)(val | 0x8);
    	}
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0x74, val};
    	write(send_data);
    	call_lock.unlock();
    }
    
    public AxisDirectionStruct getAxisDirections()
    {
    	AxisDirectionStruct axis_dir = new AxisDirectionStruct();
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0x8f};
    	write(send_data);
    	byte[] response = read(1);
    	call_lock.unlock();
    	//Determine the axis order
    	int axis_order_num = response[0] & 7;
    	if(axis_order_num == 0)
    	{
    		axis_dir.axis_order = "XYZ";
    	}
    	else if(axis_order_num == 1)
    	{
    		axis_dir.axis_order = "XZY";
    	}
    	else if(axis_order_num == 2)
    	{
    		axis_dir.axis_order = "YXZ";
    	}
    	else if(axis_order_num == 3)
    	{
    		axis_dir.axis_order = "YZX";
    	}
    	else if(axis_order_num == 4)
    	{
    		axis_dir.axis_order = "ZXY";
    	}
    	else if(axis_order_num == 5)
    	{
    		axis_dir.axis_order = "ZYX";
    	}
    	//Determine if any axes are negated
    	if((response[0] & 0x20) > 0)
    	{
    		axis_dir.neg_x = true;
    	}
    	if((response[0] & 0x10) > 0)
    	{
    		axis_dir.neg_y = true;
    	}
    	if((response[0] & 0x8) > 0)
    	{
    		axis_dir.neg_z = true;
    	}
    	return axis_dir;
    }
    
    public String getSoftwareVersion()
    {
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0xdf};
    	write(send_data);
    	byte[] response = read(12);
    	call_lock.unlock();
    	return new String(response);
    }
    
    public String getHardwareVersion()
    {
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0xe6};
    	write(send_data);
    	byte[] response = read(32);
    	call_lock.unlock();
    	return new String(response);
    }
    
    public int getSerialNumber()
    {
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0xed};
    	write(send_data);
    	byte[] response = read(4);
    	call_lock.unlock();
    	return binToInt(response)[0];
    }
    
    public void getButtonState()
    {
    	call_lock.lock();
    	//I also have to figure out how I want to
    	//do this function
    	call_lock.unlock();
    }
    
    public void setLEDMode(int mode)
    {
    	call_lock.lock();
    	byte[] send_data =new byte[]{(byte)0xc4, (byte)mode};
    	write(send_data);
    	call_lock.unlock();
    }
    
    public int getBatteryStatus()
    {
    	call_lock.lock();
    	byte[] send_data =new byte[]{(byte)0xcb};
    	write(send_data);
    	byte[] response = read(1);
    	call_lock.unlock();
    	return (int)response[0];
    }
    
    public int getBatteryLife()
    {
    	call_lock.lock();
    	byte[] send_data =new byte[]{(byte)0xca};
    	write(send_data);
    	byte[] response = read(2);
    	call_lock.unlock();
    	return (int)binToShort(response)[0];
    }

    public float[] getFiltTaredOrientQuat()
    {
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0x00};
    	write(send_data);
    	byte[] response = read(16);
    	call_lock.unlock();
    	return binToFloat(response);
    }
}
