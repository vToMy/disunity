/*
 ** 2014 November 28
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.unity.rtti;

import info.ata4.io.DataReader;
import info.ata4.io.DataReaders;
import info.ata4.io.DataWriter;
import info.ata4.io.DataWriters;
import info.ata4.io.buffer.ByteBufferUtils;
import info.ata4.unity.asset.Type;
import info.ata4.unity.asset.TypeNode;
import info.ata4.unity.asset.VersionInfo;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class ObjectSerializer {
    
    private static final boolean DEBUG = false;
    private static final int ALIGNMENT = 4;
    
    /** Spare bytes allocated to serialize objects, since padding ruins output size predictability. */
    private int spare;
    
    private ByteBuffer soundData;
    private VersionInfo versionInfo;
    
    public ByteBuffer getSoundData() {
        return soundData;
    }
    
    public void setSoundData(ByteBuffer soundData) {
        this.soundData = soundData;
    }
    
    public void setSpare(int value) {
    	spare = value;
    }
    
    public int getSpare() {
    	return spare;
    }
    
    public void serialize(ObjectData data) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void serialize(ObjectData data, int delta) throws IOException {
        versionInfo = data.versionInfo();
        
        ByteBuffer buff = ByteBuffer.allocate(data.buffer().limit() + delta + spare);
        DataWriter out = DataWriters.forByteBuffer(buff);
        out.order(versionInfo.order());
        out.position(0);
        
        TypeNode typeNode = data.typeTree();
        FieldNode instance = data.instance();
        data.instance(null);
        writeObject(out, typeNode, instance);
        
        buff = ByteBufferUtils.getSlice(buff, 0, buff.position());
        
        // check if all bytes have been written
        //if (out.hasRemaining()) {
            //throw new RuntimeTypeException("Remaining bytes: " + out.remaining());
        //}
        
        data.buffer(buff);
    }
    
    public void deserialize(ObjectData data) throws IOException {
        versionInfo = data.versionInfo();
        
        DataReader in = DataReaders.forByteBuffer(data.buffer());
        in.order(versionInfo.order());
        in.position(0);
        
        TypeNode typeNode = data.typeTree();
        FieldNode instance = readObject(in, typeNode);
        
        // check if all bytes have been read
        if (in.hasRemaining()) {
            throw new RuntimeTypeException("Remaining bytes: " + in.remaining());
        }
        
        data.instance(instance);
    }
    
    private FieldNode readObject(DataReader in, TypeNode typeNode) throws IOException {
        Type type = typeNode.type();
        
        if (DEBUG) {
            System.out.printf("%s0x%x: %s v: %d, f: 0x%x, s: %d\n",
                    StringUtils.repeat("  ", type.treeLevel()), in.position(), type.typeName(),
                    type.version(), type.metaFlag(), type.size());
        }
        
        FieldNode fieldNode = new FieldNode();
        fieldNode.setType(type);
        
        // if the type has no children, it has a primitve value
        if (typeNode.isEmpty() && type.size() > 0) {
            fieldNode.setValue(readPrimitiveValue(in, type, -1));
        }
        
        // read object fields
        for (TypeNode childTypeNode : typeNode) {
            Type childType = childTypeNode.type();
            
            // Check if the current node is an array and if the current field is
            // "data". In that case, "data" needs to be read "size" times.
            if (type.isArray() && childType.fieldName().equals("data")) {
                int size = fieldNode.getSInt32("size");
                
                FieldNode childFieldNode = new FieldNode();
                childFieldNode.setType(childType);

                // if the child type has no children, it has a primitve array
                if (childTypeNode.isEmpty()) {
                    childFieldNode.setValue(readPrimitiveValue(in, childType, size));
                } else {
                    // read list of object nodes
                    List<FieldNode> childFieldNodes = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        childFieldNodes.add(readObject(in, childTypeNode));
                    }
                    childFieldNode.setValue(childFieldNodes);
                }
                
                fieldNode.add(childFieldNode);
            } else {
                fieldNode.add(readObject(in, childTypeNode));
            }
        }
        
        // convert byte buffers to string instances in "string" fields for convenience
        if (fieldNode.getType().typeName().equals("string")) {
            // strings use "char" arrays, so it should be wrapped in a ByteBuffer
            ByteBuffer buf = fieldNode.getArrayData(ByteBuffer.class);
            fieldNode.setValue(new String(buf.array(), "UTF-8"));
        }
        
        return fieldNode;
    }
    
    private Object readPrimitiveValue(DataReader in, Type type, int size) throws IOException, RuntimeTypeException {
        long pos = 0;
        if (DEBUG) {
            pos = in.position();
        }
        
        Object value;
        if (size == -1) {
            value = readPrimitive(in, type);
            if (type.isForceAlign()) {
                 in.align(ALIGNMENT);
            }
        } else {
            value = readPrimitiveArray(in, type, size);
            if (versionInfo.assetVersion() > 5) {
                // arrays always need to be aligned in newer versions
                in.align(ALIGNMENT);
            }
        }
        
        if (DEBUG) {
            long bytes = in.position() - pos;
            System.out.printf("%s0x%x: %s %s = %s, b: %d, v: %d, f: 0x%x, s: %d\n",
                    StringUtils.repeat("  ", type.treeLevel()), pos, type.typeName(), type.fieldName(), value, bytes,
                    type.version(), type.metaFlag(), size);
        }
        
        return value;
    }
    
    private Object readPrimitive(DataReader in, Type type) throws IOException, RuntimeTypeException {
        switch (type.typeName()) {
            // 1 byte
            case "bool":
                return in.readBoolean();

            case "SInt8":
                return in.readByte();

            case "UInt8":
            case "char":
                return in.readUnsignedByte();

            // 2 bytes
            case "SInt16":
            case "short":
                return in.readShort();

            case "UInt16":
            case "unsigned short":
                return in.readUnsignedShort();

            // 4 bytes
            case "SInt32":
            case "int":
                return in.readInt();

            case "UInt32":
            case "unsigned int":
                return in.readUnsignedInt();

            case "float":
                return in.readFloat();

            // 8 bytes
            case "SInt64":
            case "long":
                return in.readLong();

            case "UInt64":
            case "unsigned long":
                return in.readUnsignedLong();

            case "double":
                return in.readDouble();

            default:
                throw new RuntimeTypeException("Unknown primitive type: " + type.typeName());
        }
    }
    
    private Object readPrimitiveArray(DataReader in, Type type, int size) throws IOException, RuntimeTypeException {
        switch (type.typeName()) {
            // read byte arrays natively and wrap them as ByteBuffers,
            // which is much faster and more efficient than a list of wrappped
            // Byte/Integer objects
            case "SInt8":
            case "UInt8":
                ByteBuffer buf;
                
                // NOTE: AudioClips "fake" the size of m_AudioData when the stream is
                // stored in a separate file. The array contains just an offset integer
                // in that case, so pay attention to the bytes remaining in the buffer
                // as well to avoid EOFExceptions.
                long remaining = in.remaining();
                if (size > remaining && remaining == 4) {
                    int offset = in.readInt();
                    // create empty sound buffer in case the .resS file doesn't
                    // exist
                    if (soundData != null) {
                        buf = ByteBufferUtils.getSlice(soundData, offset, size);
                    } else {
                        buf = ByteBufferUtils.allocate(size);
                    }
                } else {
                    buf = ByteBufferUtils.allocate(size);
                    in.readBuffer(buf);
                }

                buf.clear();
                return buf;

            // always wrap char arrays so array() is available on the buffer, which
            // is required to convert them to Java strings in readObject()
            case "char":
                byte[] raw = new byte[size];
                in.readBytes(raw, 0, size);
                return ByteBuffer.wrap(raw);

            // read a list of primitive objects
            default:
                List list = new ArrayList(size);
                for (int i = 0; i < size; i++) {
                    list.add(readPrimitive(in, type));
                }
                return list;
        }
    }
    
    private void writeObject(DataWriter out, TypeNode typeNode, FieldNode fieldNode) throws IOException {
        Type type = typeNode.type();
        
        // if the type has no children, it has a primitve value
        if (typeNode.isEmpty() && type.size() > 0) {
        	writePrimitiveValue(out, type, -1, fieldNode.getValue());
        }
        
        // write object fields
        for (TypeNode childTypeNode : typeNode) {
            Type childType = childTypeNode.type();
            
            // Check if the current node is an array and if the current field is
            // "data". In that case, "data" needs to be written "size" times.
            if (type.isArray() && childType.fieldName().equals("data")) {
                int size = fieldNode.getSInt32("size");
                
                FieldNode childFieldNode = fieldNode.getChild(childType.fieldName());

                // if the child type has no children, it has a primitve array
                if (childTypeNode.isEmpty()) {
                	writePrimitiveValue(out, childType, size, childFieldNode.getValue());
                } else {
                    // write list of object nodes
                    List<FieldNode> childFieldNodes = (List<FieldNode>) childFieldNode.getValue();
                    for (int i = 0; i < size; i++) {
                    	writeObject(out, childTypeNode, childFieldNodes.get(i));
                    }
                }
            } else {
            	writeObject(out, childTypeNode, fieldNode.getChild(childType.fieldName()));
            }
        }
    }
	
	
    private void writePrimitiveValue(DataWriter out, Type type, int size, Object object) throws IOException, RuntimeTypeException {
        if (size == -1) {
        	writePrimitive(out, type, object);
        	if (type.isForceAlign()) {
        		out.align(ALIGNMENT);
        	}
        } else {
        	writePrimitiveArray(out, type, size, object);
        	if (versionInfo.assetVersion() > 5) {
        		out.align(ALIGNMENT);
        	}
        }
    }
	
	private void writePrimitiveArray(DataWriter out, Type type, int size, Object object) throws IOException, RuntimeTypeException {
        switch (type.typeName()) {
            case "SInt8":
            case "UInt8":
                ByteBuffer buf = (ByteBuffer) object;
                
                long remaining = out.remaining();
                if (size > remaining && remaining == 4) {
                    int offset = 0; //that info isn't being store on read.
                    out.writeInt(offset);
                    if (soundData != null) {
                    	soundData.put(buf.array(), offset, size);
                    } else {
                        //Nothing to do.
                    }
                } else {
                	out.writeBuffer(buf);
                }
                break;

            case "char":
            	ByteBuffer buff = (ByteBuffer) object;
            	out.writeBuffer(buff);
            	break;

            default:
            	List list = (List) object;
            	for (int i = 0; i < list.size(); i++) {
            		writePrimitive(out, type, list.get(i));
            	}
            	break;
        }
    }
	
	private void writePrimitive(DataWriter out, Type type, Object object) throws IOException {
		switch (type.typeName()) {
        // 1 byte
        case "bool":
        	out.writeBoolean((Boolean)object);
        	break;

        case "SInt8":
        	out.writeByte((Byte)object);
        	break;

        case "UInt8":
        case "char":
        	out.writeUnsignedByte((Integer)object);
        	break;

        // 2 bytes
        case "SInt16":
        case "short":
        	out.writeShort((Short)object);
        	break;

        case "UInt16":
        case "unsigned short":
        	out.writeUnsignedShort((Integer)object);
        	break;

        // 4 bytes
        case "SInt32":
        case "int":
        	out.writeInt((Integer)object);
        	break;

        case "UInt32":
        case "unsigned int":
			out.writeUnsignedInt((Long)object);
			break;

        case "float":
        	out.writeFloat((Float)object);
        	break;

        // 8 bytes
        case "SInt64":
        case "long":
        	out.writeLong((Long)object);;
        	break;

        case "UInt64":
        case "unsigned long":
        	out.writeUnsignedLong((BigInteger)object);
        	break;

        case "double":
        	out.writeDouble((Double)object);
        	break;

        default:
            throw new RuntimeTypeException("Unknown primitive type: " + type.typeName());
		}
	}
}
