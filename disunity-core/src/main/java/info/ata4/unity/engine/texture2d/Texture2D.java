/*
 ** 2014 Juli 10
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.unity.engine.texture2d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import info.ata4.unity.engine.UnityObject;
import info.ata4.unity.rtti.FieldNode;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class Texture2D extends UnityObject {
    
    public Texture2D(FieldNode object) {
        super(object);
    }
    
    public Integer getWidth() {
        return object.getSInt32("m_Width");
    }
    
    public Integer getHeight() {
        return object.getSInt32("m_Height");
    }
    
    public long getCompleteImageSize() {
        return object.getUInt32("m_CompleteImageSize");
    }
    
    public int getTextureFormatOrd() {
    	return object.getSInt32("m_TextureFormat");
    }
    
    public TextureFormat getTextureFormat() {
    	return TextureFormat.fromOrdinal(getTextureFormatOrd());
    }
    
    public void setTextureFormat(TextureFormat textureFormat) {
    	object.setSInt32("m_TextureFormat", textureFormat.ordinal()+1);
    }
    
    public boolean isMipMap() {
    	return object.getBoolean("m_MipMap");
    }
   
    public boolean isReadable() {
    	return object.getBoolean("m_IsReadable");
    }
   
    public boolean isReadAllowed() {
    	return object.getBoolean("m_ReadAllowed");
    }
    
    public Integer getImageCount() {
    	return object.getSInt32("m_ImageCount");
    }
    
    public long getDimension() {
    	return object.getUInt32("m_TextureDimension");
    }
   
    public long getLightmapFormat() {
    	return object.getUInt32("m_LightmapFormat");
    }
   
    public long getColorSpace() {
    	return object.getUInt32("m_ColorSpace");
    }
    
    public ByteBuffer getImageBuffer() {
    	ByteBuffer imageBuffer = object.getChildArrayData("image data", ByteBuffer.class);
    	imageBuffer.order(ByteOrder.LITTLE_ENDIAN);
    	return imageBuffer;
    }
    
    public void setImageBuffer(ByteBuffer byteBuffer) {
    	object.setChildArrayData("image data", byteBuffer);
    }
}
