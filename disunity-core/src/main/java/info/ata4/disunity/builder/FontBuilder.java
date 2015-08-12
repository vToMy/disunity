package info.ata4.disunity.builder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import info.ata4.disunity.extract.FontExtractor;
import info.ata4.io.buffer.ByteBufferUtils;
import info.ata4.log.LogUtils;
import info.ata4.unity.engine.Font;
import info.ata4.unity.rtti.FieldNode;
import info.ata4.unity.rtti.ObjectData;
import info.ata4.unity.rtti.ObjectSerializer;

public class FontBuilder extends AbstractAssetBuilder<FontExtractor> {

	private static final Logger L = LogUtils.getLogger();
	
	public FontBuilder(Path inputDirectory, ObjectSerializer serializer) {
		super(new FontExtractor(), inputDirectory, serializer);
	}

	@Override
	public int serialize(ObjectData objectData, ObjectSerializer serializer) throws IOException {
		FieldNode instance = objectData.instance();
		Font font = new Font(instance);
		
		String fileName = font.getName() + ".ttf";
		L.log(Level.INFO, "Repacking: {0}.", fileName);
		
		Path filePath = inputDirectory.resolve(fileName);
		ByteBuffer originalBuffer = font.getFontData();
		if (ByteBufferUtils.isEmpty(originalBuffer)) {
			L.log(Level.WARNING, "{0} is empty.", fileName);
			return 0;
		}
		ByteBuffer byteBuffer = ByteBuffer.wrap(Files.readAllBytes(filePath));
		int delta = byteBuffer.limit() - originalBuffer.limit();
		
		instance.setChildArrayData("m_FontData", byteBuffer);
		serializer.serialize(objectData, delta);
		
		return delta;
	}

}
