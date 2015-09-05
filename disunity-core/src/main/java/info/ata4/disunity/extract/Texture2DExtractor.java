/*
 ** 2013 June 16
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.disunity.extract;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

import info.ata4.io.DataWriter;
import info.ata4.io.DataWriters;
import info.ata4.io.buffer.ByteBufferUtils;
import info.ata4.log.LogUtils;
import info.ata4.unity.engine.texture2d.DDSHeader;
import info.ata4.unity.engine.texture2d.DDSPixelFormat;
import info.ata4.unity.engine.texture2d.KTXHeader;
import info.ata4.unity.engine.texture2d.TGAHeader;
import info.ata4.unity.engine.texture2d.Texture2D;
import info.ata4.unity.engine.texture2d.TextureFormat;
import info.ata4.unity.rtti.ObjectData;
import info.ata4.unity.util.UnityClass;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class Texture2DExtractor extends AbstractAssetExtractor {
    
    private static final Logger L = LogUtils.getLogger();
    
    private Texture2D tex;
    private boolean tgaSaveMipMaps = true;
    
    public boolean isTargaSaveMipMaps() {
        return tgaSaveMipMaps;
    }

    public void setTargaSaveMipMaps(boolean tgaSaveMipMaps) {
        this.tgaSaveMipMaps = tgaSaveMipMaps;
    }
    
    @Override
    public UnityClass getUnityClass() {
        return new UnityClass("Texture2D");
    }
    
    @Override
    public void extract(ObjectData objectData) throws IOException {
        try {
            // create Texture2D from serialized object
            tex = new Texture2D(objectData.instance());
        } catch (RuntimeException ex) {
            L.log(Level.WARNING, "Deserialization error", ex);
            return;
        }
        
        if (tex.getTextureFormat() == null) {
            L.log(Level.WARNING, "Texture2D {0}: Unknown texture format {1}",
                    new Object[] {tex.getName(), tex.getTextureFormatOrd()});
            return;
        }

        // some textures (font textures?) don't have any image data, not sure why...
        if (tex.getImageBuffer().capacity() == 0) {
            L.log(Level.WARNING, "Texture2D {0}: Empty image buffer", tex.getName());
            return;
        }
        
        // choose a fitting container format
        switch (tex.getTextureFormat()) {
            case Alpha8:
            case RGB24:
            case RGBA32:
            case BGRA32:
            case ARGB32:
            case ARGB4444:
            case RGBA4444:
            case RGB565:
                extractTGA();
                break;
            
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
                extractKTX();
                break;

            case DXT1:
            case DXT5:
                extractDDS();
                break;
                
            default:
                L.log(Level.WARNING, "Texture2D {0}: Unsupported texture format {1}",
                        new Object[] {tex.getName(), tex.getTextureFormat()});
        }
    }
    
    public static int getMipMapCount(int width, int height) {
        int mipMapCount = 1;
        for (int dim = Math.max(width, height); dim > 1; dim /= 2) {
            mipMapCount++;
        }
        return mipMapCount;
    }
    
    private void extractDDS() throws IOException {
        DDSHeader header = new DDSHeader();
        header.dwWidth = tex.getWidth();
        header.dwHeight = tex.getHeight();

        switch (tex.getTextureFormat()) {
            case Alpha8:
                header.ddspf.dwFlags = DDSPixelFormat.DDPF_ALPHA;
                header.ddspf.dwABitMask = 0xff;
                header.ddspf.dwRGBBitCount = 8;
                break;
                
            case RGB24:
                header.ddspf.dwFlags = DDSPixelFormat.DDPF_RGB;
                header.ddspf.dwRBitMask = 0xff0000;
                header.ddspf.dwGBitMask = 0x00ff00;
                header.ddspf.dwBBitMask = 0x0000ff;
                header.ddspf.dwRGBBitCount = 24;
                break;
                
            case RGBA32:
                header.ddspf.dwFlags = DDSPixelFormat.DDPF_RGBA;
                header.ddspf.dwRBitMask = 0x000000ff;
                header.ddspf.dwGBitMask = 0x0000ff00;
                header.ddspf.dwBBitMask = 0x00ff0000;
                header.ddspf.dwABitMask = 0xff000000;
                header.ddspf.dwRGBBitCount = 32;
                break;
                
            case BGRA32:
                header.ddspf.dwFlags = DDSPixelFormat.DDPF_RGBA;
                header.ddspf.dwRBitMask = 0x00ff0000;
                header.ddspf.dwGBitMask = 0x0000ff00;
                header.ddspf.dwBBitMask = 0x000000ff;
                header.ddspf.dwABitMask = 0xff000000;
                header.ddspf.dwRGBBitCount = 32;
                break;
                
            case ARGB32:
                header.ddspf.dwFlags = DDSPixelFormat.DDPF_RGBA;
                header.ddspf.dwRBitMask = 0x0000ff00;
                header.ddspf.dwGBitMask = 0x00ff0000;
                header.ddspf.dwBBitMask = 0xff000000;
                header.ddspf.dwABitMask = 0x000000ff;
                header.ddspf.dwRGBBitCount = 32;
                break;
                    
            case ARGB4444:
                header.ddspf.dwFlags = DDSPixelFormat.DDPF_RGBA;
                header.ddspf.dwRBitMask = 0x0f00;
                header.ddspf.dwGBitMask = 0x00f0;
                header.ddspf.dwBBitMask = 0x000f;
                header.ddspf.dwABitMask = 0xf000;
                header.ddspf.dwRGBBitCount = 16;
                break;
                
            case RGBA4444:
                header.ddspf.dwFlags = DDSPixelFormat.DDPF_RGBA;
                header.ddspf.dwRBitMask = 0xf000;
                header.ddspf.dwGBitMask = 0x0f00;
                header.ddspf.dwBBitMask = 0x00f0;
                header.ddspf.dwABitMask = 0x000f;
                header.ddspf.dwRGBBitCount = 16;
                break;
                
            case RGB565:
                header.ddspf.dwFlags = DDSPixelFormat.DDPF_RGB;
                header.ddspf.dwRBitMask = 0xf800;
                header.ddspf.dwGBitMask = 0x07e0;
                header.ddspf.dwBBitMask = 0x001f;
                header.ddspf.dwRGBBitCount = 16;
                break;
            
            case DXT1:
                header.ddspf.dwFourCC = DDSPixelFormat.PF_DXT1;
                break;
            
            case DXT5:
                header.ddspf.dwFourCC = DDSPixelFormat.PF_DXT5; 
                break;
                
            default:
                throw new IllegalStateException("Invalid texture format for DDS: " + tex.getTextureFormat());
        }

        // set mip map flags if required
        if (tex.isMipMap()) {
            header.dwFlags |= DDSHeader.DDS_HEADER_FLAGS_MIPMAP;
            header.dwCaps |= DDSHeader.DDS_SURFACE_FLAGS_MIPMAP;
            header.dwMipMapCount = getMipMapCount(header.dwWidth, header.dwHeight);
        }
        
        // set and calculate linear size
        header.dwFlags |= DDSHeader.DDS_HEADER_FLAGS_LINEARSIZE;
        if (header.ddspf.dwFourCC != 0) {
            header.dwPitchOrLinearSize = header.dwWidth * header.dwHeight;
            
            if (tex.getTextureFormat() == TextureFormat.DXT1) {
                header.dwPitchOrLinearSize /= 2;
            }
            
            header.ddspf.dwFlags |= DDSPixelFormat.DDPF_FOURCC;
        } else {
            header.dwPitchOrLinearSize = (tex.getWidth() * tex.getHeight() * header.ddspf.dwRGBBitCount) / 8;
        }
        
        ByteBuffer bbTex = ByteBuffer.allocateDirect(DDSHeader.SIZE + tex.getImageBuffer().capacity());
        bbTex.order(ByteOrder.LITTLE_ENDIAN);
        
        // write header
        DataWriter out = DataWriters.forByteBuffer(bbTex);
        header.write(out);
        
        // write data
        bbTex.put(tex.getImageBuffer());
        
        bbTex.rewind();
        
        writeFile(tex.getName(), "dds", bbTex);
    }

    private void extractPKM() throws IOException {
        // texWidth and texHeight are width and height rounded up to multiple of 4.
        int texWidth = ((tex.getWidth() - 1) | 3) + 1;
        int texHeight = ((tex.getHeight() - 1) | 3) + 1;

        ByteBuffer res = ByteBuffer.allocateDirect(16 + tex.getImageBuffer().capacity());
        res.order(ByteOrder.BIG_ENDIAN);

        res.putLong(0x504b4d2031300000L); // PKM 10\0\0

        res.putShort((short) texWidth);
        res.putShort((short) texHeight);
        res.putShort(tex.getWidth().shortValue());
        res.putShort(tex.getHeight().shortValue());

        res.put(tex.getImageBuffer());

        res.rewind();

        writeFile(tex.getName(), "pkm", res);
    }

    private void extractTGA() throws IOException {
        TGAHeader header = new TGAHeader();
        
        switch (tex.getTextureFormat()) {
            case Alpha8:
                header.imageType = 3;
                header.pixelDepth = 8;
                break;

            case RGBA32:
            case ARGB32:
            case BGRA32:
            case RGBA4444:
            case ARGB4444:
                header.imageType = 2;
                header.pixelDepth = 32;
                break;

            case RGB24:
            case RGB565:
                header.imageType = 2;
                header.pixelDepth = 24;
                break;

            default:
                throw new IllegalStateException("Invalid texture format for TGA: " + tex.getTextureFormat());
        }
        
        convertToRGBA32();
        
        ByteBuffer bb = tex.getImageBuffer();

        int mipMapCount = 1;
        
        if (tex.isMipMap()) {
            mipMapCount = getMipMapCount(tex.getWidth(), tex.getHeight());
        }
        
        for (int i = 0; i < tex.getImageCount(); i++) {
            header.imageWidth = tex.getWidth();
            header.imageHeight = tex.getHeight();
            
            for (int j = 0; j < mipMapCount; j++) {
                int imageSize = header.imageWidth * header.imageHeight * header.pixelDepth / 8;
                
                if (tgaSaveMipMaps || j == 0) {
                    ByteBuffer bbTga = ByteBuffer.allocateDirect(TGAHeader.SIZE + imageSize);
                    bbTga.order(ByteOrder.LITTLE_ENDIAN);

                    // write TGA header
                    DataWriter out = DataWriters.forByteBuffer(bbTga);
                    header.write(out);

                    // write image data
                    bb.limit(bb.position() + imageSize);
                    bbTga.put(bb);
                    bb.limit(bb.capacity());

                    assert !bbTga.hasRemaining();

                    // write file
                    bbTga.rewind();

                    String fileName = tex.getName();

                    if (tex.getImageCount() > 1) {
                        fileName += "_" + i;
                    }

                    if (tex.isMipMap() && tgaSaveMipMaps) {
                        fileName += "_mip_" + j;
                    }

                    writeFile(fileName, "tga", bbTga);
                } else {
                    bb.position(bb.position() + imageSize);
                }

                // prepare for the next mip map
                if (header.imageWidth > 1) {
                    header.imageWidth /= 2;
                }
                if (header.imageHeight > 1) {
                    header.imageHeight /= 2;
                }
            }
        }

        assert !bb.hasRemaining();
    }
    
    private void convertToRGBA32() {
        ByteBuffer imageBuffer = tex.getImageBuffer();
        TextureFormat tf = tex.getTextureFormat();
        
        if (tf == TextureFormat.RGBA32 || tf == TextureFormat.ARGB32) {
            // convert ARGB and RGBA directly by swapping the bytes to get BGRA
            byte[] pixelOld = new byte[4];
            byte[] pixelNew = new byte[4];
            for (int i = 0; i < imageBuffer.capacity() / 4; i++) {
                imageBuffer.mark();
                imageBuffer.get(pixelOld);
                
                if (tf == TextureFormat.ARGB32) {
                    // ARGB -> BGRA
                    pixelNew[0] = pixelOld[3];
                    pixelNew[1] = pixelOld[2];
                    pixelNew[2] = pixelOld[1];
                    pixelNew[3] = pixelOld[0];
                } else {
                    // RGBA -> BGRA
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
            // convert RGB directly to BGR
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
            // convert 16 bit RGBA/ARGB to 32 bit BGRA
            int newImageSize = imageBuffer.capacity() * 2;
            ByteBuffer imageBufferNew = ByteBuffer.allocateDirect(newImageSize);
            
            byte[] pixelOld = new byte[4];
            byte[] pixelNew = new byte[4];
            for (int i = 0; i < imageBuffer.capacity() / 2; i++) {
                int pixelOldShort = imageBuffer.getShort();
                
                pixelOld[0] = (byte) ((pixelOldShort & 0xf000) >> 12);
                pixelOld[1] = (byte) ((pixelOldShort & 0x0f00) >> 8);
                pixelOld[2] = (byte) ((pixelOldShort & 0x00f0) >> 4);
                pixelOld[3] = (byte)  (pixelOldShort & 0x000f);
                
                // convert range
                pixelOld[0] <<= 4;
                pixelOld[1] <<= 4;
                pixelOld[2] <<= 4;
                pixelOld[3] <<= 4;
                
                if (tf == TextureFormat.ARGB4444) {
                    // ARBG -> BGRA
                    pixelNew[0] = pixelOld[3];
                    pixelNew[1] = pixelOld[2];
                    pixelNew[2] = pixelOld[1];
                    pixelNew[3] = pixelOld[0];
                } else {
                    // RBGA -> BGRA
                    pixelNew[0] = pixelOld[2];
                    pixelNew[1] = pixelOld[1];
                    pixelNew[2] = pixelOld[0];
                    pixelNew[3] = pixelOld[3];
                }
                
                imageBufferNew.put(pixelNew);
            }
            
            assert !imageBuffer.hasRemaining();
            assert !imageBufferNew.hasRemaining();
            
            imageBufferNew.rewind();
            imageBuffer = imageBufferNew;
        } else if (tf == TextureFormat.RGB565) {
            // convert 16 bit RGB to 24 bit
            int newImageSize = (imageBuffer.capacity() / 2) * 3;
            ByteBuffer imageBufferNew = ByteBuffer.allocateDirect(newImageSize);
            
            byte[] pixel = new byte[3];
            for (int i = 0; i < imageBuffer.capacity() / 2; i++) {
                short pixelOld = imageBuffer.getShort();

                pixel[0] = (byte) ((pixelOld & 0xf800) >> 11);
                pixel[1] = (byte) ((pixelOld & 0x07e0) >> 5);
                pixel[2] = (byte)  (pixelOld & 0x001f);

                // fix color mapping (http://stackoverflow.com/a/9069480)
                pixel[0] = (byte) ((pixel[0] * 527 + 23) >> 6);
                pixel[1] = (byte) ((pixel[1] * 259 + 33) >> 6);
                pixel[2] = (byte) ((pixel[2] * 527 + 23) >> 6);
                
                imageBufferNew.put(pixel);
            }
            
            assert !imageBuffer.hasRemaining();
            assert !imageBufferNew.hasRemaining();
            
            imageBufferNew.rewind();
            imageBuffer = imageBufferNew;
        }
        
        tex.setImageBuffer(imageBuffer);
    }
    
    private void extractKTX() throws IOException {
        KTXHeader header = new KTXHeader();
        header.swap = ByteOrder.LITTLE_ENDIAN;
        header.glTypeSize = 1;
        header.pixelWidth = tex.getWidth();
        header.pixelHeight = tex.getHeight();
        header.pixelDepth = 0;
        header.numberOfFaces = tex.getImageCount();
        header.numberOfMipmapLevels = tex.isMipMap() ? getMipMapCount(header.pixelWidth, header.pixelHeight) : 1;
        int bpp;
        
        switch (tex.getTextureFormat()) {
            case PVRTC_RGB2:
                header.glInternalFormat = KTXHeader.GL_COMPRESSED_RGB_PVRTC_2BPPV1_IMG;
                header.glBaseInternalFormat = KTXHeader.GL_RGB;
                bpp = 2;
                break;
                
            case PVRTC_RGBA2:
                header.glInternalFormat = KTXHeader.GL_COMPRESSED_RGBA_PVRTC_2BPPV1_IMG;
                header.glBaseInternalFormat = KTXHeader.GL_RGBA;
                bpp = 2;
                break;

            case PVRTC_RGB4:
                header.glInternalFormat = KTXHeader.GL_COMPRESSED_RGB_PVRTC_4BPPV1_IMG;
                header.glBaseInternalFormat = KTXHeader.GL_RGB;
                bpp = 4;
                break;
                
            case PVRTC_RGBA4:
                header.glInternalFormat = KTXHeader.GL_COMPRESSED_RGBA_PVRTC_4BPPV1_IMG;
                header.glBaseInternalFormat = KTXHeader.GL_RGBA;
                bpp = 4;
                break;
                
            case ATC_RGB4:
                header.glInternalFormat = KTXHeader.GL_ATC_RGB_AMD;
                header.glBaseInternalFormat = KTXHeader.GL_RGB;
                bpp = 4;
                break;

            case ATC_RGBA8:
                header.glInternalFormat = KTXHeader.GL_ATC_RGBA_EXPLICIT_ALPHA_AMD;
                header.glBaseInternalFormat = KTXHeader.GL_RGBA;
                bpp = 8;
                break;
                
            case ETC_RGB4:
                header.glInternalFormat = KTXHeader.GL_ETC1_RGB8_OES;
                header.glBaseInternalFormat = KTXHeader.GL_RGB;
                bpp = 4;
                break;
                
            case ETC2_RGB4:
                header.glInternalFormat = KTXHeader.GL_COMPRESSED_RGB8_ETC2;
                header.glBaseInternalFormat = KTXHeader.GL_RGB;
                bpp = 4;
                break;
                
            case ETC2_RGB4_PUNCHTHROUGH_ALPHA:
                header.glInternalFormat = KTXHeader.GL_COMPRESSED_RGB8_PUNCHTHROUGH_ALPHA1_ETC2;
                header.glBaseInternalFormat = KTXHeader.GL_RGBA;
                bpp = 4;
                break;
                
            case ETC2_RGBA8:
                header.glInternalFormat = KTXHeader.GL_COMPRESSED_RGBA8_ETC2_EAC;
                header.glBaseInternalFormat = KTXHeader.GL_RGBA;
                bpp = 8;
                break;
         
            case EAC_R:
                header.glInternalFormat = KTXHeader.GL_COMPRESSED_R11_EAC;
                header.glBaseInternalFormat = KTXHeader.GL_RED;
                bpp = 4;
                break;
                
            case EAC_R_SIGNED:
                header.glInternalFormat = KTXHeader.GL_COMPRESSED_SIGNED_R11_EAC;
                header.glBaseInternalFormat = KTXHeader.GL_RED;
                bpp = 4;
                break;
                
            case EAC_RG:
                header.glInternalFormat = KTXHeader.GL_COMPRESSED_RG11_EAC;
                header.glBaseInternalFormat = KTXHeader.GL_RG;
                bpp = 8;
                break;
                
            case EAC_RG_SIGNED:
                header.glInternalFormat = KTXHeader.GL_COMPRESSED_SIGNED_RG11_EAC;
                header.glBaseInternalFormat = KTXHeader.GL_RG;
                bpp = 4;
                break;
                
            default:
                throw new IllegalStateException("Invalid texture format for KTX: " + tex.getTextureFormat());
        }
        
        // header + raw image data + mip map image sizes
        int imageSizeTotal = KTXHeader.SIZE + tex.getImageBuffer().capacity() + header.numberOfMipmapLevels * 4;
        ByteBuffer bb = ByteBuffer.allocateDirect(imageSizeTotal);
        
        // write header
        header.write(DataWriters.forByteBuffer(bb));
        
        int mipMapWidth = header.pixelWidth;
        int mipMapHeight = header.pixelHeight;
        int mipMapOffset = 0;
        for (int i = 0; i < header.numberOfMipmapLevels; i++) {
            // write mip map size
            bb.putInt(mipMapWidth);
            
            // get mip map image data
            int mipMapSize = (mipMapWidth * mipMapHeight * bpp) / 8;
            ByteBuffer mipMapBuffer = ByteBufferUtils.getSlice(tex.getImageBuffer(), mipMapOffset, mipMapSize);

            // write image data
            bb.put(mipMapBuffer);
            
            // prepare next mip map
            mipMapWidth /= 2;
            mipMapHeight /= 2;
            mipMapOffset += mipMapSize;
        }

        // write file
        bb.rewind();

        writeFile(tex.getName(), "ktx", bb);
    }
    
}
