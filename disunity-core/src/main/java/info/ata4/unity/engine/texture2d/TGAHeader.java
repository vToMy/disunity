/*
 ** 2013 December 25
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.unity.engine.texture2d;

import java.io.IOException;

import info.ata4.io.DataReader;
import info.ata4.io.DataWriter;
import info.ata4.io.Struct;

/**
 * Very simple TGA header struct.
 * 
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class TGAHeader implements Struct {
    
    public static final int SIZE = 18;
    
    public byte idLength;
    public byte colorMapType;
    public byte imageType;
    public int cmFirstIndex;
    public int cmLength;
    public byte cmEntrySize;
    public int originX;
    public int originY;
    public int imageWidth;
    public int imageHeight;
    public byte pixelDepth;
    public byte imageDesc;

    @Override
    public void read(DataReader in) throws IOException {
        idLength = in.readByte();
        colorMapType = in.readByte();
        imageType = in.readByte();
        cmFirstIndex = in.readUnsignedShort();
        cmLength = in.readUnsignedShort();
        cmEntrySize = in.readByte();
        originX = in.readUnsignedShort();
        originY = in.readUnsignedShort();
        imageWidth = in.readUnsignedShort();
        imageHeight = in.readUnsignedShort();
        pixelDepth = in.readByte();
        imageDesc = in.readByte();
    }

    @Override
    public void write(DataWriter out) throws IOException {
        out.writeByte(idLength);
        out.writeByte(colorMapType);
        out.writeByte(imageType);
        out.writeUnsignedShort(cmFirstIndex);
        out.writeUnsignedShort(cmLength);
        out.writeByte(cmEntrySize);
        out.writeUnsignedShort(originX);
        out.writeUnsignedShort(originY);
        out.writeUnsignedShort(imageWidth);
        out.writeUnsignedShort(imageHeight);
        out.writeByte(pixelDepth);
        out.writeByte(imageDesc);
    }
    
}
