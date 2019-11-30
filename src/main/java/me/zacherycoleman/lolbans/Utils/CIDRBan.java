package me.zacherycoleman.lolbans.Utils;

import java.net.InetAddress;
import java.sql.Blob;
import javax.sql.rowset.serial.SerialBlob;

import org.jboss.netty.handler.ipfilter.CIDR;

public class CIDRBan
{
    protected CIDR ipcidr;
    
    public CIDRBan(String CIDRString)
    {
        ipcidr = CIDR.newCIDR(CIDRString);
    }

    public CIDRBan(InetAddress address, int CIDR)
    {
        ipcidr = CIDR.newCIDR(address, CIDR);
    }

    public boolean CheckRange(InetAddress address)
    {
        return this.ipcidr.contains(address);
    }

    public boolean compare(CIDRBan other)
    {
        return other.ipcidr.hashCode() == this.ipcodr.hashCode();
    }

    public Blob GetBlob()
    {
        return new SerialBlob(this.ipcidr.getBaseAddress().getAddress());
    }

    public int GetMask()
    {
        return this.ipcidr.getMask();
    }
}