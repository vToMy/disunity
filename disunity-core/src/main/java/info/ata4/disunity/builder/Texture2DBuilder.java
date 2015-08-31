package info.ata4.disunity.builder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import info.ata4.disunity.extract.Texture2DExtractor;
import info.ata4.io.buffer.ByteBufferUtils;
import info.ata4.log.LogUtils;
import info.ata4.unity.engine.texture2d.DDSHeader;
import info.ata4.unity.engine.texture2d.KTXHeader;
import info.ata4.unity.engine.texture2d.TGAHeader;
import info.ata4.unity.engine.texture2d.Texture2D;
import info.ata4.unity.engine.texture2d.TextureFormat;
import info.ata4.unity.rtti.FieldNode;
import info.ata4.unity.rtti.ObjectData;
import info.ata4.unity.rtti.ObjectSerializer;

public class Texture2DBuilder extends AbstractAssetBuilder<Texture2DExtractor> {

	private static final Logger L = LogUtils.getLogger();
	
	public Texture2DBuilder(Path inputDirectory, ObjectSerializer serializer) {
		super(new Texture2DExtractor(), inputDirectory, serializer);
	}

	@Override
	public int serialize(ObjectData objectData, ObjectSerializer serializer) throws IOException {
		FieldNode instance = objectData.instance();
		Texture2D texture2DAsset = new Texture2D(instance);
		
		String extension = getTextureExtension(texture2DAsset);
		String fileName = objectData.name() + "." + extension;
		
		ByteBuffer originalBuffer = texture2DAsset.getImageBuffer();
		if (ByteBufferUtils.isEmpty(originalBuffer)) {
			L.log(Level.WARNING, "{0} is empty.", fileName);
			return 0;
		}
		
		ByteBuffer byteBuffer = null;
		if (extension == "tga") {
			byteBuffer = getTGAData(texture2DAsset);
		} else if (extension == "dds") {
			byteBuffer = getDDSData(texture2DAsset);
		} else if (extension == "ktx") {
			byteBuffer = getKTXData(texture2DAsset);
		} else {
			throw new IOException(String.format("Unsupported texture format: %s.", extension));
		}
		if (byteBuffer == null) {
			return 0;
		}
		byteBuffer.rewind();
		L.log(Level.INFO, "Repacking: {0}.", fileName);
		
		int delta = byteBuffer.limit() - originalBuffer.limit();
		instance.setSInt32("m_CompleteImageSize", byteBuffer.limit());
		texture2DAsset.setImageBuffer(byteBuffer);
		
		serializer.serialize(objectData, delta);
		
		return delta;
	}
	
	private ByteBuffer getTGAData(Texture2D tex) throws IOException {
        int mipMapCount = 1;
		
        if (tex.isMipMap()) {
            mipMapCount = getMipMapCount(tex.getWidth(), tex.getHeight());
        }
		
        List<ByteBuffer> byteBuffers = new ArrayList<>();
        int totalSize = 0;
		for (int i = 0; i < tex.getImageCount(); i++) {
			for (int j = 0; j < mipMapCount; j++) {
				if (assetExtractor.isTargaSaveMipMaps() || j == 0) {
					
					String fileName = tex.getName();
                    if (tex.getImageCount() > 1) {
                        fileName += "_" + i;
                    }
                    if (tex.isMipMap() && assetExtractor.isTargaSaveMipMaps()) {
                        fileName += "_mip_" + j;
                    }
                    fileName += "." + getTextureExtension(tex);
					Path filePath = inputDirectory.resolve(fileName);
					if (!filePath.toFile().exists()) {
						L.log(Level.WARNING, "{0} doesn't exist. Skipping.", fileName);
						return null;
					}
					ByteBuffer data = ByteBufferUtils.getSlice(ByteBuffer.wrap(Files.readAllBytes(filePath)), TGAHeader.SIZE);
					byteBuffers.add(data);
					totalSize += data.limit();
				}
			}
		}
		
		ByteBuffer buffer = ByteBuffer.allocate(totalSize);
		for (ByteBuffer data : byteBuffers) {
			buffer.put(data);
		}
		
		assert !buffer.hasRemaining();
		
		buffer = convertToOriginalFormat(buffer, tex.getTextureFormat());
				
		return buffer;
	}
	
	private ByteBuffer getKTXData(Texture2D tex) throws IOException {
		int mipMapCount = 1;
		
        if (tex.isMipMap()) {
            mipMapCount = getMipMapCount(tex.getWidth(), tex.getHeight());
        }
        
        String fileName = tex.getName() + "." + getTextureExtension(tex);
        Path filePath = inputDirectory.resolve(fileName);
		if (!filePath.toFile().exists()) {
			L.log(Level.WARNING, "{0} doesn't exist. Skipping.", fileName);
			return null;
		}
        ByteBuffer data = ByteBuffer.wrap(Files.readAllBytes(filePath));
        ByteBuffer buffer = ByteBuffer.allocate(data.limit() - KTXHeader.SIZE - mipMapCount * Integer.BYTES);
        
        int mipMapWidth = tex.getWidth();
        int mipMapHeight = tex.getHeight();
        int mipMapOffset = KTXHeader.SIZE;
        for (int i = 0; i < mipMapCount; i++) {
        	mipMapOffset += Integer.BYTES;
        	int mipMapSize = (mipMapWidth * mipMapHeight * getBPP(tex)) / 8;
        	buffer.put(ByteBufferUtils.getSlice(data, mipMapOffset, mipMapSize));
        	
        	mipMapWidth /= 2;
            mipMapHeight /= 2;
            mipMapOffset += mipMapSize;
        }
        
        return buffer;
	}
	
	private ByteBuffer getDDSData(Texture2D tex) throws IOException {
		String fileName = tex.getName() + "." + getTextureExtension(tex);
		Path filePath = inputDirectory.resolve(fileName);
		if (!filePath.toFile().exists()) {
			L.log(Level.WARNING, "{0} doesn't exist. Skipping.", fileName);
			return null;
		}
		ByteBuffer data = ByteBufferUtils.getSlice(ByteBuffer.wrap(Files.readAllBytes(filePath)),DDSHeader.SIZE);
		return data;
	}
	
    private int getMipMapCount(int width, int height) {
        int mipMapCount = 1;
        for (int dim = Math.max(width, height); dim > 1; dim /= 2) {
            mipMapCount++;
        }
        return mipMapCount;
    }
	
	private String getTextureExtension(Texture2D tex) throws IOException {
		switch (tex.getTextureFormat()) {
        case Alpha8:
        case RGB24:
        case RGBA32:
        case BGRA32:
        case ARGB32:
        case ARGB4444:
        case RGBA4444:
        case RGB565:
            return "tga";
        
        case PVRTC_RGB2:
        case PVRTC_RGBA2:
        case PVRTC_RGB4:
        case PVRTC_RGBA4:
        case ATC_RGB4:
        case ATC_RGBA8:
        case ETC_RGB4:
        case ETC2_RGB4:
        case ETC2_RGB4_PUNCHTHROUGH_ALPHA:
        case ETC2_RGBA8:
        case EAC_R:
        case EAC_R_SIGNED:
        case EAC_RG:
        case EAC_RG_SIGNED:
            return "ktx";

        case DXT1:
        case DXT5:
            return "dds";
            
        default:
            L.log(Level.WARNING, "Texture2D {0}: Unsupported texture format {1}",
                    new Object[] {tex.getName(), tex.getTextureFormat()});
            throw new IOException(String.format("Texture2D {0}: Unsupported texture format {1}",
                    new Object[] {tex.getName(), tex.getTextureFormat()}));
		}
	}
	
	private int getBPP(Texture2D tex) {
		int bpp;
        
        switch (tex.getTextureFormat()) {
            case PVRTC_RGB2:
                bpp = 2;
                break;
                
            case PVRTC_RGBA2:
                bpp = 2;
                break;

            case PVRTC_RGB4:
                bpp = 4;
                break;
                
            case PVRTC_RGBA4:
                bpp = 4;
                break;
                
            case ATC_RGB4:
                bpp = 4;
                break;

            case ATC_RGBA8:
                bpp = 8;
                break;
                
            case ETC_RGB4:
                bpp = 4;
                break;
                
            case ETC2_RGB4:
                bpp = 4;
                break;
                
            case ETC2_RGB4_PUNCHTHROUGH_ALPHA:
                bpp = 4;
                break;
                
            case ETC2_RGBA8:
                bpp = 8;
                break;
         
            case EAC_R:
                bpp = 4;
                break;
                
            case EAC_R_SIGNED:
                bpp = 4;
                break;
                
            case EAC_RG:
                bpp = 8;
                break;
                
            case EAC_RG_SIGNED:
                bpp = 4;
                break;
                
            default:
                throw new IllegalStateException("Invalid texture format for KTX: " + tex.getTextureFormat());
        }
        return bpp;
	}
	
    private ByteBuffer convertToOriginalFormat(ByteBuffer imageBuffer, TextureFormat tf) {
        imageBuffer.rewind();
        L.log(Level.FINE, "Converting {0} to {1}.", new Object[] {TextureFormat.BGRA32, tf});
        if (tf == TextureFormat.RGBA32 || tf == TextureFormat.ARGB32) {
            // convert BGRA to ARGB and RGBA directly by swapping the bytes 
            byte[] pixelOld = new byte[4];
            byte[] pixelNew = new byte[4];
            for (int i = 0; i < imageBuffer.capacity() / 4; i++) {
                imageBuffer.mark();
                imageBuffer.get(pixelOld);
                
                if (tf == TextureFormat.ARGB32) {
                    // BGRA -> ARGB
                    pixelNew[0] = pixelOld[3];
                    pixelNew[1] = pixelOld[2];
                    pixelNew[2] = pixelOld[1];
                    pixelNew[3] = pixelOld[0];
                } else {
                    // BGRA -> RGBA 
                    pixelNew[0] = pixelOld[2];
                    pixelNew[1] = pixelOld[1];
                    pixelNew[2] = pixelOld[0];
                    pixelNew[3] = pixelOld[3];
                }
                
                imageBuffer.reset();
                imageBuffer.put(pixelNew);
            }

            imageBuffer.rewind();
        } else if (tf == TextureFormat.RGB24) {
            // convert BGR directly to RGB
            byte[] pixelOld = new byte[3];
            byte[] pixelNew = new byte[3];
            for (int i = 0; i < imageBuffer.capacity() / 3; i++) {
                imageBuffer.mark();
                imageBuffer.get(pixelOld);
                
                pixelNew[0] = pixelOld[2];
                pixelNew[1] = pixelOld[1];
                pixelNew[2] = pixelOld[0];
                
                imageBuffer.reset();
                imageBuffer.put(pixelNew);
            }

            imageBuffer.rewind();
        } else if (tf == TextureFormat.ARGB4444 || tf == TextureFormat.RGBA4444) {
            // convert 32 bit BGRA to 16 bit RGBA/ARGB
            int newImageSize = imageBuffer.capacity() / 2;
            ByteBuffer imageBufferNew = ByteBuffer.allocateDirect(newImageSize);
            
            byte[] pixelOld = new byte[4];
            byte[] pixelNew = new byte[4];
            for (int i = 0; i < imageBuffer.capacity() / 4; i++) {
            	imageBuffer.get(pixelOld);
            	
            	if (tf == TextureFormat.ARGB4444) {
                    // BGRA -> ARBG
                    pixelNew[0] = pixelOld[3];
                    pixelNew[1] = pixelOld[2];
                    pixelNew[2] = pixelOld[1];
                    pixelNew[3] = pixelOld[0];
                } else {
                    // BGRA -> RBGA 
                    pixelNew[0] = pixelOld[2];
                    pixelNew[1] = pixelOld[1];
                    pixelNew[2] = pixelOld[0];
                    pixelNew[3] = pixelOld[3];
                }
            	
            	// convert range
            	pixelNew[0] >>= 4;
            	pixelNew[1] >>= 4;
            	pixelNew[2] >>= 4;
            	pixelNew[3] >>= 4;
            	
                short pixelNewShort = (short) (((short)pixelNew[0] << 12) & 0xf000);
                pixelNewShort |= (short) (((short)pixelNew[1] << 8) & 0x0f00);
                pixelNewShort |= (short) (((short)pixelNew[2] << 4) & 0x00f0);
                pixelNewShort |= (short) ((short)pixelNew[3] & 0x000f);
                
                imageBufferNew.putShort(pixelNewShort);
            }
            
            assert !imageBuffer.hasRemaining();
            assert !imageBufferNew.hasRemaining();
            
            imageBufferNew.rewind();
            imageBuffer = imageBufferNew;
        } else if (tf == TextureFormat.RGB565) {
            // convert 24 bit to 16 bit RGB
            int newImageSize = (imageBuffer.capacity() / 3) * 2;
            ByteBuffer imageBufferNew = ByteBuffer.allocateDirect(newImageSize);
            
            byte[] pixel = new byte[3];
            for (int i = 0; i < imageBuffer.capacity() / 2; i++) {
            	imageBuffer.get(pixel);
            	
            	// fix color mapping (http://stackoverflow.com/a/9069480)
            	pixel[0] = (byte) ((pixel[0] * 249 + 1014 ) >> 11);
            	pixel[1] = (byte) ((pixel[1] * 253 +  505 ) >> 10);
                pixel[2] = (byte) ((pixel[2] * 249 + 1014 ) >> 11);

                short pixelNew = (short) (((short)pixel[0] << 11) & 0xf800);
                pixelNew |= (short) (((short)pixel[1] << 5) & 0x07e0);
                pixelNew |= (short) ((short)pixel[2] & 0x001f);
                
                imageBufferNew.putShort(pixelNew);
            }
            
            assert !imageBuffer.hasRemaining();
            assert !imageBufferNew.hasRemaining();
            
            imageBufferNew.rewind();
            imageBuffer = imageBufferNew;
        }
        
        return imageBuffer;
    }
}
