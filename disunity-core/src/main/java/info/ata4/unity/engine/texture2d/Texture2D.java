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
    
    public static final String WIDTH_FIELD_NAME = "m_Width";
    public Integer getWidth() {
        return object.getSInt32(WIDTH_FIELD_NAME);
    }
    
    public void setWidth(Integer width) {
    	object.setSInt32(WIDTH_FIELD_NAME, width);
    }
    
    public static final String HEIGHT_FIELD_NAME = "m_Height";
    public Integer getHeight() {
        return object.getSInt32(HEIGHT_FIELD_NAME);
    }
    
    public void setHeight(Integer height) {
    	object.setSInt32(HEIGHT_FIELD_NAME, height);
    }
    
    public long getCompleteImageSize() {
        return object.getUInt32("m_CompleteImageSize");
    }
    
    public static final String TEXTURE_FORMAT_FIELD_NAME = "m_TextureFormat";
    public int getTextureFormatOrd() {
    	return object.getSInt32(TEXTURE_FORMAT_FIELD_NAME);
    }
    
    private void setTextureFormatOrd(int textureFormatOrd) {
    	object.setSInt32(TEXTURE_FORMAT_FIELD_NAME, textureFormatOrd);
    }
    
    public TextureFormat getTextureFormat() {
    	return TextureFormat.fromOrdinal(getTextureFormatOrd());
    }
    
    public void setTextureFormat(TextureFormat textureFormat) {
    	setTextureFormatOrd(textureFormat.ordinal() + 1);
    }

    
    public static final String IS_MIPMAP_FIELD_NAME = "m_MipMap";
    public boolean isMipMap() {
    	return object.getBoolean(IS_MIPMAP_FIELD_NAME);
    }
    public void setIsMipMap(boolean isMipMap) {
    	object.setBoolean(IS_MIPMAP_FIELD_NAME, isMipMap);
    }
   
    public boolean isReadable() {
    	return object.getBoolean("m_IsReadable");
    }
   
    public boolean isReadAllowed() {
    	return object.getBoolean("m_ReadAllowed");
    }
    
    public static final String IMAGE_COUNT_FIELD_NAME = "m_ImageCount";
    public Integer getImageCount() {
    	return object.getSInt32(IMAGE_COUNT_FIELD_NAME);
    }
    
    public void setImageCount(Integer imageCount) {
    	object.setSInt32(IMAGE_COUNT_FIELD_NAME, imageCount);
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
