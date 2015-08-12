package info.ata4.disunity.builder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import info.ata4.disunity.extract.TextAssetExtractor;
import info.ata4.io.buffer.ByteBufferUtils;
import info.ata4.log.LogUtils;
import info.ata4.unity.engine.TextAsset;
import info.ata4.unity.rtti.FieldNode;
import info.ata4.unity.rtti.ObjectData;
import info.ata4.unity.rtti.ObjectSerializer;

public class TextAssetBuilder extends AbstractAssetBuilder<TextAssetExtractor> {

	private static final Logger L = LogUtils.getLogger();
	
	public TextAssetBuilder(Path inputDirectory, ObjectSerializer serializer) {
		super(new TextAssetExtractor(), inputDirectory, serializer);
	}
	
	protected TextAssetBuilder(TextAssetExtractor extractor, Path inputDirectory, ObjectSerializer serializer) {
		super(extractor, inputDirectory, serializer);
	}
	
	@Override
	public int serialize(ObjectData objectData, ObjectSerializer serializer) throws IOException {
		FieldNode instance = objectData.instance();
		TextAsset textAsset = new TextAsset(instance);
		
		String fileName = textAsset.getName() + "." + getExtension();
		L.log(Level.INFO, "Repacking: {0}.", fileName);
		
		Path filePath = inputDirectory.resolve(fileName);
		ByteBuffer originalBuffer = textAsset.getScriptRaw();
		if (ByteBufferUtils.isEmpty(originalBuffer)) {
			L.log(Level.WARNING, "{0} is empty.", fileName);
			return 0;
		}
		ByteBuffer byteBuffer = ByteBuffer.wrap(Files.readAllBytes(filePath));
		int delta = byteBuffer.limit() - originalBuffer.limit();
		
		instance.setChildArrayData("m_Script", byteBuffer);
		serializer.serialize(objectData, delta);
		
		return delta;
	}
	
	protected String getExtension() {
		return "txt";
	}
}
